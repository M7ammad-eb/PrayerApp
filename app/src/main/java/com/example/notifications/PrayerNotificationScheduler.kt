package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.calculator.PrayerTimesCalculator
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import com.example.data.preferences.AppPrayerSettings
import com.example.data.preferences.PrayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PrayerNotificationScheduler {

    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = PrayerPreferences(context)
            val settings = prefs.settingsFlow.first()
            scheduleDailyAlarms(context, settings)
        }
    }

    fun scheduleDailyAlarms(context: Context, settings: AppPrayerSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zoneId = try { ZoneId.of(settings.location.timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = java.time.ZonedDateTime.now(zoneId)

        val timeFormatter = if (settings.is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }

        // Schedule for today and tomorrow
        for (dayOffset in 0..1) {
            val date = LocalDate.now(zoneId).plusDays(dayOffset.toLong())
            val schedule = PrayerTimesCalculator.calculateDailySchedule(
                date = date,
                latitude = settings.location.latitude,
                longitude = settings.location.longitude,
                zoneId = zoneId,
                method = settings.calculationMethod,
                juristicMethod = settings.juristicMethod,
                highLatitudeRule = settings.highLatitudeRule,
                adjustments = settings.adjustments,
                hijriAdjustmentDays = settings.hijriAdjustmentDays
            )

            for (item in schedule.prayerItems) {
                val prayerType = item.type
                val config = settings.prayerConfigs[prayerType] ?: continue
                if (!config.enabled || config.soundType == NotificationSoundType.SILENT) continue

                val prayerZonedTime = item.zonedDateTime
                val prayerFormatted = item.time.format(timeFormatter)
                val prayerEpochMillis = prayerZonedTime.toInstant().toEpochMilli()

                // 1. Exact Prayer Time Alarm (Athan)
                if (prayerZonedTime.isAfter(now)) {
                    val requestCode = prayerType.ordinal * 10 + dayOffset
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerFormatted)
                        putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, config.soundType.name)
                        putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, false)
                        putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, settings.location.name)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prayerEpochMillis, pendingIntent)
                        } else {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, prayerEpochMillis, pendingIntent)
                        }
                    } catch (e: Exception) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, prayerEpochMillis, pendingIntent)
                    }
                }

                // 2. Pre-prayer reminder alarm if set
                if (config.preReminderMinutes > 0) {
                    val reminderZonedTime = prayerZonedTime.minusMinutes(config.preReminderMinutes.toLong())
                    if (reminderZonedTime.isAfter(now)) {
                        val reminderRequestCode = 1000 + prayerType.ordinal * 10 + dayOffset
                        val reminderIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerFormatted)
                            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, NotificationSoundType.MELODIC_TONE.name)
                            putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, true)
                            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, settings.location.name)
                        }

                        val reminderPendingIntent = PendingIntent.getBroadcast(
                            context,
                            reminderRequestCode,
                            reminderIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val triggerAtMillis = reminderZonedTime.toInstant().toEpochMilli()
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, reminderPendingIntent)
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, reminderPendingIntent)
                            }
                        } catch (e: Exception) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, reminderPendingIntent)
                        }
                    }
                }

                // 3. Dynamic Island / Status Bar Live Countdown Trigger (e.g., 15 min before prayer)
                if (settings.dynamicIslandEnabled && prayerType != PrayerType.SUNRISE) {
                    val islandLeadMinutes = settings.dynamicIslandMinutesBefore.coerceAtLeast(1)
                    val islandTriggerZonedTime = prayerZonedTime.minusMinutes(islandLeadMinutes.toLong())

                    if (islandTriggerZonedTime.isAfter(now)) {
                        val islandRequestCode = 2000 + prayerType.ordinal * 10 + dayOffset
                        val islandIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = PrayerAlarmReceiver.ACTION_DYNAMIC_ISLAND_COUNTDOWN
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerFormatted)
                            putExtra(PrayerAlarmReceiver.EXTRA_TARGET_MILLIS, prayerEpochMillis)
                            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, settings.location.name)
                        }

                        val islandPendingIntent = PendingIntent.getBroadcast(
                            context,
                            islandRequestCode,
                            islandIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val triggerAtMillis = islandTriggerZonedTime.toInstant().toEpochMilli()
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, islandPendingIntent)
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, islandPendingIntent)
                            }
                        } catch (e: Exception) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, islandPendingIntent)
                        }
                    } else if (now.isBefore(prayerZonedTime)) {
                        // Already within the 15-min countdown window today: trigger island immediately!
                        PrayerDynamicIslandManager.showCountdownIsland(
                            context = context,
                            prayerType = prayerType,
                            targetEpochMillis = prayerEpochMillis,
                            prayerTimeFormatted = prayerFormatted,
                            locationName = settings.location.name
                        )
                    }
                }
            }
        }
    }

    fun triggerTestNotification(context: Context, prayerType: PrayerType, soundType: NotificationSoundType) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, "Now")
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, false)
            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, "Test Mode")
        }
        context.sendBroadcast(intent)
    }

    fun triggerTestDynamicIsland(context: Context, prayerType: PrayerType = PrayerType.ASR, minutesInFuture: Int = 15) {
        val targetMillis = System.currentTimeMillis() + (minutesInFuture * 60 * 1000L)
        val formattedTime = java.time.LocalTime.now().plusMinutes(minutesInFuture.toLong())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
        PrayerDynamicIslandManager.showCountdownIsland(
            context = context,
            prayerType = prayerType,
            targetEpochMillis = targetMillis,
            prayerTimeFormatted = formattedTime,
            locationName = "Live Island Preview"
        )
    }

    fun dismissDynamicIsland(context: Context) {
        PrayerDynamicIslandManager.dismissCountdownIsland(context)
    }
}
