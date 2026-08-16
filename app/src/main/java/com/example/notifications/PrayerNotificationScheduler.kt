package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
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

        // Schedule alarms across the next 7 days for complete week-long coverage
        for (dayOffset in 0..6) {
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
                    val requestCode = dayOffset * 100 + prayerType.ordinal
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

                    setExactAlarm(context, alarmManager, prayerEpochMillis, pendingIntent, requestCode)
                }

                // 2. Pre-prayer reminder alarm if configured
                if (config.preReminderMinutes > 0) {
                    val reminderZonedTime = prayerZonedTime.minusMinutes(config.preReminderMinutes.toLong())
                    if (reminderZonedTime.isAfter(now)) {
                        val reminderRequestCode = 1000 + dayOffset * 100 + prayerType.ordinal
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
                        setExactAlarm(context, alarmManager, triggerAtMillis, reminderPendingIntent, reminderRequestCode)
                    }
                }

                // 3. Dynamic Island / Status Bar Live Countdown Trigger
                if (settings.dynamicIslandEnabled && prayerType != PrayerType.SUNRISE) {
                    val islandLeadMinutes = settings.dynamicIslandMinutesBefore.coerceAtLeast(1)
                    val islandTriggerZonedTime = prayerZonedTime.minusMinutes(islandLeadMinutes.toLong())

                    if (islandTriggerZonedTime.isAfter(now)) {
                        val islandRequestCode = 2000 + dayOffset * 100 + prayerType.ordinal
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
                        setExactAlarm(context, alarmManager, triggerAtMillis, islandPendingIntent, islandRequestCode)
                    } else if (dayOffset == 0 && now.isBefore(prayerZonedTime)) {
                        // Already within the countdown window today: trigger island immediately
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

    private fun setExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        requestCode: Int
    ) {
        try {
            // Gold standard for prayer/alarm notifications: AlarmClockInfo is Doze-exempt on all Android versions
            val showAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode + 50000,
                showAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e2: Exception) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } catch (e3: Exception) {
                    // Fallback log
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

    fun triggerTestAlarmInSeconds(context: Context, prayerType: PrayerType, soundType: NotificationSoundType, seconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (seconds * 1000L)
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, "In $seconds s")
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, false)
            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, "Alarm Test ($seconds s)")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, alarmManager, triggerTime, pendingIntent, 99999)
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        prayerType: PrayerType,
        prayerTime: String,
        locationName: String,
        soundType: NotificationSoundType,
        delaySeconds: Int = 300
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, "$prayerTime (Snooze)")
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, false)
            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, locationName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            88888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, alarmManager, triggerTime, pendingIntent, 88888)
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
