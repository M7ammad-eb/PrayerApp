package com.prayertimes.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prayertimes.MainActivity
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.data.models.PrayerType
import com.prayertimes.util.LocalizedStrings

// Standard Android "Live Update" style countdown notification shown ahead of Athan time, per the
// user's explicit choice to build this on Google's own promoted-ongoing notification API
// (Android 16+) rather than chasing OEM-specific island surfaces (Vivo/Xiaomi/etc), which are
// either undocumented or require a separate per-OEM developer registration.
object PrayerLiveCountdownManager {

    private const val NOTIFICATION_ID = 7777

    fun show(context: Context, prayerType: PrayerType, targetEpochMillis: Long, locationName: String, isArabic: Boolean) {
        val remainingMillis = targetEpochMillis - System.currentTimeMillis()
        if (remainingMillis <= 0L) {
            dismiss(context)
            return
        }

        val localizedRes = LocalizedStrings.forLanguage(context, isArabic)
        val localizedPrayerName = LocalizedStrings.prayerName(localizedRes, prayerType)
        val title = localizedRes.getString(R.string.live_countdown_notif_title, localizedPrayerName)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            3,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, PrayerApplication.CHANNEL_LIVE_COUNTDOWN_ID)
            .setSmallIcon(R.drawable.ic_stat_salati)
            .setContentTitle(title)
            .setContentText(locationName)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setWhen(targetEpochMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            // Independent expiry: Android removes the countdown when it reaches zero even if a
            // full-screen alarm path prevents the normal prayer-notification cleanup from running.
            .setTimeoutAfter(remainingMillis)
            .setContentIntent(openAppPendingIntent)

        // Promotion to the system's Live Update surface (status bar chip / lock screen / AOD) is
        // gated behind both the platform version and a live user-revocable capability check - the
        // manifest permission alone doesn't guarantee the user has left it enabled in Settings.
        if (Build.VERSION.SDK_INT >= 36) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val canPromote = try {
                notificationManager?.canPostPromotedNotifications() ?: false
            } catch (e: Exception) {
                false
            }
            if (canPromote) {
                builder.setRequestPromotedOngoing(true)
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun dismiss(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
