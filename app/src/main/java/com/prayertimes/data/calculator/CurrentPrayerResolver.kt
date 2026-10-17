package com.prayertimes.data.calculator
import com.prayertimes.data.models.*
import java.time.ZonedDateTime
data class CurrentPrayerPeriod(val prayerItem: PrayerTimeItem, val endsAt: ZonedDateTime, val isPrayerTimeEnded: Boolean = false, val changesAt: ZonedDateTime = endsAt)
object CurrentPrayerResolver {
    fun resolve(now: ZonedDateTime, yesterday: DailyPrayerSchedule, today: DailyPrayerSchedule, tomorrow: DailyPrayerSchedule): CurrentPrayerPeriod {
        val result = com.salati.shared.CurrentPrayerResolver.resolve(now.shared(),yesterday.shared(),today.shared(),tomorrow.shared())
        return CurrentPrayerPeriod(result.prayerItem.android(),result.endsAt.android(),result.isPrayerTimeEnded,result.changesAt.android())
    }
}
