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
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // System rebooted: reschedule alarms
            PrayerNotificationScheduler.rescheduleAll(context)
            return
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val soundTypeStr = intent.getStringExtra(EXTRA_SOUND_TYPE) ?: NotificationSoundType.FULL_ATHAN.name
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)
        val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: "your location"

        val soundType = try {
            NotificationSoundType.valueOf(soundTypeStr)
        } catch (e: Exception) {
            NotificationSoundType.FULL_ATHAN
        }

        val prayerType = try {
            PrayerType.valueOf(prayerName.uppercase())
        } catch (e: Exception) {
            PrayerType.FAJR
        }

        val title = if (isPreReminder) {
            "Approaching: $prayerName Prayer"
        } else {
            "Time for $prayerName Prayer (${prayerType.arabicName})"
        }

        val content = if (isPreReminder) {
            "Prepare for $prayerName prayer coming up at $prayerTime in $locationName."
        } else {
            "It is now time for $prayerName prayer in $locationName ($prayerTime)."
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
            action = AthanAudioService.ACTION_STOP_ATHAN
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

        // Background Audio Handling via Foreground Service for full playback reliability
        if (!isPreReminder) {
            when (soundType) {
                NotificationSoundType.SILENT -> {
                    // Do nothing
                }
                NotificationSoundType.VIBRATE_ONLY -> {
                    AthanAudioEngine.vibrateDevice(context)
                }
                else -> {
                    // Start Athan Audio Service for reliable background playback
                    AthanAudioService.startAthan(
                        context = context,
                        prayerType = prayerType,
                        soundType = soundType,
                        locationName = locationName
                    )
                }
            }
        } else {
            // Pre-reminder gentle chime or vibration
            AthanAudioEngine.playGentleChime()
        }
    }
}
