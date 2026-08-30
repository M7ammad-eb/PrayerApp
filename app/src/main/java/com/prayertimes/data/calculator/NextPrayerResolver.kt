package com.prayertimes.data.calculator

import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.PrayerTimeItem
import com.prayertimes.data.models.PrayerType
import java.time.Duration
import java.time.ZonedDateTime

data class NextPrayerPeriod(
    val prayerItem: PrayerTimeItem,
    val previousPrayerAt: ZonedDateTime,
    val totalSpanSeconds: Long,
    val isNextDayFajr: Boolean
)

/** Resolves the next displayed prayer time, including Sunrise in the six-item daily sequence. */
object NextPrayerResolver {
    fun resolve(
        now: ZonedDateTime,
        yesterday: DailyPrayerSchedule,
        today: DailyPrayerSchedule,
        tomorrow: DailyPrayerSchedule
    ): NextPrayerPeriod {
        fun DailyPrayerSchedule.item(type: PrayerType) = prayerItems.first { it.type == type }

        val nextToday = today.prayerItems.firstOrNull { it.zonedDateTime.isAfter(now) }
        val next = nextToday ?: tomorrow.item(PrayerType.FAJR)
        val previous = if (nextToday == null) {
            today.item(PrayerType.ISHA)
        } else {
            today.prayerItems.lastOrNull { it.zonedDateTime.isBefore(next.zonedDateTime) }
                ?: yesterday.item(PrayerType.ISHA)
        }
        val totalSpan = Duration.between(previous.zonedDateTime, next.zonedDateTime)
            .seconds
            .coerceAtLeast(1L)

        return NextPrayerPeriod(
            prayerItem = next,
            previousPrayerAt = previous.zonedDateTime,
            totalSpanSeconds = totalSpan,
            isNextDayFajr = nextToday == null
        )
    }
}
