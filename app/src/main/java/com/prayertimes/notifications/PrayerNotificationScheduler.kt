package com.prayertimes.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prayertimes.MainActivity
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.data.preferences.PrayerPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PrayerNotificationScheduler {

    // Suspend rather than launching its own detached scope - callers (notably
    // PrayerAlarmReceiver's boot/time-change handling) need to run this inside their own
    // goAsync()-backed coroutine so the work is guaranteed to finish before the receiver's
    // lifetime is up, instead of racing a process kill.
    suspend fun rescheduleAll(context: Context) {
        val prefs = PrayerPreferences(context)
        val settings = prefs.settingsFlow.first()
        scheduleDailyAlarms(context, settings)
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
                val config = settings.prayerConfigs[prayerType]

                val prayerZonedTime = item.zonedDateTime
                val prayerFormatted = item.time.format(timeFormatter)
                val prayerEpochMillis = prayerZonedTime.toInstant().toEpochMilli()

                val requestCode = dayOffset * 100 + prayerType.ordinal
                val reminderRequestCode = 1000 + dayOffset * 100 + prayerType.ordinal
                val countdownRequestCode = 4000 + dayOffset * 100 + prayerType.ordinal
                val widgetBoundaryRequestCode = 6000 + dayOffset * 100 + prayerType.ordinal

                // Independent widget boundary. Every calculated period is represented, including
                // Sunrise, regardless of whether its Athan notification (or all notifications) is
                // disabled. A separate action and request-code range prevent any PendingIntent
                // collision with Athan, reminder, countdown, or maintenance alarms.
                if (prayerZonedTime.isAfter(now)) {
                    val widgetBoundaryIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_WIDGET_PRAYER_BOUNDARY
                    }
                    val widgetBoundaryPendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetBoundaryRequestCode,
                        widgetBoundaryIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarmWithFallback(
                        alarmManager,
                        prayerEpochMillis,
                        widgetBoundaryPendingIntent,
                        widgetBoundaryRequestCode
                    )
                } else {
                    cancelAlarm(
                        context,
                        alarmManager,
                        widgetBoundaryRequestCode,
                        PrayerAlarmReceiver.ACTION_WIDGET_PRAYER_BOUNDARY
                    )
                }

                // 1. Exact Prayer Time Alarm (Athan). Every branch here is declarative - cancel
                // whatever shouldn't exist rather than just skipping creation - so that disabling a
                // prayer (or a setting change that makes it newly ineligible) reliably removes an
                // alarm that was already scheduled from before, instead of leaving it armed.
                // SILENT is deliberately still scheduled: it only means "no audio when this fires"
                // (see PrayerAlarmReceiver), not "don't schedule at all" - the visual notification
                // still needs to show up.
                if (config != null && config.enabled && prayerZonedTime.isAfter(now)) {
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

                    setAlarmClockAlarm(context, alarmManager, prayerEpochMillis, pendingIntent, requestCode)
                } else {
                    cancelAlarm(context, alarmManager, requestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
                }

                // 2. Pre-prayer reminder alarm if configured
                val reminderZonedTime = prayerZonedTime.minusMinutes((config?.preReminderMinutes ?: 0).toLong())
                if (config != null && config.enabled && config.preReminderMinutes > 0 && reminderZonedTime.isAfter(now)) {
                    val reminderIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerFormatted)
                        putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, NotificationSoundType.DEVICE_DEFAULT.name)
                        putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, true)
                        putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, settings.location.name)
                    }

                    val reminderPendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminderRequestCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setReminderAlarm(alarmManager, reminderZonedTime.toInstant().toEpochMilli(), reminderPendingIntent, reminderRequestCode)
                } else {
                    cancelAlarm(context, alarmManager, reminderRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
                }

                // 3. Live Athan countdown trigger (standard Android Live Update notification)
                if (settings.liveCountdownEnabled && prayerType != PrayerType.SUNRISE) {
                    val leadMinutes = settings.liveCountdownMinutesBefore.coerceIn(1, 180)
                    val countdownStartTime = prayerZonedTime.minusMinutes(leadMinutes.toLong())
                    if (countdownStartTime.isAfter(now)) {
                        val countdownIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
                            putExtra(PrayerAlarmReceiver.EXTRA_TARGET_MILLIS, prayerEpochMillis)
                            putExtra(
                                PrayerAlarmReceiver.EXTRA_INTENDED_TRIGGER_MILLIS,
                                countdownStartTime.toInstant().toEpochMilli()
                            )
                            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, settings.location.name)
                        }
                        val countdownPendingIntent = PendingIntent.getBroadcast(
                            context,
                            countdownRequestCode,
                            countdownIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setExactAlarmWithFallback(
                            alarmManager,
                            countdownStartTime.toInstant().toEpochMilli(),
                            countdownPendingIntent,
                            countdownRequestCode
                        )
                    } else if (dayOffset == 0 && prayerZonedTime.isAfter(now)) {
                        // Already inside the countdown window right now (e.g. the feature was just
                        // turned on, or the app restarted mid-window) - show it immediately instead
                        // of waiting for tomorrow's alarm.
                        PrayerLiveCountdownManager.show(
                            context = context,
                            prayerType = prayerType,
                            targetEpochMillis = prayerEpochMillis,
                            countdownStartEpochMillis = countdownStartTime.toInstant().toEpochMilli(),
                            receiverExecutionEpochMillis = System.currentTimeMillis(),
                            locationName = settings.location.name,
                            isArabic = settings.language.resolveIsArabic(context)
                        )
                        cancelAlarm(context, alarmManager, countdownRequestCode, PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN)
                    } else {
                        cancelAlarm(context, alarmManager, countdownRequestCode, PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN)
                    }
                } else {
                    cancelAlarm(context, alarmManager, countdownRequestCode, PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN)
                }

            }
        }

        scheduleMaintenanceAlarm(context, alarmManager)
    }

    // PendingIntent matching only considers action/component (not extras), so a bare lookup
    // intent sharing the requestCode and action used at schedule time is enough to find and cancel
    // whatever was armed there - whether it's a prayer alarm, a pre-reminder, or a countdown.
    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int, action: String) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            this.action = action
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

    private const val REQUEST_CODE_MAINTENANCE = 9000
    private const val MAINTENANCE_INTERVAL_MILLIS = 24L * 60 * 60 * 1000

    // ACTION_DATE_CHANGED is not on Android's implicit-broadcast exemption list for manifest
    // receivers (unlike BOOT_COMPLETED/TIME_SET/TIMEZONE_CHANGED - see
    // developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions),
    // so it can't be relied on as the sole daily trigger that replenishes the rolling 7-day alarm
    // window: an app that's never reopened, rebooted, or timezone/clock-changed for 8+ days could
    // silently run out of scheduled Athan alarms. This self-arms an explicit-component alarm
    // ~24h out that re-runs the full scheduling pass (which re-arms the next one) - independent of
    // any implicit-broadcast restriction. It's inexact and non-critical: firing a few hours late is
    // invisible against the 7-day buffer, so it costs nothing to keep this loose.
    private fun scheduleMaintenanceAlarm(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_SCHEDULE_MAINTENANCE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MAINTENANCE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = System.currentTimeMillis() + MAINTENANCE_INTERVAL_MILLIS
        setInexactAlarm(alarmManager, triggerAtMillis, pendingIntent, REQUEST_CODE_MAINTENANCE)
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
                cancelAlarm(context, alarmManager, requestCode, "com.salati.prayertimes.ACTION_DYNAMIC_ISLAND_COUNTDOWN")
            }
        }
    }

    // Three scheduling tiers, matched to how important each event actually is - not every alarm
    // deserves alarm-clock priority, which Android reserves for genuinely user-critical timing and
    // which affects Doze/power-management budget system-wide.

    // Tier 1: the real Athan/prayer alarm - the app's core exact-timing functionality, and the one
    // case that justifies alarm-clock priority. setAlarmClock() is still an exact-alarm API, so it
    // must be guarded by canScheduleExactAlarms() even though USE_EXACT_ALARM is normally granted
    // automatically. The check also protects sideloaded/OEM builds with unusual permission state.
    private fun setAlarmClockAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        requestCode: Int
    ) {
        if (!canScheduleExactAlarms(alarmManager)) {
            setInexactAlarm(alarmManager, triggerAtMillis, pendingIntent, requestCode)
            return
        }

        try {
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
        } catch (e: SecurityException) {
            // Access can theoretically change between the check and API call. Never drop the
            // Athan completely: retain an inexact alarm as the last-resort fallback.
            setInexactAlarm(alarmManager, triggerAtMillis, pendingIntent, requestCode)
        } catch (e: RuntimeException) {
            // A few OEM AlarmManager implementations reject otherwise valid alarm-clock calls.
            setInexactAlarm(alarmManager, triggerAtMillis, pendingIntent, requestCode)
        }
    }

    // Tier 2: pre-reminders, widget boundaries, and Live Countdown starts. They need exact timing
    // when access is available but do not claim alarm-clock priority. If access is absent or an
    // OEM rejects the call, retain an inexact allow-while-idle alarm rather than dropping it.
    private fun setReminderAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        requestCode: Int
    ) {
        setExactAlarmWithFallback(alarmManager, triggerAtMillis, pendingIntent, requestCode)
    }

    private fun setExactAlarmWithFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        requestCode: Int
    ) {
        if (canScheduleExactAlarms(alarmManager)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                return
            } catch (e: SecurityException) {
                // Permission revoked between the check and the call (or an OEM quirk) - degrade to
                // inexact rather than dropping the reminder entirely.
            } catch (e: RuntimeException) {
                // Gracefully handle OEM AlarmManager implementations that reject an otherwise
                // valid exact alarm.
            }
        }
        setInexactAlarm(alarmManager, triggerAtMillis, pendingIntent, requestCode)
    }

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // Tier 3: non-critical maintenance and the exact-alarm fallback path.
    private fun setInexactAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        requestCode: Int
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PrayerNotifScheduler", "Failed to schedule alarm (requestCode=$requestCode) after exhausting all fallbacks", e)
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
        val isArabic = PrayerPreferences.getInitialSettings(context).language.resolveIsArabic(context)
        val targetMillis = System.currentTimeMillis() + minutesFromNow * 60 * 1000L
        PrayerLiveCountdownManager.show(
            context = context,
            prayerType = PrayerType.DHUHR,
            targetEpochMillis = targetMillis,
            countdownStartEpochMillis = System.currentTimeMillis(),
            receiverExecutionEpochMillis = System.currentTimeMillis(),
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
        setAlarmClockAlarm(context, alarmManager, triggerTime, pendingIntent, 99999)
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
        setAlarmClockAlarm(context, alarmManager, triggerTime, pendingIntent, 88888)
    }

}
