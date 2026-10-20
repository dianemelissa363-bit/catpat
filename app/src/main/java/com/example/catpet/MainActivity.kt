package com.example.catpet

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        // AI 应用包名与网页地址（包名不符/未安装时自动回退网页）
        private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
        private const val CHATGPT_URL = "https://chatgpt.com/"
        private const val GEMINI_PACKAGE = "com.google.android.apps.bard"
        private const val GEMINI_URL = "https://gemini.google.com/"
        // DSH：本机实际包名（com.dshbox.app 未安装；失败只提示，不跳 GitHub）
        private const val DSH_PACKAGE = "com.dsharnessmobile.shell"

        // 供悬浮猫菜单在缺少相册权限时拉起本页申请权限
        const val EXTRA_REQUEST_MEDIA_PERMISSION = "extra_request_media_permission"
    }

    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var todoListContainer: LinearLayout
    private lateinit var tvTodoEmpty: TextView
    private lateinit var todoTaskStore: TodoTaskStore
    private val uiHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { renderTodoItems() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        todoListContainer = findViewById(R.id.todoListContainer)
        tvTodoEmpty = findViewById(R.id.tvTodoEmpty)
        todoTaskStore = TodoTaskStore(this)

        btnStart.setOnClickListener {
            if (!canDrawOverlays()) {
                requestOverlayPermission()
            } else {
                startPet()
            }
        }

        btnStop.setOnClickListener {
            stopPet()
        }

        // 六个入口：AI 工具箱/GitHub 已实现，其余仍为占位
        findViewById<Button>(R.id.btnTodo).setOnClickListener { openQuickNotes() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener { showComingSoon() }
        findViewById<Button>(R.id.btnAi).setOnClickListener { showAiToolsMenu() }
        findViewById<Button>(R.id.btnNotes).setOnClickListener { openTodoPage() }
        findViewById<Button>(R.id.btnGithub).setOnClickListener { openUrl("https://github.com/") }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showComingSoon() }

        // Android 13+ 需要通知权限，前台服务的常驻通知才可见
        requestNotificationPermissionIfNeeded()

        if (intent?.getBooleanExtra(EXTRA_REQUEST_MEDIA_PERMISSION, false) == true) {
            requestMediaPermissionIfNeeded()
        }
    }

    override fun onResume() {
        super.onResume()
        // 返回首页时，用服务的真实状态刷新（而非本地内存变量）
        refreshStatus()
        renderTodoItems()
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(refreshRunnable)
    }

    private fun refreshStatus() {
        tvStatus.text = when {
            !canDrawOverlays() -> getString(R.string.status_no_permission)  // 没有悬浮窗权限
            FloatingPetService.isRunning -> getString(R.string.status_running)  // 小猫正在运行
            FloatingPetService.everStarted -> getString(R.string.status_idle)  // 小猫已经停止
            else -> getString(R.string.status_ready)  // 已授权且未启动
        }
    }


    private fun renderTodoItems() {
        val now = System.currentTimeMillis()
        val tasks = todoTaskStore.load()
            .filter { !it.done }
            .sortedByDescending { it.updatedAt }

        todoListContainer.removeAllViews()

        if (tasks.isEmpty()) {
            tvTodoEmpty.visibility = View.VISIBLE
            todoListContainer.visibility = View.GONE
            return
        }

        tvTodoEmpty.visibility = View.GONE
        todoListContainer.visibility = View.VISIBLE

        tasks.forEach { task ->
            val overdue = task.dueAt != null && task.dueAt < now
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            row.addView(android.widget.CheckBox(this).apply {
                isChecked = false
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        val updated = task.copy(done = true, updatedAt = System.currentTimeMillis())
                        todoTaskStore.save(todoTaskStore.load().map { if (it.id == task.id) updated else it })
                        renderTodoItems()
                    }
                }
            })
            row.addView(TextView(this).apply {
                text = task.title
                textSize = 14f
                if (overdue) {
                    setTextColor(0xFFCC0000.toInt())
                }
                setPadding(dp(8), dp(4), dp(12), dp(4))
            })
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(4)
            todoListContainer.addView(row, params)
        }
        scheduleNextRefresh(tasks)
    }

    /**
     * Re-renders exactly when the nearest future deadline passes, so the home
     * card turns red in place. Completed/no-due tasks are ignored.
     */
    private fun scheduleNextRefresh(tasks: List<TodoTask>) {
        uiHandler.removeCallbacks(refreshRunnable)
        val now = System.currentTimeMillis()
        val nextDue = tasks
            .filter { !it.done && it.dueAt != null && it.dueAt > now }
            .minOfOrNull { it.dueAt ?: Long.MAX_VALUE }
        if (nextDue != null) {
            val delay = (nextDue - now).coerceAtLeast(1000L) + 1000L
            uiHandler.postDelayed(refreshRunnable, delay)
        }
    }

    private fun openQuickNotes() {
        startActivity(Intent(this, QuickNotesActivity::class.java))
    }

    private fun openTodoPage() {
        startActivity(Intent(this, TodoActivity::class.java))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showComingSoon() {
        Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show()
    }

    private fun showAiToolsMenu() {
        val options = arrayOf(
            getString(R.string.ai_chatgpt),
            getString(R.string.ai_gemini),
            getString(R.string.ai_dshbox)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_tools_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAiTool(CHATGPT_PACKAGE, CHATGPT_URL)
                    1 -> openAiTool(GEMINI_PACKAGE, GEMINI_URL)
                    2 -> openDsh()
                }
            }
            .show()
    }

    private fun openAiTool(packageName: String, webUrl: String) {
        // 先尝试打开已安装的应用；未安装或启动失败则用浏览器打开网页
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            try {
                startActivity(launchIntent)
                return
            } catch (e: Exception) {
                // 启动失败，回退到网页
            }
        }
        openUrl(webUrl)
    }

    // DSH：只尝试启动本机应用；失败给清晰提示，不回退 GitHub
    private fun openDsh() {
        val launchIntent = packageManager.getLaunchIntentForPackage(DSH_PACKAGE)
        if (launchIntent != null) {
            try {
                startActivity(launchIntent)
                return
            } catch (e: Exception) {
                // 启动失败，走下面的提示
            }
        }
        Toast.makeText(
            this,
            getString(R.string.toast_app_unavailable, getString(R.string.ai_dshbox)),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
        Toast.makeText(this, R.string.toast_grant_overlay, Toast.LENGTH_LONG).show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun requestMediaPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.record_permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.record_permission_need, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startPet() {
        val intent = Intent(this, FloatingPetService::class.java)
            .setAction(FloatingPetService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // 乐观更新文案；onResume 会用服务的真实状态校正
        tvStatus.setText(R.string.status_running)
    }

    private fun stopPet() {
        val intent = Intent(this, FloatingPetService::class.java)
            .setAction(FloatingPetService.ACTION_STOP)
        startService(intent)
        tvStatus.setText(R.string.status_idle)
    }
}