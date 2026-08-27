package com.prayertimes.data.calculator

import com.prayertimes.data.models.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CurrentPrayerResolverTest {
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
    fun fajrRemainsCurrentOnlyUntilSunrise() {
        val fajr = item(PrayerType.FAJR)
        val sunrise = item(PrayerType.SUNRISE)
        val duringFajr = CurrentPrayerResolver.resolve(
            fajr.zonedDateTime.plusMinutes(1), yesterday, today, tomorrow
        )

        assertEquals(PrayerType.FAJR, duringFajr.prayerItem.type)
        assertEquals(sunrise.zonedDateTime, duringFajr.endsAt)
        assertFalse(duringFajr.isPrayerTimeEnded)
    }

    @Test
    fun sunriseImmediatelyChangesToFajrEndedState() {
        val sunrise = item(PrayerType.SUNRISE)
        val period = CurrentPrayerResolver.resolve(
            sunrise.zonedDateTime, yesterday, today, tomorrow
        )

        assertEquals(PrayerType.FAJR, period.prayerItem.type)
        assertTrue(period.isPrayerTimeEnded)
        assertEquals(item(PrayerType.DHUHR).zonedDateTime, period.changesAt)
    }

    @Test
    fun eachDayPrayerEndsAtTheFollowingPrayer() {
        val dhuhr = item(PrayerType.DHUHR)
        val asr = item(PrayerType.ASR)
        val period = CurrentPrayerResolver.resolve(
            dhuhr.zonedDateTime, yesterday, today, tomorrow
        )

        assertEquals(PrayerType.DHUHR, period.prayerItem.type)
        assertEquals(asr.zonedDateTime, period.endsAt)
    }

    @Test
    fun ishaSpansMidnightUntilNextFajr() {
        val isha = item(PrayerType.ISHA)
        val tomorrowFajr = tomorrow.prayerItems.first { it.type == PrayerType.FAJR }
        val period = CurrentPrayerResolver.resolve(
            isha.zonedDateTime.plusHours(2), yesterday, today, tomorrow
        )

        assertEquals(PrayerType.ISHA, period.prayerItem.type)
        assertEquals(tomorrowFajr.zonedDateTime, period.endsAt)
    }
}
