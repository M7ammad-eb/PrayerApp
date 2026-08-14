package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.cities.CityDatabase
import com.example.data.models.AppLanguage
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
    val prayerConfigs: Map<PrayerType, NotificationPrayerConfig> = PrayerType.values().associateWith {
        NotificationPrayerConfig(
            enabled = it != PrayerType.SUNRISE, // Sunrise not a prayer, so disabled by default or reminder only
            soundType = if (it == PrayerType.SUNRISE) NotificationSoundType.MELODIC_TONE else NotificationSoundType.FULL_ATHAN,
            preReminderMinutes = 0
        )
    },
    val adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments()
)

class PrayerPreferences(private val context: Context) {

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
            val defaultSound = if (prayer == PrayerType.SUNRISE) NotificationSoundType.MELODIC_TONE else NotificationSoundType.FULL_ATHAN

            val enabled = prefs[Keys.notifEnabled(prayer)] ?: defaultEnabled
            val soundStr = prefs[Keys.notifSound(prayer)]
            val soundType = soundStr?.let {
                try { NotificationSoundType.valueOf(it) } catch (e: Exception) { defaultSound }
            } ?: defaultSound
            val preMinutes = prefs[Keys.notifPreReminder(prayer)] ?: 0

            NotificationPrayerConfig(enabled, soundType, preMinutes)
        }

        AppPrayerSettings(
            location = location,
            calculationMethod = calcMethod,
            juristicMethod = juristic,
            highLatitudeRule = highLat,
            hijriAdjustmentDays = hijriOffset,
            is24HourFormat = is24Hour,
            language = language,
            prayerConfigs = prayerConfigs,
            adjustments = adjustments
        )
    }

    suspend fun updateLocation(location: UserLocation) {
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
        context.dataStore.edit { prefs ->
            prefs[Keys.CALC_METHOD] = method.name
        }
    }

    suspend fun updateJuristicMethod(method: JuristicMethod) {
        context.dataStore.edit { prefs ->
            prefs[Keys.JURISTIC_METHOD] = method.name
        }
    }

    suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGH_LAT_RULE] = rule.name
        }
    }

    suspend fun updateHijriOffset(offset: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIJRI_OFFSET] = offset
        }
    }

    suspend fun updateIs24Hour(is24: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_24_HOUR] = is24
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.name
        }
    }

    suspend fun updatePrayerAdjustment(prayer: PrayerType, offsetMinutes: Int) {
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
