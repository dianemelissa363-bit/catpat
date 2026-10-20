package com.example.catpet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the one-shot due alarm and posts a notification.
 *
 * The task is re-read from TodoTaskStore so deleted / completed tasks never
 * notify, and a stale alarm (due time changed afterwards) is ignored.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val task = TodoTaskStore(context).load().firstOrNull { it.id == taskId } ?: return
        if (task.done) return
        val due = task.dueAt ?: return
        // Stale alarm: the deadline was moved further into the future.
        if (due > System.currentTimeMillis() + 60_000L) return
        if (SettingsStore(context).remindersEnabled.not()) return
        ReminderNotifications.show(context, task)
    }

    companion object {
        const val ACTION_REMINDER = "com.example.catpet.action.REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DUE_AT = "due_at"
    }
}
