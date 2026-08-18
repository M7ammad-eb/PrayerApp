package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AdhanPlaybackState
import com.example.audio.AthanAudioEngine
import com.example.data.calculator.PrayerTimesCalculator
import com.example.data.cities.CityDatabase
import com.example.data.models.AppLanguage
import com.example.data.models.CalculationMethod
import com.example.data.models.DailyPrayerSchedule
import com.example.data.models.HighLatitudeRule
import com.example.data.models.JuristicMethod
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerTimeItem
import com.example.data.models.PrayerType
import com.example.data.models.UserLocation
import com.example.data.preferences.AppPrayerSettings
import com.example.data.preferences.PrayerPreferences
import com.example.data.qibla.CompassSensorManager
import com.example.data.qibla.CompassState
import com.example.data.qibla.QiblaCalculator
import com.example.notifications.PrayerNotificationScheduler
import com.example.widget.PrayerAppWidgetProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NextPrayerInfo(
    val prayerItem: PrayerTimeItem? = null,
    val remainingFormatted: String = "",
    val remainingSeconds: Long = 0,
    val progressPercent: Float = 0f,
    val isNextDayFajr: Boolean = false
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrayerPreferences(application)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    val compassManager = CompassSensorManager(application)

    val settings: StateFlow<AppPrayerSettings> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PrayerPreferences.getInitialSettings(application)
    )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _dailySchedule = MutableStateFlow<DailyPrayerSchedule?>(null)
    val dailySchedule: StateFlow<DailyPrayerSchedule?> = _dailySchedule.asStateFlow()

    private val _nextPrayerInfo = MutableStateFlow(NextPrayerInfo())
    val nextPrayerInfo: StateFlow<NextPrayerInfo> = _nextPrayerInfo.asStateFlow()

    val compassState: StateFlow<CompassState> = compassManager.compassState

    val audioPlaybackState: StateFlow<AdhanPlaybackState> = AthanAudioEngine.playbackState

    private val _monthlySchedule = MutableStateFlow<List<DailyPrayerSchedule>>(emptyList())
    val monthlySchedule: StateFlow<List<DailyPrayerSchedule>> = _monthlySchedule.asStateFlow()

    private val _isGpsLoading = MutableStateFlow(false)
    val isGpsLoading: StateFlow<Boolean> = _isGpsLoading.asStateFlow()

    private val _locationErrorMessage = MutableStateFlow<String?>(null)
    val locationErrorMessage: StateFlow<String?> = _locationErrorMessage.asStateFlow()

    init {
        val initialSettings = settings.value
        compassManager.setLocation(initialSettings.location.latitude, initialSettings.location.longitude)
        recalculateSchedules(initialSettings, _selectedDate.value)
        val initialZoneId = try { ZoneId.of(initialSettings.location.timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        updateNextPrayerCountdown(initialSettings, ZonedDateTime.now(initialZoneId))

        viewModelScope.launch {
            settings.collect { currentSettings ->
                compassManager.setLocation(currentSettings.location.latitude, currentSettings.location.longitude)
                recalculateSchedules(currentSettings, _selectedDate.value)
                PrayerNotificationScheduler.scheduleDailyAlarms(getApplication(), currentSettings)
                PrayerAppWidgetProvider.updateAllWidgets(getApplication())
            }
        }

        // Live timer for countdown to next prayer
        viewModelScope.launch {
            while (isActive) {
                val currentSettings = settings.value
                val currentDate = _selectedDate.value
                val zoneId = try { ZoneId.of(currentSettings.location.timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
                val now = ZonedDateTime.now(zoneId)

                updateNextPrayerCountdown(currentSettings, now)
                delay(1000)
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        recalculateSchedules(settings.value, date)
    }

    fun selectToday() {
        setSelectedDate(LocalDate.now())
    }

    fun previousDay() {
        setSelectedDate(_selectedDate.value.minusDays(1))
    }

    fun nextDay() {
        setSelectedDate(_selectedDate.value.plusDays(1))
    }

    private fun recalculateSchedules(currentSettings: AppPrayerSettings, date: LocalDate) {
        val zoneId = try { ZoneId.of(currentSettings.location.timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = ZonedDateTime.now(zoneId)

        val schedule = PrayerTimesCalculator.calculateDailySchedule(
            date = date,
            latitude = currentSettings.location.latitude,
            longitude = currentSettings.location.longitude,
            zoneId = zoneId,
            method = currentSettings.calculationMethod,
            juristicMethod = currentSettings.juristicMethod,
            highLatitudeRule = currentSettings.highLatitudeRule,
            adjustments = currentSettings.adjustments,
            hijriAdjustmentDays = currentSettings.hijriAdjustmentDays,
            now = now
        )
        _dailySchedule.value = schedule

        // Also calculate monthly schedule
        viewModelScope.launch(Dispatchers.Default) {
            val yearMonth = YearMonth.from(date)
            val monthDays = (1..yearMonth.lengthOfMonth()).map { day ->
                val dayDate = yearMonth.atDay(day)
                PrayerTimesCalculator.calculateDailySchedule(
                    date = dayDate,
                    latitude = currentSettings.location.latitude,
                    longitude = currentSettings.location.longitude,
                    zoneId = zoneId,
                    method = currentSettings.calculationMethod,
                    juristicMethod = currentSettings.juristicMethod,
                    highLatitudeRule = currentSettings.highLatitudeRule,
                    adjustments = currentSettings.adjustments,
                    hijriAdjustmentDays = currentSettings.hijriAdjustmentDays,
                    now = now
                )
            }
            _monthlySchedule.value = monthDays
        }
    }

    private fun updateNextPrayerCountdown(currentSettings: AppPrayerSettings, now: ZonedDateTime) {
        val zoneId = try { ZoneId.of(currentSettings.location.timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        val today = now.toLocalDate()

        val todaySchedule = PrayerTimesCalculator.calculateDailySchedule(
            date = today,
            latitude = currentSettings.location.latitude,
            longitude = currentSettings.location.longitude,
            zoneId = zoneId,
            method = currentSettings.calculationMethod,
            juristicMethod = currentSettings.juristicMethod,
            highLatitudeRule = currentSettings.highLatitudeRule,
            adjustments = currentSettings.adjustments,
            hijriAdjustmentDays = currentSettings.hijriAdjustmentDays,
            now = now
        )

        // Find the first prayer today after now (excluding Sunrise as a prayer)
        val nextItem = todaySchedule.prayerItems.firstOrNull { it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(now) }

        if (nextItem != null) {
            val diffSeconds = Duration.between(now, nextItem.zonedDateTime).seconds
            val hours = diffSeconds / 3600
            val minutes = (diffSeconds % 3600) / 60
            val seconds = diffSeconds % 60
            val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            // Progress between previous prayer and next prayer
            val previousItem = todaySchedule.prayerItems.filter { it.type != PrayerType.SUNRISE && it.zonedDateTime.isBefore(nextItem.zonedDateTime) }.lastOrNull()
            val totalSpanSeconds = if (previousItem != null) {
                Duration.between(previousItem.zonedDateTime, nextItem.zonedDateTime).seconds.coerceAtLeast(1)
            } else {
                Duration.between(today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId), nextItem.zonedDateTime).seconds.coerceAtLeast(1)
            }
            val elapsed = totalSpanSeconds - diffSeconds
            val progress = (elapsed.toFloat() / totalSpanSeconds).coerceIn(0f, 1f)

            _nextPrayerInfo.value = NextPrayerInfo(
                prayerItem = nextItem,
                remainingFormatted = formatted,
                remainingSeconds = diffSeconds,
                progressPercent = progress,
                isNextDayFajr = false
            )
        } else {
            // Next prayer is tomorrow's Fajr
            val tomorrow = today.plusDays(1)
            val tomorrowSchedule = PrayerTimesCalculator.calculateDailySchedule(
                date = tomorrow,
                latitude = currentSettings.location.latitude,
                longitude = currentSettings.location.longitude,
                zoneId = zoneId,
                method = currentSettings.calculationMethod,
                juristicMethod = currentSettings.juristicMethod,
                highLatitudeRule = currentSettings.highLatitudeRule,
                adjustments = currentSettings.adjustments,
                hijriAdjustmentDays = currentSettings.hijriAdjustmentDays,
                now = now
            )
            val tomorrowFajr = tomorrowSchedule.prayerItems.first { it.type == PrayerType.FAJR }
            val diffSeconds = Duration.between(now, tomorrowFajr.zonedDateTime).seconds
            val hours = diffSeconds / 3600
            val minutes = (diffSeconds % 3600) / 60
            val seconds = diffSeconds % 60
            val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            _nextPrayerInfo.value = NextPrayerInfo(
                prayerItem = tomorrowFajr,
                remainingFormatted = formatted,
                remainingSeconds = diffSeconds,
                progressPercent = 0.5f,
                isNextDayFajr = true
            )
        }
    }

    fun selectCity(location: UserLocation) {
        viewModelScope.launch {
            prefs.updateLocation(location)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGpsLocation(context: Context) {
        _isGpsLoading.value = true
        _locationErrorMessage.value = null

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            // Resolve from the app's own language setting rather than
                            // Locale.getDefault() - on API < 33 that reflects the device's
                            // system-wide language, not the in-app override, so a user who set
                            // the app to English but has an Arabic-language phone would still
                            // get Arabic-script city/country names back from the geocoder.
                            val geocoderLocale = when (settings.value.language) {
                                AppLanguage.ARABIC -> Locale("ar")
                                AppLanguage.ENGLISH -> Locale.ENGLISH
                                AppLanguage.SYSTEM -> Locale.getDefault()
                            }
                            val geocoder = Geocoder(context, geocoderLocale)
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val address = addresses?.firstOrNull()
                            val cityName = address?.locality ?: address?.subAdminArea ?: "Current GPS Location"
                            val countryName = address?.countryName ?: ""
                            val timeZoneId = ZoneId.systemDefault().id

                            val newLoc = UserLocation(
                                name = cityName,
                                country = countryName,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timeZoneId = timeZoneId,
                                isGps = true
                            )
                            prefs.updateLocation(newLoc)
                        } catch (e: Exception) {
                            val newLoc = UserLocation(
                                name = "GPS Coordinates",
                                country = String.format("%.2f°, %.2f°", location.latitude, location.longitude),
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timeZoneId = ZoneId.systemDefault().id,
                                isGps = true
                            )
                            prefs.updateLocation(newLoc)
                        } finally {
                            _isGpsLoading.value = false
                        }
                    }
                } else {
                    _isGpsLoading.value = false
                    _locationErrorMessage.value = "Unable to get current GPS fix. Please ensure location services are enabled."
                }
            }
            .addOnFailureListener {
                _isGpsLoading.value = false
                _locationErrorMessage.value = "GPS request failed: ${it.localizedMessage}"
            }
    }

    fun clearLocationError() {
        _locationErrorMessage.value = null
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            prefs.updateCalculationMethod(method)
        }
    }

    fun updateJuristicMethod(method: JuristicMethod) {
        viewModelScope.launch {
            prefs.updateJuristicMethod(method)
        }
    }

    fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        viewModelScope.launch {
            prefs.updateHighLatitudeRule(rule)
        }
    }

    fun updateHijriOffset(offset: Int) {
        viewModelScope.launch {
            prefs.updateHijriOffset(offset)
        }
    }

    fun toggle24HourFormat() {
        viewModelScope.launch {
            prefs.updateIs24Hour(!settings.value.is24HourFormat)
        }
    }

    fun updateLanguage(language: com.example.data.models.AppLanguage) {
        viewModelScope.launch {
            prefs.updateLanguage(language)
        }
    }

    fun updateThemeMode(themeMode: com.example.data.models.AppThemeMode) {
        viewModelScope.launch {
            prefs.updateThemeMode(themeMode)
        }
    }

    fun updateColorPreset(preset: com.example.data.models.AppColorPreset) {
        viewModelScope.launch {
            prefs.updateColorPreset(preset)
        }
    }

    fun updateFollowSystemColors(follow: Boolean) {
        viewModelScope.launch {
            prefs.updateFollowSystemColors(follow)
        }
    }

    fun updatePrayerAdjustment(prayer: PrayerType, minutes: Int) {
        viewModelScope.launch {
            prefs.updatePrayerAdjustment(prayer, minutes)
        }
    }

    fun updatePrayerNotification(prayer: PrayerType, enabled: Boolean, soundType: NotificationSoundType, preReminder: Int) {
        viewModelScope.launch {
            prefs.updatePrayerNotification(prayer, enabled, soundType, preReminder)
        }
    }

    fun updateAudioStream(audioStream: com.example.data.models.AthanAudioStream) {
        viewModelScope.launch {
            prefs.updateAudioStream(audioStream)
        }
    }

    fun updateWakeScreenOnAlarm(wakeScreen: Boolean) {
        viewModelScope.launch {
            prefs.updateWakeScreenOnAlarm(wakeScreen)
        }
    }

    fun updateWidgetSettings(settings: com.example.data.models.WidgetCustomizationSettings) {
        viewModelScope.launch {
            prefs.updateWidgetSettings(settings)
        }
    }

    fun refreshAllWidgets() {
        PrayerAppWidgetProvider.updateAllWidgets(getApplication())
    }

    fun updateDynamicIslandSettings(enabled: Boolean, minutesBefore: Int) {
        viewModelScope.launch {
            prefs.updateDynamicIslandSettings(enabled, minutesBefore)
            PrayerNotificationScheduler.rescheduleAll(getApplication())
        }
    }

    fun playAthanPreview(prayerType: PrayerType = PrayerType.DHUHR, soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN) {
        AthanAudioEngine.playAthan(
            context = getApplication(),
            prayerType = prayerType,
            soundType = soundType,
            audioStream = settings.value.audioStream
        )
    }

    fun previewNotificationSound(soundType: NotificationSoundType, prayerType: PrayerType = PrayerType.DHUHR) {
        AthanAudioEngine.playSoundType(
            context = getApplication(),
            soundType = soundType,
            prayerType = prayerType,
            audioStream = settings.value.audioStream
        )
    }

    fun previewDynamicIsland(prayerType: PrayerType = PrayerType.ASR, minutesInFuture: Int = 15) {
        PrayerNotificationScheduler.triggerTestDynamicIsland(getApplication(), prayerType, minutesInFuture)
    }

    fun dismissDynamicIsland() {
        PrayerNotificationScheduler.dismissDynamicIsland(getApplication())
    }

    fun stopAudio() {
        AthanAudioEngine.stop()
        com.example.audio.AthanAudioService.stopAthan(getApplication())
    }

    fun testNotification(prayerType: PrayerType, soundType: NotificationSoundType) {
        PrayerNotificationScheduler.triggerTestNotification(getApplication(), prayerType, soundType)
    }

    fun testAlarmInSeconds(prayerType: PrayerType = PrayerType.DHUHR, soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN, seconds: Int = 5) {
        PrayerNotificationScheduler.triggerTestAlarmInSeconds(getApplication(), prayerType, soundType, seconds)
    }

    fun rescheduleAllAlarms() {
        PrayerNotificationScheduler.scheduleDailyAlarms(getApplication(), settings.value)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.updateOnboardingCompleted(true)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            prefs.updateOnboardingCompleted(completed)
        }
    }

    override fun onCleared() {
        super.onCleared()
        compassManager.stop()
        AthanAudioEngine.stop()
    }
}
