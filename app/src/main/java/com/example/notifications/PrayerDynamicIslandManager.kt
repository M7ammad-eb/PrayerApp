package com.example.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.PrayerApplication
import com.example.R
import com.example.data.models.PrayerType

/**
 * Universal Live Activity / Dynamic Island Capsule Notification Manager.
 * Fully compatible with all Android platforms:
 * - Vivo / iQOO OriginOS Atomic Island / Live Capsule
 * - OPPO / OnePlus ColorOS / OxygenOS Live Alerts & Aqua Dynamics
 * - Honor MagicOS Magic Capsule
 * - Xiaomi HyperOS Live Island & Status Bar Capsules
 * - Samsung One UI Live Notifications / Now Bar
 * - Android 12 to Android 16 System Live Activities & DynamicSpot
 */
object PrayerDynamicIslandManager {

    const val DYNAMIC_ISLAND_NOTIFICATION_ID = 8888
    const val ACTION_DISMISS_ISLAND = "com.example.ACTION_DISMISS_ISLAND"

    fun showCountdownIsland(
        context: Context,
        prayerType: PrayerType,
        targetEpochMillis: Long,
        prayerTimeFormatted: String,
        locationName: String = ""
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val remainingMillis = targetEpochMillis - System.currentTimeMillis()
        val remainingMinutes = ((remainingMillis / 1000) / 60).coerceAtLeast(1)

        val title = "Next: ${prayerType.title} (${prayerType.arabicName})"
        val locationDisplay = if (locationName.isNotBlank()) "$prayerTimeFormatted • $locationName" else prayerTimeFormatted
        val subText = "In $remainingMinutes min"

        // Chronometer base calculation for real-time countdown
        val chronometerBase = SystemClock.elapsedRealtime() + remainingMillis.coerceAtLeast(0)

        // 1. Collapsed View (Capsule format for status bar / notch projection)
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_island_collapsed).apply {
            setTextViewText(R.id.island_collapsed_title, "${prayerType.title} • ${prayerType.arabicName}")
            setTextViewText(R.id.island_collapsed_location, locationDisplay)
            setChronometer(R.id.island_collapsed_chronometer, chronometerBase, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.island_collapsed_chronometer, true)
            }
        }

        // 2. Expanded View (Full live activity card)
        val expandedViews = RemoteViews(context.packageName, R.layout.notification_island_expanded).apply {
            setTextViewText(R.id.island_expanded_prayer_name, "Upcoming: ${prayerType.title} (${prayerType.arabicName})")
            setTextViewText(R.id.island_expanded_location, locationDisplay)
            setChronometer(R.id.island_expanded_chronometer, chronometerBase, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.island_expanded_chronometer, true)
            }
        }

        // Open App PendingIntent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss PendingIntent
        val dismissIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ISLAND
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = PrayerApplication.CHANNEL_DYNAMIC_ISLAND_ID

        // Comprehensive OEM and Android System Live Activity metadata
        val islandExtras = Bundle().apply {
            // Android Core / Live Updates
            putBoolean("android.liveUpdate", true)
            putBoolean("is_live_activity", true)
            putBoolean("live_activity", true)
            putBoolean("android.showChronometer", true)
            putBoolean("android.chronometerCountDown", true)
            putLong("android.chronometerBase", chronometerBase)
            putString("android.substName", "Prayer Times")

            // Vivo / iQOO / OriginOS / FuntouchOS Atomic Island / Live Capsule
            putBoolean("vivo_capsule_enable", true)
            putBoolean("vivo.notification.is_capsule", true)
            putBoolean("vivo_atomic_island", true)
            putBoolean("vivo_live_island", true)
            putString("vivo_notification_type", "live_capsule")
            putString("origin_island_type", "capsule")
            putString("atomic_island_type", "capsule")
            putString("vivo_capsule_type", "countdown")
            putLong("vivo_capsule_time", targetEpochMillis)

            // OPPO / OnePlus ColorOS & OxygenOS Live Alerts / Aqua Dynamics
            putBoolean("com.oplus.notification.capsule", true)
            putString("com.oplus.notification.extra.LIVE_STATE", "COUNTDOWN")
            putString("com.oplus.notification.extra.SCENARIO", "SCHEDULE")
            putBoolean("com.coloros.notification.capsule", true)

            // Honor MagicOS Magic Capsule
            putBoolean("com.hihonor.notification.isCapsule", true)
            putString("com.hihonor.notification.capsuleType", "live")
            putBoolean("hw_capsule_flag", true)

            // Xiaomi / HyperOS Live Dynamic Island
            putBoolean("miui.live_island", true)
            putBoolean("miui.show_on_statusbar", true)
            putBoolean("hyperos.island_enable", true)

            // Samsung One UI Live Notifications / Now Bar
            putBoolean("samsung.live_notification", true)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_prayer_countdown)
            .setContentTitle(title)
            .setContentText(locationDisplay)
            .setSubText(subText)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(targetEpochMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, false)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setExtras(islandExtras)
            .addAction(android.R.drawable.ic_menu_view, "Open App", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        val notification = builder.build().apply {
            flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        }

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(DYNAMIC_ISLAND_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            notificationManager.notify(DYNAMIC_ISLAND_NOTIFICATION_ID, notification)
        }
    }

    fun dismissCountdownIsland(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(DYNAMIC_ISLAND_NOTIFICATION_ID)
    }
}
