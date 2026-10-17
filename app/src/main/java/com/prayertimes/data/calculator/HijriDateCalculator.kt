package com.prayertimes.data.calculator
import java.time.LocalDate
object HijriDateCalculator {
    const val CALENDAR_SOURCE = com.salati.shared.HijriDateCalculator.CALENDAR_SOURCE
    const val CALENDAR_SOURCE_AR = com.salati.shared.HijriDateCalculator.CALENDAR_SOURCE_AR
    val MONTH_NAMES_EN get() = com.salati.shared.HijriDateCalculator.MONTH_NAMES_EN
    val MONTH_NAMES_AR get() = com.salati.shared.HijriDateCalculator.MONTH_NAMES_AR
    fun convertToHijri(date: LocalDate, adjustmentDays: Int = 0) = com.salati.shared.HijriDateCalculator.convertToHijri(date.shared(),adjustmentDays)
    fun getHijriDate(date: LocalDate, adjustmentDays: Int = 0) = convertToHijri(date,adjustmentDays).formattedEn
    fun getHijriDateArabic(date: LocalDate, adjustmentDays: Int = 0) = convertToHijri(date,adjustmentDays).formattedAr
    fun observancesFor(month: Int, day: Int) = com.salati.shared.HijriDateCalculator.observancesFor(month,day)
}
