package com.prayertimes.data.calendar

import com.prayertimes.data.calculator.HijriDateCalculator
import com.prayertimes.data.models.HijriDate
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import com.prayertimes.data.calculator.shared
import com.prayertimes.data.calculator.android
import java.util.Locale

data class HijriYearMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12)
    }

    fun plusMonths(amount: Int): HijriYearMonth {
        val result = com.salati.shared.HijriYearMonth(year,month).plusMonths(amount)
        return HijriYearMonth(result.year,result.month)
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
    fun monthContaining(date: LocalDate, adjustmentDays: Int): HijriYearMonth = com.salati.shared.HijriCalendar.monthContaining(date.shared(),adjustmentDays).let { HijriYearMonth(it.year,it.month) }
    fun monthKeyFor(date: LocalDate) = com.salati.shared.HijriCalendar.monthKeyFor(date.shared())
    fun effectiveAdjustment(storedOffset: Int, storedAnchorMonth: String?, currentDate: LocalDate) = com.salati.shared.HijriCalendar.effectiveAdjustment(storedOffset,storedAnchorMonth,currentDate.shared())
    fun prewarm(anchorDate: LocalDate, adjustmentDays: Int, previousMonths: Int = 6, nextMonths: Int = 12) = com.salati.shared.HijriCalendar.prewarm(anchorDate.shared(),adjustmentDays,previousMonths,nextMonths)
    fun generate(yearMonth: HijriYearMonth, adjustmentDays: Int): HijriCalendarMonth {
        val result = com.salati.shared.HijriCalendar.generate(com.salati.shared.HijriYearMonth(yearMonth.year,yearMonth.month),adjustmentDays)
        return HijriCalendarMonth(yearMonth,result.monthNameEn,result.monthNameAr,result.firstGregorianDate.android(),result.lastGregorianDate.android(),result.days.map { HijriCalendarDay(it.gregorianDate.android(),it.hijriDate,it.isInDisplayedMonth) },result.isSacredMonth,result.hasSixShawwalReminder)
    }
    fun gregorianDateFor(yearMonth: HijriYearMonth, day: Int, adjustmentDays: Int) = com.salati.shared.HijriCalendar.gregorianDateFor(com.salati.shared.HijriYearMonth(yearMonth.year,yearMonth.month),day,adjustmentDays).android()
}
