package com.example.data.models

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class PrayerType(
    val title: String,
    val arabicName: String,
    val isFard: Boolean
) {
    FAJR("Fajr", "الفجر", true),
    SUNRISE("Sunrise", "الشروق", false),
    DHUHR("Dhuhr", "الظهر", true),
    ASR("Asr", "العصر", true),
    MAGHRIB("Maghrib", "المغرب", true),
    ISHA("Isha", "العشاء", true)
}

data class PrayerTimeItem(
    val type: PrayerType,
    val time: LocalTime,
    val zonedDateTime: ZonedDateTime,
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthNameEn: String,
    val monthNameAr: String,
    val year: Int,
    val formattedEn: String,
    val formattedAr: String,
    val calendarSource: String = "Umm al-Qura Calendar",
    val islamicEvent: String? = null,
    val islamicEventAr: String? = null,
    val isWhiteDay: Boolean = false,
    val isSacredMonth: Boolean = false
)

data class DailyPrayerSchedule(
    val date: LocalDate,
    val hijriDateString: String,
    val hijriDate: HijriDate? = null,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val islamicMidnight: LocalTime,
    val lastThirdOfNight: LocalTime,
    val dhuha: LocalTime,
    val prayerItems: List<PrayerTimeItem>
)

enum class CalculationMethod(
    val displayName: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null,
    val maghribAngle: Double? = null
) {
    MUSLIM_WORLD_LEAGUE(
        "Muslim World League (MWL)",
        "Standard method used across Europe, Far East, and parts of the Americas (Fajr 18°, Isha 17°)",
        18.0,
        17.0
    ),
    ISNA(
        "ISNA (North America)",
        "Islamic Society of North America (Fajr 15°, Isha 15°)",
        15.0,
        15.0
    ),
    EGYPTIAN(
        "Egyptian General Authority of Survey",
        "Common in Egypt, Africa, Syria, Iraq, Lebanon (Fajr 19.5°, Isha 17.5°)",
        19.5,
        17.5
    ),
    UMM_AL_QURA(
        "Umm Al-Qura University, Makkah",
        "Standard in Saudi Arabia and the Arabian Peninsula (Fajr 18.5°, Isha 90 min after Maghrib)",
        18.5,
        0.0,
        ishaMinutesAfterMaghrib = 90
    ),
    KARACHI(
        "University of Islamic Sciences, Karachi",
        "Standard in Pakistan, India, Bangladesh, Afghanistan (Fajr 18°, Isha 18°)",
        18.0,
        18.0
    ),
    TEHRAN(
        "Institute of Geophysics, Univ. of Tehran",
        "Fajr 17.7°, Maghrib 4.5°, Isha 14°",
        17.7,
        14.0,
        maghribAngle = 4.5
    ),
    SHIA_ITHNA_ASHARI(
        "Shia Ithna Ashari (Leva Institute)",
        "Fajr 16°, Maghrib 4°, Isha 14°",
        16.0,
        14.0,
        maghribAngle = 4.0
    ),
    GULF(
        "Gulf Region (UAE & Dubai)",
        "Fajr 19.5°, Isha 90 min after Maghrib",
        19.5,
        0.0,
        ishaMinutesAfterMaghrib = 90
    ),
    KUWAIT(
        "Kuwait",
        "Fajr 18°, Isha 17.5°",
        18.0,
        17.5
    ),
    QATAR(
        "Qatar",
        "Fajr 18°, Isha 90 min after Maghrib",
        18.0,
        0.0,
        ishaMinutesAfterMaghrib = 90
    ),
    SINGAPORE(
        "MUIS (Singapore)",
        "Majlis Ugama Islam Singapura (Fajr 20°, Isha 18°)",
        20.0,
        18.0
    ),
    TURKEY(
        "Diyanet İşleri Başkanlığı (Turkey)",
        "Presidency of Religious Affairs Turkey (Fajr 18°, Isha 17°)",
        18.0,
        17.0
    ),
    FRANCE_UOIF(
        "France (UOIF)",
        "Union des Organisations Islamiques de France (Fajr 12°, Isha 12°)",
        12.0,
        12.0
    ),
    RUSSIA(
        "SAMR (Russia)",
        "Spiritual Administration of Muslims of Russia (Fajr 16°, Isha 15°)",
        16.0,
        15.0
    )
}

enum class JuristicMethod(val displayName: String, val shadowFactor: Double) {
    STANDARD("Standard (Shafi, Maliki, Hanbali)", 1.0),
    HANAFI("Hanafi", 2.0)
}

enum class HighLatitudeRule(val displayName: String) {
    ANGLE_BASED("Angle-Based (Recommended)"),
    MIDNIGHT("Middle of the Night"),
    ONE_SEVENTH("One-Seventh of Night"),
    NONE("None")
}

enum class NotificationSoundType(
    val displayName: String,
    val subtitle: String = "",
    val isFullAthan: Boolean = false
) {
    FULL_ATHAN("Makkah Adhan", "Grand Mosque Bayati chant", true),
    ATHAN_MADINAH("Madinah Adhan", "Prophet's Mosque serene cadence", true),
    ATHAN_AL_AQSA("Al-Aqsa Adhan", "Soulful Jerusalem Maqam", true),
    ATHAN_CAIRO("Cairo Adhan", "Traditional Egyptian Maqam", true),
    SHORT_TAKBEER("Takbeer Only", "Allahu Akbar takbeerat alert", false),
    MELODIC_TONE("Gentle Chime", "Soft chime notification", false),
    VIBRATE_ONLY("Vibrate Only", "Haptic vibration without audio", false),
    SILENT("Silent", "Visual notification only", false)
}

data class NotificationPrayerConfig(
    val enabled: Boolean = true,
    val soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN,
    val preReminderMinutes: Int = 0 // 0 means at prayer time, 10 means 10 min before
)

data class PrayerTimeAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
)

data class UserLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val isGps: Boolean = false
)
