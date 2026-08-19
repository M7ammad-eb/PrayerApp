package com.example.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
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
}
