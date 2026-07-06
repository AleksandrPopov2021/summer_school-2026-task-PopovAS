package ru.vertical.climbing.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Канал push-уведомлений Android (NFR-004). */
object AndroidNotificationChannels {
    const val CHANNEL_ID = "vertical_training"
    const val CHANNEL_NAME = "Тренировки"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Подтверждения записей, напоминания и отмены тренировок"
        }
        manager.createNotificationChannel(channel)
    }
}
