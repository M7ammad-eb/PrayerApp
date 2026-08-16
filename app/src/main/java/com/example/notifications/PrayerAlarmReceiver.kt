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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
        const val ACTION_DYNAMIC_ISLAND_COUNTDOWN = "com.example.ACTION_DYNAMIC_ISLAND_COUNTDOWN"
        const val ACTION_DISMISS_ISLAND = "com.example.ACTION_DISMISS_ISLAND"

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

        if (action == ACTION_DISMISS_ISLAND) {
            PrayerDynamicIslandManager.dismissCountdownIsland(context)
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

        // Handle Dynamic Island Countdown Alarm
        if (action == ACTION_DYNAMIC_ISLAND_COUNTDOWN) {
            val targetEpochMillis = intent.getLongExtra(EXTRA_TARGET_MILLIS, System.currentTimeMillis() + 15 * 60 * 1000L)
            PrayerDynamicIslandManager.showCountdownIsland(
                context = context,
                prayerType = prayerType,
                targetEpochMillis = targetEpochMillis,
                prayerTimeFormatted = prayerTime,
                locationName = locationName
            )
            return
        }

        // When exact prayer time arrives, clear the dynamic island countdown
        PrayerDynamicIslandManager.dismissCountdownIsland(context)

        val soundTypeStr = intent.getStringExtra(EXTRA_SOUND_TYPE) ?: NotificationSoundType.FULL_ATHAN.name
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)

        val soundType = try {
            NotificationSoundType.valueOf(soundTypeStr)
        } catch (e: Exception) {
            NotificationSoundType.FULL_ATHAN
        }

        val title = if (isPreReminder) {
            "Approaching: ${prayerType.title} Prayer"
        } else {
            "Time for ${prayerType.title} Prayer (${prayerType.arabicName})"
        }

        val content = if (isPreReminder) {
            "Prepare for ${prayerType.title} prayer coming up at $prayerTime in $locationName."
        } else {
            "It is now time for ${prayerType.title} prayer in $locationName ($prayerTime)."
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

        val stopIntent = Intent(context, AthanAudioService::class.java).apply {
            this.action = AthanAudioService.ACTION_STOP_ATHAN
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            prayerType.ordinal + 500,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = try {
            R.drawable.ic_stat_prayer_countdown
        } catch (e: Exception) {
            android.R.drawable.ic_lock_idle_alarm
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenInteractive = pm?.isInteractive ?: false

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

        if (!isPreReminder && soundType != NotificationSoundType.SILENT && soundType != NotificationSoundType.VIBRATE_ONLY) {
            notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Athan", stopPendingIntent)
            notificationBuilder.addAction(android.R.drawable.ic_input_get, "Open Alarm View", alarmPendingIntent)
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
                                    audioStream = audioStream
                                )
                            } catch (e: Exception) {
                                // Fallback to in-process playback if background service is restricted
                                AthanAudioEngine.playSoundType(
                                    context = context,
                                    soundType = soundType,
                                    prayerType = prayerType,
                                    audioStream = audioStream
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

