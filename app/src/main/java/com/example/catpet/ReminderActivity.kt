package com.example.catpet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reminder management page. Reuses TodoTaskStore; never creates a second copy
 * of task data. Editing jumps back to the existing TodoActivity flow.
 */
class ReminderActivity : Activity() {

    private lateinit var settings: SettingsStore
    private lateinit var store: TodoTaskStore
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var cbEnabled: CheckBox
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)
        settings = SettingsStore(this)
        store = TodoTaskStore(this)
        container = findViewById(R.id.reminderContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        cbEnabled = findViewById(R.id.cbReminderEnabled)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNewTask).setOnClickListener {
            startActivity(Intent(this, TodoActivity::class.java))
        }

        cbEnabled.setOnCheckedChangeListener { _, checked ->
            settings.remindersEnabled = checked
            if (checked) {
                ReminderScheduler.rescheduleAll(this)
            } else {
                ReminderScheduler.cancelAll(this, store.load())
            }
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        cbEnabled.isChecked = settings.remindersEnabled
        val tasks = store.load()
            .filter { it.dueAt != null }
            .sortedByDescending { it.updatedAt }

        container.removeAllViews()
        if (tasks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        tvEmpty.visibility = View.GONE
        container.visibility = View.VISIBLE

        tasks.forEach { task ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    startActivity(Intent(this@ReminderActivity, TodoActivity::class.java))
                }
            }
            row.addView(TextView(this).apply {
                text = task.title
                textSize = 15f
            })
            val status = if (task.done) {
                getString(R.string.reminder_status_done)
            } else {
                getString(R.string.reminder_status_pending)
            }
            row.addView(TextView(this).apply {
                text = getString(
                    R.string.reminder_item_line,
                    timeFormat.format(Date(task.dueAt ?: 0L)),
                    status
                )
                textSize = 12f
                setTextColor(0xFF666666.toInt())
            })
            container.addView(row)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
