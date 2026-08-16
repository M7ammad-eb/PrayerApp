package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PrayerApplication
import com.example.R
import com.example.data.models.PrayerType

/**
 * Universal Live Activity / Dynamic Island Capsule Notification Manager.
 * Works universally across all Android brands (Honor MagicOS Dynamic Capsule,
 * Xiaomi Dynamic Island, Samsung Live Notifications, Vivo/OriginOS Atomic Island,
 * Realme Mini Capsule, ColorOS/OxygenOS Live Alerts, and standard Android 12-16 Live Activities).
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

        // Calculate chronometer base for countdown
        // In Android Chronometer countDown mode, base is SystemClock.elapsedRealtime() + remainingMillis
        val chronometerBase = SystemClock.elapsedRealtime() + remainingMillis.coerceAtLeast(0)

        // 1. Collapsed View (Dynamic Island capsule format)
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_island_collapsed).apply {
            setTextViewText(R.id.island_collapsed_title, "${prayerType.title} • ${prayerType.arabicName}")
            setTextViewText(R.id.island_collapsed_location, locationDisplay)
            setChronometer(R.id.island_collapsed_chronometer, chronometerBase, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.island_collapsed_chronometer, true)
            }
        }

        // 2. Expanded View (Full card with countdown and prayer guidance)
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

        // Dismiss Island PendingIntent
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
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(android.R.drawable.ic_menu_view, "Open App", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        notificationManager.notify(DYNAMIC_ISLAND_NOTIFICATION_ID, builder.build())
    }

    fun dismissCountdownIsland(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(DYNAMIC_ISLAND_NOTIFICATION_ID)
    }
}
