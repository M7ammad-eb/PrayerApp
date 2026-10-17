package com.salati.shared

import com.prayertimes.data.qibla.QiblaCalculator

// A narrow Objective-C/Swift boundary: ISO local dates, IANA zone IDs, enum identifiers and
// epoch seconds. Swift never needs to reason about Kotlin date/time internals.
data class PrayerEvent(val id: String, val title: String, val arabicTitle: String, val epochSeconds: Double)
data class PrayerDay(val dateISO: String, val hijriEnglish: String, val hijriArabic: String,
    val events: List<PrayerEvent>, val next: PrayerEvent, val currentId: String,
    val currentEnded: Boolean, val midnightEpoch: Double, val lastThirdEpoch: Double,
    val hijriMonthKey: String)
data class CalendarCell(val dateISO: String, val day: Int, val inMonth: Boolean, val observances: List<String>)
data class CalendarPage(val year: Int, val month: Int, val titleEnglish: String, val titleArabic: String, val cells: List<CalendarCell>)
class SalatiEngine {
    val calculationMethods: List<String> get() = CalculationMethod.entries.map { it.name }
    val highLatitudeRules: List<String> get() = HighLatitudeRule.entries.map { it.name }
    fun monthKey(dateISO: String) = HijriCalendar.monthKeyFor(LocalDate.parse(dateISO))
    fun suggestedMethod(zone: String): String = suggestCalculationMethod(zone).name
    @Throws(IllegalArgumentException::class)
    fun day(dateISO: String, latitude: Double, longitude: Double, zone: String, method: String,
        hanafi: Boolean, highLatitude: String, fajr: Int, sunrise: Int, dhuhr: Int, asr: Int,
        maghrib: Int, isha: Int, hijriOffset: Int, nowEpoch: Double): PrayerDay {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val date = LocalDate.parse(dateISO)
        val zoneId = ZoneId.of(zone)
        val now = ZonedDateTime.fromEpochSeconds(nowEpoch.toLong(),zoneId)
        fun schedule(d: LocalDate) = PrayerTimesCalculator.calculateDailySchedule(d,latitude,longitude,zoneId,
            CalculationMethod.valueOf(method),if(hanafi) JuristicMethod.HANAFI else JuristicMethod.STANDARD,
            HighLatitudeRule.valueOf(highLatitude),PrayerTimeAdjustments(fajr,sunrise,dhuhr,asr,maghrib,isha),hijriOffset,now)
        val today = schedule(date)
        val yesterday = schedule(date.minusDays(1))
        val tomorrow = schedule(date.plusDays(1))
        val next = NextPrayerResolver.resolve(now,yesterday,today,tomorrow)
        val current = CurrentPrayerResolver.resolve(now,yesterday,today,tomorrow)
        fun event(item: PrayerTimeItem) = PrayerEvent(item.type.name,item.type.title,item.type.arabicName,item.zonedDateTime.epochSeconds.toDouble())
        return PrayerDay(dateISO,today.hijriDate!!.formattedEn,today.hijriDate.formattedAr,
            today.prayerItems.map(::event),event(next.prayerItem),current.prayerItem.type.name,current.isPrayerTimeEnded,
            today.islamicMidnightAt!!.epochSeconds.toDouble(),today.lastThirdAt!!.epochSeconds.toDouble(),HijriCalendar.monthKeyFor(date))
    }
    @Throws(IllegalArgumentException::class)
    fun calendar(dateISO: String, monthDelta: Int, offset: Int): CalendarPage {
        val month = HijriCalendar.monthContaining(LocalDate.parse(dateISO),offset).plusMonths(monthDelta)
        val page = HijriCalendar.generate(month,offset)
        return CalendarPage(month.year,month.month,page.monthNameEn,page.monthNameAr,page.days.map {
            CalendarCell(it.gregorianDate.toString(),it.hijriDate.day,it.isInDisplayedMonth,it.hijriDate.observances.map { o -> o.name })
        })
    }
    fun searchTokens(query: String): String = query.splitToSequence(' ', '\t', '\n').flatMap { token ->
        val clean = token.map { if(it.isLetterOrDigit()) it else ' ' }.joinToString("")
        clean.splitToSequence(' ').filter { it.isNotEmpty() }
    }.joinToString(" ") { "$it*" }
    fun qiblaBearing(latitude: Double, longitude: Double) = QiblaCalculator.calculateQiblaBearing(latitude,longitude)
    fun qiblaDistance(latitude: Double, longitude: Double) = QiblaCalculator.calculateDistanceToKaabaKm(latitude,longitude)
}
