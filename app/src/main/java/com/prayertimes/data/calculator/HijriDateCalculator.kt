package com.prayertimes.data.calculator

import com.prayertimes.data.models.HijriDate
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

        val (eventEn, eventAr) = getIslamicEvent(month, day)
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
            islamicEvent = eventEn,
            islamicEventAr = eventAr,
            isWhiteDay = isWhiteDay,
            isSacredMonth = isSacredMonth
        )
    }

    private fun getIslamicEvent(month: Int, day: Int): Pair<String?, String?> {
        return when {
            month == 1 && day == 1 -> "Islamic New Year (1 Muharram)" to "رأس السنة الهجرية"
            month == 1 && day == 9 -> "Tasu'a (Fasting Recommended)" to "يوم تاسوعاء"
            month == 1 && day == 10 -> "Day of Ashura (Fasting Recommended)" to "يوم عاشوراء"
            month == 3 && day == 12 -> "Mawlid an-Nabi" to "المولد النبوي الشريف"
            month == 7 && day == 27 -> "Isra and Mi'raj" to "ذكرى الإسراء والمعراج"
            month == 8 && day == 15 -> "Mid-Sha'ban (Laylat al-Bara'at)" to "ليلة النصف من شعبان"
            month == 9 && day == 1 -> "First Day of Ramadan (Holy Month of Fasting)" to "أول أيام شهر رمضان المبارك"
            month == 9 && day == 27 -> "Laylat al-Qadr (Night of Decree)" to "ليلة القدر المباركة"
            month == 9 -> "Ramadan Mubarak" to "شهر رمضان المبارك"
            month == 10 && day == 1 -> "Eid al-Fitr (1st Day)" to "عيد الفطر المبارك"
            month == 10 && day == 2 -> "Eid al-Fitr (2nd Day)" to "ثاني أيام عيد الفطر"
            month == 10 && day == 3 -> "Eid al-Fitr (3rd Day)" to "ثالث أيام عيد الفطر"
            month == 12 && day == 8 -> "Day of Tarwiyah (Hajj)" to "يوم التروية"
            month == 12 && day == 9 -> "Day of Arafah (Hajj Peak & Fasting)" to "يوم عرفة"
            month == 12 && day == 10 -> "Eid al-Adha (Day of Sacrifice)" to "عيد الأضحى المبارك"
            month == 12 && day in 11..13 -> "Days of Tashreeq" to "أيام التشريق"
            month == 12 && day in 1..9 -> "First 10 Days of Dhu al-Hijjah" to "عشر ذي الحجة المباركة"
            else -> null to null
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
