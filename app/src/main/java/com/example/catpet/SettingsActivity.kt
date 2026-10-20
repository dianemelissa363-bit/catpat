package com.example.catpet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

/**
 * Settings page. Every control writes to SettingsStore and takes effect
 * immediately; permission state is shown truthfully instead of assumed.
 */
class SettingsActivity : Activity() {

    private lateinit var settings: SettingsStore
    private lateinit var btnDockLeft: Button
    private lateinit var btnDockRight: Button
    private lateinit var cbSnap: CheckBox
    private lateinit var tvOverlayStatus: TextView
    private lateinit var cbReminder: CheckBox
    private lateinit var tvNotificationStatus: TextView
    private lateinit var tvExactAlarmStatus: TextView
    private lateinit var btnExactAlarmSettings: Button
    private lateinit var tvRecordStatus: TextView
    private lateinit var btnToggleRecord: Button
    private lateinit var tvMediaStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = SettingsStore(this)

        btnDockLeft = findViewById(R.id.btnDockLeft)
        btnDockRight = findViewById(R.id.btnDockRight)
        cbSnap = findViewById(R.id.cbSnap)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        cbReminder = findViewById(R.id.cbReminder)
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus)
        tvExactAlarmStatus = findViewById(R.id.tvExactAlarmStatus)
        btnExactAlarmSettings = findViewById(R.id.btnExactAlarmSettings)
        tvRecordStatus = findViewById(R.id.tvRecordStatus)
        btnToggleRecord = findViewById(R.id.btnToggleRecord)
        tvMediaStatus = findViewById(R.id.tvMediaStatus)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        btnDockLeft.setOnClickListener {
            settings.defaultDockSide = SettingsStore.DOCK_LEFT
            refresh()
        }
        btnDockRight.setOnClickListener {
            settings.defaultDockSide = SettingsStore.DOCK_RIGHT
            refresh()
        }
        cbSnap.setOnCheckedChangeListener { _, checked -> settings.snapToEdge = checked }

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, R.string.settings_open_failed, Toast.LENGTH_SHORT).show()
            }
        }

        cbReminder.setOnCheckedChangeListener { _, checked ->
            settings.remindersEnabled = checked
            if (checked) {
                ReminderScheduler.rescheduleAll(this)
            } else {
                ReminderScheduler.cancelAll(this, TodoTaskStore(this).load())
            }
            refresh()
        }

        findViewById<Button>(R.id.btnNotificationSettings).setOnClickListener {
            openNotificationSettings()
        }
        btnExactAlarmSettings.setOnClickListener { openExactAlarmSettings() }

        btnToggleRecord.setOnClickListener {
            if (!FloatingPetService.isRunning) {
                Toast.makeText(this, R.string.settings_record_need_pet, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startService(
                Intent(this, FloatingPetService::class.java)
                    .setAction(FloatingPetService.ACTION_TOGGLE_RECORDING)
            )
            tvRecordStatus.postDelayed({ refresh() }, 300)
        }

        findViewById<Button>(R.id.btnMediaPermission).setOnClickListener {
            requestMediaPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val left = settings.defaultDockSide == SettingsStore.DOCK_LEFT
        btnDockLeft.isEnabled = !left
        btnDockRight.isEnabled = left
        cbSnap.isChecked = settings.snapToEdge

        tvOverlayStatus.text = getString(
            if (Settings.canDrawOverlays(this)) R.string.settings_overlay_granted
            else R.string.settings_overlay_missing
        )

        cbReminder.isChecked = settings.remindersEnabled
        tvNotificationStatus.text = getString(
            if (notificationsEnabled()) R.string.settings_notification_granted
            else R.string.settings_notification_missing
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            tvExactAlarmStatus.visibility = View.VISIBLE
            btnExactAlarmSettings.visibility = View.VISIBLE
            tvExactAlarmStatus.text = getString(
                if (alarmManager.canScheduleExactAlarms()) R.string.settings_exact_alarm_granted
                else R.string.settings_exact_alarm_missing
            )
        } else {
            tvExactAlarmStatus.visibility = View.GONE
            btnExactAlarmSettings.visibility = View.GONE
        }

        tvRecordStatus.text = getString(
            if (FloatingPetService.isRecording) R.string.settings_record_on
            else R.string.settings_record_off
        )
        btnToggleRecord.isEnabled = FloatingPetService.isRunning
        tvMediaStatus.text = getString(
            if (hasMediaPermission()) R.string.settings_media_granted
            else R.string.settings_media_missing
        )
    }

    private fun notificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.areNotificationsEnabled()
        }
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun openNotificationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, R.string.settings_open_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestMediaPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 201)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 201) {
            refresh()
        }
    }
}
