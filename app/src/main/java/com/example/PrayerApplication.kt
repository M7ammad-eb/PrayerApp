package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class PrayerApplication : Application() {

    companion object {
        const val CHANNEL_ATHAN_ID = "prayer_athan_channel"
        const val CHANNEL_REMINDER_ID = "prayer_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val athanChannel = NotificationChannel(
                CHANNEL_ATHAN_ID,
                "Prayer Times & Athan Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and Athan sounds when prayer time arrives"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setShowBadge(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "Pre-Prayer Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle pre-prayer reminders prior to adhan"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(athanChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
}
