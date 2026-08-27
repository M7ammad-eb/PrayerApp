package com.prayertimes.data.calculator

import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.PrayerTimeItem
import com.prayertimes.data.models.PrayerType
import java.time.ZonedDateTime

/** The obligatory prayer whose time currently applies, or the explicit post-Fajr gap. */
data class CurrentPrayerPeriod(
    val prayerItem: PrayerTimeItem,
    val endsAt: ZonedDateTime,
    val isPrayerTimeEnded: Boolean = false,
    val changesAt: ZonedDateTime = endsAt
)

/**
 * Resolves prayer intervals from their actual boundaries. Sunrise is deliberately an end boundary
 * for Fajr, never a prayer to count through on the way to Dhuhr.
 */
object CurrentPrayerResolver {
    fun resolve(
        now: ZonedDateTime,
        yesterday: DailyPrayerSchedule,
        today: DailyPrayerSchedule,
        tomorrow: DailyPrayerSchedule
    ): CurrentPrayerPeriod {
        fun DailyPrayerSchedule.item(type: PrayerType) = prayerItems.first { it.type == type }

        val fajr = today.item(PrayerType.FAJR)
        val sunrise = today.item(PrayerType.SUNRISE)
        val dhuhr = today.item(PrayerType.DHUHR)
        val asr = today.item(PrayerType.ASR)
        val maghrib = today.item(PrayerType.MAGHRIB)
        val isha = today.item(PrayerType.ISHA)

        return when {
            now.isBefore(fajr.zonedDateTime) -> CurrentPrayerPeriod(
                prayerItem = yesterday.item(PrayerType.ISHA),
                endsAt = fajr.zonedDateTime
            )
            now.isBefore(sunrise.zonedDateTime) -> CurrentPrayerPeriod(
                prayerItem = fajr,
                endsAt = sunrise.zonedDateTime
            )
            now.isBefore(dhuhr.zonedDateTime) -> CurrentPrayerPeriod(
                prayerItem = fajr,
                endsAt = sunrise.zonedDateTime,
                isPrayerTimeEnded = true,
                changesAt = dhuhr.zonedDateTime
            )
            now.isBefore(asr.zonedDateTime) -> CurrentPrayerPeriod(
                prayerItem = dhuhr,
                endsAt = asr.zonedDateTime
            )
            now.isBefore(maghrib.zonedDateTime) -> CurrentPrayerPeriod(
                prayerItem = asr,
                endsAt = maghrib.zonedDateTime
            )
            now.isBefore(isha.zonedDateTime) -> CurrentPrayerPeriod(
                prayerItem = maghrib,
                endsAt = isha.zonedDateTime
            )
            else -> CurrentPrayerPeriod(
                prayerItem = isha,
                endsAt = tomorrow.item(PrayerType.FAJR).zonedDateTime
            )
        }
    }
}
