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
        cancelStaleDynamicIslandAlarms(context, alarmManager)
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

                // 3. Live Athan countdown trigger (standard Android Live Update notification)
                val countdownRequestCode = 4000 + dayOffset * 100 + prayerType.ordinal
                if (settings.liveCountdownEnabled && prayerType != PrayerType.SUNRISE) {
                    val leadMinutes = settings.liveCountdownMinutesBefore.coerceIn(1, 180)
                    val countdownStartTime = prayerZonedTime.minusMinutes(leadMinutes.toLong())
                    if (countdownStartTime.isAfter(now)) {
                        val countdownIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
                            putExtra(PrayerAlarmReceiver.EXTRA_TARGET_MILLIS, prayerEpochMillis)
                            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, settings.location.name)
                        }
                        val countdownPendingIntent = PendingIntent.getBroadcast(
                            context,
                            countdownRequestCode,
                            countdownIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setExactAlarm(context, alarmManager, countdownStartTime.toInstant().toEpochMilli(), countdownPendingIntent, countdownRequestCode)
                    } else if (dayOffset == 0 && prayerZonedTime.isAfter(now)) {
                        // Already inside the countdown window right now (e.g. the feature was just
                        // turned on, or the app restarted mid-window) - show it immediately instead
                        // of waiting for tomorrow's alarm.
                        PrayerLiveCountdownManager.show(
                            context = context,
                            prayerType = prayerType,
                            targetEpochMillis = prayerEpochMillis,
                            locationName = settings.location.name,
                            isArabic = settings.language.resolveIsArabic()
                        )
                        cancelLiveCountdownAlarm(context, alarmManager, countdownRequestCode)
                    } else {
                        cancelLiveCountdownAlarm(context, alarmManager, countdownRequestCode)
                    }
                } else {
                    cancelLiveCountdownAlarm(context, alarmManager, countdownRequestCode)
                }

            }
        }
    }

    private fun cancelLiveCountdownAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // Earlier app versions scheduled a "Dynamic Island countdown" alarm 15 minutes before each
    // prayer, under a now-removed action string and its own request-code range. Those alarms live
    // in the OS's AlarmManager, not the app, so an app update doesn't clear them - and since that
    // feature (and its action handling in PrayerAlarmReceiver) is gone, one of them firing would
    // otherwise be silently ignored by the receiver's unrecognized-action guard, but the alarm
    // itself would still wake the device for nothing. Cancel any that are still pending.
    private fun cancelStaleDynamicIslandAlarms(context: Context, alarmManager: AlarmManager) {
        for (dayOffset in 0..6) {
            for (prayerOrdinal in 0..5) {
                val requestCode = 2000 + dayOffset * 100 + prayerOrdinal
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = "com.example.ACTION_DYNAMIC_ISLAND_COUNTDOWN"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
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
                    android.util.Log.e("PrayerNotifScheduler", "Failed to schedule alarm (requestCode=$requestCode) after exhausting all fallbacks", e3)
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

    fun triggerTestLiveCountdown(context: Context, minutesFromNow: Int = 2) {
        val isArabic = PrayerPreferences.getInitialSettings(context).language.resolveIsArabic()
        val targetMillis = System.currentTimeMillis() + minutesFromNow * 60 * 1000L
        PrayerLiveCountdownManager.show(
            context = context,
            prayerType = PrayerType.DHUHR,
            targetEpochMillis = targetMillis,
            locationName = "Live Countdown Test",
            isArabic = isArabic
        )
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

}
