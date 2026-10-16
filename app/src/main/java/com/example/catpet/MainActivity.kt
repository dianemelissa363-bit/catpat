package com.example.catpet

import android.Manifest
import android.app.Activity
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

        // Android 13+ 需要通知权限，前台服务的常驻通知才可见
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置授权回来时刷新状态提示
        tvStatus.text = if (canDrawOverlays()) {
            getString(R.string.status_ready)
        } else {
            getString(R.string.status_no_permission)
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
        tvStatus.setText(R.string.status_running)
    }

    private fun stopPet() {
        val intent = Intent(this, FloatingPetService::class.java)
            .setAction(FloatingPetService.ACTION_STOP)
        startService(intent)
        tvStatus.setText(R.string.status_idle)
    }
}
