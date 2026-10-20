package com.example.catpet

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Single place that schedules / cancels one-shot due reminders.
 *
 * Rules enforced here:
 * - only unfinished tasks with a future dueAt are scheduled;
 * - past-due tasks are never scheduled, so opening the app or rebooting never
 *   emits a late notification for an already expired deadline;
 * - every task uses a stable request code derived from its id.
 */
object ReminderScheduler {

    private const val REQUEST_BASE = 0x1000

    fun requestCode(taskId: String): Int = REQUEST_BASE + (taskId.hashCode() and 0x0FFFFFFF)

    fun schedule(context: Context, task: TodoTask) {
        val settings = SettingsStore(context)
        if (!settings.remindersEnabled) {
            cancel(context, task.id)
            return
        }
        val due = task.dueAt
        if (task.done || due == null || due <= System.currentTimeMillis()) {
            cancel(context, task.id)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, task.id, task.title, due)
        try {
            if (canScheduleExact(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pendingIntent)
            } else {
                // Fallback: still one-shot, but the system may batch it.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Exact alarm permission revoked at runtime: fall back to inexact.
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pendingIntent)
            } catch (_: Exception) {
                // Nothing more we can do here; settings screen reports the state.
            }
        }
    }

    fun cancel(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, taskId, "", 0L)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAll(context: Context, tasks: List<TodoTask>) {
        tasks.forEach { cancel(context, it.id) }
    }

    /** Cancels everything and re-schedules only still-relevant future tasks. */
    fun rescheduleAll(context: Context) {
        val settings = SettingsStore(context)
        val tasks = TodoTaskStore(context).load()
        cancelAll(context, tasks)
        if (!settings.remindersEnabled) return
        val now = System.currentTimeMillis()
        tasks
            .filter { !it.done && it.dueAt != null && it.dueAt > now }
            .forEach { schedule(context, it) }
    }

    fun canScheduleExact(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntent(
        context: Context,
        taskId: String,
        title: String,
        dueAt: Long
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMINDER)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderReceiver.EXTRA_TITLE, title)
            .putExtra(ReminderReceiver.EXTRA_DUE_AT, dueAt)
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
