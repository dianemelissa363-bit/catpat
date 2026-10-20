package com.example.catpet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Builds and posts the one-shot due reminder notification. */
object ReminderNotifications {

    const val CHANNEL_ID = "todo_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, task: TodoTask) {
        ensureChannel(context)
        val contentIntent = Intent(context, TodoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(TodoActivity.EXTRA_HIGHLIGHT_TASK_ID, task.id)
        val pendingIntent = PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(task.id),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(task.title)
            .setStyle(Notification.BigTextStyle().bigText(task.title))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ReminderScheduler.requestCode(task.id), notification)
    }
}
