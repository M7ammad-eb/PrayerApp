package com.prayertimes.data.calculator

import com.prayertimes.data.models.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NextPrayerResolverTest {
    private val zone = ZoneId.of("Asia/Riyadh")
    private val date = LocalDate.of(2026, 8, 27)

    private fun schedule(day: LocalDate) = PrayerTimesCalculator.calculateDailySchedule(
        date = day,
        latitude = 21.4225,
        longitude = 39.8262,
        zoneId = zone
    )

    private val yesterday by lazy { schedule(date.minusDays(1)) }
    private val today by lazy { schedule(date) }
    private val tomorrow by lazy { schedule(date.plusDays(1)) }

    private fun item(type: PrayerType) = today.prayerItems.first { it.type == type }

    @Test
    fun duringFajrSunriseIsNext() {
        val fajr = item(PrayerType.FAJR)
        val sunrise = item(PrayerType.SUNRISE)

        val period = NextPrayerResolver.resolve(
            fajr.zonedDateTime.plusMinutes(1), yesterday, today, tomorrow
        )

        assertEquals(PrayerType.SUNRISE, period.prayerItem.type)
        assertEquals(sunrise.zonedDateTime, period.prayerItem.zonedDateTime)
    }

    @Test
    fun afterSunriseDhuhrIsNext() {
        val sunrise = item(PrayerType.SUNRISE)
        val period = NextPrayerResolver.resolve(
            sunrise.zonedDateTime.plusMinutes(1), yesterday, today, tomorrow
        )

        assertEquals(PrayerType.DHUHR, period.prayerItem.type)
    }

    @Test
    fun afterIshaTomorrowFajrIsNext() {
        val isha = item(PrayerType.ISHA)
        val tomorrowFajr = tomorrow.prayerItems.first { it.type == PrayerType.FAJR }

        val period = NextPrayerResolver.resolve(
            isha.zonedDateTime.plusMinutes(1), yesterday, today, tomorrow
        )

        assertEquals(tomorrowFajr.zonedDateTime, period.prayerItem.zonedDateTime)
        assertTrue(period.isNextDayFajr)
    }
}
