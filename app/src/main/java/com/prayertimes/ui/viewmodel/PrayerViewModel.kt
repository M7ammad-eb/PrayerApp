package com.prayertimes.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prayertimes.R
import com.prayertimes.audio.AthanAudioEngine
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.calculator.PrayerSchedulePrewarmer
import com.prayertimes.data.calendar.HijriCalendar
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.DailyPrayerSchedule
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.JuristicMethod
import com.prayertimes.data.models.NotificationPrayerConfig
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerTimeAdjustments
import com.prayertimes.data.models.PrayerTimeItem
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.UserLocation
import com.prayertimes.data.models.WidgetCustomizationSettings
import com.prayertimes.data.places.PlaceRelation
import com.prayertimes.data.places.PlaceRepository
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.data.qibla.CompassSensorManager
import com.prayertimes.data.qibla.CompassState
import com.prayertimes.notifications.PrayerNotificationScheduler
import com.prayertimes.widget.glance.PrayerGlanceWidget
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class NextPrayerInfo(
    val prayerItem: PrayerTimeItem? = null,
    val remainingFormatted: String = "",
    val remainingSeconds: Long = 0,
    val progressPercent: Float = 0f,
    val isNextDayFajr: Boolean = false
)

// Narrow projections of AppPrayerSettings, one per side effect - collecting these instead of the
// whole settings object (with distinctUntilChanged()) means a cosmetic-only change (theme,
// language, widget style) can't trigger a full prayer recalculation or rebuild a week of
// AlarmManager entries, since the projection for those side effects doesn't change.
private data class CalculationInputs(
    val location: UserLocation,
    val calculationMethod: CalculationMethod,
    val juristicMethod: JuristicMethod,
    val highLatitudeRule: HighLatitudeRule,
    val hijriAdjustmentDays: Int,
    val adjustments: PrayerTimeAdjustments
)

private data class NotificationInputs(
    val calc: CalculationInputs,
    val is24HourFormat: Boolean,
    val prayerConfigs: Map<PrayerType, NotificationPrayerConfig>,
    val liveCountdownEnabled: Boolean,
    val liveCountdownMinutesBefore: Int
)

private data class WidgetInputs(
    val calc: CalculationInputs,
    val is24HourFormat: Boolean,
    val language: AppLanguage,
    val themeMode: AppThemeMode,
    val colorPreset: AppColorPreset,
    val followSystemColors: Boolean,
    val widgetSettings: WidgetCustomizationSettings
)

private fun AppPrayerSettings.calculationInputs() = CalculationInputs(
    location, calculationMethod, juristicMethod, highLatitudeRule, hijriAdjustmentDays, adjustments
)

private fun AppPrayerSettings.notificationInputs() = NotificationInputs(
    calculationInputs(), is24HourFormat, prayerConfigs, liveCountdownEnabled, liveCountdownMinutesBefore
)

private fun AppPrayerSettings.widgetInputs() = WidgetInputs(
    calculationInputs(), is24HourFormat, language, themeMode, colorPreset, followSystemColors, widgetSettings
)

// Cached next-prayer boundary so the 1Hz countdown tick only subtracts a Duration instead of
// re-running the full astronomical calculation every second - recomputed only when the boundary
// is actually crossed or the inputs it depends on change.
private data class NextPrayerBoundary(
    val nextItem: PrayerTimeItem,
    val previousZoned: ZonedDateTime?, // null for the "next prayer is tomorrow's Fajr" case
    val totalSpanSeconds: Long,
    val isNextDayFajr: Boolean,
    val computedForSettings: AppPrayerSettings
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
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

    // The stored IANA zone id can be invalid or missing for a hand-entered/legacy location, so every
    // schedule calculation falls back to the device zone rather than crashing.
    private fun AppPrayerSettings.zoneId(): ZoneId =
        try { ZoneId.of(location.timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }

    private fun scheduleFor(currentSettings: AppPrayerSettings, date: LocalDate, zoneId: ZoneId, now: ZonedDateTime): DailyPrayerSchedule =
        PrayerTimesCalculator.calculateDailySchedule(
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

    private val _isGpsLoading = MutableStateFlow(false)
    val isGpsLoading: StateFlow<Boolean> = _isGpsLoading.asStateFlow()

    private val _locationErrorMessage = MutableStateFlow<String?>(null)
    val locationErrorMessage: StateFlow<String?> = _locationErrorMessage.asStateFlow()

    // Drives whether the 1Hz countdown loop below actually ticks - the OS-scheduled AlarmManager
    // alarm is what fires the Athan/notification in the background, this loop only exists to
    // animate the visible countdown, so there's no reason to keep it running while the app isn't
    // in the foreground. Defaults true so behavior is unchanged until the Activity calls
    // setForeground() from onPause/onResume.
    private val _isForeground = MutableStateFlow(true)

    private var cachedBoundary: NextPrayerBoundary? = null

    fun setForeground(foreground: Boolean) {
        _isForeground.value = foreground
        if (foreground) viewModelScope.launch { prefs.clearExpiredHijriOffset() }
    }

    init {
        viewModelScope.launch { prefs.clearExpiredHijriOffset() }
        val initialSettings = settings.value
        compassManager.setLocation(initialSettings.location.latitude, initialSettings.location.longitude)
        recalculateSchedules(initialSettings, _selectedDate.value)
        updateNextPrayerCountdown(initialSettings, ZonedDateTime.now(initialSettings.zoneId()))

        // Split by concern (see the *Inputs projections above): a settings emission only triggers
        // the side effects whose actual inputs changed, instead of every emission rebuilding the
        // schedule, every AlarmManager entry, and the widget regardless of what changed.
        var lastCalculationInputs = initialSettings.calculationInputs()
        viewModelScope.launch(Dispatchers.Default) {
            settings.map { it.calculationInputs() }.distinctUntilChanged().collect { calc ->
                if (calc == lastCalculationInputs) return@collect
                lastCalculationInputs = calc
                compassManager.setLocation(calc.location.latitude, calc.location.longitude)
                val currentSettings = settings.value
                PrayerTimesCalculator.clearCache()
                PrayerSchedulePrewarmer.prewarm(currentSettings)
                recalculateSchedules(currentSettings, _selectedDate.value)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            settings.map { it.notificationInputs() }.distinctUntilChanged().collect {
                PrayerNotificationScheduler.scheduleDailyAlarms(getApplication(), settings.value)
            }
        }
        viewModelScope.launch {
            // Widget settings can emit rapidly (especially opacity). One projection owns all
            // automatic refreshes and coalesces a burst into a single RemoteViews rebuild.
            settings.map { it.widgetInputs() }.distinctUntilChanged().debounce(300).collect {
                PrayerGlanceWidget.refreshAll(getApplication())
            }
        }

        // Live timer for countdown to next prayer, paused while backgrounded.
        viewModelScope.launch {
            while (isActive) {
                if (_isForeground.value) {
                    val currentSettings = settings.value
                    val now = ZonedDateTime.now(currentSettings.zoneId())
                    updateNextPrayerCountdown(currentSettings, now)
                    delay(1000)
                } else {
                    _isForeground.first { it }
                }
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
        val zoneId = currentSettings.zoneId()
        val now = ZonedDateTime.now(zoneId)

        val schedule = scheduleFor(currentSettings, date, zoneId, now)
        _dailySchedule.value = schedule

    }

    private fun computeNextPrayerBoundary(currentSettings: AppPrayerSettings, now: ZonedDateTime): NextPrayerBoundary {
        val zoneId = currentSettings.zoneId()
        val today = now.toLocalDate()
        val todaySchedule = scheduleFor(currentSettings, today, zoneId, now)

        // Find the first prayer today after now (excluding Sunrise as a prayer)
        val nextItem = todaySchedule.prayerItems.firstOrNull { it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(now) }

        return if (nextItem != null) {
            val previousItem = todaySchedule.prayerItems.filter { it.type != PrayerType.SUNRISE && it.zonedDateTime.isBefore(nextItem.zonedDateTime) }.lastOrNull()
            val previousZoned = previousItem?.zonedDateTime
                ?: today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId)
            val totalSpanSeconds = Duration.between(previousZoned, nextItem.zonedDateTime).seconds.coerceAtLeast(1)
            NextPrayerBoundary(
                nextItem = nextItem,
                previousZoned = previousZoned,
                totalSpanSeconds = totalSpanSeconds,
                isNextDayFajr = false,
                computedForSettings = currentSettings
            )
        } else {
            // Next prayer is tomorrow's Fajr
            val tomorrow = today.plusDays(1)
            val tomorrowSchedule = scheduleFor(currentSettings, tomorrow, zoneId, now)
            val tomorrowFajr = tomorrowSchedule.prayerItems.first { it.type == PrayerType.FAJR }
            NextPrayerBoundary(
                nextItem = tomorrowFajr,
                previousZoned = null,
                totalSpanSeconds = 0L,
                isNextDayFajr = true,
                computedForSettings = currentSettings
            )
        }
    }

    private fun updateNextPrayerCountdown(currentSettings: AppPrayerSettings, now: ZonedDateTime) {
        // Only re-run the astronomical calculation when the boundary was actually crossed or the
        // inputs it depends on changed - every other tick just subtracts a Duration.
        val existing = cachedBoundary
        val boundary = if (existing != null && existing.computedForSettings == currentSettings && now.isBefore(existing.nextItem.zonedDateTime)) {
            existing
        } else {
            computeNextPrayerBoundary(currentSettings, now).also { cachedBoundary = it }
        }

        val diffSeconds = Duration.between(now, boundary.nextItem.zonedDateTime).seconds.coerceAtLeast(0)
        val hours = diffSeconds / 3600
        val minutes = (diffSeconds % 3600) / 60
        val seconds = diffSeconds % 60
        val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val progress = if (boundary.isNextDayFajr || boundary.previousZoned == null) {
            0.5f
        } else {
            val elapsed = boundary.totalSpanSeconds - diffSeconds
            (elapsed.toFloat() / boundary.totalSpanSeconds).coerceIn(0f, 1f)
        }

        _nextPrayerInfo.value = NextPrayerInfo(
            prayerItem = boundary.nextItem,
            remainingFormatted = formatted,
            remainingSeconds = diffSeconds,
            progressPercent = progress,
            isNextDayFajr = boundary.isNextDayFajr
        )
    }

    fun selectCity(location: UserLocation) {
        viewModelScope.launch {
            prefs.updateLocation(location)
        }
    }

    // A recent cached fix is enough to detect meaningful travel. Room-scale GPS drift is filtered
    // separately before persistence so it cannot invalidate prayer schedules and alarms.
    private val cachedLocationMaxAgeMillis = 2 * 60 * 60 * 1000L
    private val cachedLocationMaxAccuracyMeters = 500f
    private val meaningfulLocationChangeMeters = 5_000f

    @SuppressLint("MissingPermission")
    fun requestGpsLocation(context: Context) {
        _isGpsLoading.value = true
        _locationErrorMessage.value = null
        val localizedRes = com.prayertimes.util.LocalizedStrings.forLanguage(context, settings.value.language.resolveIsArabic())

        fusedLocationClient.lastLocation
            .addOnSuccessListener { cached: Location? ->
                val ageMillis = cached?.let { System.currentTimeMillis() - it.time } ?: Long.MAX_VALUE
                if (cached != null && ageMillis in 0..cachedLocationMaxAgeMillis && cached.accuracy <= cachedLocationMaxAccuracyMeters) {
                    viewModelScope.launch(Dispatchers.IO) {
                        resolveAndPersistGpsLocation(context, cached, localizedRes)
                    }
                } else {
                    requestFreshGpsFix(context, localizedRes)
                }
            }
            .addOnFailureListener {
                requestFreshGpsFix(context, localizedRes)
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshGpsFix(context: Context, localizedRes: android.content.res.Resources) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        resolveAndPersistGpsLocation(context, location, localizedRes)
                    }
                } else {
                    _isGpsLoading.value = false
                    _locationErrorMessage.value = localizedRes.getString(R.string.gps_fix_failed_error)
                }
            }
            .addOnFailureListener {
                _isGpsLoading.value = false
                _locationErrorMessage.value = localizedRes.getString(R.string.gps_request_failed_error, it.localizedMessage)
            }
    }

    private suspend fun resolveAndPersistGpsLocation(context: Context, location: Location, localizedRes: android.content.res.Resources) {
        try {
            // Resolve from the app's own language setting rather than
            // Locale.getDefault() - on API < 33 that reflects the device's
            // system-wide language, not the in-app override.
            val isArabic = when (settings.value.language) {
                AppLanguage.ARABIC -> true
                AppLanguage.ENGLISH -> false
                AppLanguage.SYSTEM -> Locale.getDefault().language == "ar"
            }
            val displayLocale = if (isArabic) Locale("ar") else Locale.ENGLISH

            val nearest = PlaceRepository.nearestPlace(context, location.latitude, location.longitude)
            val newLoc = if (nearest != null) {
                val placeName = if (isArabic) nearest.place.nameAr ?: nearest.place.nameEn else nearest.place.nameEn
                val countryName = Locale("", nearest.place.countryCode).getDisplayCountry(displayLocale)
                // Transparently flags how approximate the match is, per the offline
                // dataset's distance to the actual GPS fix, rather than silently
                // presenting a possibly-distant "nearest" place as if it were exact.
                val displayName = when (nearest.relation) {
                    PlaceRelation.SAME_CITY -> placeName
                    PlaceRelation.NEAR_CITY -> localizedRes.getString(R.string.gps_relation_near_city, placeName)
                    PlaceRelation.NEAREST_CITY -> localizedRes.getString(R.string.gps_relation_nearest_city, placeName)
                }
                UserLocation(
                    name = displayName,
                    country = countryName,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timeZoneId = nearest.place.timeZoneId,
                    isGps = true,
                    nearestPlaceDistanceKm = nearest.distanceKm
                )
            } else {
                UserLocation(
                    name = localizedRes.getString(R.string.gps_coordinates_fallback),
                    country = String.format(Locale.US, "%.4f°, %.4f°", location.latitude, location.longitude),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timeZoneId = ZoneId.systemDefault().id,
                    isGps = true
                )
            }
            if (isMeaningfulLocationChange(settings.value.location, newLoc)) {
                prefs.updateLocation(newLoc)
            }
        } catch (e: Exception) {
            val newLoc = UserLocation(
                name = localizedRes.getString(R.string.gps_coordinates_fallback),
                country = String.format(Locale.US, "%.4f°, %.4f°", location.latitude, location.longitude),
                latitude = location.latitude,
                longitude = location.longitude,
                timeZoneId = ZoneId.systemDefault().id,
                isGps = true
            )
            if (isMeaningfulLocationChange(settings.value.location, newLoc)) {
                prefs.updateLocation(newLoc)
            }
        } finally {
            _isGpsLoading.value = false
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
        viewModelScope.launch(Dispatchers.Default) {
            val anchor = HijriCalendar.monthKeyFor(LocalDate.now())
            // Fill the newly adjusted pages before DataStore publishes the new setting and causes
            // the visible calendar to recompose.
            HijriCalendar.prewarm(
                anchorDate = LocalDate.now(),
                adjustmentDays = offset,
                previousMonths = 1,
                nextMonths = 1
            )
            prefs.updateHijriOffset(offset, anchor)
        }
    }

    private fun isMeaningfulLocationChange(current: UserLocation, candidate: UserLocation): Boolean {
        if (!current.isGps || current.timeZoneId != candidate.timeZoneId) return true
        val distanceMeters = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            candidate.latitude,
            candidate.longitude,
            distanceMeters
        )
        return distanceMeters[0] >= meaningfulLocationChangeMeters
    }

    fun toggle24HourFormat() {
        viewModelScope.launch {
            prefs.updateIs24Hour(!settings.value.is24HourFormat)
        }
    }

    fun updateLanguage(language: com.prayertimes.data.models.AppLanguage) {
        viewModelScope.launch {
            prefs.updateLanguage(language)
        }
    }

    fun updateThemeMode(themeMode: com.prayertimes.data.models.AppThemeMode) {
        viewModelScope.launch {
            prefs.updateThemeMode(themeMode)
        }
    }

    fun updateColorPreset(preset: com.prayertimes.data.models.AppColorPreset) {
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

    fun updateAudioStream(audioStream: com.prayertimes.data.models.AthanAudioStream) {
        viewModelScope.launch {
            prefs.updateAudioStream(audioStream)
        }
    }

    fun updateWakeScreenOnAlarm(wakeScreen: Boolean) {
        viewModelScope.launch {
            prefs.updateWakeScreenOnAlarm(wakeScreen)
        }
    }

    fun updateLiveCountdownSettings(enabled: Boolean, minutesBefore: Int) {
        viewModelScope.launch {
            prefs.updateLiveCountdownSettings(enabled, minutesBefore)
            if (!enabled) {
                com.prayertimes.notifications.PrayerLiveCountdownManager.dismiss(getApplication())
            }
        }
    }

    private var widgetSettingsUpdateJob: kotlinx.coroutines.Job? = null

    fun updateWidgetSettings(settings: com.prayertimes.data.models.WidgetCustomizationSettings) {
        widgetSettingsUpdateJob?.cancel()
        widgetSettingsUpdateJob = viewModelScope.launch {
            prefs.updateWidgetSettings(settings)
            // The debounced WidgetInputs collector above refreshes after DataStore commits.
        }
    }

    fun previewNotificationSound(soundType: NotificationSoundType, prayerType: PrayerType = PrayerType.DHUHR) {
        AthanAudioEngine.playSoundType(
            context = getApplication(),
            soundType = soundType,
            prayerType = prayerType,
            audioStream = settings.value.audioStream,
            isArabic = settings.value.language.resolveIsArabic()
        )
    }

    fun testNotification(prayerType: PrayerType, soundType: NotificationSoundType) {
        PrayerNotificationScheduler.triggerTestNotification(getApplication(), prayerType, soundType)
    }

    fun testAlarmInSeconds(prayerType: PrayerType = PrayerType.DHUHR, soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN, seconds: Int = 5) {
        PrayerNotificationScheduler.triggerTestAlarmInSeconds(getApplication(), prayerType, soundType, seconds)
    }

    fun testLiveCountdown() {
        PrayerNotificationScheduler.triggerTestLiveCountdown(getApplication())
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
