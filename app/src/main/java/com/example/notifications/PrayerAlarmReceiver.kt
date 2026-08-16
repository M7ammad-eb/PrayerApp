package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PrayerApplication
import com.example.audio.AthanAudioEngine
import com.example.audio.AthanAudioService
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType

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

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // System rebooted: reschedule alarms
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

        // PendingIntent to launch app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
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

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)

        if (!isPreReminder && soundType != NotificationSoundType.SILENT && soundType != NotificationSoundType.VIBRATE_ONLY) {
            notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Athan", stopPendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(prayerType.ordinal + if (isPreReminder) 100 else 0, notificationBuilder.build())

        // Update widget status
        com.example.widget.PrayerAppWidgetProvider.updateAllWidgets(context)

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
                    AthanAudioService.startAthan(
                        context = context,
                        prayerType = prayerType,
                        soundType = soundType,
                        locationName = locationName
                    )
                }
            }
        } else {
            AthanAudioEngine.playGentleChime(context)
        }
    }
}
