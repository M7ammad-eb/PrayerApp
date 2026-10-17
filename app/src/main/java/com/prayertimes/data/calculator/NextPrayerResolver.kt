package com.prayertimes.data.calculator
import com.prayertimes.data.models.*
import java.time.ZonedDateTime
data class NextPrayerPeriod(val prayerItem: PrayerTimeItem, val previousPrayerAt: ZonedDateTime, val totalSpanSeconds: Long, val isNextDayFajr: Boolean)
object NextPrayerResolver {
    fun resolve(now: ZonedDateTime, yesterday: DailyPrayerSchedule, today: DailyPrayerSchedule, tomorrow: DailyPrayerSchedule): NextPrayerPeriod {
        val result = com.salati.shared.NextPrayerResolver.resolve(now.shared(),yesterday.shared(),today.shared(),tomorrow.shared())
        return NextPrayerPeriod(result.prayerItem.android(),result.previousPrayerAt.android(),result.totalSpanSeconds,result.isNextDayFajr)
    }
}
