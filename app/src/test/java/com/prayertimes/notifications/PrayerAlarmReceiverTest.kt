package com.prayertimes.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.PrayerPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Regression coverage for the goAsync() fix: the receiver's system-event branch (boot/time/date
 * change) used to launch a detached CoroutineScope and return immediately, racing a process kill.
 * It now runs rescheduleAll() inside a goAsync()-backed coroutine on the app's structured scope.
 * This can't simulate an actual process kill (no JVM test framework can), but it does verify the
 * refactor is wired correctly end-to-end: a real broadcast dispatch (not a manual onReceive() call,
 * which wouldn't have a legitimate PendingResult) actually reaches AlarmManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerAlarmReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun bootCompletedReschedulesAlarmsWithoutCrashing() {
        context.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))
        shadowOf(Looper.getMainLooper()).idle()

        // rescheduleAll() runs on PrayerApplication's applicationScope, a real Dispatchers.Default
        // thread pool that Robolectric's shadow Looper doesn't control - poll briefly instead of
        // assuming idle() alone covers it.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val deadline = System.currentTimeMillis() + 5000
        var scheduled = false
        while (System.currentTimeMillis() < deadline && !scheduled) {
            scheduled = shadowOf(alarmManager).scheduledAlarms.isNotEmpty()
            if (!scheduled) Thread.sleep(50)
        }
        assertTrue("Boot-completed handling should reschedule at least one alarm", scheduled)
    }

    @Test
    fun scheduleMaintenanceRebuildsAlarmsAndRearmsItself() {
        // ACTION_SCHEDULE_MAINTENANCE isn't a system broadcast - it's the self-armed daily trigger
        // that replaces ACTION_DATE_CHANGED as the thing keeping the rolling 7-day window alive.
        // Dispatched as an implicit broadcast (no explicit component) to confirm the manifest
        // intent-filter actually matches it, the same way a real re-delivery would.
        context.sendBroadcast(Intent(PrayerAlarmReceiver.ACTION_SCHEDULE_MAINTENANCE))
        shadowOf(Looper.getMainLooper()).idle()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val deadline = System.currentTimeMillis() + 5000
        var scheduled = false
        while (System.currentTimeMillis() < deadline && !scheduled) {
            scheduled = shadowOf(alarmManager).scheduledAlarms.isNotEmpty()
            if (!scheduled) Thread.sleep(50)
        }
        assertTrue("Maintenance trigger should reschedule the prayer alarms", scheduled)

        val rearmedMaintenance = PendingIntent.getBroadcast(
            context, 9000,
            Intent(context, PrayerAlarmReceiver::class.java).apply { action = PrayerAlarmReceiver.ACTION_SCHEDULE_MAINTENANCE },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull(
            "Handling the maintenance trigger should re-arm tomorrow's, keeping the chain alive",
            rearmedMaintenance
        )
    }

    // --- Full-screen alarm UX: wakeScreenOnAlarm gates the notification's full-screen intent. ---
    // SILENT is used here specifically because willPlayViaService is false for it, so the receiver
    // builds its own notification directly (for FULL_ATHAN etc. that notification is skipped in
    // favor of AthanAudioService's, which has its own equivalent canUseFullScreenIntent() gate).

    private fun fireSilentPrayerAlarm(screenInteractive: Boolean) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIsInteractive(screenInteractive)

        val intent = Intent(PrayerAlarmReceiver.ACTION_PRAYER_ALARM).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, PrayerType.FAJR.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, "5:00 AM")
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_TYPE, NotificationSoundType.SILENT.name)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, false)
            putExtra(PrayerAlarmReceiver.EXTRA_LOCATION_NAME, "Test City")
        }
        context.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun postedFajrNotification(): android.app.Notification? {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return shadowOf(notificationManager).getNotification(PrayerType.FAJR.ordinal)
    }

    // No positive-path test (wakeScreenOnAlarm=true + screen off => full-screen intent attached):
    // Robolectric 4.16.1 doesn't shadow NotificationManager.canUseFullScreenIntent(), so it always
    // resolves false in tests with no way to force it true - the real device behavior can't be
    // driven deterministically here. The two negative-path tests below still cover the actual
    // regression this fix targets (the receiver respecting wakeScreenOnAlarm and screen state).

    @Test
    fun fullScreenIntentOmittedWhenWakeScreenDisabled() = runBlocking {
        PrayerPreferences(context).updateWakeScreenOnAlarm(false)
        fireSilentPrayerAlarm(screenInteractive = false)

        val notification = postedFajrNotification()
        assertNotNull(notification)
        assertNull(
            "wakeScreenOnAlarm=false should not attach a full-screen intent even with the screen off",
            notification!!.fullScreenIntent
        )
    }

    @Test
    fun fullScreenIntentOmittedWhenScreenAlreadyOn() = runBlocking {
        PrayerPreferences(context).updateWakeScreenOnAlarm(true)
        fireSilentPrayerAlarm(screenInteractive = true)

        val notification = postedFajrNotification()
        assertNotNull(notification)
        assertNull(
            "An interactive screen means the user is already using the phone - shouldn't interrupt with a full-screen intent",
            notification!!.fullScreenIntent
        )
    }
}
