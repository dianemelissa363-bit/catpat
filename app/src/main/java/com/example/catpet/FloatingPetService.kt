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
import android.widget.TextView

class FloatingPetService : Service() {

    companion object {
        const val ACTION_START = "com.example.catpet.action.START"
        const val ACTION_STOP = "com.example.catpet.action.STOP"
        private const val CHANNEL_ID = "pet_service"
        private const val NOTIFICATION_ID = 1
    }

    private var windowManager: WindowManager? = null
    private var petView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var floatAnimator: ValueAnimator? = null

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

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

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

        windowManager?.addView(view, params)
        petView = view
        layoutParams = params

        startIdleAnimation(view)
    }

    private fun hidePet() {
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
    }

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
