package com.example.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.models.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Provides application-wide localization strings, layout direction, and formatting.
 * Guaranteed to use Western Arabic numerals (1, 2, 3, 4...) across all Arabic interfaces.
 */
class AppStrings(val isArabic: Boolean) {

    // Bottom Navigation
    val navPrayerTimes: String = if (isArabic) "أوقات الصلاة" else "Prayer Times"
    val navQibla: String = if (isArabic) "القبلة" else "Qibla"
    val navCalendar: String = if (isArabic) "التقويم" else "Calendar"
    val navSettings: String = if (isArabic) "الإعدادات" else "Settings"

    // Common
    val appTitle: String = if (isArabic) "أوقات الصلاة" else "Prayer Times"
    val today: String = if (isArabic) "اليوم" else "Today"
    val tomorrow: String = if (isArabic) "غداً" else "Tomorrow"
    val yesterday: String = if (isArabic) "أمس" else "Yesterday"
    val days: String = if (isArabic) "أيام" else "days"
    val day: String = if (isArabic) "يوم" else "day"
    val cancel: String = if (isArabic) "إلغاء" else "Cancel"
    val save: String = if (isArabic) "حفظ" else "Save"
    val done: String = if (isArabic) "تم" else "Done"
    val gotIt: String = if (isArabic) "فهمت" else "Got It"
    val close: String = if (isArabic) "إغلاق" else "Close"
    val search: String = if (isArabic) "بحث" else "Search"
    val remaining: String = if (isArabic) "متبقي" else "Remaining"
    val timeForPrayer: String = if (isArabic) "حان الآن وقت الصلاة" else "Time for Prayer!"
    val now: String = if (isArabic) "الآن" else "Now"

    // Next Prayer / Hero
    val nextPrayerLabel: String = if (isArabic) "الصلاة القادمة" else "Next Prayer"
    val nextLabelPrefix: String = if (isArabic) "القادمة:" else "NEXT:"
    val athanAlert: String = if (isArabic) "تنبيه الأذان" else "Athan Alert"
    val stopAudio: String = if (isArabic) "إيقاف الصوت" else "Stop Audio"
    val listenAthan: String = if (isArabic) "استماع للأذان" else "Listen Athan"
    val hijriCalendarDetails: String = if (isArabic) "تفاصيل التقويم الهجري" else "Hijri Calendar Details"

    // Prayer Names
    fun prayerName(type: com.example.data.models.PrayerType): String {
        return if (isArabic) {
            when (type) {
                com.example.data.models.PrayerType.FAJR -> "الفجر"
                com.example.data.models.PrayerType.SUNRISE -> "الشروق"
                com.example.data.models.PrayerType.DHUHR -> "الظهر"
                com.example.data.models.PrayerType.ASR -> "العصر"
                com.example.data.models.PrayerType.MAGHRIB -> "المغرب"
                com.example.data.models.PrayerType.ISHA -> "العشاء"
            }
        } else {
            type.title
        }
    }

    fun prayerSubtitle(type: com.example.data.models.PrayerType): String {
        return if (isArabic) {
            when (type) {
                com.example.data.models.PrayerType.FAJR -> "صلاة الصبح"
                com.example.data.models.PrayerType.SUNRISE -> "وقت الإشراق"
                com.example.data.models.PrayerType.DHUHR -> "صلاة الظهيرة"
                com.example.data.models.PrayerType.ASR -> "صلاة العصر"
                com.example.data.models.PrayerType.MAGHRIB -> "صلاة الغروب والإفطار"
                com.example.data.models.PrayerType.ISHA -> "صلاة الليل"
            }
        } else {
            when (type) {
                com.example.data.models.PrayerType.FAJR -> "Dawn Prayer"
                com.example.data.models.PrayerType.SUNRISE -> "Sunrise / Shurooq"
                com.example.data.models.PrayerType.DHUHR -> "Midday Prayer"
                com.example.data.models.PrayerType.ASR -> "Afternoon Prayer"
                com.example.data.models.PrayerType.MAGHRIB -> "Sunset / Iftar Prayer"
                com.example.data.models.PrayerType.ISHA -> "Night Prayer"
            }
        }
    }

    // Additional Times Card
    val additionalTimesTitle: String = if (isArabic) "أوقات إضافية ومستحبة" else "Additional Islamic Times"
    val midnight: String = if (isArabic) "منتصف الليل الإسلامي" else "Islamic Midnight"
    val lastThirdNight: String = if (isArabic) "الثلث الأخير من الليل (قيام الليل)" else "Last Third of Night (Qiyam)"
    val imsak: String = if (isArabic) "الإمساك (الصيام)" else "Imsak (Fasting Stop)"
    val duha: String = if (isArabic) "صلاة الضحى" else "Duha Prayer"

    // Qibla Screen
    val qiblaTitle: String = if (isArabic) "بوصلة القبلة الدقيقة" else "Qibla Direction Compass"
    val qiblaAlignedMessage: String = if (isArabic) "أنت الآن باتجاه الكعبة المشرفة بدقة!" else "You are facing the Kaaba precisely!"
    val qiblaRotatePrompt: String = if (isArabic) "قم بتدوير الهاتف حتى يتطابق المؤشر الذهبي مع الأعلى" else "Rotate your device to align with the Golden Kaaba marker"
    val qiblaAngle: String = if (isArabic) "زاوية القبلة:" else "Qibla Bearing:"
    val currentHeading: String = if (isArabic) "الاتجاه الحالي:" else "Current Heading:"
    val distanceToKaaba: String = if (isArabic) "المسافة إلى مكة المكرمة:" else "Distance to Kaaba:"
    val kmUnit: String = if (isArabic) "كم" else "km"
    val degUnit: String = if (isArabic) "°" else "°"
    val compassCalibTitle: String = if (isArabic) "معايرة البوصلة" else "Compass Calibration"
    val compassCalibText: String = if (isArabic) "لمعايرة بوصلة الهاتف، حرّك جهازك بشكل رقم 8 في الهواء عدة مرات بعيداً عن المعادن والمغناطيس." else "To calibrate your device's magnetic compass, move your phone smoothly in a figure-8 motion several times away from metal objects."
    val manualCompassMode: String = if (isArabic) "وضع البوصلة اليدوي (اختبار)" else "Manual Compass Test Slider"

    // Monthly Calendar
    val monthlyCalendarTitle: String = if (isArabic) "جدول أوقات الصلاة للشهر" else "Monthly Prayer Timetable"
    val dateCol: String = if (isArabic) "التاريخ" else "Date"
    val hijriCol: String = if (isArabic) "الهجري" else "Hijri"

    // Settings
    val settingsTitle: String = if (isArabic) "الإعدادات والخيارات" else "Settings & Preferences"
    val languageSection: String = if (isArabic) "اللغة والعرض" else "Language & Localization"
    val appLanguage: String = if (isArabic) "لغة التطبيق" else "App Language"
    val locationSection: String = if (isArabic) "الموقع والإحداثيات" else "Location & Coordinates"
    val useGps: String = if (isArabic) "تحديد الموقع تلقائياً (GPS)" else "Use GPS Location"
    val chooseCity: String = if (isArabic) "اختيار مدينة عالمية" else "World Cities"
    val manualCoordinates: String = if (isArabic) "إدخال الإحداثيات يدوياً (خط الطول/العرض)" else "Enter Lat / Lon Coordinates"
    val calcMethodSection: String = if (isArabic) "طريقة الحساب الفلكي" else "Calculation Method"
    val juristicMethodTitle: String = if (isArabic) "المذهب الفقهي لصلاة العصر" else "Asr Juristic Method"
    val standardJuristic: String = if (isArabic) "الجمهور (الشافعي، المالكي، الحنبلي)" else "Standard (Shafi/Maliki/Hanbali)"
    val hanafiJuristic: String = if (isArabic) "الحنفي (ظل المثلين)" else "Hanafi"
    val highLatitudeSection: String = if (isArabic) "قاعدة المناطق ذات خطوط العرض العالية" else "High Latitude Rule"
    val hijriOffsetTitle: String = if (isArabic) "تعديل التاريخ الهجري (تقويم أم القرى)" else "Hijri Date Offset (Umm al-Qura)"
    val timeFormatTitle: String = if (isArabic) "نظام الوقت 24 ساعة" else "24-Hour Time Format"
    val minuteAdjustmentsTitle: String = if (isArabic) "تعديل الدقائق يدوياً لكل صلاة" else "Manual Minute Adjustments"
    val minuteAdjustmentsSubtitle: String = if (isArabic) "لمطابقة توقيت المسجد المحلي بالضبط" else "Sync with your local mosque timetable"
    val notifSectionTitle: String = if (isArabic) "تنبيهات الأذان والإشعارات" else "Athan & Prayer Notifications"
    val athanSound: String = if (isArabic) "صوت الأذان والتنبيه" else "Athan Sound"
    val preReminder: String = if (isArabic) "تنبيه مبكر قبل الصلاة" else "Pre-Prayer Reminder"
    val minutesBefore: String = if (isArabic) "دقيقة قبل" else "min before"
    val testAlert: String = if (isArabic) "تجربة التنبيه" else "Test Alert"

    // Number & Time formatters that STRICTLY use 123 (Western Arabic Digits)
    fun formatNumber(number: Number): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val df = DecimalFormat("#,##0.##", symbols)
        return df.format(number)
    }

    fun formatCountdown(seconds: Long): String {
        if (seconds <= 0) return if (isArabic) "حان وقت الصلاة الآن!" else "Time for Prayer!"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val sec = seconds % 60
        return if (isArabic) {
            when {
                hours > 0 -> "باقي ${hours}س و ${minutes}د"
                minutes > 0 -> "باقي ${minutes}د و ${sec}ث"
                else -> "باقي ${sec} ثانية"
            }
        } else {
            when {
                hours > 0 -> "In ${hours}h ${minutes}m"
                minutes > 0 -> "In ${minutes}m ${sec}s"
                else -> "In ${sec}s"
            }
        }
    }
}

val LocalAppStrings = staticCompositionLocalOf { AppStrings(false) }

@Composable
fun ProvideAppLocale(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit
) {
    val isArabic = when (appLanguage) {
        AppLanguage.SYSTEM -> {
            val systemLang = Locale.getDefault().language
            systemLang.equals("ar", ignoreCase = true)
        }
        AppLanguage.ARABIC -> true
        AppLanguage.ENGLISH -> false
    }

    val appStrings = AppStrings(isArabic)
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}
