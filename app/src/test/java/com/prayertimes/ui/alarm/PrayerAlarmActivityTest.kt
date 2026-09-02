package com.prayertimes.ui.alarm

import android.content.Intent
import android.os.Looper
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.notifications.PrayerAlarmReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerAlarmActivityTest {

    private fun launch(prayerType: PrayerType): PrayerAlarmActivity {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = PrayerAlarmActivity.createIntent(
            context = context,
            prayerType = prayerType,
            prayerTime = "12:00",
            locationName = "Test",
            soundType = NotificationSoundType.SILENT
        )
        return Robolectric.buildActivity(PrayerAlarmActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .get()
    }

    @Test
    fun keepScreenOnIsClearedAfterFiveMinutesWithoutFinishingActivity() {
        val activity = launch(PrayerType.DHUHR)
        assertTrue(
            activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        )

        shadowOf(Looper.getMainLooper()).idleFor(
            Duration.ofMillis(PrayerAlarmActivity.KEEP_SCREEN_ON_MAX_MILLIS)
        )

        assertFalse(
            activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        )
        assertFalse(activity.isFinishing)
        activity.finish()
    }

    @Test
    fun fajrScreenFinishesAtSunriseBoundary() {
        val activity = launch(PrayerType.FAJR)
        activity.sendBroadcast(boundaryIntent(PrayerType.SUNRISE))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.isFinishing)
        activity.finish()
    }

    @Test
    fun ishaScreenFinishesAtNextFajrBoundary() {
        val activity = launch(PrayerType.ISHA)
        activity.sendBroadcast(boundaryIntent(PrayerType.FAJR))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.isFinishing)
        activity.finish()
    }

    @Test
    fun samePrayerBoundaryDoesNotCloseNewAlarmThatRacedBoundaryDelivery() {
        val activity = launch(PrayerType.DHUHR)
        activity.sendBroadcast(boundaryIntent(PrayerType.DHUHR))
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(activity.isFinishing)
        activity.finish()
    }

    private fun boundaryIntent(prayerType: PrayerType) =
        Intent(PrayerAlarmReceiver.ACTION_PRAYER_BOUNDARY_REACHED).apply {
            setPackage(ApplicationProvider.getApplicationContext<android.content.Context>().packageName)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.name)
        }
}
