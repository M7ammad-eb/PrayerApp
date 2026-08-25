package com.prayertimes.data.calculator

import com.prayertimes.data.preferences.AppPrayerSettings
import java.time.LocalDate
import java.time.ZoneId

/** Bridges persisted app settings to the calculator's background cache warmer. */
object PrayerSchedulePrewarmer {
    fun prewarm(
        settings: AppPrayerSettings,
        anchorDate: LocalDate = LocalDate.now(),
        previousDays: Int = 7,
        nextDays: Int = 30
    ) {
        val zoneId = runCatching { ZoneId.of(settings.location.timeZoneId) }
            .getOrDefault(ZoneId.systemDefault())
        PrayerTimesCalculator.prewarm(
            anchorDate = anchorDate,
            latitude = settings.location.latitude,
            longitude = settings.location.longitude,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustments = settings.adjustments,
            hijriAdjustmentDays = settings.hijriAdjustmentDays,
            previousDays = previousDays,
            nextDays = nextDays
        )
    }
}
