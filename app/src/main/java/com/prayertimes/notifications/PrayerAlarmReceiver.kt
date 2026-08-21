package com.prayertimes.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prayertimes.MainActivity
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.audio.AthanAudioEngine
import com.prayertimes.audio.AthanAudioService
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.ui.alarm.PrayerAlarmActivity
import com.prayertimes.util.LocalizedStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.prayertimes.ACTION_PRAYER_ALARM"
        const val ACTION_LIVE_COUNTDOWN = "com.prayertimes.ACTION_LIVE_COUNTDOWN"
        // Self-scheduled, explicit-component daily trigger that replenishes the rolling 7-day
        // alarm window - see PrayerNotificationScheduler.scheduleMaintenanceAlarm() for why this
        // exists instead of relying solely on ACTION_DATE_CHANGED.
        const val ACTION_SCHEDULE_MAINTENANCE = "com.salati.prayertimes.ACTION_SCHEDULE_MAINTENANCE"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_TARGET_MILLIS = "extra_target_millis"
    }

    // Android 14+ can revoke an app's full-screen-intent capability regardless of the manifest
    // permission (only default dialer/alarm-category apps or ones the user manually re-enabled in
    // Settings keep it) - checking canUseFullScreenIntent() before calling setFullScreenIntent()
    // avoids relying on it silently degrading to a heads-up notification on its own.
    private fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.canUseFullScreenIntent()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == ACTION_SCHEDULE_MAINTENANCE) {
            // System event / reboot / timezone change / daily maintenance: reschedule all alarms.
            // ACTION_DATE_CHANGED is NOT on Android's implicit-broadcast exemption list for
            // manifest receivers (unlike BOOT_COMPLETED/TIME_SET/TIMEZONE_CHANGED), so it can't be
            // relied on alone to replenish the rolling 7-day window daily - ACTION_SCHEDULE_MAINTENANCE
            // is a self-armed, explicit-component alarm that doesn't depend on that exemption list at
            // all. rescheduleAll() -> scheduleDailyAlarms() re-arms tomorrow's maintenance trigger as
            // part of the same call, so this chain keeps itself alive indefinitely without needing the
            // app to ever be reopened. goAsync() extends the receiver's lifetime past onReceive()
            // returning - without it, Android is free to kill the process right after this call
            // returns (very plausible right after a boot), and rescheduleAll's coroutine could be cut
            // off partway through, leaving only some of the week's alarms restored.
            val pendingResult = goAsync()
            PrayerApplication.instance.applicationScope.launch {
                try {
                    PrayerNotificationScheduler.rescheduleAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (action == ACTION_LIVE_COUNTDOWN) {
            val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
            val prayerType = try {
                PrayerType.valueOf(prayerName.uppercase())
            } catch (e: Exception) {
                PrayerType.FAJR
            }
            val targetMillis = intent.getLongExtra(EXTRA_TARGET_MILLIS, System.currentTimeMillis())
            val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: ""
            val isArabic = PrayerPreferences.getInitialSettings(context).language.resolveIsArabic()
            PrayerLiveCountdownManager.show(context, prayerType, targetMillis, locationName, isArabic)
            return
        }

        if (action != ACTION_PRAYER_ALARM) {
            // Unrecognized action (e.g. a stale alarm scheduled by a since-removed feature under
            // its own action string) - ignore it rather than falling through and treating it as
            // a real prayer alarm.
            return
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerType = try {
            PrayerType.valueOf(prayerName.uppercase())
        } catch (e: Exception) {
            PrayerType.FAJR
        }
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: "your location"

        val soundTypeStr = intent.getStringExtra(EXTRA_SOUND_TYPE) ?: NotificationSoundType.FULL_ATHAN.name
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)

        val soundType = try {
            NotificationSoundType.valueOf(soundTypeStr)
        } catch (e: Exception) {
            NotificationSoundType.FULL_ATHAN
        }

        // Fast synchronous cache read (same one already used for language elsewhere in this
        // receiver) rather than awaiting the DataStore flow - lets wakeScreenOnAlarm factor into
        // the notification built below instead of only being available later inside the coroutine.
        val settings = PrayerPreferences.getInitialSettings(context)
        val isArabic = settings.language.resolveIsArabic()
        val localizedRes = LocalizedStrings.forLanguage(context, isArabic)
        val localizedPrayerName = LocalizedStrings.prayerName(localizedRes, prayerType)

        if (!isPreReminder) {
            // Prayer time has arrived - the live countdown (if one was showing) is no longer relevant.
            PrayerLiveCountdownManager.dismiss(context)
        }

        val title = if (isPreReminder) {
            localizedRes.getString(R.string.notif_alarm_approaching_title, localizedPrayerName)
        } else {
            localizedRes.getString(R.string.notif_alarm_time_title, localizedPrayerName)
        }

        val content = if (isPreReminder) {
            localizedRes.getString(R.string.notif_alarm_approaching_body, localizedPrayerName, prayerTime, locationName)
        } else {
            localizedRes.getString(R.string.notif_alarm_time_body, localizedPrayerName, locationName, prayerTime)
        }

        // PendingIntent to launch Full-Screen Prayer Alarm UI or Main App
        val alarmIntent = PrayerAlarmActivity.createIntent(
            context = context,
            prayerType = prayerType,
            prayerTime = prayerTime,
            locationName = locationName,
            soundType = soundType
        )
        val alarmPendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal + 200,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isPreReminder) PrayerApplication.CHANNEL_REMINDER_ID else PrayerApplication.CHANNEL_ATHAN_ID

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenInteractive = pm?.isInteractive ?: false

        // When real athan audio is about to play, AthanAudioService posts its own ongoing
        // notification with the Stop Athan action. Posting a second one here would duplicate it -
        // and since only the service's notification actually controls playback, users could swipe
        // this one away thinking it stopped the sound when it didn't.
        val willPlayViaService = !isPreReminder &&
            soundType != NotificationSoundType.SILENT &&
            soundType != NotificationSoundType.VIBRATE_ONLY

        if (!willPlayViaService) {
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_salati)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                // Body includes the user's location name - keep it off the lock screen.
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(if (isPreReminder) mainPendingIntent else alarmPendingIntent)
                .setSound(null)

            // Only attach the full-screen intent when the screen is locked/off (so it doesn't
            // interrupt active phone usage), the user hasn't opted out via wakeScreenOnAlarm, and
            // the OS is actually honoring full-screen intents for this app (Android 14+).
            if (!isPreReminder && !isScreenInteractive && settings.wakeScreenOnAlarm && canUseFullScreenIntent(context)) {
                notificationBuilder.setFullScreenIntent(alarmPendingIntent, true)
            }

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(prayerType.ordinal + if (isPreReminder) 100 else 0, notificationBuilder.build())
            } catch (e: SecurityException) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(prayerType.ordinal + if (isPreReminder) 100 else 0, notificationBuilder.build())
            } catch (e: Exception) {
                // Log or ignore
            }
        }

        // Update widget status
        com.prayertimes.widget.PrayerAppWidgetProvider.updateAllWidgets(context)

        val pendingResult = goAsync()
        val audioStream = settings.audioStream
        val wakeScreen = settings.wakeScreenOnAlarm

        // Start playback & UI. The full-screen alarm Activity is no longer launched directly from
        // here via startActivity() - modern Android restricts starting activities from a
        // background context like this, and it raced/duplicated the notification's own
        // full-screen-intent launch. AthanAudioService's foreground-service notification (below)
        // is the actual launch path now, using its own canUseFullScreenIntent() check, with the
        // heads-up notification built above as the fallback if that's not permitted.
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Audio Playback
                if (!isPreReminder) {
                    when (soundType) {
                        NotificationSoundType.SILENT -> {
                            // Do nothing
                        }
                        NotificationSoundType.VIBRATE_ONLY -> {
                            AthanAudioEngine.vibrateDevice(context)
                        }
                        else -> {
                            try {
                                AthanAudioService.startAthan(
                                    context = context,
                                    prayerType = prayerType,
                                    soundType = soundType,
                                    locationName = locationName,
                                    audioStream = audioStream,
                                    prayerTime = prayerTime,
                                    showFullScreenAlarm = wakeScreen && !isScreenInteractive
                                )
                            } catch (e: Exception) {
                                // Fallback to in-process playback if background service is restricted
                                AthanAudioEngine.playSoundType(
                                    context = context,
                                    soundType = soundType,
                                    prayerType = prayerType,
                                    audioStream = audioStream,
                                    isArabic = isArabic
                                )
                            }
                        }
                    }
                } else {
                    AthanAudioEngine.playGentleChime(context, audioStream = audioStream)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

