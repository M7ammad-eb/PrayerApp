package com.prayertimes.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HijriCorrectionRetentionTest {
    @Test fun historicalCorrectionIsRetainedForCalendarBrowsing() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("prayer_fast_cache",Context.MODE_PRIVATE)
        try {
            prefs.edit().putInt("cached_hijri_offset",2).putString("cached_hijri_offset_anchor","1440-09").commit()
            val settings=PrayerPreferences.getInitialSettings(context)
            assertEquals(2,settings.hijriAdjustmentDays)
            assertEquals("1440-09",settings.hijriAdjustmentAnchorMonth)
            val month=com.prayertimes.data.calendar.HijriCalendar.generateScoped(com.prayertimes.data.calendar.HijriYearMonth(1440,9),settings.hijriAdjustmentDays,settings.hijriAdjustmentAnchorMonth)
            assertEquals(1,month.days.first {it.isInDisplayedMonth}.hijriDate.day)
        } finally {prefs.edit().clear().commit()}
    }
    @Test fun MissingAnchorCannotTurnScopedCorrectionIntoGlobalOffset() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("prayer_fast_cache",Context.MODE_PRIVATE)
        try {
            prefs.edit().putInt("cached_hijri_offset",2).remove("cached_hijri_offset_anchor").commit()
            assertEquals(0,PrayerPreferences.getInitialSettings(context).hijriAdjustmentDays)
        } finally {prefs.edit().clear().commit()}
    }
}
