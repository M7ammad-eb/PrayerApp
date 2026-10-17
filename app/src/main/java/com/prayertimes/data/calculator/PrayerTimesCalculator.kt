package com.prayertimes.data.calculator
import com.prayertimes.data.models.*
import java.time.*
object PrayerTimesCalculator {
    fun clearCache() = com.salati.shared.PrayerTimesCalculator.clearCache()
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
        return com.salati.shared.PrayerTimesCalculator.calculateDailySchedule(date.shared(),latitude,longitude,
            com.salati.shared.ZoneId.of(zoneId.id),method,juristicMethod,highLatitudeRule,adjustments,hijriAdjustmentDays,now.shared()).android()
    }
}
