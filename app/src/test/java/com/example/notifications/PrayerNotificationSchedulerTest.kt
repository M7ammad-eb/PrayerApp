package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.NotificationPrayerConfig
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import com.example.data.models.UserLocation
import com.example.data.preferences.AppPrayerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Regression tests for the scheduler-state bugs where a setting change (disabling a prayer,
 * switching to SILENT, zeroing a pre-reminder, toggling live countdown) failed to cancel an
 * alarm that had already been armed under the old setting. Each test looks the alarm up the same
 * way PrayerNotificationScheduler itself does when cancelling - a bare PendingIntent.getBroadcast
 * with FLAG_NO_CREATE - since that's exactly the mechanism the bug left broken.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerNotificationSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Day offset 3 (of the scheduler's 7-day horizon) is comfortably in the future no matter what
    // time the test happens to run at, avoiding flakiness near a prayer boundary "today".
    private val dayOffset = 3
    private val prayerRequestCode = dayOffset * 100 + PrayerType.FAJR.ordinal
    private val reminderRequestCode = 1000 + dayOffset * 100 + PrayerType.FAJR.ordinal
    private val countdownRequestCode = 4000 + dayOffset * 100 + PrayerType.FAJR.ordinal

    private fun baseSettings(): AppPrayerSettings = AppPrayerSettings(
        location = UserLocation(
            name = "Riyadh", country = "Saudi Arabia",
            latitude = 24.7136, longitude = 46.6753, timeZoneId = "Asia/Riyadh"
        )
    )

    private fun existingPendingIntent(requestCode: Int, action: String): PendingIntent? {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun withFajrConfig(settings: AppPrayerSettings, config: NotificationPrayerConfig): AppPrayerSettings =
        settings.copy(prayerConfigs = settings.prayerConfigs + (PrayerType.FAJR to config))

    @Test
    fun enabledPrayerGetsAnAlarmScheduled() {
        val settings = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN)
        )

        PrayerNotificationScheduler.scheduleDailyAlarms(context, settings)

        assertNotNull(
            "Enabled prayer should have a scheduled alarm",
            existingPendingIntent(prayerRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
        )
    }

    @Test
    fun disablingPrayerCancelsThePreviouslyScheduledAlarm() {
        val enabled = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, enabled)
        assertNotNull(existingPendingIntent(prayerRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM))

        val disabled = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = false, soundType = NotificationSoundType.FULL_ATHAN)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, disabled)

        assertNull(
            "Disabling the prayer should cancel the alarm that was already scheduled",
            existingPendingIntent(prayerRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
        )
    }

    @Test
    fun silentSoundTypeStillSchedulesTheAlarm() {
        // SILENT means "no audio when it fires", not "don't schedule it" - the visual notification
        // still has to show up at prayer time.
        val settings = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.SILENT)
        )

        PrayerNotificationScheduler.scheduleDailyAlarms(context, settings)

        assertNotNull(
            "SILENT should still schedule the alarm/notification, just without audio",
            existingPendingIntent(prayerRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
        )
    }

    @Test
    fun zeroingPreReminderCancelsThePreviouslyScheduledReminder() {
        val withReminder = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN, preReminderMinutes = 10)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, withReminder)
        assertNotNull(existingPendingIntent(reminderRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM))

        val withoutReminder = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN, preReminderMinutes = 0)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, withoutReminder)

        assertNull(
            "Zeroing the pre-reminder should cancel the reminder alarm that was already scheduled",
            existingPendingIntent(reminderRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
        )
    }

    @Test
    fun disablingLiveCountdownCancelsThePreviouslyScheduledCountdownAlarm() {
        val enabled = baseSettings().copy(liveCountdownEnabled = true, liveCountdownMinutesBefore = 15)
        PrayerNotificationScheduler.scheduleDailyAlarms(context, enabled)
        assertNotNull(existingPendingIntent(countdownRequestCode, PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN))

        val disabled = baseSettings().copy(liveCountdownEnabled = false)
        PrayerNotificationScheduler.scheduleDailyAlarms(context, disabled)

        assertNull(
            "Disabling live countdown should cancel the countdown alarm that was already scheduled",
            existingPendingIntent(countdownRequestCode, PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN)
        )
    }

    // --- Alarm-priority tiering: not every alarm should claim alarm-clock priority. ---

    private fun scheduledAlarmFor(pendingIntent: PendingIntent?): ShadowAlarmManager.ScheduledAlarm? {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return shadowOf(alarmManager).scheduledAlarms.find { it.operation == pendingIntent }
    }

    @Test
    fun realPrayerAlarmUsesAlarmClockTier() {
        val settings = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, settings)

        val scheduled = scheduledAlarmFor(existingPendingIntent(prayerRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM))
        assertNotNull("Prayer alarm should be tracked by AlarmManager", scheduled)
        assertNotNull(
            "The real Athan alarm should use setAlarmClock - the one case that justifies it",
            scheduled!!.alarmClockInfo
        )
    }

    @Test
    fun preReminderIsExactButNotAlarmClockWhenPermissionIsGranted() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val settings = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN, preReminderMinutes = 10)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, settings)

        val scheduled = scheduledAlarmFor(existingPendingIntent(reminderRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM))
        assertNotNull(scheduled)
        assertNull("Reminder should not claim alarm-clock priority", scheduled!!.alarmClockInfo)
        assertEquals("Reminder should be exact when the user has granted exact-alarm access", 0L, scheduled.windowLengthMs)
    }

    @Test
    fun preReminderDegradesToInexactWithoutExactAlarmPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val settings = withFajrConfig(
            baseSettings(),
            NotificationPrayerConfig(enabled = true, soundType = NotificationSoundType.FULL_ATHAN, preReminderMinutes = 10)
        )
        PrayerNotificationScheduler.scheduleDailyAlarms(context, settings)

        val scheduled = scheduledAlarmFor(existingPendingIntent(reminderRequestCode, PrayerAlarmReceiver.ACTION_PRAYER_ALARM))
        assertNotNull(
            "Without exact-alarm permission the reminder should degrade to inexact rather than being dropped",
            scheduled
        )
        assertNull(scheduled!!.alarmClockInfo)
        assertEquals(-1L, scheduled.windowLengthMs)
    }

    @Test
    fun liveCountdownTriggerIsNeverExactOrAlarmClock() {
        // Exact-alarm permission granted or not shouldn't matter - the countdown trigger is
        // cosmetic and should never even ask for exact timing.
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val settings = baseSettings().copy(liveCountdownEnabled = true, liveCountdownMinutesBefore = 15)
        PrayerNotificationScheduler.scheduleDailyAlarms(context, settings)

        val scheduled = scheduledAlarmFor(existingPendingIntent(countdownRequestCode, PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN))
        assertNotNull(scheduled)
        assertNull("Countdown trigger should never claim alarm-clock priority", scheduled!!.alarmClockInfo)
        assertEquals("Countdown trigger should be inexact, not exact", -1L, scheduled.windowLengthMs)
    }
}
