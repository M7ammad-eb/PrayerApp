package com.example.ui.locale

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.models.AppLanguage
import com.example.data.models.CalculationMethod
import com.example.data.models.HighLatitudeRule
import com.example.data.models.JuristicMethod
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Provides comprehensive application-wide localization strings, layout direction, and formatting.
 * Guaranteed to use Western Arabic numerals (1, 2, 3, 4...) across all Arabic interfaces.
 */
class AppStrings(val isArabic: Boolean, val language: AppLanguage = if (isArabic) AppLanguage.ARABIC else AppLanguage.ENGLISH) {

    // App Branding
    val appBrandName: String = if (isArabic) "صلاتي" else "Salati"
    val appSubtitle: String = if (isArabic) "أوقات الصلاة والقبلة" else "Prayer Times & Qibla"

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
    val gotIt: String = if (isArabic) "حسناً" else "Got It"
    val close: String = if (isArabic) "إغلاق" else "Close"
    val search: String = if (isArabic) "بحث" else "Search"
    val change: String = if (isArabic) "تغيير" else "Change"
    val remaining: String = if (isArabic) "متبقي" else "Remaining"
    val timeForPrayer: String = if (isArabic) "حان الآن وقت الصلاة" else "Time for Prayer!"
    val now: String = if (isArabic) "الآن" else "Now"
    val select: String = if (isArabic) "اختيار" else "Select"
    val active: String = if (isArabic) "مفعّل" else "Active"

    // Next Prayer / Hero
    val nextPrayerLabel: String = if (isArabic) "الصلاة القادمة" else "NEXT PRAYER"
    val tomorrowPrayerLabel: String = if (isArabic) "صلاة الغد" else "TOMORROW'S PRAYER"
    val nextLabelPrefix: String = if (isArabic) "القادمة:" else "NEXT:"
    val athanAlert: String = if (isArabic) "تنبيه الأذان" else "Athan Alert"
    val athanAt: String = if (isArabic) "الأذان في" else "Athan at"
    val stopAudio: String = if (isArabic) "إيقاف الصوت" else "Stop Audio"
    val listenAthan: String = if (isArabic) "استماع للأذان" else "Listen to Athan"
    val hijriCalendarDetails: String = if (isArabic) "تفاصيل التقويم الهجري" else "Hijri Calendar Details"

    // Bento Quick Cards
    val qiblaCardTitle: String = if (isArabic) "القبلة" else "QIBLA"
    val qiblaAccurateDirection: String = if (isArabic) "الاتجاه الدقيق" else "Accurate Direction"
    val ummAlQuraTitle: String = if (isArabic) "تقويم أم القرى" else "UMM AL-QURA"
    val makkahCalendar: String = if (isArabic) "تقويم مكة المكرمة" else "Makkah Calendar"
    val whiteDayBadge: String = if (isArabic) "الأيام البيض (سنة)" else "White Day (Sunnah)"
    val sacredMonthBadge: String = if (isArabic) "شهر حرام" else "Sacred Month"

    // Date Picker / Navigation
    val previousDay: String = if (isArabic) "اليوم السابق" else "Previous Day"
    val nextDay: String = if (isArabic) "اليوم التالي" else "Next Day"
    val goToToday: String = if (isArabic) "العودة لليوم" else "Go to Today"
    val pickDate: String = if (isArabic) "اختيار تاريخ" else "Pick Date"

    // Prayer Names (Single clean standard name per language)
    fun prayerName(type: PrayerType): String {
        return when (language) {
            AppLanguage.ARABIC -> when (type) {
                PrayerType.FAJR -> "الفجر"
                PrayerType.SUNRISE -> "الشروق"
                PrayerType.DHUHR -> "الظهر"
                PrayerType.ASR -> "العصر"
                PrayerType.MAGHRIB -> "المغرب"
                PrayerType.ISHA -> "العشاء"
            }
            AppLanguage.ENGLISH -> type.title
            AppLanguage.SYSTEM -> {
                if (isArabic) {
                    when (type) {
                        PrayerType.FAJR -> "الفجر"
                        PrayerType.SUNRISE -> "الشروق"
                        PrayerType.DHUHR -> "الظهر"
                        PrayerType.ASR -> "العصر"
                        PrayerType.MAGHRIB -> "المغرب"
                        PrayerType.ISHA -> "العشاء"
                    }
                } else {
                    type.title
                }
            }
        }
    }

    // Additional / Sunnah Times Card
    val additionalTimesTitle: String = if (isArabic) "أوقات السنن وقيام الليل" else "Sunnah & Night Calculations"
    val midnight: String = if (isArabic) "منتصف الليل الإسلامي" else "Islamic Midnight"
    val lastThirdNight: String = if (isArabic) "ثلث الليل الأخير (قيام الليل والتهجد)" else "Qiyam / Last 1/3 of Night"
    val imsak: String = if (isArabic) "الإمساك (الصيام)" else "Imsak (Fasting Stop)"
    val duha: String = if (isArabic) "وقت صلاة الضحى (تقريبي)" else "Duha Time (approx.)"

    // Notification Sound Types
    fun soundTypeName(type: NotificationSoundType): String {
        return if (isArabic) {
            when (type) {
                NotificationSoundType.ATHAN_MAKKAH_MULLA -> "أذان مكة المكرمة - علي أحمد ملا"
                NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY -> "أذان الفجر (الكويت) - مشاري العفاسي"
                NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA -> "أذان الفجر (الأردن) - كامل اللالا"
                NotificationSoundType.ATHAN_RIYADH_QATAMI -> "أذان الرياض - ناصر القطامي"
                NotificationSoundType.ATHAN_QATAR_NABET -> "أذان قطر - صالح النابت"
                NotificationSoundType.ATHAN_QUDS_QAZAZ_1 -> "أذان المسجد الأقصى - ناجي قزاز (١)"
                NotificationSoundType.ATHAN_QUDS_QAZAZ_2 -> "أذان المسجد الأقصى - ناجي قزاز (٢)"
                NotificationSoundType.ATHAN_EGYPT_DAWOD -> "أذان مصر - أحمد داود"
                NotificationSoundType.ATHAN_EGYPT_ALALFI -> "أذان مصر - صلاح الألفي"
                NotificationSoundType.ATHAN_EGYPT_ABDULAATI -> "أذان مصر (الحسين) - سيد عبد العاطي"
                NotificationSoundType.ATHAN_IRAQ_ALAMOURI -> "أذان العراق - أبو عمر العامري"
                NotificationSoundType.ATHAN_GEORGIA -> "أذان جورجيا (تبليسي)"
                NotificationSoundType.SHORT_TAKBEER -> "تكبير فقط (الله أكبر)"
                NotificationSoundType.MELODIC_TONE -> "نغمة هادئة"
                NotificationSoundType.VIBRATE_ONLY -> "اهتزاز فقط"
                NotificationSoundType.SILENT -> "صامت / إيقاف"
            }
        } else {
            when (type) {
                NotificationSoundType.ATHAN_MAKKAH_MULLA -> "Makkah - Ali Bin Ahmad Mullah"
                NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY -> "Fajr (Kuwait) - Mishary Alafasy"
                NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA -> "Fajr (Jordan) - Kamel Allala"
                NotificationSoundType.ATHAN_RIYADH_QATAMI -> "Riyadh - Nasser Al-Qatami"
                NotificationSoundType.ATHAN_QATAR_NABET -> "Qatar - Saleh Al-Nabet"
                NotificationSoundType.ATHAN_QUDS_QAZAZ_1 -> "Al-Aqsa Al-Quds - Naji Qazzaz (1)"
                NotificationSoundType.ATHAN_QUDS_QAZAZ_2 -> "Al-Aqsa Al-Quds - Naji Qazzaz (2)"
                NotificationSoundType.ATHAN_EGYPT_DAWOD -> "Egypt - Ahmad Dawod"
                NotificationSoundType.ATHAN_EGYPT_ALALFI -> "Egypt - Salah Al-Alfi"
                NotificationSoundType.ATHAN_EGYPT_ABDULAATI -> "Egypt (Al-Hussein) - Sayed Abdulaati"
                NotificationSoundType.ATHAN_IRAQ_ALAMOURI -> "Iraq - Abu Omar Al-Amouri"
                NotificationSoundType.ATHAN_GEORGIA -> "Georgia Mosque Adhan"
                NotificationSoundType.SHORT_TAKBEER -> "Takbeer Only"
                NotificationSoundType.MELODIC_TONE -> "Gentle Chime"
                NotificationSoundType.VIBRATE_ONLY -> "Vibrate Only"
                NotificationSoundType.SILENT -> "Silent / Off"
            }
        }
    }

    fun soundTypeSubtitle(type: NotificationSoundType): String {
        return if (isArabic) {
            when (type) {
                NotificationSoundType.ATHAN_MAKKAH_MULLA -> "أذان الحرم المكي الشريف بصوت شيخ المؤذنين"
                NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY -> "أذان الفجر المخصوص بعبارة (الصلاة خير من النوم)"
                NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA -> "أذان الفجر التراثي من المسجد الحسيني الكبير"
                NotificationSoundType.ATHAN_RIYADH_QATAMI -> "أذان ندي وخاشع من مساجد الرياض"
                NotificationSoundType.ATHAN_QATAR_NABET -> "أذan جامع الإمام محمد بن عبد الوهاب بدولة قطر"
                NotificationSoundType.ATHAN_QUDS_QAZAZ_1 -> "أذان المسجد الأقصى المبارك بمقام الحجاز"
                NotificationSoundType.ATHAN_QUDS_QAZAZ_2 -> "أذان المسجد الأقصى المبارك بمقام البياتي"
                NotificationSoundType.ATHAN_EGYPT_DAWOD -> "أذان الإذاعة المصرية التراثي الأصيل"
                NotificationSoundType.ATHAN_EGYPT_ALALFI -> "أذان الجامع الأزهر الشريف ومساجد القاهرة"
                NotificationSoundType.ATHAN_EGYPT_ABDULAATI -> "أذان مسجد الإمام الحسين عليه السلام بالقاهرة"
                NotificationSoundType.ATHAN_IRAQ_ALAMOURI -> "أذان بغداد والمساجد العراقية بنغم البياتي العريق"
                NotificationSoundType.ATHAN_GEORGIA -> "أذان جامع تبليسي التاريخي في جورجيا"
                NotificationSoundType.SHORT_TAKBEER -> "تكبيرات العيد والأذان المختصرة"
                NotificationSoundType.MELODIC_TONE -> "نغمة هادئة للتنبيه في العمل والاجتماعات"
                NotificationSoundType.VIBRATE_ONLY -> "اهتزاز فقط دون إصدار صوت"
                NotificationSoundType.SILENT -> "إشعار بصري فقط"
            }
        } else {
            type.subtitle
        }
    }

    // Qibla Screen
    val qiblaTitle: String = if (isArabic) "بوصلة القبلة الدقيقة" else "Qibla Direction Compass"
    val qiblaAlignedMessage: String = if (isArabic) "أنت الآن باتجاه الكعبة المشرفة بدقة!" else "You are facing the Kaaba precisely!"
    val qiblaRotatePrompt: String = if (isArabic) "قم بتدوير الهاتف حتى يتطابق المؤشر الذهبي مع الأعلى" else "Rotate your device to align with the Golden Kaaba marker"
    val qiblaBearingLabel: String = if (isArabic) "زاوية القبلة:" else "Qibla Bearing:"
    val currentHeadingLabel: String = if (isArabic) "الاتجاه الحالي:" else "Current Heading:"
    val distanceToKaabaLabel: String = if (isArabic) "المسافة إلى الكعبة المشرفة:" else "Distance to Kaaba:"
    val kmUnit: String = if (isArabic) "كم" else "km"
    val degUnit: String = if (isArabic) "°" else "°"
    val compassCalibTitle: String = if (isArabic) "معايرة البوصلة" else "Compass Calibration"
    val compassCalibText: String = if (isArabic) "لمعايرة بوصلة الهاتف، حرّك جهازك بسلاسة بشكل رقم 8 في الهواء عدة مرات بعيداً عن المعادن والمغناطيس." else "To calibrate your device's magnetic compass, move your phone smoothly in a figure-8 motion several times away from metal objects."
    val manualCompassMode: String = if (isArabic) "وضع البوصلة اليدوي (اختبار)" else "Manual Compass Test Slider"

    // Cardinal directions
    fun cardinalDirection(heading: Double): String {
        val normalized = ((heading % 360) + 360) % 360
        return if (isArabic) {
            when {
                normalized >= 337.5 || normalized < 22.5 -> "شمال (N)"
                normalized < 67.5 -> "شمال شرق (NE)"
                normalized < 112.5 -> "شرق (E)"
                normalized < 157.5 -> "جنوب شرق (SE)"
                normalized < 202.5 -> "جنوب (S)"
                normalized < 247.5 -> "جنوب غرب (SW)"
                normalized < 292.5 -> "غرب (W)"
                else -> "شمال غرب (NW)"
            }
        } else {
            when {
                normalized >= 337.5 || normalized < 22.5 -> "N"
                normalized < 67.5 -> "NE"
                normalized < 112.5 -> "E"
                normalized < 157.5 -> "SE"
                normalized < 202.5 -> "S"
                normalized < 247.5 -> "SW"
                normalized < 292.5 -> "W"
                else -> "NW"
            }
        }
    }

    // Monthly Calendar
    val monthlyCalendarTitle: String = if (isArabic) "جدول أوقات الصلاة للشهر" else "Monthly Prayer Timetable"
    val dateCol: String = if (isArabic) "اليوم" else "Date"
    val hijriCol: String = if (isArabic) "الهجري" else "Hijri"
    val prevMonth: String = if (isArabic) "الشهر السابق" else "Previous Month"
    val nextMonth: String = if (isArabic) "الشهر التالي" else "Next Month"

    // Settings Screen
    val settingsTitle: String = if (isArabic) "الإعدادات والتفضيلات" else "Settings & Preferences"
    val themeSection: String = if (isArabic) "المظهر والألوان" else "Appearance & Theme"
    val themeModeTitle: String = if (isArabic) "وضع المظهر (فاتح / داكن)" else "Theme Mode"
    val systemThemeDesc: String = if (isArabic) "متابعة إعدادات النظام تلقائياً" else "Follow system dark/light settings"
    val lightThemeDesc: String = if (isArabic) "الوضع الفاتح دائماً" else "Always use light theme"
    val darkThemeDesc: String = if (isArabic) "الوضع الداكن دائماً ومريح للعين" else "Always use dark theme"

    fun themeModeName(mode: com.example.data.models.AppThemeMode): String {
        return if (isArabic) mode.arabicTitle else mode.title
    }

    val colorPaletteSection: String = if (isArabic) "لوحة الألوان وسمة التطبيق" else "Color Palette & Accent Colors"
    val followSystemColorsTitle: String = if (isArabic) "متابعة ألوان النظام في التطبيق (Material You)" else "App Follow System Colors"
    val followSystemColorsDesc: String = if (isArabic) "استخراج الألوان التلقائي المتناسق مع خلفية جهازك لواجهة التطبيق" else "Dynamically match device wallpaper and system accents in app"
    val presetColorsTitle: String = if (isArabic) "لوحات ألوان جاهزة" else "Preset Color Palettes"
    val presetColorsDesc: String = if (isArabic) "اختر لوناً مميزاً لتخصيص واجهة التطبيق" else "Select a curated color palette for the app interface"

    fun colorPresetName(preset: com.example.data.models.AppColorPreset): String {
        return if (isArabic) preset.arabicTitle else preset.title
    }

    val languageSection: String = if (isArabic) "اللغة والعرض" else "Language & Localization"
    val appLanguage: String = if (isArabic) "لغة التطبيق" else "App Language"
    val selectLanguageSubtitle: String = if (isArabic) "اختر لغة واجهة التطبيق" else "Choose application interface language"
    val locationSection: String = if (isArabic) "الموقع والإحداثيات" else "Location & Coordinates"
    val useGps: String = if (isArabic) "تحديد الموقع تلقائياً (GPS)" else "Use GPS Location"
    val chooseCity: String = if (isArabic) "اختيار مدينة عالمية" else "World Cities"
    val manualCoordinates: String = if (isArabic) "إدخال الإحداثيات يدوياً (خط الطول/العرض)" else "Enter Lat / Lon Coordinates"
    val calcMethodSection: String = if (isArabic) "طريقة الحساب الفلكي" else "Calculation Method"
    val juristicMethodTitle: String = if (isArabic) "المذهب الفقهي لصلاة العصر" else "Asr Juristic Method"
    val standardJuristic: String = if (isArabic) "الجمهور (الشافعي، المالكي، الحنبلي)" else "Standard (Shafi/Maliki/Hanbali)"
    val hanafiJuristic: String = if (isArabic) "المذهب الحنفي (ظل المثلين)" else "Hanafi"
    val highLatitudeSection: String = if (isArabic) "قاعدة المناطق ذات خطوط العرض العالية" else "High Latitude Rule"
    val displayHijriSection: String = if (isArabic) "العرض والتقويم الهجري" else "Display & Hijri Adjustments"
    val hijriOffsetTitle: String = if (isArabic) "تعديل التاريخ الهجري (تقويم أم القرى)" else "Hijri Date Offset (Umm al-Qura)"
    val timeFormatTitle: String = if (isArabic) "نظام الوقت 24 ساعة" else "24-Hour Time Format"
    val minuteAdjustmentsTitle: String = if (isArabic) "تعديل الدقائق يدوياً لكل صلاة" else "Manual Minute Adjustments"
    val minuteAdjustmentsSubtitle: String = if (isArabic) "لمطابقة توقيت المسجد المحلي بالضبط" else "Sync with your local mosque timetable"
    val notifSectionTitle: String = if (isArabic) "تنبيهات الأذان والإشعارات" else "Athan & Prayer Notifications"
    val athanSound: String = if (isArabic) "صوت الأذان والتنبيه" else "Athan Sound"
    val preReminder: String = if (isArabic) "تنبيه مبكر قبل الصلاة" else "Pre-Prayer Reminder"
    val minutesBefore: String = if (isArabic) "دقيقة قبل" else "min before"
    val testAlert: String = if (isArabic) "تجربة التنبيه الفوري" else "Test Instant Alert"
    val testAlarm5s: String = if (isArabic) "تجربة إنذار الصلاة (بعد 5 ثوانٍ)" else "Test Prayer Alarm (in 5s)"
    val reschedule7Days: String = if (isArabic) "إعادة جدولة جميع منبهات الأسبوع (7 أيام)" else "Reschedule 7-Day Alarms"
    val notificationsDisabledWarning: String = if (isArabic) "تنبيهات الأذان معطلة في إعدادات النظام. يرجى تفعيل الإشعارات حتى يعمل الأذان في موعده." else "Prayer alerts & Athan notifications are disabled in system settings. Please enable them to receive timely prayer alarms."
    val enableNotificationsBtn: String = if (isArabic) "تفعيل الإشعارات الآن" else "Enable Notifications"
    val notificationsStatusActive: String = if (isArabic) "الإشعارات ومنبهات الأذان مفعلة وجاهزة بنجاح." else "Prayer notifications & exact alarms are active and ready."

    // Audio Output Channel & Wake Screen Settings
    val audioStreamSectionTitle: String = if (isArabic) "قناة صوت الأذان (Audio Stream)" else "Athan Audio Stream Channel"
    val audioStreamDesc: String = if (isArabic) "اختر القناة الصوتية التي يعمل بها صوت الأذان ليتبع مستوى صوت النظام المختار." else "Choose which system audio channel the Athan plays through. It will follow your device's system volume."
    val audioStreamAlarmTitle: String = if (isArabic) "مستوى صوت المنبه (Alarm)" else "Alarm Stream (Recommended)"
    val audioStreamAlarmDesc: String = if (isArabic) "يعمل بمستوى صوت منبه الهاتف، ويرن حتى في الوضع الصامت إذا سمح النظام." else "Plays at alarm volume level, sounds reliably when idle or silent."
    val audioStreamMediaTitle: String = if (isArabic) "مستوى صوت الوسائط (Media)" else "Media Stream"
    val audioStreamMediaDesc: String = if (isArabic) "يعمل بمستوى صوت الموسيقى والفيديوهات." else "Plays at music/video media volume level."
    val audioStreamRingtoneTitle: String = if (isArabic) "مستوى صوت رنين الهاتف (Ringtone)" else "Ringtone Stream"
    val audioStreamRingtoneDesc: String = if (isArabic) "يعمل بمستوى نغمة رنين المكالمات." else "Plays at incoming call ringtone volume level."
    
    val wakeScreenTitle: String = if (isArabic) "إيقاظ الشاشة وعرض منبه الأذان بالكامل (عند إغلاق الشاشة فقط)" else "Wake Screen & Full-Screen Alarm (Screen Off Only)"
    val wakeScreenDesc: String = if (isArabic) "إضاءة الشاشة تلقائياً وعرض اللوحة الفنية الكاملة فقط إذا كانت الشاشة مغلقة. أثناء استخدام الهاتف، يظهر إشعار علوي أنيق دون مقاطعتك." else "Turn on the display and show full-screen prayer artwork only if the phone is locked/idle. When actively using your phone, a heads-up notification appears."
    val previewFullScreenAlarmBtn: String = if (isArabic) "🖼️ معاينة شاشة منبه الأذان الفنية الكاملة" else "🖼️ Preview Full-Screen Alarm Artwork"

    // Dynamic Island / Live Countdown Feature
    val dynamicIslandSectionTitle: String = if (isArabic) "الجزيرة التفاعلية والعد التنازلي المباشر (Live Activity)" else "Dynamic Island & Live Activity Countdown"
    val dynamicIslandDesc: String = if (isArabic) "عرض كبسولة تفاعلية وعد تنازلي مباشر في أعلى الشاشة وشريط الإشعارات قبل حلول وقت الصلاة (مثل Keeta و Live Activities)." else "Display a live interactive countdown capsule at the top of your screen and notification bar before prayer time (similar to Keeta Live Activities)."
    val dynamicIslandEnableTitle: String = if (isArabic) "تفعيل كبسولة الجزيرة التفاعلية" else "Enable Dynamic Island Capsule"
    val dynamicIslandLeadTimeTitle: String = if (isArabic) "بدء العد التنازلي قبل الصلاة بـ:" else "Start Countdown Before Prayer:"
    val dynamicIslandVivoTip: String = if (isArabic) "💡 لأجهزة Vivo / iQOO / OriginOS: لتفعيل كبسولة الجزيرة (Atomic Island)، يرجى التأكد من تشغيل 'الأنشطة الحية / Live Alerts' لتطبيق مواقيت الصلاة من إعدادات الهاتف > الإشعارات." else "💡 For Vivo / iQOO / OriginOS users: Ensure 'Live Alerts / Atomic Island' is turned ON for Prayer Times in Phone Settings > Notifications."
    val previewDynamicIslandBtn: String = if (isArabic) "✨ معاينة كبسولة الجزيرة التفاعلية الآن" else "✨ Preview Dynamic Island Countdown"
    val dismissDynamicIslandBtn: String = if (isArabic) "إخفاء الجزيرة التفاعلية" else "Dismiss Island Preview"

    // Calculation Method translations
    fun calcMethodName(method: CalculationMethod): String {
        return if (isArabic) {
            when (method) {
                CalculationMethod.UMM_AL_QURA -> "جامعة أم القرى (مكة المكرمة)"
                CalculationMethod.MUSLIM_WORLD_LEAGUE -> "رابطة العالم الإسلامي"
                CalculationMethod.EGYPTIAN -> "الهيئة المصرية العامة للمساحة"
                CalculationMethod.KARACHI -> "جامعة العلوم الإسلامية بكراتشي"
                CalculationMethod.ISNA -> "الجمعية الإسلامية لأمريكا الشمالية (ISNA)"
                CalculationMethod.GULF -> "منطقة الخليج العربي (دبي والإمارات)"
                CalculationMethod.QATAR -> "وزارة الأوقاف والشؤون الإسلامية بقطر"
                CalculationMethod.KUWAIT -> "وزارة الأوقاف والشؤون الإسلامية بالكويت"
                CalculationMethod.TURKEY -> "رئاسة الشؤون الدينية التركية (ديانت)"
                CalculationMethod.TEHRAN -> "معهد الجيوفيزياء بجامعة طهران"
                CalculationMethod.SHIA_ITHNA_ASHARI -> "معهد لواء (الشيعة الإثنا عشرية)"
                CalculationMethod.SINGAPORE -> "مجلس الشؤون الإسلامية بسنغافورة (MUIS)"
                CalculationMethod.FRANCE_UOIF -> "اتحاد المنظمات الإسلامية بفرنسا (UOIF)"
                CalculationMethod.RUSSIA -> "الإدارة الدينية لمسلمي روسيا (SAMR)"
            }
        } else {
            method.displayName
        }
    }

    // High Latitude Rule translations
    fun highLatitudeName(rule: HighLatitudeRule): String {
        return if (isArabic) {
            when (rule) {
                HighLatitudeRule.MIDNIGHT -> "منتصف الليل (Middle of Night)"
                HighLatitudeRule.ONE_SEVENTH -> "سُبع الليل (One Seventh of Night)"
                HighLatitudeRule.ANGLE_BASED -> "على أساس الزاوية النسبية (Angle-Based)"
                HighLatitudeRule.NONE -> "بدون تعديل (None)"
            }
        } else {
            rule.displayName
        }
    }

    // Athan Player Dialog
    val athanDialogTitle: String = if (isArabic) "تلاوة الأذان الشريف" else "Athan Recitation"
    val playAdhan: String = if (isArabic) "تشغيل الأذان" else "Play Adhan"
    val fajrAdhan: String = if (isArabic) "أذان الفجر" else "Fajr Adhan"
    val stopBtn: String = if (isArabic) "إيقاف" else "Stop"
    val athanPromptText: String = if (isArabic) "استمع إلى النداء المقدس لنداء الصلاة" else "Listen to the sacred Islamic call to prayer"

    // Hijri Dialog
    val hijriCalendarHeader: String = if (isArabic) "التقويم الهجري" else "Hijri Calendar"
    val ummAlQuraStandard: String = if (isArabic) "معيار تقويم أم القرى (مكة المكرمة)" else "Umm al-Qura Standard"
    val convertDate: String = if (isArabic) "تحويل التاريخ" else "Convert Date"
    val adjustDays: String = if (isArabic) "تعديل الأيام" else "Adjust Days"
    val sunnahFastingDay: String = if (isArabic) "يوم صيام سنة" else "Sunnah Fasting Day"
    val whiteDaysTitle: String = if (isArabic) "الأيام البيض (الأيام 13 و 14 و 15)" else "White Days (13th, 14th, 15th)"
    val whiteDaysDesc: String = if (isArabic) "صيام الأيام البيض من كل شهر هجري سنة مؤكدة عن النبي ﷺ." else "Fasting the 13th, 14th, and 15th of each Hijri month is an established Sunnah of the Prophet ﷺ."
    val sacredMonthTitle: String = if (isArabic) "الأشهر الحرم (المحرم، رجب، ذو القعدة، ذو الحجة)" else "Sacred Months (Muharram, Rajab, Dhu al-Qi'dah, Dhu al-Hijjah)"
    val sacredMonthDesc: String = if (isArabic) "من الأشهر الحرم الأربعة التي عظم الله شأنها وتتضاعف فيها الأجور." else "One of the four sacred months in Islam in which good deeds are amplified."
    val mondayThursdayDesc: String = if (isArabic) "صيام يومي الإثنين والخميس سنة نبوية مباركة." else "Fasting on Mondays and Thursdays is an authentic Sunnah practice."
    val aboutUmmAlQuraTitle: String = if (isArabic) "عن تقويم أم القرى الرسمي" else "About Umm al-Qura Calendar"
    val aboutUmmAlQuraDesc: String = if (isArabic) "تقويم أم القرى هو التقويم القمري الرسمي الصادر عن مدينة الملك عبد العزيز للعلوم والتقنية (KACST) لمدينة مكة المكرمة ويعتمد الحسابات الفلكية الشرعية الدقيقة." else "The Umm al-Qura calendar is the official standardized Islamic lunar calendar calculated for the holy city of Makkah al-Mukarramah."

    // Manual Coordinates Dialog
    val customCoordinates: String = if (isArabic) "الإحداثيات اليدوية" else "Custom Coordinates"
    val enterCoordinates: String = if (isArabic) "إدخال خط الطول وخط العرض بدقة" else "Enter Latitude & Longitude"
    val manualCoordinatesDesc: String = if (isArabic) "حدد إحداثيات جغرافية دقيقة للمناطق البرية، المخيمات الصحراوية، أو الأماكن التي لا يتوفر فيها اتصال GPS." else "Specify precise geographical coordinates for off-grid areas, desert camps, or locations without GPS signal."
    val quickPresets: String = if (isArabic) "إحداثيات مدن سريعة" else "Quick Presets"
    val latitudeLabel: String = if (isArabic) "خط العرض (°)" else "Latitude (°)"
    val longitudeLabel: String = if (isArabic) "خط الطول (°)" else "Longitude (°)"
    val locationNameLabel: String = if (isArabic) "اسم الموقع / الوصف" else "Location Name / Label"
    val timeZoneLabel: String = if (isArabic) "المنطقة الزمنية" else "Time Zone"
    val applyCoordinates: String = if (isArabic) "تطبيق الإحداثيات" else "Apply Coordinates"

    // Setup & First-Time Onboarding
    val welcomeToApp: String = if (isArabic) "مرحباً بك في صلاتي" else "Welcome to Salati"
    val setupSubtitle: String = if (isArabic) "دعنا نضبط تطبيقك في خطوات بسيطة لتجربة صلاة متكاملة ودقيقة." else "Let's personalize your prayer experience in a few quick steps."
    val stepLanguageTitle: String = if (isArabic) "اختر لغة التطبيق" else "Choose Language"
    val stepLanguageDesc: String = if (isArabic) "يمكنك التبديل بين العربية والإنجليزية في أي وقت من الإعدادات." else "You can switch between Arabic and English anytime in settings."
    val stepLocationTitle: String = if (isArabic) "حدد موقعك الجغرافي" else "Set Your Location"
    val stepLocationDesc: String = if (isArabic) "نحتاج إلى موقعك لحساب أوقات الصلاة واتجاه القبلة بدقة شرعية عالية." else "Required to calculate exact prayer times and Qibla direction for your location."
    val privacyNoticeTitle: String = if (isArabic) "خصوصيتك محمية بالكامل" else "Your Privacy is 100% Protected"
    val privacyNoticeDesc: String = if (isArabic) "بيانات موقعك لا تُجمع، ولا تُخزن على أي خوادم خارجية، ولا يتم تتبعك إطلاقاً. تُستخدم الإحداثيات محلياً على جهازك فقط لحساب المواقيت والقبلة." else "Your location data is never collected, tracked, or uploaded to any server. All astronomical prayer and Qibla calculations run entirely offline on your device."
    val useGpsButton: String = if (isArabic) "تحديد الموقع التلقائي (GPS)" else "Detect Location (GPS)"
    val searchCityPlaceholder: String = if (isArabic) "أو ابحث عن مدينتك (مكة، القاهرة، لندن...)" else "Or search your city (Makkah, Cairo, London...)"
    val selectedLocationLabel: String = if (isArabic) "الموقع المحدد:" else "Selected Location:"
    val stepNotificationsTitle: String = if (isArabic) "تنبيهات الأذان والإشعارات" else "Athan Alerts & Notifications"
    val stepNotificationsDesc: String = if (isArabic) "تفعيل إذن الإشعارات والمنبه الدقيق لضمان انطلاق صوت الأذان والتنبيهات في وقت الصلاة تماماً حتى عند قفل الهاتف." else "Enable notifications and exact alarm permissions so the Athan sounds on time, even when your phone is locked or screen is off."
    val stepNotificationsExplanation: String = if (isArabic) "لماذا نحتاج هذا الإذن؟\n• إطلاق صوت الأذان الشريف مع دخول وقت الصلاة.\n• عرض كبسولة العد التنازلي التفاعلية (Dynamic Island).\n• تذكيرك بالسنن والأيام البيض." else "Why are permissions needed?\n• Play the holy Athan recitation at exact prayer times.\n• Show interactive countdown activities before prayer.\n• Remind you of Sunnah fasting and White Days."
    val grantNotificationPermissionBtn: String = if (isArabic) "منح إذن الإشعارات" else "Grant Notification Permission"
    val permissionGrantedStatus: String = if (isArabic) "تم تفعيل الإشعارات بنجاح ✓" else "Notifications Enabled ✓"
    val stepAthanSoundsTitle: String = if (isArabic) "أصوات الأذان والتنبيهات" else "Athan Sounds & Alerts"
    val stepAthanSoundsDesc: String = if (isArabic) "اختر صوت الأذان المفضل لكل صلاة. يمكنك تغييرها وتخصيصها لاحقاً." else "Choose your preferred Athan recitation for each prayer. You can customize them anytime."
    val stepStyleTitle: String = if (isArabic) "المظهر والتصميم" else "Appearance & Style"
    val stepStyleDesc: String = if (isArabic) "اختر الثيم ونمط الألوان المفضل لديك." else "Choose your favorite theme and color palette."
    val getStartedBtn: String = if (isArabic) "ابدأ الآن واستمتع بصلاتي" else "Get Started"
    val nextStepBtn: String = if (isArabic) "التالي" else "Next"
    val previousStepBtn: String = if (isArabic) "السابق" else "Back"
    val stepIndicatorText: String = if (isArabic) "الخطوة" else "Step"
    val ofStepText: String = if (isArabic) "من" else "of"
    val gpsDetecting: String = if (isArabic) "جارٍ تحديد الموقع عبر GPS..." else "Detecting GPS location..."

    // Widget Customization Strings
    val widgetsSection: String = if (isArabic) "التطبيقات المصغرة (الودجت)" else "Home Screen Widgets"
    val widgetsSectionSubtitle: String = if (isArabic) "تخصيص الألوان، الشفافية، الخط والمحتوى" else "Customize colors, opacity, fonts & content"
    val widgetThemeModeTitle: String = if (isArabic) "ثيم وألوان الودجت" else "Widget Theme & Palette"
    val widgetBgStyleTitle: String = if (isArabic) "نمط الخلفية" else "Background Style"
    val widgetOpacityTitle: String = if (isArabic) "درجة الشفافية" else "Background Opacity"
    val widgetFontSizeTitle: String = if (isArabic) "حجم الخط" else "Font Size"
    val widgetContentTitle: String = if (isArabic) "محتوى وعناصر الودجت" else "Widget Content & Toggles"
    val widgetPreviewTitle: String = if (isArabic) "معاينة تفاعلية حية" else "Live Interactive Preview"
    val widgetRefreshAll: String = if (isArabic) "تحديث وتطبيق الودجت الآن" else "Apply & Refresh Widgets"
    val widgetResetDefaults: String = if (isArabic) "استعادة الإعدادات الافتراضية" else "Reset to Defaults"

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

    fun monthName(month: Int): String {
        return if (isArabic) {
            when (month) {
                1 -> "يناير"
                2 -> "فبراير"
                3 -> "مارس"
                4 -> "أبريل"
                5 -> "مايو"
                6 -> "يونيو"
                7 -> "يوليو"
                8 -> "أغسطس"
                9 -> "سبتمبر"
                10 -> "أكتوبر"
                11 -> "نوفمبر"
                else -> "ديسمبر"
            }
        } else {
            when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                else -> "December"
            }
        }
    }

    fun formatDateShort(date: LocalDate): String {
        val day = date.dayOfMonth
        val month = monthName(date.monthValue)
        return "$day $month"
    }

    fun dayOfWeekName(dayOfWeek: DayOfWeek): String {
        return if (isArabic) {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "الإثنين"
                DayOfWeek.TUESDAY -> "الثلاثاء"
                DayOfWeek.WEDNESDAY -> "الأربعاء"
                DayOfWeek.THURSDAY -> "الخميس"
                DayOfWeek.FRIDAY -> "الجمعة"
                DayOfWeek.SATURDAY -> "السبت"
                DayOfWeek.SUNDAY -> "الأحد"
            }
        } else {
            dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

val LocalAppStrings = staticCompositionLocalOf {
    val isAr = Locale.getDefault().language.equals("ar", ignoreCase = true)
    AppStrings(isAr, if (isAr) AppLanguage.ARABIC else AppLanguage.ENGLISH)
}

@Composable
fun ProvideAppLocale(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isArabic = when (appLanguage) {
        AppLanguage.SYSTEM -> {
            val systemLang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val appLocales = try {
                    val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                    val locales = localeManager?.applicationLocales
                    if (locales != null && !locales.isEmpty) locales.get(0)?.language else null
                } catch (e: Exception) { null }
                appLocales ?: Locale.getDefault().language
            } else {
                Locale.getDefault().language
            }
            systemLang.equals("ar", ignoreCase = true)
        }
        AppLanguage.ARABIC -> true
        AppLanguage.ENGLISH -> false
    }

    val appStrings = AppStrings(isArabic, appLanguage)
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}
