package com.salati.shared
import kotlin.test.*
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
class BaselineParityTest {
    @Test fun allUmmAlQuraDaysMatchJava() {
        for(epoch in ummAlQuraMonthStarts.first() until ummAlQuraMonthStarts.last()) {
            val expected = HijrahChronology.INSTANCE.date(java.time.LocalDate.ofEpochDay(epoch.toLong()))
            val actual = HijriDateCalculator.convertToHijri(LocalDate.ofEpochDay(epoch.toLong()))
            assertEquals(expected.get(ChronoField.YEAR),actual.year,"epoch $epoch")
            assertEquals(expected.get(ChronoField.MONTH_OF_YEAR),actual.month)
            assertEquals(expected.get(ChronoField.DAY_OF_MONTH),actual.day)
        }
    }
    @Test fun differentialPrayerMatrix() {
        val cities = listOf(Triple(24.4672,39.6111,"Asia/Riyadh"),Triple(21.4225,39.8262,"Asia/Riyadh"),
            Triple(33.5138,36.2765,"Asia/Damascus"),Triple(51.5072,-0.1276,"Europe/London"),
            Triple(40.7128,-74.0060,"America/New_York"),Triple(35.6762,139.6503,"Asia/Tokyo"),
            Triple(-36.8485,174.7633,"Pacific/Auckland"),Triple(21.3069,-157.8583,"Pacific/Honolulu"),
            Triple(69.6492,18.9553,"Europe/Oslo"))
        val dates = listOf("2026-01-15","2027-02-20","2024-03-10","2024-11-03","2026-06-21","2026-10-25")
        for((lat,lon,zone) in cities) for(date in dates) for(method in CalculationMethod.entries)
            for(juristic in JuristicMethod.entries) for(rule in HighLatitudeRule.entries) for(offset in listOf(-2,0,2)) {
            val adjustments = PrayerTimeAdjustments(7,-1,2,3,-2,-5)
            val actual = PrayerTimesCalculator.calculateDailySchedule(LocalDate.parse(date),lat,lon,ZoneId.of(zone),method,juristic,rule,adjustments,offset)
            val expected = baseline.PrayerTimesCalculator.calculateDailySchedule(java.time.LocalDate.parse(date),lat,lon,java.time.ZoneId.of(zone),
                baseline.CalculationMethod.valueOf(method.name),baseline.JuristicMethod.valueOf(juristic.name),baseline.HighLatitudeRule.valueOf(rule.name),
                baseline.PrayerTimeAdjustments(7,-1,2,3,-2,-5),offset)
            assertEquals(expected.prayerItems.map { it.zonedDateTime.toEpochSecond() },actual.prayerItems.map { it.zonedDateTime.epochSeconds },"$date $zone $method $juristic $rule offset=$offset")
            assertEquals(expected.islamicMidnight.toSecondOfDay(),actual.islamicMidnight.toSecondOfDay())
            assertEquals(expected.lastThirdOfNight.toSecondOfDay(),actual.lastThirdOfNight.toSecondOfDay())
            assertEquals(expected.hijriDate!!.formattedEn,actual.hijriDate!!.formattedEn)
        }
    }
}
