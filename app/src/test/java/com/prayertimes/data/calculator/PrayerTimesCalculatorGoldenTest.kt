package com.prayertimes.data.calculator

import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.JuristicMethod
import com.prayertimes.data.models.PrayerTimeAdjustments
import com.prayertimes.data.models.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Regression tests for the correctness bugs fixed in PrayerTimesCalculator. Unlike pure ordering
 * checks (Fajr < Sunrise < Dhuhr < ...), these encode the specific numeric relationship each fix
 * is supposed to hold, so a systematically-shifted schedule cannot pass by accident.
 */
class PrayerTimesCalculatorGoldenTest {

    private val london = ZoneId.of("Europe/London")
    private val newYork = ZoneId.of("America/New_York")
    private val riyadh = ZoneId.of("Asia/Riyadh")

    @Test
    fun highLatitudeAngleBasedRuleUsesFullNightPortion() {
        // London near the summer solstice: MWL's 18 degree Fajr angle regularly needs the
        // angle-based high-latitude correction here. The correction must equal
        // (angle / 60) * full night duration, not half of it.
        val date = LocalDate.of(2026, 6, 21)
        val schedule = PrayerTimesCalculator.calculateDailySchedule(
            date = date,
            latitude = 51.5072,
            longitude = -0.1276,
            zoneId = london,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.ANGLE_BASED
        )

        val sunriseMin = schedule.sunrise.toSecondOfDay() / 60.0
        // MWL has no custom maghrib angle, so maghrib == astronomical sunset.
        val sunsetMin = schedule.maghrib.toSecondOfDay() / 60.0
        val nightDurationMin = (24 * 60) - (sunsetMin - sunriseMin)

        val expectedFajrPortionMin = (CalculationMethod.MUSLIM_WORLD_LEAGUE.fajrAngle / 60.0) * nightDurationMin
        val actualFajrPortionMin = sunriseMin - (schedule.fajr.toSecondOfDay() / 60.0)

        assertTrue(
            "Expected the angle-based rule to actually be constraining Fajr in this scenario " +
                "(actual portion $actualFajrPortionMin should be close to the full-night formula)",
            actualFajrPortionMin > 0
        )
        assertEquals(expectedFajrPortionMin, actualFajrPortionMin, 1.5)
    }

    @Test
    fun dstTransitionDayDoesNotShiftDhuhrByAnHour() {
        // 2024-03-10 is the US spring-forward DST transition (clocks jump 02:00 -> 03:00). Solar
        // noon drifts by well under a couple of minutes per day in March, so if the transition day
        // is computed correctly, Dhuhr should land within a few minutes of the following day's.
        // The old bug (freezing the day's offset at midnight) shows up here as a ~59-61 minute gap.
        val transitionDay = LocalDate.of(2024, 3, 10)
        val dayAfter = LocalDate.of(2024, 3, 11)

        val scheduleTransition = PrayerTimesCalculator.calculateDailySchedule(
            date = transitionDay, latitude = 40.7128, longitude = -74.0060, zoneId = newYork
        )
        val scheduleDayAfter = PrayerTimesCalculator.calculateDailySchedule(
            date = dayAfter, latitude = 40.7128, longitude = -74.0060, zoneId = newYork
        )

        val diffMinutes = abs(
            Duration.between(scheduleTransition.dhuhr, scheduleDayAfter.dhuhr).toMinutes()
        )
        assertTrue(
            "Dhuhr shifted by ${diffMinutes}min across the DST transition day - expected < 5min " +
                "of equation-of-time drift, not a ~60min frozen-offset error",
            diffMinutes < 5
        )
    }

    @Test
    fun islamicMidnightUsesTomorrowsActualSunrise() {
        // Pick a date where day length is changing quickly (near the equinox) so an approximation
        // based on *today's* sunrise would measurably diverge from one based on tomorrow's actual
        // sunrise.
        val date = LocalDate.of(2026, 3, 5)
        val schedule = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = 51.5072, longitude = -0.1276, zoneId = london
        )
        val tomorrowSchedule = PrayerTimesCalculator.calculateDailySchedule(
            date = date.plusDays(1), latitude = 51.5072, longitude = -0.1276, zoneId = london
        )

        val maghribZoned = date.atTime(schedule.maghrib).atZone(london)
        val tomorrowSunriseZoned = date.plusDays(1).atTime(tomorrowSchedule.sunrise).atZone(london)
        val expectedMidnight = maghribZoned.plus(Duration.between(maghribZoned, tomorrowSunriseZoned).dividedBy(2))

        // Compare as time-of-day only: islamicMidnight legitimately rolls past 00:00 into the
        // next calendar date, so re-anchoring it to `date` before diffing would itself introduce
        // a spurious ~24h offset.
        val diffMinutes = abs(
            Duration.between(schedule.islamicMidnight, expectedMidnight.toLocalTime()).toMinutes()
        )
        assertTrue(
            "islamicMidnight should match halfway between maghrib and tomorrow's actual sunrise " +
                "(off by ${diffMinutes}min)",
            diffMinutes <= 2
        )
    }

    @Test
    fun ummAlQuraIshaIsOneTwentyMinutesDuringRamadanAndNinetyOtherwise() {
        val outsideRamadan = LocalDate.of(2026, 1, 15)
        val duringRamadan = LocalDate.of(2027, 2, 20) // expected to fall within Ramadan 1448H

        val normalSchedule = PrayerTimesCalculator.calculateDailySchedule(
            date = outsideRamadan, latitude = 21.4225, longitude = 39.8262, zoneId = riyadh,
            method = CalculationMethod.UMM_AL_QURA
        )
        val ramadanSchedule = PrayerTimesCalculator.calculateDailySchedule(
            date = duringRamadan, latitude = 21.4225, longitude = 39.8262, zoneId = riyadh,
            method = CalculationMethod.UMM_AL_QURA
        )

        // Hard requirement, not a soft skip: if these fixture dates ever stop landing where
        // expected in the Hijri calendar, the test must fail loudly rather than silently pass.
        assertTrue("outsideRamadan fixture must not itself be in Ramadan", outsideRamadan.let {
            PrayerTimesCalculator.calculateDailySchedule(
                date = it, latitude = 21.4225, longitude = 39.8262, zoneId = riyadh
            ).hijriDate?.month != 9
        })
        assertEquals(9, ramadanSchedule.hijriDate?.month)

        val normalGapMinutes = Duration.between(
            outsideRamadan.atTime(normalSchedule.maghrib), outsideRamadan.atTime(normalSchedule.isha)
        ).toMinutes()
        val ramadanGapMinutes = Duration.between(
            duringRamadan.atTime(ramadanSchedule.maghrib), duringRamadan.atTime(ramadanSchedule.isha)
        ).toMinutes()

        assertEquals(90L, normalGapMinutes)
        assertEquals(120L, ramadanGapMinutes)
    }

    @Test
    fun hanafiAsrIsLaterThanStandardAsr() {
        val date = LocalDate.of(2026, 6, 15)
        val standard = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = 24.7136, longitude = 46.6753, zoneId = riyadh,
            juristicMethod = JuristicMethod.STANDARD
        )
        val hanafi = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = 24.7136, longitude = 46.6753, zoneId = riyadh,
            juristicMethod = JuristicMethod.HANAFI
        )

        assertTrue(
            "Hanafi's shadow-factor-2 Asr should fall after Standard's shadow-factor-1 Asr",
            hanafi.asr.isAfter(standard.asr)
        )
    }

    @Test
    fun manualAdjustmentsShiftByExactlyTheConfiguredMinutes() {
        val date = LocalDate.of(2026, 6, 15)
        val baseline = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = 24.7136, longitude = 46.6753, zoneId = riyadh
        )
        val adjusted = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = 24.7136, longitude = 46.6753, zoneId = riyadh,
            adjustments = PrayerTimeAdjustments(fajr = 7, isha = -5)
        )

        val fajrDiff = Duration.between(
            date.atTime(baseline.fajr), date.atTime(adjusted.fajr)
        ).toMinutes()
        val ishaDiff = Duration.between(
            date.atTime(baseline.isha), date.atTime(adjusted.isha)
        ).toMinutes()

        assertEquals(7L, fajrDiff)
        assertEquals(-5L, ishaDiff)
    }

    // --- UTC day-rollover regression: a signed UTC-relative hour (e.g. Fajr computed as -5.2,
    // meaning 18:48 UTC the *previous* UTC day) used to get wrapped into [0,24) before being
    // anchored to an instant, silently discarding which UTC calendar day the event actually fell
    // on. The displayed LocalTime still looked correct, which is exactly why plain ordering/
    // invariant checks didn't catch it - these assert the full ZonedDateTime's calendar date. ---

    private fun assertAllPrayersOnRequestedDate(date: LocalDate, zoneId: ZoneId, latitude: Double, longitude: Double) {
        val schedule = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = latitude, longitude = longitude, zoneId = zoneId
        )
        schedule.prayerItems.forEach { item ->
            assertEquals(
                "${item.type} zonedDateTime landed on ${item.zonedDateTime.toLocalDate()} instead of " +
                    "the requested $date",
                date, item.zonedDateTime.toLocalDate()
            )
        }
    }

    @Test
    fun tokyoPrayersStayOnTheRequestedCalendarDate() {
        assertAllPrayersOnRequestedDate(
            date = LocalDate.of(2026, 6, 21), zoneId = ZoneId.of("Asia/Tokyo"),
            latitude = 35.6762, longitude = 139.6503
        )
    }

    @Test
    fun sydneyPrayersStayOnTheRequestedCalendarDate() {
        assertAllPrayersOnRequestedDate(
            date = LocalDate.of(2026, 6, 21), zoneId = ZoneId.of("Australia/Sydney"),
            latitude = -33.8688, longitude = 151.2093
        )
    }

    @Test
    fun aucklandPrayersStayOnTheRequestedCalendarDate() {
        assertAllPrayersOnRequestedDate(
            date = LocalDate.of(2026, 6, 21), zoneId = ZoneId.of("Pacific/Auckland"),
            latitude = -36.8485, longitude = 174.7633
        )
    }

    @Test
    fun honoluluPrayersStayOnTheRequestedCalendarDate() {
        assertAllPrayersOnRequestedDate(
            date = LocalDate.of(2026, 12, 21), zoneId = ZoneId.of("Pacific/Honolulu"),
            latitude = 21.3069, longitude = -157.8583
        )
    }

    @Test
    fun tokyoFajrUtcInstantFallsOnThePreviousUtcCalendarDay() {
        // Concrete numeric trace of the bug: Tokyo (UTC+9) is far enough east that MWL's 18-degree
        // Fajr angle resolves to a negative UTC-frame hour - the instant is actually still on the
        // *previous* UTC calendar day. This is the exact mechanism the wrap used to break.
        val date = LocalDate.of(2026, 6, 21)
        val zoneId = ZoneId.of("Asia/Tokyo")
        val schedule = PrayerTimesCalculator.calculateDailySchedule(
            date = date, latitude = 35.6762, longitude = 139.6503, zoneId = zoneId
        )
        val fajr = schedule.prayerItems.first { it.type == PrayerType.FAJR }

        assertEquals("Fajr should stay on the requested date in Tokyo local time", date, fajr.zonedDateTime.toLocalDate())
        assertEquals(
            "Fajr's underlying UTC instant should fall on the previous UTC calendar day at this longitude",
            date.minusDays(1), fajr.zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate()
        )
    }

    @Test
    fun cachedScheduleRefreshesPassedAndNextPrayerStatus() {
        PrayerTimesCalculator.clearCache()
        val date = LocalDate.of(2026, 8, 25)
        val beforeFajr = date.atStartOfDay(riyadh)
        val initial = PrayerTimesCalculator.calculateDailySchedule(
            date = date,
            latitude = 24.7136,
            longitude = 46.6753,
            zoneId = riyadh,
            now = beforeFajr
        )
        assertTrue(initial.prayerItems.first { it.type == PrayerType.FAJR }.isNext)

        val afterFajr = initial.prayerItems.first { it.type == PrayerType.FAJR }
            .zonedDateTime.plusMinutes(1)
        val cached = PrayerTimesCalculator.calculateDailySchedule(
            date = date,
            latitude = 24.7136,
            longitude = 46.6753,
            zoneId = riyadh,
            now = afterFajr
        )

        assertTrue(cached.prayerItems.first { it.type == PrayerType.FAJR }.isPassed)
        assertTrue(cached.prayerItems.first { it.type == PrayerType.SUNRISE }.isNext)
    }
}
