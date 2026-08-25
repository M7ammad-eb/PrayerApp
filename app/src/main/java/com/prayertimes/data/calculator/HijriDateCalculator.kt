package com.prayertimes.data.calculator

import com.prayertimes.data.models.HijriDate
import com.prayertimes.data.models.IslamicObservance
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField

/**
 * Hijri Date Calculator based on the authentic Umm al-Qura Calendar of Saudi Arabia (Makkah).
 * Uses Java 8+ [HijrahChronology.INSTANCE] ("Hijrah-umalqura") with an algorithmic tabular fallback.
 */
object HijriDateCalculator {

    const val CALENDAR_SOURCE = "Umm al-Qura Calendar"
    const val CALENDAR_SOURCE_AR = "تقويم أم القرى"

    val MONTH_NAMES_EN = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    val MONTH_NAMES_AR = listOf(
        "محرّم",
        "صفر",
        "ربيع الأول",
        "ربيع الثاني",
        "جمادى الأولى",
        "جمادى الثانية",
        "رجب",
        "شعبان",
        "رمضان",
        "شوّال",
        "ذو القعدة",
        "ذو الحجة"
    )

    // Sacred months (الأشهر الحرم): Muharram (1), Rajab (7), Dhu al-Qi'dah (11), Dhu al-Hijjah (12)
    private val SACRED_MONTHS = setOf(1, 7, 11, 12)

    /**
     * Converts a Gregorian [LocalDate] to a structured [HijriDate] using the Umm al-Qura calendar.
     */
    fun convertToHijri(date: LocalDate, adjustmentDays: Int = 0): HijriDate {
        return try {
            val adjustedDate = date.plusDays(adjustmentDays.toLong())
            val hijrahDate = HijrahChronology.INSTANCE.date(adjustedDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(ChronoField.YEAR)

            buildHijriDate(day, month, year)
        } catch (e: Exception) {
            // Algorithmic Tabular Umm Al-Qura / Hijri fallback
            calculateTabularHijri(date.plusDays(adjustmentDays.toLong()))
        }
    }

    fun getHijriDate(date: LocalDate, adjustmentDays: Int = 0): String {
        return convertToHijri(date, adjustmentDays).formattedEn
    }

    fun getHijriDateArabic(date: LocalDate, adjustmentDays: Int = 0): String {
        return convertToHijri(date, adjustmentDays).formattedAr
    }

    private fun buildHijriDate(day: Int, month: Int, year: Int): HijriDate {
        val monthIdx = (month - 1).coerceIn(0, 11)
        val monthNameEn = MONTH_NAMES_EN[monthIdx]
        val monthNameAr = MONTH_NAMES_AR[monthIdx]

        val formattedEn = "$day $monthNameEn $year AH"
        val formattedAr = "$day $monthNameAr $year هـ"

        val isWhiteDay = day in 13..15
        val isSacredMonth = month in SACRED_MONTHS

        return HijriDate(
            day = day,
            month = month,
            monthNameEn = monthNameEn,
            monthNameAr = monthNameAr,
            year = year,
            formattedEn = formattedEn,
            formattedAr = formattedAr,
            calendarSource = CALENDAR_SOURCE,
            observances = observancesFor(month, day),
            isWhiteDay = isWhiteDay,
            isSacredMonth = isSacredMonth
        )
    }

    fun observancesFor(month: Int, day: Int): List<IslamicObservance> = buildList {
        if (month == 1 && day == 1) add(IslamicObservance.HIJRI_NEW_YEAR)
        if (month == 1 && day == 9) add(IslamicObservance.TASUA)
        if (month == 1 && day == 10) add(IslamicObservance.ASHURA)
        // The 13th of Dhu al-Hijjah is a white day astronomically, but it is also
        // a day of Tashreeq when fasting is prohibited. Do not present conflicting
        // fasting guidance to the user.
        if (day in 13..15 && month != 9 && !(month == 12 && day == 13)) {
            add(IslamicObservance.WHITE_DAY)
        }

        if (month == 9 && day == 1) add(IslamicObservance.RAMADAN_START)
        if (month == 9 && day >= 21) add(IslamicObservance.RAMADAN_LAST_TEN_NIGHTS)
        if (month == 9 && day in setOf(21, 23, 25, 27, 29)) add(IslamicObservance.RAMADAN_ODD_NIGHT)

        if (month == 10 && day == 1) {
            add(IslamicObservance.EID_AL_FITR)
            add(IslamicObservance.FASTING_PROHIBITED)
        }

        if (month == 12 && day in 1..9) add(IslamicObservance.FIRST_TEN_DHU_AL_HIJJAH)
        if (month == 12 && day == 8) add(IslamicObservance.TARWIYAH)
        if (month == 12 && day == 9) add(IslamicObservance.ARAFAH)
        if (month == 12 && day == 10) {
            add(IslamicObservance.EID_AL_ADHA)
            add(IslamicObservance.FASTING_PROHIBITED)
        }
        if (month == 12 && day in 11..13) {
            add(IslamicObservance.TASHREEQ)
            add(IslamicObservance.FASTING_PROHIBITED)
        }
    }

    private fun calculateTabularHijri(date: LocalDate): HijriDate {
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth

        var jd = (1461 * (year + 4800 + (month - 14) / 12)) / 4 +
                (367 * (month - 2 - 12 * ((month - 14) / 12))) / 12 -
                (3 * ((year + 4900 + (month - 14) / 12) / 100)) / 4 +
                day - 32075

        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val m = (24 * l) / 709
        val d = l - (709 * m) / 24
        val y = 30 * n + j - 30

        return buildHijriDate(d, m, y)
    }
}
