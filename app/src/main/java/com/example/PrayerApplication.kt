package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class PrayerApplication : Application() {

    companion object {
        const val CHANNEL_ATHAN_ID = "prayer_athan_channel_v2"
        const val CHANNEL_REMINDER_ID = "prayer_reminder_channel_v2"
        const val CHANNEL_DYNAMIC_ISLAND_ID = "prayer_dynamic_island_channel_v2"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete old channels if they had default sounds attached by Android
            try {
                notificationManager.deleteNotificationChannel("prayer_athan_channel")
                notificationManager.deleteNotificationChannel("prayer_dynamic_island_channel")
            } catch (e: Exception) {
                // Ignore
            }

            val athanChannel = NotificationChannel(
                CHANNEL_ATHAN_ID,
                "Prayer Times & Athan Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and Athan sounds when prayer time arrives"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
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

            val islandChannel = NotificationChannel(
                CHANNEL_DYNAMIC_ISLAND_ID,
                "Dynamic Island & Live Prayer Countdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing countdown shown in the Dynamic Island and status bar before prayer"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(athanChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(islandChannel)
        }
    }
}
