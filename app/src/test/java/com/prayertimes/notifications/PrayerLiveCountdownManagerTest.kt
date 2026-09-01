package com.prayertimes.notifications

import android.app.Notification
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.prayertimes.data.models.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerLiveCountdownManagerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun android16NotificationRequestsPromotionAndUsesProgressStyle() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val now = System.currentTimeMillis()
        PrayerLiveCountdownManager.show(
            context = context,
            prayerType = PrayerType.DHUHR,
            targetEpochMillis = now + 6 * 60_000L,
            countdownStartEpochMillis = now - 3 * 60_000L,
            receiverExecutionEpochMillis = now,
            locationName = "Test location",
            isArabic = false
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = shadowOf(manager).getNotification(PrayerLiveCountdownManager.NOTIFICATION_ID)
        assertNotNull(notification)
        assertTrue(NotificationCompat.isRequestPromotedOngoing(notification))
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)

        val style = NotificationCompat.Style.extractStyleFromNotification(notification)
        assertTrue(style is NotificationCompat.ProgressStyle)
        assertEquals(33, (style as NotificationCompat.ProgressStyle).progress)
        assertEquals("Dhuhr", NotificationCompat.getShortCriticalText(notification))

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val updateIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
        }
        val updatePendingIntent = PendingIntent.getBroadcast(
            context,
            PrayerLiveCountdownManager.PROGRESS_UPDATE_REQUEST_CODE,
            updateIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull(updatePendingIntent)
        val progressAlarm = shadowOf(alarmManager).scheduledAlarms.firstOrNull {
            it.operation == updatePendingIntent
        }
        assertNotNull(progressAlarm)
        assertEquals(AlarmManager.RTC, progressAlarm!!.type)
        assertEquals("Progress refresh should be exact when access exists", 0L, progressAlarm.windowLengthMs)
        assertTrue(progressAlarm.triggerAtMs in (now + 2 * 60_000L)..(now + 4 * 60_000L))
        PrayerLiveCountdownManager.dismiss(context)
    }

    @Test
    fun progressRefreshDegradesToInexactWithoutExactAccess() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val now = System.currentTimeMillis()
        PrayerLiveCountdownManager.show(
            context = context,
            prayerType = PrayerType.ASR,
            targetEpochMillis = now + 10 * 60_000L,
            countdownStartEpochMillis = now,
            receiverExecutionEpochMillis = now,
            locationName = "Test location",
            isArabic = false
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val updatePendingIntent = PendingIntent.getBroadcast(
            context,
            PrayerLiveCountdownManager.PROGRESS_UPDATE_REQUEST_CODE,
            Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_LIVE_COUNTDOWN
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        val progressAlarm = shadowOf(alarmManager).scheduledAlarms.firstOrNull {
            it.operation == updatePendingIntent
        }
        assertNotNull(progressAlarm)
        assertEquals("Fallback progress refresh should be inexact", -1L, progressAlarm!!.windowLengthMs)
        PrayerLiveCountdownManager.dismiss(context)
    }
}
