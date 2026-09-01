package com.prayertimes.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prayertimes.MainActivity
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.data.models.PrayerType
import com.prayertimes.util.LocalizedStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Standard Android "Live Update" style countdown notification shown ahead of Athan time, per the
// user's explicit choice to build this on Google's own promoted-ongoing notification API
// (Android 16+) rather than chasing OEM-specific island surfaces (Vivo/Xiaomi/etc), which are
// either undocumented or require a separate per-OEM developer registration.
object PrayerLiveCountdownManager {

    internal const val NOTIFICATION_ID = 7777
    internal const val PROGRESS_UPDATE_REQUEST_CODE = 4999
    private const val TAG = "SalatiLiveCountdown"
    private const val PROGRESS_UPDATE_INTERVAL_MILLIS = 3L * 60L * 1000L

    fun show(
        context: Context,
        prayerType: PrayerType,
        targetEpochMillis: Long,
        countdownStartEpochMillis: Long,
        receiverExecutionEpochMillis: Long,
        locationName: String,
        isArabic: Boolean
    ) {
        val nowMillis = System.currentTimeMillis()
        val remainingMillis = targetEpochMillis - nowMillis
        if (remainingMillis <= 0L) {
            Log.d(
                TAG,
                "Ignoring expired countdown: intendedTrigger=$countdownStartEpochMillis " +
                    "receiverExecution=$receiverExecutionEpochMillis target=$targetEpochMillis " +
                    "remainingMs=$remainingMillis"
            )
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

        var promotionRequested = false
        if (Build.VERSION.SDK_INT >= 36) {
            val journeyDuration = (targetEpochMillis - countdownStartEpochMillis).coerceAtLeast(1L)
            val elapsed = (receiverExecutionEpochMillis - countdownStartEpochMillis)
                .coerceIn(0L, journeyDuration)
            val progressPercent = ((elapsed.toDouble() / journeyDuration.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
            builder
                .setStyle(NotificationCompat.ProgressStyle().setProgress(progressPercent))
                // This field is a static status-chip label, not a Chronometer. A minute snapshot
                // such as "10m" therefore looks frozen on OEM surfaces (confirmed on Vivo's
                // Android 16 promoted chip). Use the stable prayer name and leave timing to the
                // native chronometer/timeout rather than waking the app every minute.
                .setShortCriticalText(localizedPrayerName)
                // Capability state is diagnostic only. Always construct an eligible request and
                // let Android/Samsung decide whether it is promoted or shown normally.
                .setRequestPromotedOngoing(true)
            promotionRequested = true
        }

        val notification = builder.build()
        logDiagnostics(
            context = context,
            notification = notification,
            intendedTriggerEpochMillis = countdownStartEpochMillis,
            receiverExecutionEpochMillis = receiverExecutionEpochMillis,
            targetEpochMillis = targetEpochMillis,
            remainingMillis = remainingMillis,
            promotionRequested = promotionRequested
        )

        var postAccepted = false
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            postAccepted = true
        } catch (e: SecurityException) {
            Log.e(TAG, "NotificationCompat.notify was denied", e)
            runCatching {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }.onSuccess {
                postAccepted = true
            }.onFailure { Log.e(TAG, "Platform notify fallback failed", it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post Live Countdown notification", e)
        }

        if (Build.VERSION.SDK_INT >= 36 && postAccepted && canDisplayLiveCountdown(context)) {
            scheduleNextProgressUpdate(
                context = context,
                prayerType = prayerType,
                countdownStartEpochMillis = countdownStartEpochMillis,
                targetEpochMillis = targetEpochMillis,
                locationName = locationName
            )
        } else {
            cancelProgressUpdate(context)
        }
        logPostedPromotionFlagAfterPost(context)
    }

    /** User capability, never a prerequisite for building/posting a promotion request. */
    fun canPostPromotedNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        return runCatching { NotificationManagerCompat.from(context).canPostPromotedNotifications() }
            .onFailure { Log.e(TAG, "Unable to query promoted-notification capability", it) }
            .getOrDefault(false)
    }

    /** Returns null on OEM builds that do not expose Android's promotion settings activity. */
    fun promotionSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < 36) return null
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        return intent.takeIf {
            runCatching { it.resolveActivity(context.packageManager) != null }
                .onFailure { error -> Log.e(TAG, "Unable to resolve promotion settings", error) }
                .getOrDefault(false)
        }
    }

    fun isSamsungDevice(): Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    private fun logDiagnostics(
        context: Context,
        notification: Notification,
        intendedTriggerEpochMillis: Long,
        receiverExecutionEpochMillis: Long,
        targetEpochMillis: Long,
        remainingMillis: Long,
        promotionRequested: Boolean
    ) {
        val platformManager = context.getSystemService(NotificationManager::class.java)
        val compatManager = NotificationManagerCompat.from(context)
        val notificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            platformManager?.getNotificationChannel(PrayerApplication.CHANNEL_LIVE_COUNTDOWN_ID)
        } else {
            null
        }
        val channelImportance = channel?.importance
        val channelBlocked = channelImportance == NotificationManager.IMPORTANCE_NONE
        val compatCanPromote = if (Build.VERSION.SDK_INT >= 36) {
            runCatching { compatManager.canPostPromotedNotifications() }
                .onFailure { Log.e(TAG, "Compat promotion capability query failed", it) }
                .getOrDefault(false)
        } else false
        val platformCanPromote = if (Build.VERSION.SDK_INT >= 36) {
            runCatching { platformManager?.canPostPromotedNotifications() ?: false }
                .onFailure { Log.e(TAG, "Platform promotion capability query failed", it) }
                .getOrDefault(false)
        } else false
        val hasPromotableCharacteristics = if (Build.VERSION.SDK_INT >= 36) {
            runCatching { NotificationCompat.hasPromotableCharacteristics(notification) }
                .onFailure { Log.e(TAG, "Promotable-characteristics check failed", it) }
                .getOrDefault(false)
        } else false
        val samsungBuild = if (isSamsungDevice()) {
            // Android exposes no public One UI version API. Build.DISPLAY is the safest useful
            // Samsung build identifier and avoids unsupported reflection/system-property hacks.
            "oneUiVersion=not_exposed_by_public_api samsungBuildDisplay=${Build.DISPLAY}"
        } else {
            "oneUiVersion=not_samsung"
        }

        Log.i(
            TAG,
            "device=${Build.MANUFACTURER}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT} $samsungBuild " +
                "intendedTrigger=$intendedTriggerEpochMillis " +
                "receiverExecution=$receiverExecutionEpochMillis target=$targetEpochMillis " +
                "remainingMs=$remainingMillis notificationsEnabled=${compatManager.areNotificationsEnabled()} " +
                "postPermission=$notificationPermission channelImportance=$channelImportance " +
                "channelBlocked=$channelBlocked promotionRequested=$promotionRequested " +
                "hasPromotableCharacteristics=$hasPromotableCharacteristics " +
                "compatCanPostPromoted=$compatCanPromote platformCanPostPromoted=$platformCanPromote"
        )
    }

    private fun logPostedPromotionFlagAfterPost(context: Context) {
        if (Build.VERSION.SDK_INT < 36) return
        // NotificationManager.notify() crosses into system_server asynchronously. Querying in the
        // same stack frame can falsely report that the notification is absent, so inspect once
        // after a brief grace period. This is diagnostics only and never updates the notification.
        PrayerApplication.instance.applicationScope.launch {
            delay(750L)
            runCatching {
                val manager = context.getSystemService(NotificationManager::class.java)
                val active = manager?.activeNotifications?.firstOrNull { it.id == NOTIFICATION_ID }
                val promoted = active != null &&
                    (active.notification.flags and Notification.FLAG_PROMOTED_ONGOING) != 0
                Log.i(TAG, "activeNotificationFound=${active != null} flagPromotedOngoing=$promoted")
            }.onFailure {
                Log.e(TAG, "Unable to inspect posted notification promotion flag", it)
            }
        }
    }

    private fun canDisplayLiveCountdown(context: Context): Boolean {
        val compatManager = NotificationManagerCompat.from(context)
        if (!compatManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(PrayerApplication.CHANNEL_LIVE_COUNTDOWN_ID)?.importance !=
            NotificationManager.IMPORTANCE_NONE
    }

    private fun scheduleNextProgressUpdate(
        context: Context,
        prayerType: PrayerType,
        countdownStartEpochMillis: Long,
        targetEpochMillis: Long,
        locationName: String
    ) {
        val now = System.currentTimeMillis()
        val elapsed = (now - countdownStartEpochMillis).coerceAtLeast(0L)
        val nextStep = elapsed / PROGRESS_UPDATE_INTERVAL_MILLIS + 1L
        val nextUpdate = countdownStartEpochMillis + nextStep * PROGRESS_UPDATE_INTERVAL_MILLIS
        if (nextUpdate >= targetEpochMillis) {
            cancelProgressUpdate(context)
            return
        }

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_TARGET_MILLIS, targetEpochMillis)
            putExtra(PrayerAlarmReceiver.EXTRA_INTENDED_TRIGGER_MILLIS, countdownStartEpochMillis)
            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, locationName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PROGRESS_UPDATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // Progress is cosmetic, so RTC (not RTC_WAKEUP) guarantees a sleeping phone is never
        // awakened merely to move the bar. While the phone is awake, use exact timing when the app
        // already has access so OEM batching cannot turn a three-minute step into a much longer one;
        // otherwise retain a normal inexact RTC fallback.
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canUseExact) {
            try {
                alarmManager.setExact(AlarmManager.RTC, nextUpdate, pendingIntent)
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact non-wakeup progress refresh denied; using inexact fallback", e)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Exact non-wakeup progress refresh rejected; using inexact fallback", e)
            }
        }
        alarmManager.set(AlarmManager.RTC, nextUpdate, pendingIntent)
    }

    private fun cancelProgressUpdate(context: Context) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PROGRESS_UPDATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun dismiss(context: Context) {
        cancelProgressUpdate(context)
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss Live Countdown notification", e)
        }
    }
}
