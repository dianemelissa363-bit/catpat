package com.example.catpet

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.jvm.Volatile

class FloatingPetService : Service() {

    companion object {
        const val ACTION_START = "com.example.catpet.action.START"
        const val ACTION_STOP = "com.example.catpet.action.STOP"
        private const val CHANNEL_ID = "pet_service"
        private const val NOTIFICATION_ID = 1

        // 悬浮猫菜单的三个目标应用
        private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
        private const val GEMINI_PACKAGE = "com.google.android.apps.bard"
        private const val DSH_PACKAGE = "com.dsharnessmobile.shell"

        // 供 MainActivity 读取的运行状态（进程内共享，Activity 重建后仍保留）
        @Volatile var isRunning = false
        @Volatile var everStarted = false
    }

    private var windowManager: WindowManager? = null
    private var petView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var floatAnimator: ValueAnimator? = null
    private var menuView: View? = null

    // 触摸拖动状态
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showPet()
            }
            ACTION_STOP -> {
                hidePet()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        hidePet()
        super.onDestroy()
    }

    // ---------- 通知 ----------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    // ---------- 悬浮窗 ----------
    private fun showPet() {
        if (petView != null) return

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val view = TextView(this).apply {
            text = "\uD83D\uDC31"   // 🐱
            textSize = 72f
            setOnTouchListener { _, event -> handleTouch(event) }
        }

        try {
            wm.addView(view, params)
        } catch (e: SecurityException) {
            // 悬浮窗权限被撤销/未授予：清理引用，保持未运行
            petView = null
            layoutParams = null
            windowManager = null
            isRunning = false
            return
        } catch (e: WindowManager.BadTokenException) {
            // 窗口 token 无效：清理引用，保持未运行
            petView = null
            layoutParams = null
            windowManager = null
            isRunning = false
            return
        }

        petView = view
        layoutParams = params
        isRunning = true
        everStarted = true

        try {
            startIdleAnimation(view)
        } catch (e: Exception) {
            // 动画失败不影响显示：视图已成功添加，isRunning 保持 true 是正确的
        }
    }

    private fun hidePet() {
        hideAiMenu()
        floatAnimator?.cancel()
        floatAnimator = null
        petView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (e: Exception) {
                // 窗口可能已被移除，忽略
            }
        }
        petView = null
        layoutParams = null
        windowManager = null
        isRunning = false
    }

    // ---------- 触摸：区分点击和拖动 ----------
    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
                if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    isDragging = true
                    // 开始拖动时收起菜单，避免菜单与小猫位置错位
                    hideAiMenu()
                }
                if (isDragging) {
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    try {
                        windowManager?.updateViewLayout(petView, params)
                    } catch (e: Exception) {
                        // 忽略
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    onPetTapped()
                }
                return true
            }
        }
        return false
    }

    // ---------- 点击互动 ----------
    private fun onPetTapped() {
        // 弹跳反馈
        petView?.let { v ->
            v.text = "\uD83D\uDE38"   // 😸
            v.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(150)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    v.text = "\uD83D\uDC31"   // 🐱
                }
                .start()
        }
        // 切换 AI 菜单：已展开则收起，否则弹出
        if (menuView != null) {
            hideAiMenu()
        } else {
            showAiMenu()
        }
    }

    // ---------- 悬浮猫 AI 菜单 ----------
    private fun showAiMenu() {
        if (menuView != null) return
        val wm = windowManager ?: return
        val petParams = layoutParams ?: return
        val pet = petView ?: return

        // 1) 菜单视图（与原来一致）
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE6FFFFFF.toInt())  // 半透明白底
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        menu.addView(makeMenuItem(getString(R.string.ai_chatgpt)) {
            openPetApp(CHATGPT_PACKAGE, getString(R.string.ai_chatgpt))
        })
        menu.addView(makeMenuItem(getString(R.string.ai_gemini)) {
            openPetApp(GEMINI_PACKAGE, getString(R.string.ai_gemini))
        })
        menu.addView(makeMenuItem(getString(R.string.ai_dshbox)) {
            openPetApp(DSH_PACKAGE, getString(R.string.ai_dshbox))
        })

        // 2) 实测菜单与小猫尺寸（不假设固定宽度）
        val unbound = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        menu.measure(unbound, unbound)
        val menuW = menu.measuredWidth
        val menuH = menu.measuredHeight
        if (pet.width <= 0 || pet.height <= 0) {
            pet.measure(unbound, unbound)
        }
        val petW = if (pet.width > 0) pet.width else pet.measuredWidth
        val petH = if (pet.height > 0) pet.height else pet.measuredHeight

        // 3) 可用区域：左右 0..screenW，上边界避开状态栏，下边界到屏幕底
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val statusBarRes = resources.getIdentifier("status_bar_height", "dimen", "android")
        val topBound = if (statusBarRes > 0) resources.getDimensionPixelSize(statusBarRes) else 0
        val bottomBound = screenH
        val gap = dp(8)

        // 4) 竖直：优先下方 -> 上方 -> 兜底，最后钳制在可见区
        val petLeft = petParams.x
        val petTop = petParams.y
        val belowTop = petTop + petH + gap
        val aboveTop = petTop - menuH - gap
        val desiredY = when {
            belowTop + menuH <= bottomBound -> belowTop
            aboveTop >= topBound -> aboveTop
            else -> petTop + gap
        }
        val maxY = maxOf(topBound, bottomBound - menuH)
        val menuY = desiredY.coerceIn(topBound, maxY)

        // 5) 水平：以小猫为中心，按实测宽度钳制到屏幕内
        val desiredX = petLeft + (petW - menuW) / 2
        val maxX = maxOf(0, screenW - menuW)
        val menuX = desiredX.coerceIn(0, maxX)

        // 6) 应用位置并添加窗口（gravity 仍为 TOP|START，x/y 即左上角）
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = menuX
            y = menuY
        }

        try {
            wm.addView(menu, params)
            menuView = menu
        } catch (e: Exception) {
            menuView = null
        }
    }

    private fun hideAiMenu() {
        menuView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (e: Exception) {
                // 忽略
            }
        }
        menuView = null
    }

    private fun makeMenuItem(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            setPadding(dp(20), dp(10), dp(20), dp(10))
            setOnClickListener {
                hideAiMenu()
                onClick()
            }
        }
    }

    private fun openPetApp(packageName: String, label: String) {
        // 优先按包名启动已安装应用；未安装或启动失败给出明确提示，不擅自打开其它应用
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            try {
                startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                // 启动失败，走下面的提示
            }
        }
        Toast.makeText(this, getString(R.string.toast_app_unavailable, label), Toast.LENGTH_SHORT).show()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- 待机漂浮动画 ----------
    private fun startIdleAnimation(view: View) {
        floatAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                view.translationY = (fraction - 0.5f) * 30f
            }
            start()
        }
    }
}