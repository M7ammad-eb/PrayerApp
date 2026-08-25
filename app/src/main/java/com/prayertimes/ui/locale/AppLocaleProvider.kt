package com.prayertimes.ui.locale

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Provides comprehensive application-wide localization strings, layout direction, and formatting.
 * Backed by Android string resources (res/values/strings.xml + res/values-ar/strings.xml) rather
 * than hardcoded literals, resolved via a Resources instance pinned to the app's chosen language
 * (which can differ from the system locale) so future languages only require new values-* folders.
 * Guaranteed to use Western Arabic numerals (1, 2, 3, 4...) across all Arabic interfaces.
 */
class AppStrings(
    val isArabic: Boolean,
    val language: AppLanguage = if (isArabic) AppLanguage.ARABIC else AppLanguage.ENGLISH,
    private val res: Resources = PrayerApplication.instance.resources
) {
    private fun s(id: Int): String = res.getString(id)

    // App Branding
    val appBrandName: String = s(R.string.app_brand_name)
    val appSubtitle: String = s(R.string.app_subtitle)

    // Bottom Navigation
    val navPrayerTimes: String = s(R.string.nav_prayer_times)
    val navQibla: String = s(R.string.nav_qibla)
    val navCalendar: String = s(R.string.nav_calendar)
    val navSettings: String = s(R.string.nav_settings)

    // Common
    val appTitle: String = s(R.string.app_title)
    val today: String = s(R.string.common_today)
    val tomorrow: String = s(R.string.common_tomorrow)
    val yesterday: String = s(R.string.common_yesterday)
    val days: String = s(R.string.common_days)
    val day: String = s(R.string.common_day)
    val cancel: String = s(R.string.common_cancel)
    val save: String = s(R.string.common_save)
    val done: String = s(R.string.common_done)
    val gotIt: String = s(R.string.common_got_it)
    val close: String = s(R.string.common_close)
    val back: String = s(R.string.common_back)
    val clear: String = s(R.string.common_clear)
    val search: String = s(R.string.common_search)
    val change: String = s(R.string.common_change)
    val remaining: String = s(R.string.common_remaining)
    val timeForPrayer: String = s(R.string.common_time_for_prayer)
    val now: String = s(R.string.common_now)
    val select: String = s(R.string.common_select)
    val active: String = s(R.string.common_active)
    val currentLocationFallback: String = s(R.string.common_current_location)

    // Next Prayer / Hero
    val nextPrayerLabel: String = s(R.string.hero_next_prayer_label)
    val tomorrowPrayerLabel: String = s(R.string.hero_tomorrow_prayer_label)
    val nextLabelPrefix: String = s(R.string.hero_next_label_prefix)
    val athanAlert: String = s(R.string.hero_athan_alert)
    val athanAt: String = s(R.string.hero_athan_at)
    val stopAudio: String = s(R.string.hero_stop_audio)
    val listenAthan: String = s(R.string.hero_listen_athan)
    val hijriCalendarDetails: String = s(R.string.hero_hijri_calendar_details)

    // Bento Quick Cards
    val qiblaCardTitle: String = s(R.string.bento_qibla_card_title)
    val qiblaAccurateDirection: String = s(R.string.bento_qibla_accurate_direction)
    val ummAlQuraTitle: String = s(R.string.bento_umm_al_qura_title)
    val makkahCalendar: String = s(R.string.bento_makkah_calendar)
    val whiteDayBadge: String = s(R.string.bento_white_day_badge)
    val sacredMonthBadge: String = s(R.string.bento_sacred_month_badge)

    // Date Picker / Navigation
    val previousDay: String = s(R.string.date_previous_day)
    val nextDay: String = s(R.string.date_next_day)
    val goToToday: String = s(R.string.date_go_to_today)
    val pickDate: String = s(R.string.date_pick_date)

    // Prayer Names (Single clean standard name per language)
    private val prayerNameRes: Map<PrayerType, Int> = mapOf(
        PrayerType.FAJR to R.string.prayer_name_fajr,
        PrayerType.SUNRISE to R.string.prayer_name_sunrise,
        PrayerType.DHUHR to R.string.prayer_name_dhuhr,
        PrayerType.ASR to R.string.prayer_name_asr,
        PrayerType.MAGHRIB to R.string.prayer_name_maghrib,
        PrayerType.ISHA to R.string.prayer_name_isha
    )
    fun prayerName(type: PrayerType): String = s(prayerNameRes.getValue(type))

    // Full-Screen Prayer Alarm
    fun alarmBadge(prayerName: String): String = res.getString(R.string.alarm_badge_prayer, prayerName)
    fun alarmTimeForPrayer(prayerName: String): String = res.getString(R.string.alarm_time_for_prayer, prayerName)
    val alarmStopAthanBtn: String = s(R.string.alarm_stop_athan_btn)
    val alarmSnooze5mBtn: String = s(R.string.alarm_snooze_5m_btn)
    val alarmDuaBtn: String = s(R.string.alarm_dua_btn)
    val alarmOpenAppBtn: String = s(R.string.alarm_open_app_btn)
    val alarmDuaSheetTitle: String = s(R.string.alarm_dua_sheet_title)

    // Additional / Sunnah Times Card
    val additionalTimesTitle: String = s(R.string.additional_times_title)
    val midnight: String = s(R.string.additional_midnight)
    val lastThirdNight: String = s(R.string.additional_last_third_night)
    val imsak: String = s(R.string.additional_imsak)
    val duha: String = s(R.string.additional_duha)

    // Notification Sound Types
    private val soundNameRes: Map<NotificationSoundType, Int> = mapOf(
        NotificationSoundType.ATHAN_MAKKAH_MULLA to R.string.sound_name_athan_makkah_mulla,
        NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY to R.string.sound_name_athan_fajr1_kwait_alafasy,
        NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA to R.string.sound_name_athan_fajr2_jordan_allala,
        NotificationSoundType.ATHAN_RIYADH_QATAMI to R.string.sound_name_athan_riyadh_qatami,
        NotificationSoundType.ATHAN_QATAR_NABET to R.string.sound_name_athan_qatar_nabet,
        NotificationSoundType.ATHAN_QUDS_QAZAZ_1 to R.string.sound_name_athan_quds_qazaz_1,
        NotificationSoundType.ATHAN_QUDS_QAZAZ_2 to R.string.sound_name_athan_quds_qazaz_2,
        NotificationSoundType.ATHAN_EGYPT_DAWOD to R.string.sound_name_athan_egypt_dawod,
        NotificationSoundType.ATHAN_EGYPT_ALALFI to R.string.sound_name_athan_egypt_alalfi,
        NotificationSoundType.ATHAN_EGYPT_ABDULAATI to R.string.sound_name_athan_egypt_abdulaati,
        NotificationSoundType.ATHAN_IRAQ_ALAMOURI to R.string.sound_name_athan_iraq_alamouri,
        NotificationSoundType.ATHAN_GEORGIA to R.string.sound_name_athan_georgia,
        NotificationSoundType.SHORT_TAKBEER to R.string.sound_name_short_takbeer,
        NotificationSoundType.MELODIC_TONE to R.string.sound_name_melodic_tone,
        NotificationSoundType.VIBRATE_ONLY to R.string.sound_name_vibrate_only,
        NotificationSoundType.SILENT to R.string.sound_name_silent
    )
    private val soundSubtitleRes: Map<NotificationSoundType, Int> = mapOf(
        NotificationSoundType.ATHAN_MAKKAH_MULLA to R.string.sound_subtitle_athan_makkah_mulla,
        NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY to R.string.sound_subtitle_athan_fajr1_kwait_alafasy,
        NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA to R.string.sound_subtitle_athan_fajr2_jordan_allala,
        NotificationSoundType.ATHAN_RIYADH_QATAMI to R.string.sound_subtitle_athan_riyadh_qatami,
        NotificationSoundType.ATHAN_QATAR_NABET to R.string.sound_subtitle_athan_qatar_nabet,
        NotificationSoundType.ATHAN_QUDS_QAZAZ_1 to R.string.sound_subtitle_athan_quds_qazaz_1,
        NotificationSoundType.ATHAN_QUDS_QAZAZ_2 to R.string.sound_subtitle_athan_quds_qazaz_2,
        NotificationSoundType.ATHAN_EGYPT_DAWOD to R.string.sound_subtitle_athan_egypt_dawod,
        NotificationSoundType.ATHAN_EGYPT_ALALFI to R.string.sound_subtitle_athan_egypt_alalfi,
        NotificationSoundType.ATHAN_EGYPT_ABDULAATI to R.string.sound_subtitle_athan_egypt_abdulaati,
        NotificationSoundType.ATHAN_IRAQ_ALAMOURI to R.string.sound_subtitle_athan_iraq_alamouri,
        NotificationSoundType.ATHAN_GEORGIA to R.string.sound_subtitle_athan_georgia,
        NotificationSoundType.SHORT_TAKBEER to R.string.sound_subtitle_short_takbeer,
        NotificationSoundType.MELODIC_TONE to R.string.sound_subtitle_melodic_tone,
        NotificationSoundType.VIBRATE_ONLY to R.string.sound_subtitle_vibrate_only,
        NotificationSoundType.SILENT to R.string.sound_subtitle_silent
    )
    fun soundTypeName(type: NotificationSoundType): String = s(soundNameRes.getValue(type))
    fun soundTypeSubtitle(type: NotificationSoundType): String = s(soundSubtitleRes.getValue(type))

    // Qibla Screen
    val qiblaTitle: String = s(R.string.qibla_title)
    val qiblaAlignedMessage: String = s(R.string.qibla_aligned_message)
    val qiblaRotatePrompt: String = s(R.string.qibla_rotate_prompt)
    val qiblaBearingLabel: String = s(R.string.qibla_bearing_label)
    val currentHeadingLabel: String = s(R.string.qibla_current_heading_label)
    val distanceToKaabaLabel: String = s(R.string.qibla_distance_to_kaaba_label)
    val kmUnit: String = s(R.string.qibla_km_unit)
    val degUnit: String = s(R.string.qibla_deg_unit)
    val compassCalibTitle: String = s(R.string.qibla_compass_calib_title)
    val compassCalibText: String = s(R.string.qibla_compass_calib_text)
    val qiblaSensorLabel: String = s(R.string.qibla_sensor_label)
    val qiblaAccuracyHigh: String = s(R.string.qibla_accuracy_high)
    val qiblaAccuracyMedium: String = s(R.string.qibla_accuracy_medium)
    val qiblaAccuracyLow: String = s(R.string.qibla_accuracy_low)
    val qiblaAccuracyUnreliable: String = s(R.string.qibla_accuracy_unreliable)
    val qiblaCalibrateButton: String = s(R.string.qibla_calibrate_button)
    val qiblaSensorUnavailable: String = s(R.string.qibla_sensor_unavailable)
    fun qiblaTurnRight(degrees: Int): String = res.getString(R.string.qibla_turn_right, degrees)
    fun qiblaTurnLeft(degrees: Int): String = res.getString(R.string.qibla_turn_left, degrees)

    // Cardinal directions
    private val cardinalDirections: List<String> = res.getStringArray(R.array.cardinal_directions).toList()
    fun cardinalDirection(heading: Double): String {
        val normalized = ((heading % 360) + 360) % 360
        val index = when {
            normalized >= 337.5 || normalized < 22.5 -> 0 // N
            normalized < 67.5 -> 1 // NE
            normalized < 112.5 -> 2 // E
            normalized < 157.5 -> 3 // SE
            normalized < 202.5 -> 4 // S
            normalized < 247.5 -> 5 // SW
            normalized < 292.5 -> 6 // W
            else -> 7 // NW
        }
        return cardinalDirections[index]
    }

    // Monthly Calendar
    val monthlyCalendarTitle: String = s(R.string.calendar_title)
    val dateCol: String = s(R.string.calendar_date_col)
    val hijriCol: String = s(R.string.calendar_hijri_col)
    val prevMonth: String = s(R.string.calendar_prev_month)
    val nextMonth: String = s(R.string.calendar_next_month)

    // Settings Screen
    val settingsTitle: String = s(R.string.settings_title)
    val themeSection: String = s(R.string.settings_theme_section)
    val themeModeTitle: String = s(R.string.settings_theme_mode_title)
    val systemThemeDesc: String = s(R.string.settings_system_theme_desc)
    val lightThemeDesc: String = s(R.string.settings_light_theme_desc)
    val darkThemeDesc: String = s(R.string.settings_dark_theme_desc)

    private val themeModeNameRes: Map<AppThemeMode, Int> = mapOf(
        AppThemeMode.SYSTEM to R.string.theme_mode_system,
        AppThemeMode.LIGHT to R.string.theme_mode_light,
        AppThemeMode.DARK to R.string.theme_mode_dark
    )
    fun themeModeName(mode: AppThemeMode): String = s(themeModeNameRes.getValue(mode))

    val colorPaletteSection: String = s(R.string.settings_color_palette_section)
    val followSystemColorsTitle: String = s(R.string.settings_follow_system_colors_title)
    val followSystemColorsDesc: String = s(R.string.settings_follow_system_colors_desc)
    val presetColorsTitle: String = s(R.string.settings_preset_colors_title)
    val presetColorsDesc: String = s(R.string.settings_preset_colors_desc)

    private val colorPresetNameRes: Map<AppColorPreset, Int> = mapOf(
        AppColorPreset.SYSTEM_DYNAMIC to R.string.color_preset_system_dynamic,
        AppColorPreset.EMERALD_GOLD to R.string.color_preset_emerald_gold,
        AppColorPreset.ROYAL_AMBER to R.string.color_preset_royal_amber,
        AppColorPreset.SAPPHIRE_NIGHT to R.string.color_preset_sapphire_night,
        AppColorPreset.MEDINA_TEAL to R.string.color_preset_medina_teal,
        AppColorPreset.ROSE_CLOVE to R.string.color_preset_rose_clove,
        AppColorPreset.SLATE_CHARCOAL to R.string.color_preset_slate_charcoal
    )
    fun colorPresetName(preset: AppColorPreset): String = s(colorPresetNameRes.getValue(preset))

    val languageSection: String = s(R.string.settings_language_section)
    val appLanguage: String = s(R.string.settings_app_language)
    val selectLanguageSubtitle: String = s(R.string.settings_select_language_subtitle)
    val locationSection: String = s(R.string.settings_location_section)
    val useGps: String = s(R.string.settings_use_gps)
    val gpsTapToDetect: String = s(R.string.gps_tap_to_detect)
    val gpsButtonLabel: String = s(R.string.gps_button_label)
    val gpsLocationDisclaimer: String = s(R.string.gps_location_disclaimer)
    val locationSearchHint: String = s(R.string.location_search_hint)
    val currentLocationLabel: String = s(R.string.location_current_label)
    val locationSourceGps: String = s(R.string.location_source_gps)
    val locationSourceManual: String = s(R.string.location_source_manual)
    val chooseCity: String = s(R.string.settings_choose_city)
    val manualCoordinates: String = s(R.string.settings_manual_coordinates)
    val calcMethodSection: String = s(R.string.settings_calc_method_section)
    val juristicMethodTitle: String = s(R.string.settings_juristic_method_title)
    val standardJuristic: String = s(R.string.settings_standard_juristic)
    val hanafiJuristic: String = s(R.string.settings_hanafi_juristic)
    val highLatitudeSection: String = s(R.string.settings_high_latitude_section)
    val displayHijriSection: String = s(R.string.settings_display_hijri_section)
    val hijriOffsetTitle: String = s(R.string.settings_hijri_offset_title)
    val timeFormatTitle: String = s(R.string.settings_time_format_title)
    val minuteAdjustmentsTitle: String = s(R.string.settings_minute_adjustments_title)
    val minuteAdjustmentsSubtitle: String = s(R.string.settings_minute_adjustments_subtitle)
    val notifSectionTitle: String = s(R.string.settings_notif_section_title)
    val athanSound: String = s(R.string.settings_athan_sound)
    val preReminder: String = s(R.string.settings_pre_reminder)
    val minutesBefore: String = s(R.string.settings_minutes_before)
    val testAlert: String = s(R.string.settings_test_alert)
    val testAlarm5s: String = s(R.string.settings_test_alarm_5s)
    val reschedule7Days: String = s(R.string.settings_reschedule_7_days)
    val notificationsDisabledWarning: String = s(R.string.settings_notifications_disabled_warning)
    val enableNotificationsBtn: String = s(R.string.settings_enable_notifications_btn)
    val notificationsStatusActive: String = s(R.string.settings_notifications_status_active)
    fun notifCountActive(count: Int): String = res.getString(R.string.settings_notif_count_active, count)
    val adjustmentsCustomActive: String = s(R.string.settings_adjustments_custom_active)
    val adjustmentsStandard: String = s(R.string.settings_adjustments_standard)
    val aboutHubTitle: String = s(R.string.settings_about_hub_title)
    val aboutHubSubtitle: String = s(R.string.settings_about_hub_subtitle)
    val aboutDataAttributionTitle: String = s(R.string.about_data_attribution_title)
    val aboutDataAttributionBody: String = s(R.string.about_data_attribution_body)
    val rerunSetupTitle: String = s(R.string.settings_rerun_setup_title)
    val rerunSetupSubtitle: String = s(R.string.settings_rerun_setup_subtitle)
    val juristicStandardDesc: String = s(R.string.settings_juristic_standard_desc)
    val juristicHanafiDesc: String = s(R.string.settings_juristic_hanafi_desc)
    val selectArtworkPreview: String = s(R.string.settings_select_artwork_preview)
    val hijriOffsetDesc: String = s(R.string.settings_hijri_offset_desc)
    val resetAllAdjustments: String = s(R.string.settings_reset_all_adjustments)
    val aboutScreenTitle: String = s(R.string.settings_about_screen_title)
    val versionEngineLabel: String = s(R.string.settings_version_engine_label)
    val astroEngineTitle: String = s(R.string.settings_astro_engine_title)
    val astroEngineDesc: String = s(R.string.settings_astro_engine_desc)

    // Audio Output Channel & Wake Screen Settings
    val audioStreamSectionTitle: String = s(R.string.settings_audio_stream_section_title)
    val audioStreamDesc: String = s(R.string.settings_audio_stream_desc)
    val audioStreamAlarmTitle: String = s(R.string.settings_audio_stream_alarm_title)
    val audioStreamAlarmDesc: String = s(R.string.settings_audio_stream_alarm_desc)
    val audioStreamMediaTitle: String = s(R.string.settings_audio_stream_media_title)
    val audioStreamMediaDesc: String = s(R.string.settings_audio_stream_media_desc)
    val audioStreamRingtoneTitle: String = s(R.string.settings_audio_stream_ringtone_title)
    val audioStreamRingtoneDesc: String = s(R.string.settings_audio_stream_ringtone_desc)

    val wakeScreenTitle: String = s(R.string.settings_wake_screen_title)
    val wakeScreenDesc: String = s(R.string.settings_wake_screen_desc)
    val previewFullScreenAlarmBtn: String = s(R.string.settings_preview_full_screen_alarm_btn)


    // Calculation Method translations
    private val calcMethodNameRes: Map<CalculationMethod, Int> = mapOf(
        CalculationMethod.UMM_AL_QURA to R.string.calc_method_umm_al_qura,
        CalculationMethod.MUSLIM_WORLD_LEAGUE to R.string.calc_method_mwl,
        CalculationMethod.EGYPTIAN to R.string.calc_method_egyptian,
        CalculationMethod.KARACHI to R.string.calc_method_karachi,
        CalculationMethod.ISNA to R.string.calc_method_isna,
        CalculationMethod.GULF to R.string.calc_method_gulf,
        CalculationMethod.QATAR to R.string.calc_method_qatar,
        CalculationMethod.KUWAIT to R.string.calc_method_kuwait,
        CalculationMethod.TURKEY to R.string.calc_method_turkey,
        CalculationMethod.TEHRAN to R.string.calc_method_tehran,
        CalculationMethod.SHIA_ITHNA_ASHARI to R.string.calc_method_shia_ithna_ashari,
        CalculationMethod.SINGAPORE to R.string.calc_method_singapore,
        CalculationMethod.FRANCE_UOIF to R.string.calc_method_france_uoif,
        CalculationMethod.RUSSIA to R.string.calc_method_russia
    )
    fun calcMethodName(method: CalculationMethod): String = s(calcMethodNameRes.getValue(method))

    // High Latitude Rule translations
    private val highLatitudeNameRes: Map<HighLatitudeRule, Int> = mapOf(
        HighLatitudeRule.MIDNIGHT to R.string.high_latitude_midnight,
        HighLatitudeRule.ONE_SEVENTH to R.string.high_latitude_one_seventh,
        HighLatitudeRule.ANGLE_BASED to R.string.high_latitude_angle_based,
        HighLatitudeRule.NONE to R.string.high_latitude_none
    )
    fun highLatitudeName(rule: HighLatitudeRule): String = s(highLatitudeNameRes.getValue(rule))

    // Athan Player Dialog
    val athanDialogTitle: String = s(R.string.athan_dialog_title)
    val playAdhan: String = s(R.string.athan_play_adhan)
    val fajrAdhan: String = s(R.string.athan_fajr_adhan)
    val stopBtn: String = s(R.string.athan_stop_btn)
    val athanPromptText: String = s(R.string.athan_prompt_text)

    // Hijri Dialog
    val hijriCalendarHeader: String = s(R.string.hijri_calendar_header)
    val ummAlQuraStandard: String = s(R.string.hijri_umm_al_qura_standard)
    val convertDate: String = s(R.string.hijri_convert_date)
    val adjustDays: String = s(R.string.hijri_adjust_days)
    val sunnahFastingDay: String = s(R.string.hijri_sunnah_fasting_day)
    val whiteDaysTitle: String = s(R.string.hijri_white_days_title)
    val whiteDaysDesc: String = s(R.string.hijri_white_days_desc)
    val sacredMonthTitle: String = s(R.string.hijri_sacred_month_title)
    val sacredMonthDesc: String = s(R.string.hijri_sacred_month_desc)
    val mondayThursdayDesc: String = s(R.string.hijri_monday_thursday_desc)
    val aboutUmmAlQuraTitle: String = s(R.string.hijri_about_umm_al_qura_title)
    val aboutUmmAlQuraDesc: String = s(R.string.hijri_about_umm_al_qura_desc)

    // Manual Coordinates Dialog
    val customCoordinates: String = s(R.string.coords_custom_coordinates)
    val enterCoordinates: String = s(R.string.coords_enter_coordinates)
    val manualCoordinatesDesc: String = s(R.string.coords_manual_coordinates_desc)
    val quickPresets: String = s(R.string.coords_quick_presets)
    val latitudeLabel: String = s(R.string.coords_latitude_label)
    val longitudeLabel: String = s(R.string.coords_longitude_label)
    val locationNameLabel: String = s(R.string.coords_location_name_label)
    val timeZoneLabel: String = s(R.string.coords_time_zone_label)
    val applyCoordinates: String = s(R.string.coords_apply_coordinates)
    val defaultCustomCoordinatesName: String = s(R.string.coords_default_custom_name)
    val presetMakkah: String = s(R.string.coords_preset_makkah)
    val presetMadinah: String = s(R.string.coords_preset_madinah)
    val presetJerusalem: String = s(R.string.coords_preset_jerusalem)
    val presetCairo: String = s(R.string.coords_preset_cairo)
    val presetDubai: String = s(R.string.coords_preset_dubai)
    val presetIstanbul: String = s(R.string.coords_preset_istanbul)
    val presetLondon: String = s(R.string.coords_preset_london)
    val presetNewYork: String = s(R.string.coords_preset_new_york)
    val deviceDefault: String = s(R.string.coords_device_default)

    // Setup & First-Time Onboarding
    val welcomeToApp: String = s(R.string.onboarding_welcome_to_app)
    val setupSubtitle: String = s(R.string.onboarding_setup_subtitle)
    val stepLanguageTitle: String = s(R.string.onboarding_step_language_title)
    val stepLanguageDesc: String = s(R.string.onboarding_step_language_desc)
    val stepLocationTitle: String = s(R.string.onboarding_step_location_title)
    val stepLocationDesc: String = s(R.string.onboarding_step_location_desc)
    val stepCalcMethodTitle: String = s(R.string.onboarding_step_calc_method_title)
    val stepCalcMethodDesc: String = s(R.string.onboarding_step_calc_method_desc)
    fun calcMethodSuggestedBanner(methodName: String): String = res.getString(R.string.onboarding_calc_method_suggested_banner, methodName)
    val skipBtn: String = s(R.string.onboarding_skip_btn)
    val privacyNoticeTitle: String = s(R.string.onboarding_privacy_notice_title)
    val privacyNoticeDesc: String = s(R.string.onboarding_privacy_notice_desc)
    val useGpsButton: String = s(R.string.onboarding_use_gps_button)
    val selectedLocationLabel: String = s(R.string.onboarding_selected_location_label)
    val stepNotificationsTitle: String = s(R.string.onboarding_step_notifications_title)
    val stepNotificationsDesc: String = s(R.string.onboarding_step_notifications_desc)
    val stepNotificationsExplanation: String = s(R.string.onboarding_step_notifications_explanation)
    val grantNotificationPermissionBtn: String = s(R.string.onboarding_grant_notification_permission_btn)
    val openAppSettingsBtn: String = s(R.string.onboarding_open_app_settings_btn)
    val permissionDeniedOpenSettingsDesc: String = s(R.string.onboarding_permission_denied_open_settings_desc)
    val permissionGrantedStatus: String = s(R.string.onboarding_permission_granted_status)
    val stepAthanSoundsTitle: String = s(R.string.onboarding_step_athan_sounds_title)
    val stepAthanSoundsDesc: String = s(R.string.onboarding_step_athan_sounds_desc)
    val stepStyleTitle: String = s(R.string.onboarding_step_style_title)
    val stepStyleDesc: String = s(R.string.onboarding_step_style_desc)
    val getStartedBtn: String = s(R.string.onboarding_get_started_btn)
    val nextStepBtn: String = s(R.string.onboarding_next_step_btn)
    val previousStepBtn: String = s(R.string.onboarding_previous_step_btn)
    val stepIndicatorText: String = s(R.string.onboarding_step_indicator_text)
    val ofStepText: String = s(R.string.onboarding_of_step_text)
    val gpsDetecting: String = s(R.string.onboarding_gps_detecting)
    val systemDefaultLangTitle: String = s(R.string.onboarding_system_default_title)
    val systemDefaultLangSubtitle: String = s(R.string.onboarding_system_default_subtitle)

    // Widget Customization Strings
    val widgetsSection: String = s(R.string.widget_settings_section)
    val widgetsSectionSubtitle: String = s(R.string.widget_settings_section_subtitle)
    val widgetThemeModeTitle: String = s(R.string.widget_settings_theme_mode_title)
    val widgetBgStyleTitle: String = s(R.string.widget_settings_bg_style_title)
    val widgetOpacityTitle: String = s(R.string.widget_settings_opacity_title)
    val widgetFontSizeTitle: String = s(R.string.widget_settings_font_size_title)
    val widgetContentTitle: String = s(R.string.widget_settings_content_title)
    val widgetResetDefaults: String = s(R.string.widget_settings_reset_defaults)
    val widgetAppearanceSectionTitle: String = s(R.string.widget_settings_appearance_section_title)
    val widgetAppearanceSectionSubtitle: String = s(R.string.widget_settings_appearance_section_subtitle)
    val widgetBehaviorSectionTitle: String = s(R.string.widget_settings_behavior_section_title)
    val widgetBehaviorSectionSubtitle: String = s(R.string.widget_settings_behavior_section_subtitle)
    val widgetContentSectionSubtitle: String = s(R.string.widget_settings_content_section_subtitle)
    val widgetToggleShowHeroDesc: String = s(R.string.widget_settings_toggle_show_hero_desc)
    val widgetToggleShowCountdownDesc: String = s(R.string.widget_settings_toggle_show_countdown_desc)
    val widgetToggleShowAllPrayersDesc: String = s(R.string.widget_settings_toggle_show_all_prayers_desc)
    val widgetToggleShowSunriseDesc: String = s(R.string.widget_settings_toggle_show_sunrise_desc)
    val widgetToggleShowLocationDesc: String = s(R.string.widget_settings_toggle_show_location_desc)
    val widgetToggleShowHijriDesc: String = s(R.string.widget_settings_toggle_show_hijri_desc)

    // Number & Time formatters that STRICTLY use 123 (Western Arabic Digits)
    fun formatNumber(number: Number): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val df = DecimalFormat("#,##0.##", symbols)
        return df.format(number)
    }

    fun formatCountdown(seconds: Long): String {
        if (seconds <= 0) return s(R.string.countdown_time_for_prayer_now)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val sec = seconds % 60
        return when {
            hours > 0 -> res.getString(R.string.countdown_hours_minutes, hours, minutes)
            minutes > 0 -> res.getString(R.string.countdown_minutes_seconds, minutes, sec)
            else -> res.getQuantityString(R.plurals.countdown_seconds_only, sec.toInt(), sec.toInt())
        }
    }

    fun formatSince(seconds: Long): String {
        if (seconds <= 60) return s(R.string.widget_since_just_now)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) res.getString(R.string.widget_since_hours_minutes, hours, minutes)
        else res.getString(R.string.widget_since_minutes_only, minutes)
    }

    private val monthNames: List<String> = res.getStringArray(R.array.month_names).toList()
    fun monthName(month: Int): String = monthNames[month - 1]

    fun formatDateShort(date: LocalDate): String {
        val day = date.dayOfMonth
        val month = monthName(date.monthValue)
        return "$day $month"
    }

    private val dayOfWeekNames: List<String> = res.getStringArray(R.array.day_of_week_names).toList()
    fun dayOfWeekName(dayOfWeek: DayOfWeek): String = dayOfWeekNames[dayOfWeek.value - 1]
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

    // The app's chosen language can differ from the actual system/Activity locale (there's no
    // AppCompatDelegate.setApplicationLocales adoption, and the raw LocaleManager override in
    // PrayerPreferences only works on API 33+), so string resources are resolved against an
    // explicitly-locale-pinned Resources instance rather than trusting context.resources to
    // already reflect isArabic.
    val localizedResources = remember(isArabic) {
        val locale = Locale(if (isArabic) "ar" else "en")
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        context.createConfigurationContext(config).resources
    }

    val appStrings = remember(isArabic, appLanguage, localizedResources) {
        AppStrings(isArabic, appLanguage, localizedResources)
    }
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}
