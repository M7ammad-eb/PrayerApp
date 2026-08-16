package com.example.data.preferences

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.cities.CityDatabase
import com.example.data.models.AppColorPreset
import com.example.data.models.AppLanguage
import com.example.data.models.AppThemeMode
import com.example.data.models.AthanAudioStream
import com.example.data.models.CalculationMethod
import com.example.data.models.HighLatitudeRule
import com.example.data.models.JuristicMethod
import com.example.data.models.NotificationPrayerConfig
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerTimeAdjustments
import com.example.data.models.PrayerType
import com.example.data.models.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prayer_settings")

data class AppPrayerSettings(
    val location: UserLocation = CityDatabase.DEFAULT_LOCATION,
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    val hijriAdjustmentDays: Int = 0,
    val is24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPreset: AppColorPreset = AppColorPreset.SYSTEM_DYNAMIC,
    val followSystemColors: Boolean = true,
    val dynamicIslandEnabled: Boolean = true,
    val dynamicIslandMinutesBefore: Int = 15,
    val audioStream: AthanAudioStream = AthanAudioStream.ALARM,
    val wakeScreenOnAlarm: Boolean = true,
    val prayerConfigs: Map<PrayerType, NotificationPrayerConfig> = PrayerType.values().associateWith {
        NotificationPrayerConfig(
            enabled = it != PrayerType.SUNRISE, // Sunrise not a prayer, so disabled by default or reminder only
            soundType = when (it) {
                PrayerType.FAJR -> NotificationSoundType.ATHAN_FAJR
                PrayerType.SUNRISE -> NotificationSoundType.MELODIC_TONE
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
        private const val KEY_FOLLOW_SYSTEM_COLORS = "cached_follow_system_colors"
        private const val KEY_24H = "cached_24h"

        private const val KEY_LOC_NAME = "cached_loc_name"
        private const val KEY_LOC_COUNTRY = "cached_loc_country"
        private const val KEY_LOC_LAT = "cached_loc_lat"
        private const val KEY_LOC_LON = "cached_loc_lon"
        private const val KEY_LOC_TZ = "cached_loc_tz"
        private const val KEY_LOC_IS_GPS = "cached_loc_is_gps"

        private const val KEY_CALC_METHOD = "cached_calc_method"
        private const val KEY_JURISTIC_METHOD = "cached_juristic_method"
        private const val KEY_HIGH_LAT_RULE = "cached_high_lat_rule"
        private const val KEY_HIJRI_OFFSET = "cached_hijri_offset"

        private const val KEY_ADJ_FAJR = "cached_adj_fajr"
        private const val KEY_ADJ_SUNRISE = "cached_adj_sunrise"
        private const val KEY_ADJ_DHUHR = "cached_adj_dhuhr"
        private const val KEY_ADJ_ASR = "cached_adj_asr"
        private const val KEY_ADJ_MAGHRIB = "cached_adj_maghrib"
        private const val KEY_ADJ_ISHA = "cached_adj_isha"

        private const val KEY_DYNAMIC_ISLAND_ENABLED = "cached_dyn_island_enabled"
        private const val KEY_DYNAMIC_ISLAND_MINUTES = "cached_dyn_island_minutes"
        private const val KEY_AUDIO_STREAM = "cached_audio_stream"
        private const val KEY_WAKE_SCREEN = "cached_wake_screen"

        fun getInitialSettings(context: Context): AppPrayerSettings {
            val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)

            // 1. Location
            val locName = fastPrefs.getString(KEY_LOC_NAME, null)
            val locCountry = fastPrefs.getString(KEY_LOC_COUNTRY, null)
            val locLat = if (fastPrefs.contains(KEY_LOC_LAT)) {
                java.lang.Double.longBitsToDouble(fastPrefs.getLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(CityDatabase.DEFAULT_LOCATION.latitude)))
            } else {
                CityDatabase.DEFAULT_LOCATION.latitude
            }
            val locLon = if (fastPrefs.contains(KEY_LOC_LON)) {
                java.lang.Double.longBitsToDouble(fastPrefs.getLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(CityDatabase.DEFAULT_LOCATION.longitude)))
            } else {
                CityDatabase.DEFAULT_LOCATION.longitude
            }
            val locTz = fastPrefs.getString(KEY_LOC_TZ, CityDatabase.DEFAULT_LOCATION.timeZoneId) ?: CityDatabase.DEFAULT_LOCATION.timeZoneId
            val locIsGps = fastPrefs.getBoolean(KEY_LOC_IS_GPS, false)

            val location = if (locName != null && locCountry != null) {
                UserLocation(locName, locCountry, locLat, locLon, locTz, locIsGps)
            } else {
                CityDatabase.DEFAULT_LOCATION
            }

            // 2. Calculation Methods
            val calcMethodStr = fastPrefs.getString(KEY_CALC_METHOD, null)
            val calcMethod = calcMethodStr?.let {
                try { CalculationMethod.valueOf(it) } catch (e: Exception) { CalculationMethod.MUSLIM_WORLD_LEAGUE }
            } ?: CalculationMethod.MUSLIM_WORLD_LEAGUE

            val juristicStr = fastPrefs.getString(KEY_JURISTIC_METHOD, null)
            val juristic = juristicStr?.let {
                try { JuristicMethod.valueOf(it) } catch (e: Exception) { JuristicMethod.STANDARD }
            } ?: JuristicMethod.STANDARD

            val highLatStr = fastPrefs.getString(KEY_HIGH_LAT_RULE, null)
            val highLat = highLatStr?.let {
                try { HighLatitudeRule.valueOf(it) } catch (e: Exception) { HighLatitudeRule.ANGLE_BASED }
            } ?: HighLatitudeRule.ANGLE_BASED

            val hijriOffset = fastPrefs.getInt(KEY_HIJRI_OFFSET, 0)

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
            val langStr = fastPrefs.getString(KEY_LANG, null)
            val lang = langStr?.let {
                try { AppLanguage.valueOf(it) } catch (e: Exception) { AppLanguage.SYSTEM }
            } ?: AppLanguage.SYSTEM

            val themeStr = fastPrefs.getString(KEY_THEME, null)
            val theme = themeStr?.let {
                try { AppThemeMode.valueOf(it) } catch (e: Exception) { AppThemeMode.SYSTEM }
            } ?: AppThemeMode.SYSTEM

            val colorStr = fastPrefs.getString(KEY_COLOR_PRESET, null)
            val color = colorStr?.let {
                try { AppColorPreset.valueOf(it) } catch (e: Exception) { AppColorPreset.SYSTEM_DYNAMIC }
            } ?: AppColorPreset.SYSTEM_DYNAMIC

            val followSys = fastPrefs.getBoolean(KEY_FOLLOW_SYSTEM_COLORS, true)
            val is24h = fastPrefs.getBoolean(KEY_24H, false)

            val dynIsland = fastPrefs.getBoolean(KEY_DYNAMIC_ISLAND_ENABLED, true)
            val dynMinutes = fastPrefs.getInt(KEY_DYNAMIC_ISLAND_MINUTES, 15)

            val audioStreamStr = fastPrefs.getString(KEY_AUDIO_STREAM, null)
            val audioStream = audioStreamStr?.let {
                try { AthanAudioStream.valueOf(it) } catch (e: Exception) { AthanAudioStream.ALARM }
            } ?: AthanAudioStream.ALARM

            val wakeScreen = fastPrefs.getBoolean(KEY_WAKE_SCREEN, true)

            return AppPrayerSettings(
                location = location,
                calculationMethod = calcMethod,
                juristicMethod = juristic,
                highLatitudeRule = highLat,
                hijriAdjustmentDays = hijriOffset,
                is24HourFormat = is24h,
                language = lang,
                themeMode = theme,
                colorPreset = color,
                followSystemColors = followSys,
                dynamicIslandEnabled = dynIsland,
                dynamicIslandMinutesBefore = dynMinutes,
                audioStream = audioStream,
                wakeScreenOnAlarm = wakeScreen,
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

        val CALC_METHOD = stringPreferencesKey("calc_method")
        val JURISTIC_METHOD = stringPreferencesKey("juristic_method")
        val HIGH_LAT_RULE = stringPreferencesKey("high_lat_rule")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PRESET = stringPreferencesKey("color_preset")
        val FOLLOW_SYSTEM_COLORS = booleanPreferencesKey("follow_system_colors")
        val DYNAMIC_ISLAND_ENABLED = booleanPreferencesKey("dynamic_island_enabled")
        val DYNAMIC_ISLAND_MINUTES = intPreferencesKey("dynamic_island_minutes")
        val AUDIO_STREAM = stringPreferencesKey("audio_stream")
        val WAKE_SCREEN_ON_ALARM = booleanPreferencesKey("wake_screen_on_alarm")

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

    val settingsFlow: Flow<AppPrayerSettings> = context.dataStore.data.map { prefs ->
        val locName = prefs[Keys.LOC_NAME] ?: CityDatabase.DEFAULT_LOCATION.name
        val locCountry = prefs[Keys.LOC_COUNTRY] ?: CityDatabase.DEFAULT_LOCATION.country
        val locLat = prefs[Keys.LOC_LAT] ?: CityDatabase.DEFAULT_LOCATION.latitude
        val locLon = prefs[Keys.LOC_LON] ?: CityDatabase.DEFAULT_LOCATION.longitude
        val locTz = prefs[Keys.LOC_TZ] ?: CityDatabase.DEFAULT_LOCATION.timeZoneId
        val locIsGps = prefs[Keys.LOC_IS_GPS] ?: false

        val location = UserLocation(locName, locCountry, locLat, locLon, locTz, locIsGps)

        val calcMethod = prefs[Keys.CALC_METHOD]?.let {
            try { CalculationMethod.valueOf(it) } catch (e: Exception) { CalculationMethod.MUSLIM_WORLD_LEAGUE }
        } ?: CalculationMethod.MUSLIM_WORLD_LEAGUE

        val juristic = prefs[Keys.JURISTIC_METHOD]?.let {
            try { JuristicMethod.valueOf(it) } catch (e: Exception) { JuristicMethod.STANDARD }
        } ?: JuristicMethod.STANDARD

        val highLat = prefs[Keys.HIGH_LAT_RULE]?.let {
            try { HighLatitudeRule.valueOf(it) } catch (e: Exception) { HighLatitudeRule.ANGLE_BASED }
        } ?: HighLatitudeRule.ANGLE_BASED

        val hijriOffset = prefs[Keys.HIJRI_OFFSET] ?: 0
        val is24Hour = prefs[Keys.IS_24_HOUR] ?: false
        val languageStr = prefs[Keys.APP_LANGUAGE]
        val language = languageStr?.let {
            try { AppLanguage.valueOf(it) } catch (e: Exception) { AppLanguage.SYSTEM }
        } ?: AppLanguage.SYSTEM

        val themeStr = prefs[Keys.THEME_MODE]
        val themeMode = themeStr?.let {
            try { AppThemeMode.valueOf(it) } catch (e: Exception) { AppThemeMode.SYSTEM }
        } ?: AppThemeMode.SYSTEM

        val colorPresetStr = prefs[Keys.COLOR_PRESET]
        val colorPreset = colorPresetStr?.let {
            try { AppColorPreset.valueOf(it) } catch (e: Exception) { AppColorPreset.SYSTEM_DYNAMIC }
        } ?: AppColorPreset.SYSTEM_DYNAMIC

        val followSystemColors = prefs[Keys.FOLLOW_SYSTEM_COLORS] ?: (colorPreset == AppColorPreset.SYSTEM_DYNAMIC)

        val audioStreamStr = prefs[Keys.AUDIO_STREAM]
        val audioStream = audioStreamStr?.let {
            try { AthanAudioStream.valueOf(it) } catch (e: Exception) { AthanAudioStream.ALARM }
        } ?: AthanAudioStream.ALARM

        val wakeScreenOnAlarm = prefs[Keys.WAKE_SCREEN_ON_ALARM] ?: true

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
                PrayerType.SUNRISE -> NotificationSoundType.MELODIC_TONE
                else -> NotificationSoundType.FULL_ATHAN
            }

            val enabled = prefs[Keys.notifEnabled(prayer)] ?: defaultEnabled
            val soundStr = prefs[Keys.notifSound(prayer)]
            val soundType = soundStr?.let {
                try { NotificationSoundType.valueOf(it) } catch (e: Exception) { defaultSound }
            } ?: defaultSound
            val preMinutes = prefs[Keys.notifPreReminder(prayer)] ?: 0

            NotificationPrayerConfig(enabled, soundType, preMinutes)
        }

        val dynamicIslandEnabled = prefs[Keys.DYNAMIC_ISLAND_ENABLED] ?: true
        val dynamicIslandMinutes = prefs[Keys.DYNAMIC_ISLAND_MINUTES] ?: 15

        // Update fast cache for zero-latency instant startup
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit()
            .putString(KEY_LANG, language.name)
            .putString(KEY_THEME, themeMode.name)
            .putString(KEY_COLOR_PRESET, colorPreset.name)
            .putBoolean(KEY_FOLLOW_SYSTEM_COLORS, followSystemColors)
            .putBoolean(KEY_24H, is24Hour)
            .putString(KEY_LOC_NAME, location.name)
            .putString(KEY_LOC_COUNTRY, location.country)
            .putLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(location.longitude))
            .putString(KEY_LOC_TZ, location.timeZoneId)
            .putBoolean(KEY_LOC_IS_GPS, location.isGps)
            .putString(KEY_CALC_METHOD, calcMethod.name)
            .putString(KEY_JURISTIC_METHOD, juristic.name)
            .putString(KEY_HIGH_LAT_RULE, highLat.name)
            .putInt(KEY_HIJRI_OFFSET, hijriOffset)
            .putInt(KEY_ADJ_FAJR, adjustments.fajr)
            .putInt(KEY_ADJ_SUNRISE, adjustments.sunrise)
            .putInt(KEY_ADJ_DHUHR, adjustments.dhuhr)
            .putInt(KEY_ADJ_ASR, adjustments.asr)
            .putInt(KEY_ADJ_MAGHRIB, adjustments.maghrib)
            .putInt(KEY_ADJ_ISHA, adjustments.isha)
            .putBoolean(KEY_DYNAMIC_ISLAND_ENABLED, dynamicIslandEnabled)
            .putInt(KEY_DYNAMIC_ISLAND_MINUTES, dynamicIslandMinutes)
            .putString(KEY_AUDIO_STREAM, audioStream.name)
            .putBoolean(KEY_WAKE_SCREEN, wakeScreenOnAlarm)
            .apply()

        AppPrayerSettings(
            location = location,
            calculationMethod = calcMethod,
            juristicMethod = juristic,
            highLatitudeRule = highLat,
            hijriAdjustmentDays = hijriOffset,
            is24HourFormat = is24Hour,
            language = language,
            themeMode = themeMode,
            colorPreset = colorPreset,
            followSystemColors = followSystemColors,
            dynamicIslandEnabled = dynamicIslandEnabled,
            dynamicIslandMinutesBefore = dynamicIslandMinutes,
            audioStream = audioStream,
            wakeScreenOnAlarm = wakeScreenOnAlarm,
            prayerConfigs = prayerConfigs,
            adjustments = adjustments
        )
    }

    suspend fun updateAudioStream(audioStream: AthanAudioStream) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_AUDIO_STREAM, audioStream.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.AUDIO_STREAM] = audioStream.name
        }
    }

    suspend fun updateWakeScreenOnAlarm(enabled: Boolean) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putBoolean(KEY_WAKE_SCREEN, enabled).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.WAKE_SCREEN_ON_ALARM] = enabled
        }
    }

    suspend fun updateDynamicIslandSettings(enabled: Boolean, minutesBefore: Int) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit()
            .putBoolean(KEY_DYNAMIC_ISLAND_ENABLED, enabled)
            .putInt(KEY_DYNAMIC_ISLAND_MINUTES, minutesBefore)
            .apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_ISLAND_ENABLED] = enabled
            prefs[Keys.DYNAMIC_ISLAND_MINUTES] = minutesBefore
        }
    }

    suspend fun updateLocation(location: UserLocation) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit()
            .putString(KEY_LOC_NAME, location.name)
            .putString(KEY_LOC_COUNTRY, location.country)
            .putLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(location.longitude))
            .putString(KEY_LOC_TZ, location.timeZoneId)
            .putBoolean(KEY_LOC_IS_GPS, location.isGps)
            .apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.LOC_NAME] = location.name
            prefs[Keys.LOC_COUNTRY] = location.country
            prefs[Keys.LOC_LAT] = location.latitude
            prefs[Keys.LOC_LON] = location.longitude
            prefs[Keys.LOC_TZ] = location.timeZoneId
            prefs[Keys.LOC_IS_GPS] = location.isGps
        }
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_CALC_METHOD, method.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.CALC_METHOD] = method.name
        }
    }

    suspend fun updateJuristicMethod(method: JuristicMethod) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_JURISTIC_METHOD, method.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.JURISTIC_METHOD] = method.name
        }
    }

    suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_HIGH_LAT_RULE, rule.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGH_LAT_RULE] = rule.name
        }
    }

    suspend fun updateHijriOffset(offset: Int) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putInt(KEY_HIJRI_OFFSET, offset).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.HIJRI_OFFSET] = offset
        }
    }

    suspend fun updateIs24Hour(is24: Boolean) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putBoolean(KEY_24H, is24).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_24_HOUR] = is24
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_LANG, language.name).apply()

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

        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.name
        }
    }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_THEME, themeMode.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateColorPreset(preset: AppColorPreset) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putString(KEY_COLOR_PRESET, preset.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.COLOR_PRESET] = preset.name
            if (preset != AppColorPreset.SYSTEM_DYNAMIC) {
                prefs[Keys.FOLLOW_SYSTEM_COLORS] = false
            }
        }
    }

    suspend fun updateFollowSystemColors(follow: Boolean) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        fastPrefs.edit().putBoolean(KEY_FOLLOW_SYSTEM_COLORS, follow).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.FOLLOW_SYSTEM_COLORS] = follow
            if (follow) {
                prefs[Keys.COLOR_PRESET] = AppColorPreset.SYSTEM_DYNAMIC.name
            }
        }
    }

    suspend fun updatePrayerAdjustment(prayer: PrayerType, offsetMinutes: Int) {
        val fastPrefs = context.getSharedPreferences(FAST_CACHE_PREFS, Context.MODE_PRIVATE)
        when (prayer) {
            PrayerType.FAJR -> fastPrefs.edit().putInt(KEY_ADJ_FAJR, offsetMinutes).apply()
            PrayerType.SUNRISE -> fastPrefs.edit().putInt(KEY_ADJ_SUNRISE, offsetMinutes).apply()
            PrayerType.DHUHR -> fastPrefs.edit().putInt(KEY_ADJ_DHUHR, offsetMinutes).apply()
            PrayerType.ASR -> fastPrefs.edit().putInt(KEY_ADJ_ASR, offsetMinutes).apply()
            PrayerType.MAGHRIB -> fastPrefs.edit().putInt(KEY_ADJ_MAGHRIB, offsetMinutes).apply()
            PrayerType.ISHA -> fastPrefs.edit().putInt(KEY_ADJ_ISHA, offsetMinutes).apply()
        }
        context.dataStore.edit { prefs ->
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
        context.dataStore.edit { prefs ->
            prefs[Keys.notifEnabled(prayer)] = enabled
            prefs[Keys.notifSound(prayer)] = soundType.name
            prefs[Keys.notifPreReminder(prayer)] = preReminderMinutes
        }
    }
}
