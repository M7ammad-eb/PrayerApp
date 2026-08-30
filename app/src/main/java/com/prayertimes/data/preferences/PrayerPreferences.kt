package com.prayertimes.data.preferences

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prayertimes.data.calendar.HijriCalendar
import com.prayertimes.data.cities.CityDatabase
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.AthanAudioStream
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.JuristicMethod
import com.prayertimes.data.models.NotificationPrayerConfig
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerTimeAdjustments
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.UserLocation
import com.prayertimes.data.models.WidgetCustomizationSettings
import com.prayertimes.data.models.WidgetFontSize
import com.prayertimes.data.models.WidgetTextStyle
import com.prayertimes.data.models.WidgetThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prayer_settings")

// Shared by both the fast-cache (SharedPreferences) and DataStore read paths below, which each
// parse ~15 enum settings with the same "stored name string -> enum, or default if missing/stale"
// fallback - a plain generic helper here avoids that try/catch being duplicated at every call site.
private inline fun <reified T : Enum<T>> parseEnumOrDefault(value: String?, default: T): T =
    value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

private fun legacyWidgetBackgroundWasEnabled(value: String?): Boolean =
    value != "TRANSPARENT_CLEAN" && value != "MINIMAL_BORDER"

data class AppPrayerSettings(
    val location: UserLocation,
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    val hijriAdjustmentDays: Int = 0,
    val hijriAdjustmentAnchorMonth: String? = null,
    val is24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPreset: AppColorPreset = AppColorPreset.SYSTEM_DYNAMIC,
    val customColorSeed: Long = 0xFF6750A4,
    val followSystemColors: Boolean = true,
    val widgetSettings: WidgetCustomizationSettings = WidgetCustomizationSettings(),
    val audioStream: AthanAudioStream = AthanAudioStream.ALARM,
    val wakeScreenOnAlarm: Boolean = true,
    val liveCountdownEnabled: Boolean = true,
    val liveCountdownMinutesBefore: Int = 15,
    val onboardingCompleted: Boolean = false,
    val prayerConfigs: Map<PrayerType, NotificationPrayerConfig> = PrayerType.values().associateWith {
        NotificationPrayerConfig(
            enabled = it != PrayerType.SUNRISE, // Sunrise not a prayer, so disabled by default or reminder only
            soundType = when (it) {
                PrayerType.FAJR -> NotificationSoundType.ATHAN_FAJR
                PrayerType.SUNRISE -> NotificationSoundType.DEVICE_DEFAULT
                else -> NotificationSoundType.FULL_ATHAN
            },
            preReminderMinutes = 0
        )
    },
    val adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments()
)

class PrayerPreferences(private val context: Context) {

    companion object {
        private const val FAST_CACHE_PREFS = "prayer_fast_cache"
        private const val KEY_LANG = "cached_language"
        private const val KEY_THEME = "cached_theme"
        private const val KEY_COLOR_PRESET = "cached_color_preset"
        private const val KEY_CUSTOM_COLOR_SEED = "cached_custom_color_seed"
        private const val KEY_FOLLOW_SYSTEM_COLORS = "cached_follow_system_colors"
        private const val KEY_24H = "cached_24h"

        private const val KEY_WIDGET_THEME = "cached_w_theme"
        private const val LEGACY_KEY_WIDGET_BG = "cached_w_bg"
        private const val KEY_WIDGET_SHOW_BACKGROUND = "cached_w_show_background"
        private const val KEY_WIDGET_OPACITY = "cached_w_opacity"
        private const val KEY_WIDGET_SHOW_BORDER = "cached_w_show_border"
        private const val KEY_WIDGET_FONT = "cached_w_font"
        private const val KEY_WIDGET_TEXT_STYLE = "cached_w_text_style"
        private const val KEY_WIDGET_SHOW_LOC = "cached_w_show_loc"
        private const val KEY_WIDGET_SHOW_HIJRI = "cached_w_show_hijri"
        private const val KEY_WIDGET_SHOW_COUNTDOWN = "cached_w_show_cd"
        private const val KEY_WIDGET_SHOW_SUNRISE = "cached_w_show_sunrise"
        private const val KEY_WIDGET_SHOW_ALL = "cached_w_show_all"
        private const val KEY_WIDGET_SHOW_HERO = "cached_w_show_hero"

        private const val KEY_LOC_NAME = "cached_loc_name"
        private const val KEY_LOC_COUNTRY = "cached_loc_country"
        private const val KEY_LOC_LAT = "cached_loc_lat"
        private const val KEY_LOC_LON = "cached_loc_lon"
        private const val KEY_LOC_TZ = "cached_loc_tz"
        private const val KEY_LOC_IS_GPS = "cached_loc_is_gps"
        private const val KEY_LOC_DISTANCE_KM = "cached_loc_distance_km"

        private const val KEY_CALC_METHOD = "cached_calc_method"
        private const val KEY_JURISTIC_METHOD = "cached_juristic_method"
        private const val KEY_HIGH_LAT_RULE = "cached_high_lat_rule"
        private const val KEY_HIJRI_OFFSET = "cached_hijri_offset"
        private const val KEY_HIJRI_OFFSET_ANCHOR = "cached_hijri_offset_anchor"

        private const val KEY_ADJ_FAJR = "cached_adj_fajr"
        private const val KEY_ADJ_SUNRISE = "cached_adj_sunrise"
        private const val KEY_ADJ_DHUHR = "cached_adj_dhuhr"
        private const val KEY_ADJ_ASR = "cached_adj_asr"
        private const val KEY_ADJ_MAGHRIB = "cached_adj_maghrib"
        private const val KEY_ADJ_ISHA = "cached_adj_isha"

        private const val KEY_AUDIO_STREAM = "cached_audio_stream"
        private const val KEY_WAKE_SCREEN = "cached_wake_screen"
        private const val KEY_ONBOARDING_COMPLETED = "cached_onboarding_completed"
        private const val KEY_LIVE_COUNTDOWN_ENABLED = "cached_live_countdown_enabled"
        private const val KEY_LIVE_COUNTDOWN_MINUTES = "cached_live_countdown_minutes"

        fun getInitialSettings(context: Context): AppPrayerSettings {
            val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)

            // 1. Location
            val locName = fastPrefs.getString(KEY_LOC_NAME, null)
            val locCountry = fastPrefs.getString(KEY_LOC_COUNTRY, null)
            val locLat = if (fastPrefs.contains(KEY_LOC_LAT)) {
                java.lang.Double.longBitsToDouble(fastPrefs.getLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(CityDatabase.DEFAULT_PRESET.latitude)))
            } else {
                CityDatabase.DEFAULT_PRESET.latitude
            }
            val locLon = if (fastPrefs.contains(KEY_LOC_LON)) {
                java.lang.Double.longBitsToDouble(fastPrefs.getLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(CityDatabase.DEFAULT_PRESET.longitude)))
            } else {
                CityDatabase.DEFAULT_PRESET.longitude
            }
            val locTz = fastPrefs.getString(KEY_LOC_TZ, CityDatabase.DEFAULT_PRESET.timeZoneId) ?: CityDatabase.DEFAULT_PRESET.timeZoneId
            val locIsGps = fastPrefs.getBoolean(KEY_LOC_IS_GPS, false)
            val locDistanceKm = if (fastPrefs.contains(KEY_LOC_DISTANCE_KM)) {
                java.lang.Double.longBitsToDouble(fastPrefs.getLong(KEY_LOC_DISTANCE_KM, 0L))
            } else {
                null
            }

            val location = if (locName != null && locCountry != null) {
                UserLocation(locName, locCountry, locLat, locLon, locTz, locIsGps, locDistanceKm)
            } else {
                CityDatabase.defaultLocation(context.resources)
            }

            // 2. Calculation Methods
            val calcMethod = parseEnumOrDefault(fastPrefs.getString(KEY_CALC_METHOD, null), CalculationMethod.MUSLIM_WORLD_LEAGUE)
            val juristic = parseEnumOrDefault(fastPrefs.getString(KEY_JURISTIC_METHOD, null), JuristicMethod.STANDARD)
            val highLat = parseEnumOrDefault(fastPrefs.getString(KEY_HIGH_LAT_RULE, null), HighLatitudeRule.ANGLE_BASED)

            val rawHijriOffset = fastPrefs.getInt(KEY_HIJRI_OFFSET, 0)
            val rawHijriAnchor = fastPrefs.getString(KEY_HIJRI_OFFSET_ANCHOR, null)?.takeIf { it.isNotBlank() }
            val hijriOffset = HijriCalendar.effectiveAdjustment(rawHijriOffset, rawHijriAnchor, LocalDate.now())
            val hijriOffsetAnchor = rawHijriAnchor.takeIf { hijriOffset != 0 }

            // 3. Adjustments
            val adjustments = PrayerTimeAdjustments(
                fajr = fastPrefs.getInt(KEY_ADJ_FAJR, 0),
                sunrise = fastPrefs.getInt(KEY_ADJ_SUNRISE, 0),
                dhuhr = fastPrefs.getInt(KEY_ADJ_DHUHR, 0),
                asr = fastPrefs.getInt(KEY_ADJ_ASR, 0),
                maghrib = fastPrefs.getInt(KEY_ADJ_MAGHRIB, 0),
                isha = fastPrefs.getInt(KEY_ADJ_ISHA, 0)
            )

            // 4. Language & UI Theme
            val lang = parseEnumOrDefault(fastPrefs.getString(KEY_LANG, null), AppLanguage.SYSTEM)
            val theme = parseEnumOrDefault(fastPrefs.getString(KEY_THEME, null), AppThemeMode.SYSTEM)
            val color = parseEnumOrDefault(fastPrefs.getString(KEY_COLOR_PRESET, null), AppColorPreset.SYSTEM_DYNAMIC)
            val customColorSeed = fastPrefs.getLong(KEY_CUSTOM_COLOR_SEED, 0xFF6750A4)

            val followSys = fastPrefs.getBoolean(KEY_FOLLOW_SYSTEM_COLORS, true)
            val is24h = fastPrefs.getBoolean(KEY_24H, false)

            val audioStream = parseEnumOrDefault(fastPrefs.getString(KEY_AUDIO_STREAM, null), AthanAudioStream.ALARM)

            val wakeScreen = fastPrefs.getBoolean(KEY_WAKE_SCREEN, true)
            val onboardingCompleted = fastPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
            val liveCountdownEnabled = fastPrefs.getBoolean(KEY_LIVE_COUNTDOWN_ENABLED, true)
            val liveCountdownMinutes = fastPrefs.getInt(KEY_LIVE_COUNTDOWN_MINUTES, 15)

            // Widget Customization
            val wTheme = parseEnumOrDefault(fastPrefs.getString(KEY_WIDGET_THEME, null), WidgetThemeMode.APP_THEME)
            val rawWBg = fastPrefs.getString(LEGACY_KEY_WIDGET_BG, null)
            val wShowBackground = if (fastPrefs.contains(KEY_WIDGET_SHOW_BACKGROUND)) {
                fastPrefs.getBoolean(KEY_WIDGET_SHOW_BACKGROUND, true)
            } else {
                legacyWidgetBackgroundWasEnabled(rawWBg)
            }
            val wOpacity = fastPrefs.getInt(KEY_WIDGET_OPACITY, 100)
            val wShowBorder = if (fastPrefs.contains(KEY_WIDGET_SHOW_BORDER)) {
                fastPrefs.getBoolean(KEY_WIDGET_SHOW_BORDER, false)
            } else {
                false
            }
            val wFont = parseEnumOrDefault(fastPrefs.getString(KEY_WIDGET_FONT, null), WidgetFontSize.STANDARD)
            val wTextStyle = parseEnumOrDefault(fastPrefs.getString(KEY_WIDGET_TEXT_STYLE, null), WidgetTextStyle.AUTO)

            val wShowLoc = fastPrefs.getBoolean(KEY_WIDGET_SHOW_LOC, true)
            val wShowHijri = fastPrefs.getBoolean(KEY_WIDGET_SHOW_HIJRI, true)
            val wShowCd = fastPrefs.getBoolean(KEY_WIDGET_SHOW_COUNTDOWN, true)
            val wShowSunrise = fastPrefs.getBoolean(KEY_WIDGET_SHOW_SUNRISE, true)
            val wShowAll = fastPrefs.getBoolean(KEY_WIDGET_SHOW_ALL, true)
            val wShowHero = fastPrefs.getBoolean(KEY_WIDGET_SHOW_HERO, true)

            val widgetSettings = WidgetCustomizationSettings(
                themeMode = wTheme,
                showBackground = wShowBackground,
                opacityPercent = wOpacity,
                showBorder = wShowBorder,
                fontSize = wFont,
                textStyle = wTextStyle,
                showLocation = wShowLoc,
                showHijriDate = wShowHijri,
                showCountdown = wShowCd,
                showSunrise = wShowSunrise,
                showAllPrayersList = wShowAll,
                showHeroCard = wShowHero
            )

            return AppPrayerSettings(
                location = location,
                calculationMethod = calcMethod,
                juristicMethod = juristic,
                highLatitudeRule = highLat,
                hijriAdjustmentDays = hijriOffset,
                hijriAdjustmentAnchorMonth = hijriOffsetAnchor,
                is24HourFormat = is24h,
                language = lang,
                themeMode = theme,
                colorPreset = color,
                customColorSeed = customColorSeed,
                followSystemColors = followSys,
                widgetSettings = widgetSettings,
                audioStream = audioStream,
                wakeScreenOnAlarm = wakeScreen,
                onboardingCompleted = onboardingCompleted,
                liveCountdownEnabled = liveCountdownEnabled,
                liveCountdownMinutesBefore = liveCountdownMinutes,
                adjustments = adjustments
            )
        }
    }

    private object Keys {
        val LOC_NAME = stringPreferencesKey("loc_name")
        val LOC_COUNTRY = stringPreferencesKey("loc_country")
        val LOC_LAT = doublePreferencesKey("loc_lat")
        val LOC_LON = doublePreferencesKey("loc_lon")
        val LOC_TZ = stringPreferencesKey("loc_tz")
        val LOC_IS_GPS = booleanPreferencesKey("loc_is_gps")
        val LOC_DISTANCE_KM = doublePreferencesKey("loc_distance_km")

        val CALC_METHOD = stringPreferencesKey("calc_method")
        val JURISTIC_METHOD = stringPreferencesKey("juristic_method")
        val HIGH_LAT_RULE = stringPreferencesKey("high_lat_rule")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val HIJRI_OFFSET_ANCHOR = stringPreferencesKey("hijri_offset_anchor")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PRESET = stringPreferencesKey("color_preset")
        val CUSTOM_COLOR_SEED = longPreferencesKey("custom_color_seed")
        val FOLLOW_SYSTEM_COLORS = booleanPreferencesKey("follow_system_colors")
        val AUDIO_STREAM = stringPreferencesKey("audio_stream")
        val WAKE_SCREEN_ON_ALARM = booleanPreferencesKey("wake_screen_on_alarm")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LIVE_COUNTDOWN_ENABLED = booleanPreferencesKey("live_countdown_enabled")
        val LIVE_COUNTDOWN_MINUTES = intPreferencesKey("live_countdown_minutes")

        // Widget Settings
        val WIDGET_THEME_MODE = stringPreferencesKey("widget_theme_mode")
        val LEGACY_WIDGET_BG_STYLE = stringPreferencesKey("widget_bg_style")
        val WIDGET_SHOW_BACKGROUND = booleanPreferencesKey("widget_show_background")
        val WIDGET_OPACITY = intPreferencesKey("widget_opacity")
        val WIDGET_SHOW_BORDER = booleanPreferencesKey("widget_show_border")
        val WIDGET_FONT_SIZE = stringPreferencesKey("widget_font_size")
        val WIDGET_TEXT_STYLE = stringPreferencesKey("widget_text_style")
        val WIDGET_SHOW_LOC = booleanPreferencesKey("widget_show_loc")
        val WIDGET_SHOW_HIJRI = booleanPreferencesKey("widget_show_hijri")
        val WIDGET_SHOW_COUNTDOWN = booleanPreferencesKey("widget_show_countdown")
        val WIDGET_SHOW_SUNRISE = booleanPreferencesKey("widget_show_sunrise")
        val WIDGET_SHOW_ALL = booleanPreferencesKey("widget_show_all")
        val WIDGET_SHOW_HERO = booleanPreferencesKey("widget_show_hero")

        // Adjustments
        val ADJ_FAJR = intPreferencesKey("adj_fajr")
        val ADJ_SUNRISE = intPreferencesKey("adj_sunrise")
        val ADJ_DHUHR = intPreferencesKey("adj_dhuhr")
        val ADJ_ASR = intPreferencesKey("adj_asr")
        val ADJ_MAGHRIB = intPreferencesKey("adj_maghrib")
        val ADJ_ISHA = intPreferencesKey("adj_isha")

        // Per-prayer notification settings
        fun notifEnabled(prayer: PrayerType) = booleanPreferencesKey("notif_enabled_${prayer.name}")
        fun notifSound(prayer: PrayerType) = stringPreferencesKey("notif_sound_${prayer.name}")
        fun notifPreReminder(prayer: PrayerType) = intPreferencesKey("notif_pre_${prayer.name}")
    }

    private fun settingsFromDataStore(prefs: Preferences): AppPrayerSettings {
        val defaultLocation = CityDatabase.defaultLocation(context.resources)
        val locName = prefs[Keys.LOC_NAME] ?: defaultLocation.name
        val locCountry = prefs[Keys.LOC_COUNTRY] ?: defaultLocation.country
        val locLat = prefs[Keys.LOC_LAT] ?: defaultLocation.latitude
        val locLon = prefs[Keys.LOC_LON] ?: defaultLocation.longitude
        val locTz = prefs[Keys.LOC_TZ] ?: defaultLocation.timeZoneId
        val locIsGps = prefs[Keys.LOC_IS_GPS] ?: false
        val locDistanceKm = prefs[Keys.LOC_DISTANCE_KM]

        val location = UserLocation(locName, locCountry, locLat, locLon, locTz, locIsGps, locDistanceKm)

        val calcMethod = parseEnumOrDefault(prefs[Keys.CALC_METHOD], CalculationMethod.MUSLIM_WORLD_LEAGUE)
        val juristic = parseEnumOrDefault(prefs[Keys.JURISTIC_METHOD], JuristicMethod.STANDARD)
        val highLat = parseEnumOrDefault(prefs[Keys.HIGH_LAT_RULE], HighLatitudeRule.ANGLE_BASED)

        val rawHijriOffset = prefs[Keys.HIJRI_OFFSET] ?: 0
        val rawHijriAnchor = prefs[Keys.HIJRI_OFFSET_ANCHOR]?.takeIf { it.isNotBlank() }
        val hijriOffset = HijriCalendar.effectiveAdjustment(rawHijriOffset, rawHijriAnchor, LocalDate.now())
        val hijriOffsetAnchor = rawHijriAnchor.takeIf { hijriOffset != 0 }
        val is24Hour = prefs[Keys.IS_24_HOUR] ?: false
        val language = parseEnumOrDefault(prefs[Keys.APP_LANGUAGE], AppLanguage.SYSTEM)
        val themeMode = parseEnumOrDefault(prefs[Keys.THEME_MODE], AppThemeMode.SYSTEM)
        val colorPreset = parseEnumOrDefault(prefs[Keys.COLOR_PRESET], AppColorPreset.SYSTEM_DYNAMIC)
        val customColorSeed = prefs[Keys.CUSTOM_COLOR_SEED] ?: 0xFF6750A4

        val followSystemColors = prefs[Keys.FOLLOW_SYSTEM_COLORS] ?: (colorPreset == AppColorPreset.SYSTEM_DYNAMIC)

        // Widget settings
        val wTheme = parseEnumOrDefault(prefs[Keys.WIDGET_THEME_MODE], WidgetThemeMode.APP_THEME)
        val rawWBg = prefs[Keys.LEGACY_WIDGET_BG_STYLE]
        val wShowBackground = prefs[Keys.WIDGET_SHOW_BACKGROUND]
            ?: legacyWidgetBackgroundWasEnabled(rawWBg)
        val wOpacity = prefs[Keys.WIDGET_OPACITY] ?: 100
        val wShowBorder = prefs[Keys.WIDGET_SHOW_BORDER]
            ?: false
        val wFont = parseEnumOrDefault(prefs[Keys.WIDGET_FONT_SIZE], WidgetFontSize.STANDARD)
        val wTextStyle = parseEnumOrDefault(prefs[Keys.WIDGET_TEXT_STYLE], WidgetTextStyle.AUTO)

        val wShowLoc = prefs[Keys.WIDGET_SHOW_LOC] ?: true
        val wShowHijri = prefs[Keys.WIDGET_SHOW_HIJRI] ?: true
        val wShowCd = prefs[Keys.WIDGET_SHOW_COUNTDOWN] ?: true
        val wShowSunrise = prefs[Keys.WIDGET_SHOW_SUNRISE] ?: true
        val wShowAll = prefs[Keys.WIDGET_SHOW_ALL] ?: true
        val wShowHero = prefs[Keys.WIDGET_SHOW_HERO] ?: true

        val widgetSettings = WidgetCustomizationSettings(
            themeMode = wTheme,
            showBackground = wShowBackground,
            opacityPercent = wOpacity,
            showBorder = wShowBorder,
            fontSize = wFont,
            textStyle = wTextStyle,
            showLocation = wShowLoc,
            showHijriDate = wShowHijri,
            showCountdown = wShowCd,
            showSunrise = wShowSunrise,
            showAllPrayersList = wShowAll,
            showHeroCard = wShowHero
        )

        val audioStream = parseEnumOrDefault(prefs[Keys.AUDIO_STREAM], AthanAudioStream.ALARM)

        val wakeScreenOnAlarm = prefs[Keys.WAKE_SCREEN_ON_ALARM] ?: true
        val onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        val liveCountdownEnabled = prefs[Keys.LIVE_COUNTDOWN_ENABLED] ?: true
        val liveCountdownMinutes = prefs[Keys.LIVE_COUNTDOWN_MINUTES] ?: 15

        val adjustments = PrayerTimeAdjustments(
            fajr = prefs[Keys.ADJ_FAJR] ?: 0,
            sunrise = prefs[Keys.ADJ_SUNRISE] ?: 0,
            dhuhr = prefs[Keys.ADJ_DHUHR] ?: 0,
            asr = prefs[Keys.ADJ_ASR] ?: 0,
            maghrib = prefs[Keys.ADJ_MAGHRIB] ?: 0,
            isha = prefs[Keys.ADJ_ISHA] ?: 0
        )

        val prayerConfigs = PrayerType.values().associateWith { prayer ->
            val defaultEnabled = prayer != PrayerType.SUNRISE
            val defaultSound = when (prayer) {
                PrayerType.FAJR -> NotificationSoundType.ATHAN_FAJR
                PrayerType.SUNRISE -> NotificationSoundType.DEVICE_DEFAULT
                else -> NotificationSoundType.FULL_ATHAN
            }

            val enabled = prefs[Keys.notifEnabled(prayer)] ?: defaultEnabled
            val soundType = parseEnumOrDefault(prefs[Keys.notifSound(prayer)], defaultSound)
            val preMinutes = prefs[Keys.notifPreReminder(prayer)] ?: 0

            NotificationPrayerConfig(enabled, soundType, preMinutes)
        }

        return AppPrayerSettings(
            location = location,
            calculationMethod = calcMethod,
            juristicMethod = juristic,
            highLatitudeRule = highLat,
            hijriAdjustmentDays = hijriOffset,
            hijriAdjustmentAnchorMonth = hijriOffsetAnchor,
            is24HourFormat = is24Hour,
            language = language,
            themeMode = themeMode,
            colorPreset = colorPreset,
            customColorSeed = customColorSeed,
            followSystemColors = followSystemColors,
            widgetSettings = widgetSettings,
            audioStream = audioStream,
            wakeScreenOnAlarm = wakeScreenOnAlarm,
            onboardingCompleted = onboardingCompleted,
            liveCountdownEnabled = liveCountdownEnabled,
            liveCountdownMinutesBefore = liveCountdownMinutes,
            prayerConfigs = prayerConfigs,
            adjustments = adjustments
        )
    }

    /** Mirrors the canonical DataStore snapshot needed by synchronous startup and receivers. */
    private fun syncFastCache(settings: AppPrayerSettings) {
        val location = settings.location
        val widget = settings.widgetSettings
        val adjustments = settings.adjustments
        context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANG, settings.language.name)
            .putString(KEY_THEME, settings.themeMode.name)
            .putString(KEY_COLOR_PRESET, settings.colorPreset.name)
            .putLong(KEY_CUSTOM_COLOR_SEED, settings.customColorSeed)
            .putBoolean(KEY_FOLLOW_SYSTEM_COLORS, settings.followSystemColors)
            .putString(KEY_WIDGET_THEME, widget.themeMode.name)
            .putBoolean(KEY_WIDGET_SHOW_BACKGROUND, widget.showBackground)
            .remove(LEGACY_KEY_WIDGET_BG)
            .putInt(KEY_WIDGET_OPACITY, widget.opacityPercent)
            .putBoolean(KEY_WIDGET_SHOW_BORDER, widget.showBorder)
            .putString(KEY_WIDGET_FONT, widget.fontSize.name)
            .putString(KEY_WIDGET_TEXT_STYLE, widget.textStyle.name)
            .putBoolean(KEY_WIDGET_SHOW_LOC, widget.showLocation)
            .putBoolean(KEY_WIDGET_SHOW_HIJRI, widget.showHijriDate)
            .putBoolean(KEY_WIDGET_SHOW_COUNTDOWN, widget.showCountdown)
            .putBoolean(KEY_WIDGET_SHOW_SUNRISE, widget.showSunrise)
            .putBoolean(KEY_WIDGET_SHOW_ALL, widget.showAllPrayersList)
            .putBoolean(KEY_WIDGET_SHOW_HERO, widget.showHeroCard)
            .putBoolean(KEY_24H, settings.is24HourFormat)
            .putString(KEY_LOC_NAME, location.name)
            .putString(KEY_LOC_COUNTRY, location.country)
            .putLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(location.longitude))
            .putString(KEY_LOC_TZ, location.timeZoneId)
            .putBoolean(KEY_LOC_IS_GPS, location.isGps)
            .let { editor ->
                location.nearestPlaceDistanceKm?.let {
                    editor.putLong(KEY_LOC_DISTANCE_KM, java.lang.Double.doubleToRawLongBits(it))
                } ?: editor.remove(KEY_LOC_DISTANCE_KM)
            }
            .putString(KEY_CALC_METHOD, settings.calculationMethod.name)
            .putString(KEY_JURISTIC_METHOD, settings.juristicMethod.name)
            .putString(KEY_HIGH_LAT_RULE, settings.highLatitudeRule.name)
            .putInt(KEY_HIJRI_OFFSET, settings.hijriAdjustmentDays)
            .putString(KEY_HIJRI_OFFSET_ANCHOR, settings.hijriAdjustmentAnchorMonth.orEmpty())
            .putInt(KEY_ADJ_FAJR, adjustments.fajr)
            .putInt(KEY_ADJ_SUNRISE, adjustments.sunrise)
            .putInt(KEY_ADJ_DHUHR, adjustments.dhuhr)
            .putInt(KEY_ADJ_ASR, adjustments.asr)
            .putInt(KEY_ADJ_MAGHRIB, adjustments.maghrib)
            .putInt(KEY_ADJ_ISHA, adjustments.isha)
            .putString(KEY_AUDIO_STREAM, settings.audioStream.name)
            .putBoolean(KEY_WAKE_SCREEN, settings.wakeScreenOnAlarm)
            .putBoolean(KEY_ONBOARDING_COMPLETED, settings.onboardingCompleted)
            .putBoolean(KEY_LIVE_COUNTDOWN_ENABLED, settings.liveCountdownEnabled)
            .putInt(KEY_LIVE_COUNTDOWN_MINUTES, settings.liveCountdownMinutesBefore)
            .apply()
    }

    private suspend fun editSettings(transform: (MutablePreferences) -> Unit) {
        val updated = context.dataStore.edit(transform)
        syncFastCache(settingsFromDataStore(updated))
    }

    val settingsFlow: Flow<AppPrayerSettings> = context.dataStore.data.map { prefs ->
        settingsFromDataStore(prefs).also(::syncFastCache)
    }

    suspend fun updateWidgetSettings(settings: WidgetCustomizationSettings) {
        editSettings { prefs ->
            prefs[Keys.WIDGET_THEME_MODE] = settings.themeMode.name
            prefs[Keys.WIDGET_SHOW_BACKGROUND] = settings.showBackground
            prefs.remove(Keys.LEGACY_WIDGET_BG_STYLE)
            prefs[Keys.WIDGET_OPACITY] = settings.opacityPercent
            prefs[Keys.WIDGET_SHOW_BORDER] = settings.showBorder
            prefs[Keys.WIDGET_FONT_SIZE] = settings.fontSize.name
            prefs[Keys.WIDGET_TEXT_STYLE] = settings.textStyle.name
            prefs[Keys.WIDGET_SHOW_LOC] = settings.showLocation
            prefs[Keys.WIDGET_SHOW_HIJRI] = settings.showHijriDate
            prefs[Keys.WIDGET_SHOW_COUNTDOWN] = settings.showCountdown
            prefs[Keys.WIDGET_SHOW_SUNRISE] = settings.showSunrise
            prefs[Keys.WIDGET_SHOW_ALL] = settings.showAllPrayersList
            prefs[Keys.WIDGET_SHOW_HERO] = settings.showHeroCard
        }
    }

    suspend fun updateAudioStream(audioStream: AthanAudioStream) {
        editSettings { prefs ->
            prefs[Keys.AUDIO_STREAM] = audioStream.name
        }
    }

    suspend fun updateWakeScreenOnAlarm(enabled: Boolean) {
        editSettings { prefs ->
            prefs[Keys.WAKE_SCREEN_ON_ALARM] = enabled
        }
    }

    suspend fun updateLiveCountdownSettings(enabled: Boolean, minutesBefore: Int) {
        editSettings { prefs ->
            prefs[Keys.LIVE_COUNTDOWN_ENABLED] = enabled
            prefs[Keys.LIVE_COUNTDOWN_MINUTES] = minutesBefore
        }
    }


    suspend fun updateLocation(location: UserLocation) {
        editSettings { prefs ->
            prefs[Keys.LOC_NAME] = location.name
            prefs[Keys.LOC_COUNTRY] = location.country
            prefs[Keys.LOC_LAT] = location.latitude
            prefs[Keys.LOC_LON] = location.longitude
            prefs[Keys.LOC_TZ] = location.timeZoneId
            prefs[Keys.LOC_IS_GPS] = location.isGps
            if (location.nearestPlaceDistanceKm != null) {
                prefs[Keys.LOC_DISTANCE_KM] = location.nearestPlaceDistanceKm
            } else {
                prefs.remove(Keys.LOC_DISTANCE_KM)
            }
        }
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        editSettings { prefs ->
            prefs[Keys.CALC_METHOD] = method.name
        }
    }

    suspend fun updateJuristicMethod(method: JuristicMethod) {
        editSettings { prefs ->
            prefs[Keys.JURISTIC_METHOD] = method.name
        }
    }

    suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        editSettings { prefs ->
            prefs[Keys.HIGH_LAT_RULE] = rule.name
        }
    }

    suspend fun updateHijriOffset(
        offset: Int,
        anchorMonth: String? = if (offset == 0) null else HijriCalendar.monthKeyFor(LocalDate.now())
    ) {
        val boundedOffset = offset.coerceIn(-2, 2)
        val storedAnchor = anchorMonth.takeIf { boundedOffset != 0 }
        editSettings { prefs ->
            prefs[Keys.HIJRI_OFFSET] = boundedOffset
            if (storedAnchor == null) prefs.remove(Keys.HIJRI_OFFSET_ANCHOR)
            else prefs[Keys.HIJRI_OFFSET_ANCHOR] = storedAnchor
        }
    }

    suspend fun clearExpiredHijriOffset() {
        val currentMonth = HijriCalendar.monthKeyFor(LocalDate.now())
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        val offset = fastPrefs.getInt(KEY_HIJRI_OFFSET, 0)
        val anchor = fastPrefs.getString(KEY_HIJRI_OFFSET_ANCHOR, null)?.takeIf { it.isNotBlank() }
        if (offset != 0 && anchor != currentMonth) updateHijriOffset(0, null)
    }

    suspend fun updateIs24Hour(is24: Boolean) {
        editSettings { prefs ->
            prefs[Keys.IS_24_HOUR] = is24
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        // Persist before touching LocaleManager. Assigning applicationLocales can recreate the
        // Activity immediately; when it happened first, that recreation cancelled this coroutine
        // before DataStore was updated. A fresh Arabic setup then reopened with SYSTEM/English
        // data while Android had already switched the window to RTL.
        editSettings { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.name
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                when (language) {
                    AppLanguage.SYSTEM -> localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
                    AppLanguage.ARABIC -> localeManager?.applicationLocales = LocaleList.forLanguageTags("ar")
                    AppLanguage.ENGLISH -> localeManager?.applicationLocales = LocaleList.forLanguageTags("en")
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        editSettings { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateColorPreset(preset: AppColorPreset) {
        editSettings { prefs ->
            prefs[Keys.COLOR_PRESET] = preset.name
            if (preset != AppColorPreset.SYSTEM_DYNAMIC) {
                prefs[Keys.FOLLOW_SYSTEM_COLORS] = false
            }
        }
    }

    suspend fun updateCustomColorSeed(seed: Long) {
        editSettings { prefs ->
            prefs[Keys.CUSTOM_COLOR_SEED] = seed
            prefs[Keys.COLOR_PRESET] = AppColorPreset.CUSTOM.name
            prefs[Keys.FOLLOW_SYSTEM_COLORS] = false
        }
    }

    suspend fun updateFollowSystemColors(follow: Boolean) {
        editSettings { prefs ->
            prefs[Keys.FOLLOW_SYSTEM_COLORS] = follow
            if (follow) {
                prefs[Keys.COLOR_PRESET] = AppColorPreset.SYSTEM_DYNAMIC.name
            }
        }
    }

    suspend fun updatePrayerAdjustment(prayer: PrayerType, offsetMinutes: Int) {
        editSettings { prefs ->
            when (prayer) {
                PrayerType.FAJR -> prefs[Keys.ADJ_FAJR] = offsetMinutes
                PrayerType.SUNRISE -> prefs[Keys.ADJ_SUNRISE] = offsetMinutes
                PrayerType.DHUHR -> prefs[Keys.ADJ_DHUHR] = offsetMinutes
                PrayerType.ASR -> prefs[Keys.ADJ_ASR] = offsetMinutes
                PrayerType.MAGHRIB -> prefs[Keys.ADJ_MAGHRIB] = offsetMinutes
                PrayerType.ISHA -> prefs[Keys.ADJ_ISHA] = offsetMinutes
            }
        }
    }

    suspend fun updatePrayerNotification(
        prayer: PrayerType,
        enabled: Boolean,
        soundType: NotificationSoundType,
        preReminderMinutes: Int
    ) {
        editSettings { prefs ->
            prefs[Keys.notifEnabled(prayer)] = enabled
            prefs[Keys.notifSound(prayer)] = soundType.name
            prefs[Keys.notifPreReminder(prayer)] = preReminderMinutes
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        editSettings { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
}
