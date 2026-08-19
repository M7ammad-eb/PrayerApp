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
    val displayNameAr: String = "",
    val isFullAthan: Boolean = false
) {
    ATHAN_MAKKAH_MULLA("Makkah - Ali Bin Ahmad Mullah", "Holy Mosque in Makkah", "مكة المكرمة - علي بن أحمد ملا", true),
    ATHAN_FAJR1_KWAIT_ALAFASY("Fajr (Kuwait) - Mishary Alafasy", "Fajr Adhan with As-Salatu Khayrun Minan-Nawm", "أذان الفجر (الكويت) - مشاري العفاسي", true),
    ATHAN_FAJR2_JORDAN_ALLALA("Fajr (Jordan) - Kamel Allala", "Grand Hussein Mosque Fajr Adhan", "أذان الفجر (الأردن) - كامل اللالا", true),
    ATHAN_RIYADH_QATAMI("Riyadh - Nasser Al-Qatami", "Saudi capital soulful recitation", "الرياض - ناصر القطامي", true),
    ATHAN_QATAR_NABET("Qatar - Saleh Al-Nabet", "State Grand Mosque of Qatar", "قطر - صالح النابت", true),
    ATHAN_QUDS_QAZAZ_1("Al-Aqsa Al-Quds - Naji Qazzaz (1)", "Blessed Jerusalem Al-Aqsa Mosque", "المسجد الأقصى - ناجي قزاز (1)", true),
    ATHAN_QUDS_QAZAZ_2("Al-Aqsa Al-Quds - Naji Qazzaz (2)", "Blessed Jerusalem Al-Aqsa Mosque", "المسجد الأقصى - ناجي قزاز (2)", true),
    ATHAN_EGYPT_DAWOD("Egypt - Ahmad Dawod", "Classic Cairo melodic Maqam", "مصر - أحمد داود", true),
    ATHAN_EGYPT_ALALFI("Egypt - Salah Al-Alfi", "Historic Egyptian recitation", "مصر - صلاح الألفي", true),
    ATHAN_EGYPT_ABDULAATI("Egypt (Al-Hussein) - Sayed Abdulaati", "Imam Al-Hussein Mosque Cairo", "مصر (الحسين) - سيد عبد العاطي", true),
    ATHAN_IRAQ_ALAMOURI("Iraq - Abu Omar Al-Amouri", "Traditional Iraqi Maqam", "العراق - أبو عمر العامري", true),
    ATHAN_GEORGIA("Georgia Mosque Adhan", "Tbilisi Juma Mosque", "أذان جورجيا", true),
    SHORT_TAKBEER("Takbeer Only", "Allahu Akbar takbeerat alert", "تكبيرات فقط", false),
    MELODIC_TONE("Gentle Chime", "Soft chime notification", "نغمة هادئة", false),
    VIBRATE_ONLY("Vibrate Only", "Haptic vibration without audio", "اهتزاز فقط", false),
    SILENT("Silent", "Visual notification only", "صامت", false);

    fun localizedDisplayName(isArabic: Boolean): String =
        if (isArabic && displayNameAr.isNotBlank()) displayNameAr else displayName

    companion object {
        // Fallback backward-compatibility mappings
        val FULL_ATHAN get() = ATHAN_MAKKAH_MULLA
        val ATHAN_FAJR get() = ATHAN_FAJR1_KWAIT_ALAFASY
        val ATHAN_MADINAH get() = ATHAN_RIYADH_QATAMI
        val ATHAN_AL_AQSA get() = ATHAN_QUDS_QAZAZ_1
        val ATHAN_CAIRO get() = ATHAN_EGYPT_DAWOD
    }
}

enum class AthanAudioStream(
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val streamType: Int,
    val audioUsage: Int
) {
    ALARM(
        titleEn = "Alarm Volume",
        titleAr = "صوت المنبه (Alarm)",
        descriptionEn = "Recommended: Sounds aloud even when phone is in Silent or Vibrate mode",
        descriptionAr = "موصى به: يرن بصوت مسموع حتى وإن كان الهاتف في وضع الصامت أو الاهتزاز",
        streamType = android.media.AudioManager.STREAM_ALARM,
        audioUsage = android.media.AudioAttributes.USAGE_ALARM
    ),
    MEDIA(
        titleEn = "Media Volume",
        titleAr = "صوت الوسائط (Media)",
        descriptionEn = "Uses current media/music volume slider",
        descriptionAr = "يتبع مستوى صوت الموسيقى والوسائط الحالي",
        streamType = android.media.AudioManager.STREAM_MUSIC,
        audioUsage = android.media.AudioAttributes.USAGE_MEDIA
    ),
    RINGTONE(
        titleEn = "Ringtone Volume",
        titleAr = "صوت نغمة الرنين (Ringtone)",
        descriptionEn = "Follows phone call & ringtone volume level",
        descriptionAr = "يتبع مستوى صوت نغمة رنين المكالمات والهاتف",
        streamType = android.media.AudioManager.STREAM_RING,
        audioUsage = android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE
    )
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
