package com.prayertimes.data.models

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.text.Collator
import java.util.Locale

typealias PrayerType = com.salati.shared.PrayerType

data class PrayerTimeItem(
    val type: PrayerType,
    val time: LocalTime,
    val zonedDateTime: ZonedDateTime,
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)

typealias IslamicObservance = com.salati.shared.IslamicObservance
typealias HijriDate = com.salati.shared.HijriDate

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
typealias CalculationMethod = com.salati.shared.CalculationMethod
typealias JuristicMethod = com.salati.shared.JuristicMethod
typealias HighLatitudeRule = com.salati.shared.HighLatitudeRule

fun suggestCalculationMethod(timeZoneId: String) = com.salati.shared.suggestCalculationMethod(timeZoneId)

enum class NotificationSoundType(
    val displayName: String,
    val subtitle: String = "",
    val displayNameAr: String = "",
    val isFullAthan: Boolean = false
) {
    ATHAN_DEFAULT("Default Athan", "", "الأذان الافتراضي", true),
    ATHAN_EGYPT_ALALFI("Fajr (Egypt) - Salah Al-Alfi", "Fajr Athan from Al-Azhar and Cairo mosques", "أذان الفجر (مصر) - صلاح الألفي", true),
    ATHAN_FAJR2_JORDAN_ALLALA("Fajr (Jordan) - Kamel Allala", "Grand Hussein Mosque Fajr Adhan", "أذان الفجر (الأردن) - كامل اللالا", true),
    ATHAN_FAJR1_KWAIT_ALAFASY("Fajr (Kuwait) - Mishary Alafasy", "Fajr Athan with As-Salatu Khayrun Minan-Nawm", "أذان الفجر (الكويت) - مشاري العفاسي", true),
    ATHAN_QUDS_QAZAZ_1("Al-Aqsa Al-Quds - Naji Qazzaz", "Blessed Jerusalem Al-Aqsa Mosque", "المسجد الأقصى - ناجي قزاز", true),
    ATHAN_EGYPT_DAWOD("Egypt - Ahmad Dawod", "Classic Cairo melodic Maqam", "مصر - أحمد داود", true),
    ATHAN_EGYPT_ABDULAATI("Egypt - Sayed Abdulaati", "Imam Al-Hussein Mosque Cairo", "مصر - سيد عبد العاطي", true),
    ATHAN_IRAQ_ALAMOURI("Iraq - Abu Omar Al-Maamouri", "Traditional Iraqi Maqam", "العراق - أبو عمر المعموري", true),
    ATHAN_MAKKAH_MULLA("Makkah - Ali Bin Ahmad Mullah", "Holy Mosque in Makkah", "مكة المكرمة - علي بن أحمد ملا", true),
    ATHAN_QATAR_NABET("Qatar - Saleh Al-Nabet", "State Grand Mosque of Qatar", "قطر - صالح النابت", true),
    ATHAN_RIYADH_QATAMI("Riyadh - Nasser Al-Qatami", "Saudi capital soulful recitation", "الرياض - ناصر القطامي", true),
    DEVICE_DEFAULT("Device Default", "Use the device notification sound", "صوت الإشعارات الافتراضي للجهاز", false),
    VIBRATE_ONLY("Vibrate Only", "Haptic vibration without audio", "اهتزاز فقط", false),
    SILENT("Silent", "Visual notification only", "صامت", false);

    fun localizedDisplayName(isArabic: Boolean): String =
        if (isArabic && displayNameAr.isNotBlank()) displayNameAr else displayName

    companion object {
        private val fajrAthans = listOf(
            ATHAN_EGYPT_ALALFI,
            ATHAN_FAJR2_JORDAN_ALLALA,
            ATHAN_FAJR1_KWAIT_ALAFASY
        )
        private val regularAthans = entries.filter { it.isFullAthan && it != ATHAN_DEFAULT && it !in fajrAthans }

        fun selectableValues(isArabic: Boolean): List<NotificationSoundType> {
            val collator = Collator.getInstance(Locale.forLanguageTag(if (isArabic) "ar" else "en"))
            val alphabetizedAthans = regularAthans.sortedWith { first, second ->
                collator.compare(first.localizedDisplayName(isArabic), second.localizedDisplayName(isArabic))
            }
            return listOf(ATHAN_DEFAULT) +
                fajrAthans +
                alphabetizedAthans +
                listOf(DEVICE_DEFAULT, VIBRATE_ONLY, SILENT)
        }

        // Fallback backward-compatibility mappings
        val FULL_ATHAN get() = ATHAN_DEFAULT
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

typealias PrayerTimeAdjustments = com.salati.shared.PrayerTimeAdjustments
typealias UserLocation = com.salati.shared.UserLocation
