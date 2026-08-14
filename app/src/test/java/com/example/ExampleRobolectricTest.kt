package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.calculator.PrayerTimesCalculator
import com.example.data.cities.CityDatabase
import com.example.data.models.CalculationMethod
import com.example.data.models.JuristicMethod
import com.example.data.models.PrayerType
import com.example.data.preferences.PrayerPreferences
import com.example.data.qibla.QiblaCalculator
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Prayer Times", appName)
  }

  @Test
  fun `calculate Makkah prayer times`() {
    val date = LocalDate.of(2026, 8, 14)
    val zoneId = ZoneId.of("Asia/Riyadh")
    val schedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 21.4225,
      longitude = 39.8262,
      zoneId = zoneId,
      method = CalculationMethod.UMM_AL_QURA
    )

    assertNotNull(schedule)
    assertEquals(6, schedule.prayerItems.size)
    assertTrue("Fajr should precede sunrise", schedule.fajr.isBefore(schedule.sunrise))
    assertTrue("Sunrise should precede Dhuhr", schedule.sunrise.isBefore(schedule.dhuhr))
    assertTrue("Dhuhr should precede Asr", schedule.dhuhr.isBefore(schedule.asr))
    assertTrue("Asr should precede Maghrib", schedule.asr.isBefore(schedule.maghrib))
    assertTrue("Maghrib should precede Isha", schedule.maghrib.isBefore(schedule.isha))

    // For Umm Al-Qura, Isha is 90 minutes after Maghrib
    val maghribMinutes = schedule.maghrib.hour * 60 + schedule.maghrib.minute
    val ishaMinutes = schedule.isha.hour * 60 + schedule.isha.minute
    assertEquals(90, ishaMinutes - maghribMinutes)
  }

  @Test
  fun `hanafi asr is later than standard shafii asr`() {
    val date = LocalDate.of(2026, 8, 14)
    val zoneId = ZoneId.of("Asia/Karachi")
    val standardSchedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 24.8607,
      longitude = 67.0011,
      zoneId = zoneId,
      method = CalculationMethod.KARACHI,
      juristicMethod = JuristicMethod.STANDARD
    )
    val hanafiSchedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 24.8607,
      longitude = 67.0011,
      zoneId = zoneId,
      method = CalculationMethod.KARACHI,
      juristicMethod = JuristicMethod.HANAFI
    )

    assertTrue("Hanafi Asr should be strictly after Standard Asr", hanafiSchedule.asr.isAfter(standardSchedule.asr))
  }

  @Test
  fun `test prayer times calculation across international cities`() {
    val date = LocalDate.of(2026, 8, 14)

    // New York (ISNA)
    val nySchedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 40.7128,
      longitude = -74.0060,
      zoneId = ZoneId.of("America/New_York"),
      method = CalculationMethod.ISNA
    )
    assertNotNull(nySchedule)
    assertTrue(nySchedule.fajr.isBefore(nySchedule.sunrise))
    assertTrue(nySchedule.dhuhr.isBefore(nySchedule.asr))

    // London (MWL)
    val londonSchedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 51.5074,
      longitude = -0.1278,
      zoneId = ZoneId.of("Europe/London"),
      method = CalculationMethod.MUSLIM_WORLD_LEAGUE
    )
    assertNotNull(londonSchedule)
    assertTrue(londonSchedule.fajr.isBefore(londonSchedule.sunrise))

    // Cairo (Egyptian)
    val cairoSchedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 30.0444,
      longitude = 31.2357,
      zoneId = ZoneId.of("Africa/Cairo"),
      method = CalculationMethod.EGYPTIAN
    )
    assertNotNull(cairoSchedule)
    assertTrue(cairoSchedule.maghrib.isBefore(cairoSchedule.isha))

    // Tokyo (MWL)
    val tokyoSchedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 35.6762,
      longitude = 139.6503,
      zoneId = ZoneId.of("Asia/Tokyo"),
      method = CalculationMethod.MUSLIM_WORLD_LEAGUE
    )
    assertNotNull(tokyoSchedule)
    assertTrue(tokyoSchedule.fajr.isBefore(tokyoSchedule.sunrise))
  }

  @Test
  fun `test night and sunnah calculations`() {
    val date = LocalDate.of(2026, 8, 14)
    val zoneId = ZoneId.of("Asia/Riyadh")
    val schedule = PrayerTimesCalculator.calculateDailySchedule(
      date = date,
      latitude = 21.4225,
      longitude = 39.8262,
      zoneId = zoneId,
      method = CalculationMethod.UMM_AL_QURA
    )

    assertNotNull(schedule.dhuha)
    assertNotNull(schedule.islamicMidnight)
    assertNotNull(schedule.lastThirdOfNight)
    assertTrue("Dhuha is after sunrise", schedule.dhuha.isAfter(schedule.sunrise))
  }

  @Test
  fun `calculate Qibla direction from Cairo`() {
    // Cairo coordinates: ~30.0444° N, 31.2357° E
    val bearing = QiblaCalculator.calculateQiblaBearing(30.0444, 31.2357)
    // Qibla from Cairo is South-East (~135-137 degrees)
    assertTrue("Cairo Qibla bearing should be around 136°", bearing in 130.0..140.0)
  }

  @Test
  fun `calculate Qibla distance from London`() {
    val dist = QiblaCalculator.calculateDistanceToKaabaKm(51.5074, -0.1278)
    assertTrue("Distance from London to Mecca should be ~4700-4800 km", dist in 4700.0..4900.0)
  }

  @Test
  fun `city database contains major cities`() {
    assertTrue("Preset cities list should not be empty", CityDatabase.PRESET_CITIES.size >= 100)
    val mecca = CityDatabase.PRESET_CITIES.find { it.name.contains("Makkah", ignoreCase = true) }
    assertNotNull(mecca)
  }

  @Test
  fun `prayer preferences persists calculation method selection in DataStore`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = PrayerPreferences(context)

    // Update to ISNA
    preferences.updateCalculationMethod(CalculationMethod.ISNA)
    var settings = preferences.settingsFlow.first()
    assertEquals(CalculationMethod.ISNA, settings.calculationMethod)

    // Switch to Umm Al-Qura
    preferences.updateCalculationMethod(CalculationMethod.UMM_AL_QURA)
    settings = preferences.settingsFlow.first()
    assertEquals(CalculationMethod.UMM_AL_QURA, settings.calculationMethod)

    // Switch to Egyptian
    preferences.updateCalculationMethod(CalculationMethod.EGYPTIAN)
    settings = preferences.settingsFlow.first()
    assertEquals(CalculationMethod.EGYPTIAN, settings.calculationMethod)
  }

  @Test
  fun `convert Gregorian to Umm al-Qura Hijri date`() {
    val date = LocalDate.of(2026, 8, 14)
    val hijriDate = com.example.data.calculator.HijriDateCalculator.convertToHijri(date)

    assertNotNull(hijriDate)
    assertEquals("Umm al-Qura Calendar", hijriDate.calendarSource)
    assertTrue("Hijri date string should not be empty", hijriDate.formattedEn.isNotEmpty())
    assertTrue("Arabic Hijri date string should not be empty", hijriDate.formattedAr.isNotEmpty())
    assertTrue("Month should be valid (1..12)", hijriDate.month in 1..12)
    assertTrue("Day should be valid (1..30)", hijriDate.day in 1..30)
    assertTrue("Year should be around 1448 AH", hijriDate.year in 1445..1450)
  }

  @Test
  fun `find nearest city and calculate distance for manual coordinates`() {
    // Riyadh coordinates roughly 24.71, 46.67
    val nearest = com.example.data.cities.CityDatabase.findNearestCity(24.71, 46.67)
    assertNotNull(nearest)
    assertEquals("Riyadh", nearest!!.first.name)
    assertTrue("Distance should be within 10 km", nearest.second < 10.0)

    // Estimate timezone
    val tz = com.example.data.cities.CityDatabase.estimateTimeZone(24.71, 46.67)
    assertEquals("Asia/Riyadh", tz)
  }

  @Test
  fun `manual coordinates location persistence in prayer preferences`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = PrayerPreferences(context)

    val customLoc = com.example.data.models.UserLocation(
      name = "Desert Camp Al-Ula",
      country = "Manual Coordinates",
      latitude = 26.6176,
      longitude = 37.9250,
      timeZoneId = "Asia/Riyadh",
      isGps = false
    )

    preferences.updateLocation(customLoc)
    val settings = preferences.settingsFlow.first()
    assertEquals("Desert Camp Al-Ula", settings.location.name)
    assertEquals(26.6176, settings.location.latitude, 0.0001)
    assertEquals(37.9250, settings.location.longitude, 0.0001)
    assertEquals(false, settings.location.isGps)
  }

  @Test
  fun `main activity builds and launches successfully`() {
    org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
  }
}

