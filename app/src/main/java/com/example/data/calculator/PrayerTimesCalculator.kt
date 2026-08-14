package com.example.data.calculator

import com.example.data.models.CalculationMethod
import com.example.data.models.DailyPrayerSchedule
import com.example.data.models.HighLatitudeRule
import com.example.data.models.JuristicMethod
import com.example.data.models.PrayerTimeAdjustments
import com.example.data.models.PrayerTimeItem
import com.example.data.models.PrayerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimesCalculator {

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    private fun dSin(d: Double): Double = sin(d * DEG_TO_RAD)
    private fun dCos(d: Double): Double = cos(d * DEG_TO_RAD)
    private fun dTan(d: Double): Double = tan(d * DEG_TO_RAD)
    private fun dAsin(x: Double): Double = asin(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun dAcos(x: Double): Double = acos(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun dAtan(x: Double): Double = atan(x) * RAD_TO_DEG
    private fun dAtan2(y: Double, x: Double): Double = atan2(y, x) * RAD_TO_DEG

    private fun fixAngle(a: Double): Double {
        var angle = a - 360.0 * floor(a / 360.0)
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun fixHour(h: Double): Double {
        var hour = h - 24.0 * floor(h / 24.0)
        if (hour < 0) hour += 24.0
        return hour
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunCoordinates(
        val declination: Double, // in degrees
        val equationOfTime: Double // in hours
    )

    private fun sunPosition(jd: Double): SunCoordinates {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(dAtan2(dCos(e) * dSin(l), dCos(l))) / 15.0

        val declination = dAsin(dSin(e) * dSin(l))
        val equationOfTime = q / 15.0 - fixHour(ra)

        return SunCoordinates(declination, equationOfTime)
    }

    private fun computeMidDay(timeZone: Double, lon: Double, eqt: Double): Double {
        return fixHour(12.0 + timeZone - (lon / 15.0) - eqt)
    }

    private fun computeTime(angle: Double, lat: Double, declination: Double, midDay: Double, isMorning: Boolean): Double {
        val cosT = (-dSin(angle) - dSin(lat) * dSin(declination)) / (dCos(lat) * dCos(declination))
        if (cosT < -1.0 || cosT > 1.0) {
            // Extreme latitude edge case
            return if (isMorning) midDay - 3.0 else midDay + 3.0
        }
        val t = dAcos(cosT) / 15.0
        return if (isMorning) midDay - t else midDay + t
    }

    private fun computeAsr(shadowFactor: Double, lat: Double, declination: Double, midDay: Double): Double {
        val d = abs(lat - declination)
        val angle = -dAtan(1.0 / (shadowFactor + dTan(d)))
        return computeTime(angle, lat, declination, midDay, isMorning = false)
    }

    fun calculateDailySchedule(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
        adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
        hijriAdjustmentDays: Int = 0,
        now: ZonedDateTime = ZonedDateTime.now(zoneId)
    ): DailyPrayerSchedule {
        val timeZoneOffsetHours = zoneId.rules.getOffset(date.atStartOfDay()).totalSeconds / 3600.0
        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth)

        val sun = sunPosition(jd)
        val midDay = computeMidDay(timeZoneOffsetHours, longitude, sun.equationOfTime)

        // Standard angles: Sunrise / Sunset standard zenith = 90.833° -> angle = 0.833° below horizon
        var sunriseHour = computeTime(0.833, latitude, sun.declination, midDay, isMorning = true)
        var sunsetHour = computeTime(0.833, latitude, sun.declination, midDay, isMorning = false)

        var fajrHour = computeTime(method.fajrAngle, latitude, sun.declination, midDay, isMorning = true)

        var ishaHour = if (method.ishaMinutesAfterMaghrib != null) {
            sunsetHour + (method.ishaMinutesAfterMaghrib / 60.0)
        } else {
            computeTime(method.ishaAngle, latitude, sun.declination, midDay, isMorning = false)
        }

        var maghribHour = if (method.maghribAngle != null) {
            computeTime(method.maghribAngle, latitude, sun.declination, midDay, isMorning = false)
        } else {
            sunsetHour
        }

        val asrHour = computeAsr(juristicMethod.shadowFactor, latitude, sun.declination, midDay)

        // High Latitude Adjustments
        val nightDuration = fixHour(24.0 + sunriseHour - sunsetHour)
        if (highLatitudeRule != HighLatitudeRule.NONE && nightDuration in 0.0..24.0) {
            when (highLatitudeRule) {
                HighLatitudeRule.ANGLE_BASED -> {
                    val fajrPortion = (method.fajrAngle / 60.0) * (nightDuration / 2.0)
                    if (sunriseHour - fajrHour > fajrPortion || fajrHour > sunriseHour) {
                        fajrHour = sunriseHour - fajrPortion
                    }
                    val ishaPortion = ((if (method.ishaAngle > 0) method.ishaAngle else 18.0) / 60.0) * (nightDuration / 2.0)
                    if (ishaHour - sunsetHour > ishaPortion || ishaHour < sunsetHour) {
                        ishaHour = sunsetHour + ishaPortion
                    }
                }
                HighLatitudeRule.MIDNIGHT -> {
                    val halfNight = nightDuration / 2.0
                    if (sunriseHour - fajrHour > halfNight) fajrHour = sunriseHour - halfNight
                    if (ishaHour - sunsetHour > halfNight) ishaHour = sunsetHour + halfNight
                }
                HighLatitudeRule.ONE_SEVENTH -> {
                    val seventhNight = nightDuration / 7.0
                    if (sunriseHour - fajrHour > seventhNight) fajrHour = sunriseHour - seventhNight
                    if (ishaHour - sunsetHour > seventhNight) ishaHour = sunsetHour + seventhNight
                }
                HighLatitudeRule.NONE -> {}
            }
        }

        // Convert to LocalTime and apply manual adjustments
        val fajrTime = decimalToLocalTime(fajrHour).plusMinutes(adjustments.fajr.toLong())
        val sunriseTime = decimalToLocalTime(sunriseHour).plusMinutes(adjustments.sunrise.toLong())
        val dhuhrTime = decimalToLocalTime(midDay).plusMinutes(adjustments.dhuhr.toLong())
        val asrTime = decimalToLocalTime(asrHour).plusMinutes(adjustments.asr.toLong())
        val maghribTime = decimalToLocalTime(maghribHour).plusMinutes(adjustments.maghrib.toLong())
        val ishaTime = decimalToLocalTime(ishaHour).plusMinutes(adjustments.isha.toLong())

        // Calculate Islamic Midnight (halfway between sunset and next sunrise)
        // and Last Third of Night (Qiyam / Tahajjud time)
        val sunsetMinutes = maghribTime.toSecondOfDay() / 60
        val sunriseMinutes = sunriseTime.toSecondOfDay() / 60
        val nightMinutes = (24 * 60 - sunsetMinutes) + sunriseMinutes
        val midnightMinutes = (sunsetMinutes + nightMinutes / 2) % (24 * 60)
        val lastThirdMinutes = (sunsetMinutes + (nightMinutes * 2) / 3) % (24 * 60)

        val midnightTime = LocalTime.ofSecondOfDay((midnightMinutes * 60L).coerceIn(0, 86399))
        val lastThirdTime = LocalTime.ofSecondOfDay((lastThirdMinutes * 60L).coerceIn(0, 86399))
        val dhuhaTime = sunriseTime.plusMinutes(20)

        val hijriDateObj = HijriDateCalculator.convertToHijri(date, hijriAdjustmentDays)
        val hijriDateStr = hijriDateObj.formattedEn

        // Build prayer items
        val prayerTypesWithTimes = listOf(
            PrayerType.FAJR to fajrTime,
            PrayerType.SUNRISE to sunriseTime,
            PrayerType.DHUHR to dhuhrTime,
            PrayerType.ASR to asrTime,
            PrayerType.MAGHRIB to maghribTime,
            PrayerType.ISHA to ishaTime
        )

        // Determine next and passed prayers
        var foundNext = false
        val items = prayerTypesWithTimes.map { (type, time) ->
            val prayerZoned = date.atTime(time).atZone(zoneId)
            val isPassed = now.isAfter(prayerZoned)
            val isNext = if (!foundNext && !isPassed && date == now.toLocalDate()) {
                foundNext = true
                true
            } else false

            PrayerTimeItem(
                type = type,
                time = time,
                zonedDateTime = prayerZoned,
                isNext = isNext,
                isPassed = isPassed
            )
        }

        return DailyPrayerSchedule(
            date = date,
            hijriDateString = hijriDateStr,
            hijriDate = hijriDateObj,
            fajr = fajrTime,
            sunrise = sunriseTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            maghrib = maghribTime,
            isha = ishaTime,
            islamicMidnight = midnightTime,
            lastThirdOfNight = lastThirdTime,
            dhuha = dhuhaTime,
            prayerItems = items
        )
    }

    private fun decimalToLocalTime(decimalHours: Double): LocalTime {
        val fixed = fixHour(decimalHours)
        val hour = fixed.toInt().coerceIn(0, 23)
        val minutesDecimal = (fixed - hour) * 60.0
        val minute = (minutesDecimal + 0.5).toInt().coerceIn(0, 59)
        return LocalTime.of(hour, minute)
    }
}
