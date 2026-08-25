package com.prayertimes.data.calendar

import com.prayertimes.data.calculator.HijriDateCalculator
import com.prayertimes.data.models.IslamicObservance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

class HijriCalendarTest {
    @Test
    fun `calendar month always has a complete 42 cell grid`() {
        val target = HijriYearMonth(1448, 1)
        val month = HijriCalendar.generate(target, adjustmentDays = 0)
        val inMonth = month.days.filter { it.isInDisplayedMonth }

        assertEquals(42, month.days.size)
        assertTrue(inMonth.size in 29..30)
        assertEquals(1, inMonth.first().hijriDate.day)
        assertEquals(inMonth.size, inMonth.last().hijriDate.day)
        assertEquals(
            inMonth.size.toLong(),
            ChronoUnit.DAYS.between(month.firstGregorianDate, month.lastGregorianDate) + 1
        )
        assertEquals(DayOfWeek.SATURDAY, month.days.first().gregorianDate.dayOfWeek)
    }

    @Test
    fun `positive adjustment moves a Hijri month earlier on Gregorian calendar`() {
        val target = HijriYearMonth(1448, 2)
        val normal = HijriCalendar.generate(target, adjustmentDays = 0)
        val adjusted = HijriCalendar.generate(target, adjustmentDays = 1)

        assertEquals(normal.firstGregorianDate.minusDays(1), adjusted.firstGregorianDate)
        assertEquals(normal.lastGregorianDate.minusDays(1), adjusted.lastGregorianDate)
    }

    @Test
    fun `month scoped adjustment expires when current Hijri month changes`() {
        val current = LocalDate.of(2026, 8, 25)
        val anchor = HijriCalendar.monthKeyFor(current)
        val nextMonthDate = HijriCalendar.generate(
            HijriCalendar.monthContaining(current, 0).plusMonths(1),
            0
        ).firstGregorianDate

        assertEquals(1, HijriCalendar.effectiveAdjustment(1, anchor, current))
        assertEquals(0, HijriCalendar.effectiveAdjustment(1, anchor, nextMonthDate))
    }

    @Test
    fun `uncertain fixed observances are not included`() {
        assertTrue(HijriDateCalculator.observancesFor(3, 12).isEmpty())
        assertTrue(HijriDateCalculator.observancesFor(7, 27).isEmpty())
        assertTrue(HijriDateCalculator.observancesFor(8, 15).contains(IslamicObservance.WHITE_DAY))
        assertFalse(
            HijriDateCalculator.observancesFor(8, 15).any { it != IslamicObservance.WHITE_DAY }
        )
    }

    @Test
    fun `Ramadan odd nights are marked without asserting a fixed Laylat al-Qadr`() {
        val twentySeventh = HijriDateCalculator.observancesFor(9, 27)
        val twentyEighth = HijriDateCalculator.observancesFor(9, 28)

        assertTrue(twentySeventh.contains(IslamicObservance.RAMADAN_ODD_NIGHT))
        assertFalse(twentyEighth.contains(IslamicObservance.RAMADAN_ODD_NIGHT))
        assertTrue(twentySeventh.contains(IslamicObservance.RAMADAN_LAST_TEN_NIGHTS))
    }

    @Test
    fun `Ramadan does not show separate white day fasting events`() {
        for (day in 13..15) {
            assertFalse(
                HijriDateCalculator.observancesFor(9, day)
                    .contains(IslamicObservance.WHITE_DAY)
            )
        }
    }

    @Test
    fun `Tashreeq does not present conflicting white day fasting guidance`() {
        val thirteenthDhulHijjah = HijriDateCalculator.observancesFor(12, 13)

        assertFalse(thirteenthDhulHijjah.contains(IslamicObservance.WHITE_DAY))
        assertTrue(thirteenthDhulHijjah.contains(IslamicObservance.TASHREEQ))
        assertTrue(thirteenthDhulHijjah.contains(IslamicObservance.FASTING_PROHIBITED))
    }
}
