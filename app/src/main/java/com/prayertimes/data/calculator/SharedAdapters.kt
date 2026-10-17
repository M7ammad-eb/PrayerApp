package com.prayertimes.data.calculator

import com.prayertimes.data.models.*
import java.time.*
internal fun LocalDate.shared() = com.salati.shared.LocalDate.parse(toString())
internal fun ZonedDateTime.shared() = com.salati.shared.ZonedDateTime.fromEpochSeconds(toEpochSecond(),com.salati.shared.ZoneId.of(zone.id),nano)
internal fun com.salati.shared.LocalDate.android(): LocalDate = LocalDate.parse(toString())
internal fun com.salati.shared.LocalTime.android(): LocalTime = LocalTime.of(hour,minute,second,nano)
internal fun com.salati.shared.ZonedDateTime.android(): ZonedDateTime = Instant.ofEpochSecond(epochSeconds,nanosecond.toLong()).atZone(ZoneId.of(zone.id))
internal fun PrayerTimeItem.shared() = com.salati.shared.PrayerTimeItem(type,com.salati.shared.LocalTime(time.hour,time.minute,time.second,time.nano),zonedDateTime.shared(),isNext,isPassed)
internal fun com.salati.shared.PrayerTimeItem.android() = PrayerTimeItem(type,time.android(),zonedDateTime.android(),isNext,isPassed)
internal fun DailyPrayerSchedule.shared(): com.salati.shared.DailyPrayerSchedule {
    fun LocalTime.shared() = com.salati.shared.LocalTime(hour,minute,second,nano)
    return com.salati.shared.DailyPrayerSchedule(date.shared(),hijriDateString,hijriDate,fajr.shared(),sunrise.shared(),dhuhr.shared(),asr.shared(),maghrib.shared(),isha.shared(),islamicMidnight.shared(),lastThirdOfNight.shared(),dhuha.shared(),prayerItems.map { it.shared() })
}
internal fun com.salati.shared.DailyPrayerSchedule.android() = DailyPrayerSchedule(date.android(),hijriDateString,hijriDate,fajr.android(),sunrise.android(),dhuhr.android(),asr.android(),maghrib.android(),isha.android(),islamicMidnight.android(),lastThirdOfNight.android(),dhuha.android(),prayerItems.map { it.android() })
