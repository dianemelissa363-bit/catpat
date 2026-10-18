package com.example.catpet

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
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
    }

    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

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
        findViewById<Button>(R.id.btnTodo).setOnClickListener { showComingSoon() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener { showComingSoon() }
        findViewById<Button>(R.id.btnAi).setOnClickListener { showAiToolsMenu() }
        findViewById<Button>(R.id.btnNotes).setOnClickListener { showComingSoon() }
        findViewById<Button>(R.id.btnGithub).setOnClickListener { openUrl("https://github.com/") }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showComingSoon() }

        // Android 13+ 需要通知权限，前台服务的常驻通知才可见
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // 返回首页时，用服务的真实状态刷新（而非本地内存变量）
        refreshStatus()
    }

    private fun refreshStatus() {
        tvStatus.text = when {
            !canDrawOverlays() -> getString(R.string.status_no_permission)  // 没有悬浮窗权限
            FloatingPetService.isRunning -> getString(R.string.status_running)  // 小猫正在运行
            FloatingPetService.everStarted -> getString(R.string.status_idle)  // 小猫已经停止
            else -> getString(R.string.status_ready)  // 已授权且未启动
        }
    }

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