package com.salati.shared


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

enum class IslamicObservance {
    HIJRI_NEW_YEAR,
    TASUA,
    ASHURA,
    WHITE_DAY,
    RAMADAN,
    RAMADAN_START,
    RAMADAN_LAST_TEN_NIGHTS,
    RAMADAN_ODD_NIGHT,
    EID_AL_FITR,
    FIRST_TEN_DHU_AL_HIJJAH,
    TARWIYAH,
    ARAFAH,
    EID_AL_ADHA,
    TASHREEQ,
    FASTING_PROHIBITED
}

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthNameEn: String,
    val monthNameAr: String,
    val year: Int,
    val formattedEn: String,
    val formattedAr: String,
    val calendarSource: String = "Umm al-Qura Calendar",
    val observances: List<IslamicObservance> = emptyList(),
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
    val prayerItems: List<PrayerTimeItem>,
    val islamicMidnightAt: ZonedDateTime? = null,
    val lastThirdAt: ZonedDateTime? = null,
    val tomorrowFajrAt: ZonedDateTime? = null
)

// Display name/description are resolved via AppLocaleProvider.calcMethodName(method), not stored
// here, so the same catalog works for every language without a second set of literal fields.
enum class CalculationMethod(
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null,
    val maghribAngle: Double? = null
) {
    MUSLIM_WORLD_LEAGUE(18.0, 17.0),
    ISNA(15.0, 15.0),
    EGYPTIAN(19.5, 17.5),
    UMM_AL_QURA(18.5, 0.0, ishaMinutesAfterMaghrib = 90),
    KARACHI(18.0, 18.0),
    TEHRAN(17.7, 14.0, maghribAngle = 4.5),
    SHIA_ITHNA_ASHARI(16.0, 14.0, maghribAngle = 4.0),
    GULF(19.5, 0.0, ishaMinutesAfterMaghrib = 90),
    KUWAIT(18.0, 17.5),
    QATAR(18.0, 0.0, ishaMinutesAfterMaghrib = 90),
    SINGAPORE(20.0, 18.0),
    TURKEY(18.0, 17.0),
    FRANCE_UOIF(12.0, 12.0),
    RUSSIA(16.0, 15.0)
}

/**
 * A reasonable starting-point calculation method for a location, based on its IANA timezone -
 * available for every location source (GPS, manual entry, or a preset) without needing a country
 * code. Only meant as an onboarding default; the user can always pick a different method.
 */
fun suggestCalculationMethod(timeZoneId: String): CalculationMethod = when {
    timeZoneId == "Asia/Riyadh" -> CalculationMethod.UMM_AL_QURA
    timeZoneId == "Asia/Qatar" -> CalculationMethod.QATAR
    timeZoneId == "Asia/Kuwait" -> CalculationMethod.KUWAIT
    timeZoneId in setOf("Asia/Dubai", "Asia/Bahrain", "Asia/Muscat") -> CalculationMethod.GULF
    timeZoneId == "Africa/Cairo" -> CalculationMethod.EGYPTIAN
    timeZoneId == "Asia/Karachi" -> CalculationMethod.KARACHI
    timeZoneId == "Asia/Tehran" -> CalculationMethod.TEHRAN
    timeZoneId == "Asia/Singapore" || timeZoneId == "Asia/Kuala_Lumpur" -> CalculationMethod.SINGAPORE
    timeZoneId == "Europe/Istanbul" -> CalculationMethod.TURKEY
    timeZoneId == "Europe/Paris" -> CalculationMethod.FRANCE_UOIF
    timeZoneId.startsWith("Europe/Moscow") || timeZoneId.startsWith("Europe/Kaliningrad") ||
        timeZoneId.startsWith("Asia/Yekaterinburg") || timeZoneId.startsWith("Asia/Novosibirsk") ||
        timeZoneId.startsWith("Asia/Krasnoyarsk") || timeZoneId.startsWith("Asia/Irkutsk") ||
        timeZoneId.startsWith("Asia/Vladivostok") -> CalculationMethod.RUSSIA
    timeZoneId.startsWith("America/") -> CalculationMethod.ISNA
    else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
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
    val isGps: Boolean = false,
    // Distance in km from the GPS fix to the nearest offline-database place that produced [name] -
    // null for manually-picked/preset locations, which are exact rather than approximated.
    val nearestPlaceDistanceKm: Double? = null
)
