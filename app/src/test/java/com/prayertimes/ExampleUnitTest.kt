package com.prayertimes

import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.PrayerType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPrayerTimesCalculationForMakkah() {
    val date = LocalDate.of(2026, 8, 15)
    val zoneId = ZoneId.of("Asia/Riyadh")
    val schedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 21.4225,
      longitude = 39.8262,
      zoneId = zoneId,
      method = CalculationMethod.UMM_AL_QURA
    )

    assertEquals(6, schedule.prayerItems.size)
    assertTrue(schedule.fajr.isBefore(schedule.sunrise))
    assertTrue(schedule.sunrise.isBefore(schedule.dhuhr))
    assertTrue(schedule.dhuhr.isBefore(schedule.asr))
    assertTrue(schedule.asr.isBefore(schedule.maghrib))
    assertTrue(schedule.maghrib.isBefore(schedule.isha))
  }

  @Test
  fun testNextPrayerIdentification() {
    val date = LocalDate.of(2026, 8, 15)
    val zoneId = ZoneId.of("Asia/Riyadh")
    val noon = ZonedDateTime.of(date, java.time.LocalTime.of(12, 0), zoneId)

    val schedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 21.4225,
      longitude = 39.8262,
      zoneId = zoneId,
      method = CalculationMethod.UMM_AL_QURA,
      now = noon
    )

    val nextPrayer = schedule.prayerItems.firstOrNull { it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(noon) }
    assertNotNull(nextPrayer)
    assertTrue(nextPrayer!!.type == PrayerType.DHUHR || nextPrayer.type == PrayerType.ASR)
  }
}
