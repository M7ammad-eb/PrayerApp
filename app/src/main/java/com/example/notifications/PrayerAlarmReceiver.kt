package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.PrayerApplication
import com.example.R
import com.example.audio.AthanAudioEngine
import com.example.audio.AthanAudioService
import com.example.data.models.AthanAudioStream
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import com.example.data.preferences.PrayerPreferences
import com.example.ui.alarm.PrayerAlarmActivity
import com.example.util.LocalizedStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
        const val ACTION_LIVE_COUNTDOWN = "com.example.ACTION_LIVE_COUNTDOWN"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_TARGET_MILLIS = "extra_target_millis"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED) {
            // System event / reboot / timezone change: reschedule all alarms immediately
            PrayerNotificationScheduler.rescheduleAll(context)
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

        val isArabic = PrayerPreferences.getInitialSettings(context).language.resolveIsArabic()
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

        val iconRes = try {
            R.drawable.ic_stat_prayer_countdown
        } catch (e: Exception) {
            android.R.drawable.ic_lock_idle_alarm
        }

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
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(if (isPreReminder) mainPendingIntent else alarmPendingIntent)
                .setSound(null)

            // Only attach Full-Screen intent when the screen is locked/off so it doesn't interrupt active phone usage
            if (!isPreReminder && !isScreenInteractive) {
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
        com.example.widget.PrayerAppWidgetProvider.updateAllWidgets(context)

        val pendingResult = goAsync()

        // Read audio stream & screen wake preferences and start playback & UI
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val prefs = PrayerPreferences(context)
                val settings = prefs.settingsFlow.firstOrNull()
                val audioStream = settings?.audioStream ?: AthanAudioStream.ALARM
                val wakeScreen = settings?.wakeScreenOnAlarm ?: true

                // Wake the screen and show full screen alarm activity ONLY IF screen is OFF (user not actively using phone)
                if (!isPreReminder && wakeScreen && !isScreenInteractive) {
                    try {
                        val wakeLock = pm?.newWakeLock(
                            PowerManager.FULL_WAKE_LOCK or
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                                    PowerManager.ON_AFTER_RELEASE,
                            "PrayerApp:ScreenWakeAlarm"
                        )
                        wakeLock?.acquire(10 * 1000L) // 10 seconds wake lock to illuminate screen
                    } catch (e: Exception) {
                        // Ignore wake lock failure if not permitted
                    }

                    try {
                        context.startActivity(alarmIntent)
                    } catch (e: Exception) {
                        // Fallback to notification intent
                    }
                }

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
                                    showFullScreenAlarm = !isScreenInteractive
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

