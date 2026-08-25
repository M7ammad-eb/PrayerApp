package com.prayertimes.data.calculator

import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.JuristicMethod
import com.prayertimes.data.models.PrayerTimeAdjustments
import com.prayertimes.data.models.PrayerTimeItem
import com.prayertimes.data.models.PrayerType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimesCalculator {

    private const val MAX_CACHED_SCHEDULES = 48

    private data class ScheduleCacheKey(
        val date: LocalDate,
        val latitude: Double,
        val longitude: Double,
        val zoneId: String,
        val method: CalculationMethod,
        val juristicMethod: JuristicMethod,
        val highLatitudeRule: HighLatitudeRule,
        val adjustments: PrayerTimeAdjustments,
        val hijriAdjustmentDays: Int
    )

    private val scheduleCache = object : LinkedHashMap<ScheduleCacheKey, DailyPrayerSchedule>(
        MAX_CACHED_SCHEDULES,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<ScheduleCacheKey, DailyPrayerSchedule>?
        ): Boolean = size > MAX_CACHED_SCHEDULES
    }
    private var cacheGeneration = 0L

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    // Above the polar circles the sun angle equation has no solution (cosT out of [-1, 1] range)
    // for part of the year - falling back to a fixed offset either side of solar noon keeps the
    // schedule usable there instead of returning NaN.
    private const val EXTREME_LATITUDE_FALLBACK_HOURS = 3.0

    // Dhuha begins once the sun has fully cleared the horizon glare, conventionally ~20 minutes
    // after sunrise rather than a second angle-based calculation.
    private const val DHUHA_MINUTES_AFTER_SUNRISE = 20L

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

    // Deliberately NOT fixHour()-wrapped: this is a UTC-relative offset that events get computed
    // from via +/- an hour-angle (see computeTime), and the sign/magnitude of that offset from
    // *this specific date's* UTC midnight is exactly what tells zonedTimeFromUtcDecimalHours which
    // UTC calendar day an event actually falls on. Wrapping it here would make that indistinguishable
    // from a same-day value - see zonedTimeFromUtcDecimalHours for what that broke.
    private fun computeMidDay(timeZone: Double, lon: Double, eqt: Double): Double {
        return 12.0 + timeZone - (lon / 15.0) - eqt
    }

    private fun computeTime(angle: Double, lat: Double, declination: Double, midDay: Double, isMorning: Boolean): Double {
        val cosT = (-dSin(angle) - dSin(lat) * dSin(declination)) / (dCos(lat) * dCos(declination))
        if (cosT < -1.0 || cosT > 1.0) {
            // Extreme latitude edge case
            return if (isMorning) midDay - EXTREME_LATITUDE_FALLBACK_HOURS else midDay + EXTREME_LATITUDE_FALLBACK_HOURS
        }
        val t = dAcos(cosT) / 15.0
        return if (isMorning) midDay - t else midDay + t
    }

    private fun computeAsr(shadowFactor: Double, lat: Double, declination: Double, midDay: Double): Double {
        val d = abs(lat - declination)
        val angle = -dAtan(1.0 / (shadowFactor + dTan(d)))
        return computeTime(angle, lat, declination, midDay, isMorning = false)
    }

    private data class SunTimes(
        val sunriseHour: Double,
        val sunsetHour: Double,
        val declination: Double,
        val midDayHour: Double
    )

    // Computed with timeZone fixed at 0 (UTC frame) rather than baking in a single local offset
    // for the whole day - see zonedTimeFromUtcDecimalHours for why that matters on DST-transition
    // dates.
    private fun computeSunTimes(date: LocalDate, latitude: Double, longitude: Double): SunTimes {
        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth)
        val sun = sunPosition(jd)
        val midDay = computeMidDay(0.0, longitude, sun.equationOfTime)
        val sunriseHour = computeTime(0.833, latitude, sun.declination, midDay, isMorning = true)
        val sunsetHour = computeTime(0.833, latitude, sun.declination, midDay, isMorning = false)
        return SunTimes(sunriseHour, sunsetHour, sun.declination, midDay)
    }

    // A single UTC-offset frozen for the entire calendar day (the previous approach) produces a
    // one-hour error for events that fall after a DST transition occurring earlier that same day.
    // Anchoring each event as a real UTC instant and converting via withZoneSameInstant applies
    // whatever offset actually holds at that specific instant instead.
    //
    // decimalHours is a SIGNED offset from `date`'s UTC midnight, not a wall-clock hour - it can
    // legitimately be negative (e.g. -5.2, meaning 18:48 UTC the *previous* UTC day - typical for
    // Fajr at far-eastern longitudes) or exceed 24 (e.g. 28.7, meaning 04:42 UTC the *next* UTC
    // day). Rounding it into [0,24) before adding it (as an earlier version of this function did)
    // silently discarded which UTC day the event actually fell on, shifting the resulting
    // ZonedDateTime a full day off in either direction while the displayed LocalTime still looked
    // correct - see PrayerTimesCalculatorGoldenTest's Tokyo/Sydney/Auckland/Honolulu cases.
    // plusMinutes() on a signed value rolls across the day boundary correctly on its own.
    private fun zonedTimeFromUtcDecimalHours(date: LocalDate, decimalHours: Double, zoneId: ZoneId): ZonedDateTime {
        val signedMinutes = (decimalHours * 60.0).roundToInt()
        return date.atStartOfDay(ZoneOffset.UTC).plusMinutes(signedMinutes.toLong()).withZoneSameInstant(zoneId)
    }

    fun clearCache() {
        synchronized(scheduleCache) {
            scheduleCache.clear()
            cacheGeneration++
        }
    }

    fun prewarm(
        anchorDate: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        method: CalculationMethod,
        juristicMethod: JuristicMethod,
        highLatitudeRule: HighLatitudeRule,
        adjustments: PrayerTimeAdjustments,
        hijriAdjustmentDays: Int,
        previousDays: Int = 7,
        nextDays: Int = 30
    ) {
        val now = ZonedDateTime.now(zoneId)
        for (offset in -previousDays.coerceAtLeast(0)..nextDays.coerceAtLeast(0)) {
            calculateDailySchedule(
                date = anchorDate.plusDays(offset.toLong()),
                latitude = latitude,
                longitude = longitude,
                zoneId = zoneId,
                method = method,
                juristicMethod = juristicMethod,
                highLatitudeRule = highLatitudeRule,
                adjustments = adjustments,
                hijriAdjustmentDays = hijriAdjustmentDays,
                now = now
            )
        }
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
        val key = ScheduleCacheKey(
            date,
            latitude,
            longitude,
            zoneId.id,
            method,
            juristicMethod,
            highLatitudeRule,
            adjustments,
            hijriAdjustmentDays
        )
        val generation: Long
        synchronized(scheduleCache) {
            scheduleCache[key]?.let { return it.withPrayerStatus(now) }
            generation = cacheGeneration
        }

        val generated = calculateDailyScheduleUncached(
            date,
            latitude,
            longitude,
            zoneId,
            method,
            juristicMethod,
            highLatitudeRule,
            adjustments,
            hijriAdjustmentDays,
            now
        ).withoutPrayerStatus()

        val cached = synchronized(scheduleCache) {
            if (generation == cacheGeneration) {
                scheduleCache.getOrPut(key) { generated }
            } else {
                generated
            }
        }
        return cached.withPrayerStatus(now)
    }

    private fun calculateDailyScheduleUncached(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        method: CalculationMethod,
        juristicMethod: JuristicMethod,
        highLatitudeRule: HighLatitudeRule,
        adjustments: PrayerTimeAdjustments,
        hijriAdjustmentDays: Int,
        now: ZonedDateTime
    ): DailyPrayerSchedule {
        val sunToday = computeSunTimes(date, latitude, longitude)
        val declination = sunToday.declination
        val midDay = sunToday.midDayHour

        // Standard angles: Sunrise / Sunset standard zenith = 90.833° -> angle = 0.833° below horizon
        var sunriseHour = sunToday.sunriseHour
        var sunsetHour = sunToday.sunsetHour

        var fajrHour = computeTime(method.fajrAngle, latitude, declination, midDay, isMorning = true)

        val hijriDateObj = HijriDateCalculator.convertToHijri(date, hijriAdjustmentDays)
        val hijriDateStr = hijriDateObj.formattedEn

        // The Umm al-Qura method's fixed Isha interval extends from 90 to 120 minutes after
        // Maghrib during Ramadan (to accommodate Tarawih), per the calculation's own convention.
        val ishaMinutesAfterMaghrib = if (method == CalculationMethod.UMM_AL_QURA && hijriDateObj.month == 9) {
            120
        } else {
            method.ishaMinutesAfterMaghrib
        }

        var ishaHour = if (ishaMinutesAfterMaghrib != null) {
            sunsetHour + (ishaMinutesAfterMaghrib / 60.0)
        } else {
            computeTime(method.ishaAngle, latitude, declination, midDay, isMorning = false)
        }

        var maghribHour = if (method.maghribAngle != null) {
            computeTime(method.maghribAngle, latitude, declination, midDay, isMorning = false)
        } else {
            sunsetHour
        }

        val asrHour = computeAsr(juristicMethod.shadowFactor, latitude, declination, midDay)

        // High Latitude Adjustments
        val nightDuration = fixHour(24.0 + sunriseHour - sunsetHour)
        if (highLatitudeRule != HighLatitudeRule.NONE && nightDuration in 0.0..24.0) {
            when (highLatitudeRule) {
                HighLatitudeRule.ANGLE_BASED -> {
                    // Night portion is angle/60 of the *full* night, not half of it - halving it
                    // pulls Fajr/Isha too close to sunrise/sunset at high latitudes.
                    val fajrPortion = (method.fajrAngle / 60.0) * nightDuration
                    if (sunriseHour - fajrHour > fajrPortion || fajrHour > sunriseHour) {
                        fajrHour = sunriseHour - fajrPortion
                    }
                    val ishaPortion = ((if (method.ishaAngle > 0) method.ishaAngle else 18.0) / 60.0) * nightDuration
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

        // Convert each event to its real zoned instant individually (see
        // zonedTimeFromUtcDecimalHours) and apply manual per-prayer adjustments on top.
        val fajrZoned = zonedTimeFromUtcDecimalHours(date, fajrHour, zoneId).plusMinutes(adjustments.fajr.toLong())
        val sunriseZoned = zonedTimeFromUtcDecimalHours(date, sunriseHour, zoneId).plusMinutes(adjustments.sunrise.toLong())
        val dhuhrZoned = zonedTimeFromUtcDecimalHours(date, midDay, zoneId).plusMinutes(adjustments.dhuhr.toLong())
        val asrZoned = zonedTimeFromUtcDecimalHours(date, asrHour, zoneId).plusMinutes(adjustments.asr.toLong())
        val maghribZoned = zonedTimeFromUtcDecimalHours(date, maghribHour, zoneId).plusMinutes(adjustments.maghrib.toLong())
        val ishaZoned = zonedTimeFromUtcDecimalHours(date, ishaHour, zoneId).plusMinutes(adjustments.isha.toLong())

        val fajrTime = fajrZoned.toLocalTime()
        val sunriseTime = sunriseZoned.toLocalTime()
        val dhuhrTime = dhuhrZoned.toLocalTime()
        val asrTime = asrZoned.toLocalTime()
        val maghribTime = maghribZoned.toLocalTime()
        val ishaTime = ishaZoned.toLocalTime()

        // Islamic Midnight (halfway between sunset and the *next* day's actual sunrise) and Last
        // Third of Night (Qiyam / Tahajjud), computed from real instants - using tomorrow's actual
        // sunrise rather than projecting today's forward, and correctly spanning any DST change
        // that falls overnight.
        val sunTomorrow = computeSunTimes(date.plusDays(1), latitude, longitude)
        val tomorrowSunriseZoned = zonedTimeFromUtcDecimalHours(date.plusDays(1), sunTomorrow.sunriseHour, zoneId)
        val night = Duration.between(maghribZoned, tomorrowSunriseZoned)
        val midnightZoned = maghribZoned.plus(night.dividedBy(2))
        val lastThirdZoned = maghribZoned.plus(night.multipliedBy(2).dividedBy(3))

        val midnightTime = midnightZoned.toLocalTime()
        val lastThirdTime = lastThirdZoned.toLocalTime()
        val dhuhaTime = sunriseTime.plusMinutes(DHUHA_MINUTES_AFTER_SUNRISE)

        // Build prayer items
        val prayerTypesWithZoned = listOf(
            PrayerType.FAJR to fajrZoned,
            PrayerType.SUNRISE to sunriseZoned,
            PrayerType.DHUHR to dhuhrZoned,
            PrayerType.ASR to asrZoned,
            PrayerType.MAGHRIB to maghribZoned,
            PrayerType.ISHA to ishaZoned
        )

        // Determine next and passed prayers
        var foundNext = false
        val items = prayerTypesWithZoned.map { (type, prayerZoned) ->
            val isPassed = now.isAfter(prayerZoned)
            val isNext = if (!foundNext && !isPassed && date == now.toLocalDate()) {
                foundNext = true
                true
            } else false

            PrayerTimeItem(
                type = type,
                time = prayerZoned.toLocalTime(),
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

    private fun DailyPrayerSchedule.withoutPrayerStatus(): DailyPrayerSchedule = copy(
        prayerItems = prayerItems.map { it.copy(isNext = false, isPassed = false) }
    )

    private fun DailyPrayerSchedule.withPrayerStatus(now: ZonedDateTime): DailyPrayerSchedule {
        var foundNext = false
        return copy(
            prayerItems = prayerItems.map { item ->
                val isPassed = now.isAfter(item.zonedDateTime)
                val isNext = !foundNext && !isPassed && date == now.toLocalDate()
                if (isNext) foundNext = true
                item.copy(isNext = isNext, isPassed = isPassed)
            }
        )
    }
}
