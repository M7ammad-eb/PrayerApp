package com.prayertimes.data.calendar

import com.prayertimes.data.calculator.HijriDateCalculator
import com.prayertimes.data.models.HijriDate
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import java.util.Locale

data class HijriYearMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12)
    }

    fun plusMonths(amount: Int): HijriYearMonth {
        val zeroBased = year * 12L + (month - 1) + amount
        return HijriYearMonth(
            year = Math.floorDiv(zeroBased, 12L).toInt(),
            month = (Math.floorMod(zeroBased, 12L) + 1).toInt()
        )
    }

    val key: String get() = String.format(Locale.US, "%04d-%02d", year, month)
}

data class HijriCalendarDay(
    val gregorianDate: LocalDate,
    val hijriDate: HijriDate,
    val isInDisplayedMonth: Boolean
)

data class HijriCalendarMonth(
    val yearMonth: HijriYearMonth,
    val monthNameEn: String,
    val monthNameAr: String,
    val firstGregorianDate: LocalDate,
    val lastGregorianDate: LocalDate,
    val days: List<HijriCalendarDay>,
    val isSacredMonth: Boolean,
    val hasSixShawwalReminder: Boolean
)

object HijriCalendar {
    fun monthContaining(date: LocalDate, adjustmentDays: Int): HijriYearMonth {
        val hijri = HijriDateCalculator.convertToHijri(date, adjustmentDays)
        return HijriYearMonth(hijri.year, hijri.month)
    }

    fun monthKeyFor(date: LocalDate): String = monthContaining(date, adjustmentDays = 0).key

    fun effectiveAdjustment(
        storedOffset: Int,
        storedAnchorMonth: String?,
        currentDate: LocalDate
    ): Int = if (storedAnchorMonth == monthKeyFor(currentDate)) storedOffset.coerceIn(-2, 2) else 0

    fun generate(yearMonth: HijriYearMonth, adjustmentDays: Int): HijriCalendarMonth {
        val firstGregorian = gregorianDateFor(yearMonth, day = 1, adjustmentDays)
        val nextFirst = gregorianDateFor(yearMonth.plusMonths(1), day = 1, adjustmentDays)
        val lastGregorian = nextFirst.minusDays(1)
        val leadingDays = (firstGregorian.dayOfWeek.value + 1) % 7 // Saturday = 0
        val gridStart = firstGregorian.minusDays(leadingDays.toLong())

        val days = List(42) { index ->
            val gregorian = gridStart.plusDays(index.toLong())
            val hijri = HijriDateCalculator.convertToHijri(gregorian, adjustmentDays)
            HijriCalendarDay(
                gregorianDate = gregorian,
                hijriDate = hijri,
                isInDisplayedMonth = hijri.year == yearMonth.year && hijri.month == yearMonth.month
            )
        }
        val firstHijri = HijriDateCalculator.convertToHijri(firstGregorian, adjustmentDays)
        return HijriCalendarMonth(
            yearMonth = yearMonth,
            monthNameEn = firstHijri.monthNameEn,
            monthNameAr = firstHijri.monthNameAr,
            firstGregorianDate = firstGregorian,
            lastGregorianDate = lastGregorian,
            days = days,
            isSacredMonth = firstHijri.isSacredMonth,
            hasSixShawwalReminder = yearMonth.month == 10
        )
    }

    fun gregorianDateFor(yearMonth: HijriYearMonth, day: Int, adjustmentDays: Int): LocalDate {
        val hijrahDate = HijrahChronology.INSTANCE.date(yearMonth.year, yearMonth.month, day)
        val epochDay = hijrahDate.getLong(ChronoField.EPOCH_DAY)
        return LocalDate.ofEpochDay(epochDay).minusDays(adjustmentDays.toLong())
    }
}
