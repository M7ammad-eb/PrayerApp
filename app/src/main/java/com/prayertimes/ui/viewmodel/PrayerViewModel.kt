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
import com.prayertimes.data.calculator.CurrentPrayerPeriod
import com.prayertimes.data.calculator.CurrentPrayerResolver
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class CurrentPrayerInfo(
    val prayerItem: PrayerTimeItem? = null,
    val remainingSeconds: Long = 0,
    val progressPercent: Float = 0f,
    val isPrayerTimeEnded: Boolean = false
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
private data class CurrentPrayerBoundary(
    val period: CurrentPrayerPeriod,
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

    private val _currentPrayerInfo = MutableStateFlow(CurrentPrayerInfo())
    val currentPrayerInfo: StateFlow<CurrentPrayerInfo> = _currentPrayerInfo.asStateFlow()

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

    // The 1Hz loop exists only to animate the visible home-screen countdown. AlarmManager owns
    // background prayer timing, so the loop should run only when both the app and Prayer Times tab
    // are visible.
    private val _isForeground = MutableStateFlow(true)
    private val _isPrayerTabVisible = MutableStateFlow(false)

    private var cachedBoundary: CurrentPrayerBoundary? = null

    fun setForeground(foreground: Boolean) {
        _isForeground.value = foreground
        if (foreground) viewModelScope.launch { prefs.clearExpiredHijriOffset() }
    }

    fun setPrayerTabVisible(visible: Boolean) {
        _isPrayerTabVisible.value = visible
    }

    init {
        viewModelScope.launch { prefs.clearExpiredHijriOffset() }
        val initialSettings = settings.value
        compassManager.setLocation(initialSettings.location.latitude, initialSettings.location.longitude)
        recalculateSchedules(initialSettings, _selectedDate.value)
        updateCurrentPrayerCountdown(initialSettings, ZonedDateTime.now(initialSettings.zoneId()))

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

        // Live timer for the active prayer's remaining time, paused unless its UI is visible.
        val countdownActive = combine(_isForeground, _isPrayerTabVisible) { foreground, visible ->
            foreground && visible
        }.distinctUntilChanged()
        viewModelScope.launch {
            while (isActive) {
                countdownActive.first { it }
                val currentSettings = settings.value
                val now = ZonedDateTime.now(currentSettings.zoneId())
                updateCurrentPrayerCountdown(currentSettings, now)
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
        val zoneId = currentSettings.zoneId()
        val now = ZonedDateTime.now(zoneId)

        val schedule = scheduleFor(currentSettings, date, zoneId, now)
        _dailySchedule.value = schedule

    }

    private fun computeCurrentPrayerBoundary(currentSettings: AppPrayerSettings, now: ZonedDateTime): CurrentPrayerBoundary {
        val zoneId = currentSettings.zoneId()
        val today = now.toLocalDate()
        val yesterdaySchedule = scheduleFor(currentSettings, today.minusDays(1), zoneId, now)
        val todaySchedule = scheduleFor(currentSettings, today, zoneId, now)
        val tomorrowSchedule = scheduleFor(currentSettings, today.plusDays(1), zoneId, now)
        return CurrentPrayerBoundary(
            period = CurrentPrayerResolver.resolve(now, yesterdaySchedule, todaySchedule, tomorrowSchedule),
            computedForSettings = currentSettings
        )
    }

    private fun updateCurrentPrayerCountdown(currentSettings: AppPrayerSettings, now: ZonedDateTime) {
        // Only re-run the astronomical calculation when the boundary was actually crossed or the
        // inputs it depends on changed - every other tick just subtracts a Duration.
        val existing = cachedBoundary
        val boundary = if (existing != null && existing.computedForSettings == currentSettings && now.isBefore(existing.period.changesAt)) {
            existing
        } else {
            computeCurrentPrayerBoundary(currentSettings, now).also { cachedBoundary = it }
        }

        val period = boundary.period
        val diffSeconds = if (period.isPrayerTimeEnded) {
            0L
        } else {
            Duration.between(now, period.endsAt).seconds.coerceAtLeast(0)
        }
        val totalSpanSeconds = Duration.between(period.prayerItem.zonedDateTime, period.endsAt).seconds.coerceAtLeast(1)
        val elapsed = totalSpanSeconds - diffSeconds
        val progress = if (period.isPrayerTimeEnded) 1f
        else (elapsed.toFloat() / totalSpanSeconds).coerceIn(0f, 1f)

        _currentPrayerInfo.value = CurrentPrayerInfo(
            prayerItem = period.prayerItem,
            remainingSeconds = diffSeconds,
            progressPercent = progress,
            isPrayerTimeEnded = period.isPrayerTimeEnded
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
    private val explicitLocationMaxAgeMillis = 15 * 60 * 1000L
    private val explicitLocationMaxAccuracyMeters = 250f
    private val meaningfulLocationChangeMeters = 5_000f

    @SuppressLint("MissingPermission")
    fun requestGpsLocation(context: Context, forceRefresh: Boolean = true) {
        _isGpsLoading.value = true
        _locationErrorMessage.value = null
        val language = settings.value.language
        val localizedRes = com.prayertimes.util.LocalizedStrings.forLanguage(context, language.resolveIsArabic(context))

        // A button press must always persist/relocalize the selected location, but waiting for a
        // brand-new high-accuracy satellite fix can take a long time indoors. Prefer Android's
        // recent, reasonably accurate fused fix; fall back to a fresh fix only when it is stale.
        if (forceRefresh) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { cached: Location? ->
                    val ageMillis = cached?.let { System.currentTimeMillis() - it.time } ?: Long.MAX_VALUE
                    if (cached != null && ageMillis in 0..explicitLocationMaxAgeMillis && cached.accuracy <= explicitLocationMaxAccuracyMeters) {
                        viewModelScope.launch(Dispatchers.IO) {
                            resolveAndPersistGpsLocation(context, cached, localizedRes, language, forcePersist = true)
                        }
                    } else {
                        requestFreshGpsFix(context, localizedRes, language, forcePersist = true)
                    }
                }
                .addOnFailureListener {
                    requestFreshGpsFix(context, localizedRes, language, forcePersist = true)
                }
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { cached: Location? ->
                val ageMillis = cached?.let { System.currentTimeMillis() - it.time } ?: Long.MAX_VALUE
                if (cached != null && ageMillis in 0..cachedLocationMaxAgeMillis && cached.accuracy <= cachedLocationMaxAccuracyMeters) {
                    viewModelScope.launch(Dispatchers.IO) {
                        resolveAndPersistGpsLocation(context, cached, localizedRes, language)
                    }
                } else {
                    requestFreshGpsFix(context, localizedRes, language)
                }
            }
            .addOnFailureListener {
                requestFreshGpsFix(context, localizedRes, language)
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshGpsFix(
        context: Context,
        localizedRes: android.content.res.Resources,
        language: AppLanguage,
        forcePersist: Boolean = false
    ) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        resolveAndPersistGpsLocation(context, location, localizedRes, language, forcePersist)
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

    private suspend fun resolveAndPersistGpsLocation(
        context: Context,
        location: Location,
        localizedRes: android.content.res.Resources,
        language: AppLanguage,
        forcePersist: Boolean = false
    ) {
        try {
            val newLoc = resolveGpsLocation(
                context = context,
                latitude = location.latitude,
                longitude = location.longitude,
                language = language,
                localizedRes = localizedRes
            )
            if (forcePersist || isMeaningfulLocationChange(settings.value.location, newLoc)) {
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
            if (forcePersist || isMeaningfulLocationChange(settings.value.location, newLoc)) {
                prefs.updateLocation(newLoc)
            }
        } finally {
            _isGpsLoading.value = false
        }
    }

    private suspend fun resolveGpsLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        language: AppLanguage,
        localizedRes: android.content.res.Resources
    ): UserLocation {
        val isArabic = language.resolveIsArabic(context)
        val displayLocale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val nearest = PlaceRepository.nearestPlace(context, latitude, longitude)
        return if (nearest != null) {
            val placeName = if (isArabic) nearest.place.nameAr ?: nearest.place.nameEn else nearest.place.nameEn
            val countryName = Locale("", nearest.place.countryCode).getDisplayCountry(displayLocale)
            val displayName = when (nearest.relation) {
                PlaceRelation.SAME_CITY -> placeName
                PlaceRelation.NEAR_CITY -> localizedRes.getString(R.string.gps_relation_near_city, placeName)
                PlaceRelation.NEAREST_CITY -> localizedRes.getString(R.string.gps_relation_nearest_city, placeName)
            }
            UserLocation(
                name = displayName,
                country = countryName,
                latitude = latitude,
                longitude = longitude,
                timeZoneId = nearest.place.timeZoneId,
                isGps = true,
                nearestPlaceDistanceKm = nearest.distanceKm
            )
        } else {
            gpsFallbackLocation(latitude, longitude, localizedRes)
        }
    }

    private fun gpsFallbackLocation(
        latitude: Double,
        longitude: Double,
        localizedRes: android.content.res.Resources
    ) = UserLocation(
        name = localizedRes.getString(R.string.gps_coordinates_fallback),
        country = String.format(Locale.US, "%.4f°, %.4f°", latitude, longitude),
        latitude = latitude,
        longitude = longitude,
        timeZoneId = ZoneId.systemDefault().id,
        isGps = true
    )

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
            val currentLocation = settings.value.location
            if (currentLocation.isGps) {
                val localizedRes = com.prayertimes.util.LocalizedStrings.forLanguage(
                    getApplication(),
                    language.resolveIsArabic(getApplication())
                )
                val relocalized = withContext(Dispatchers.IO) {
                    runCatching {
                        resolveGpsLocation(
                            context = getApplication(),
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude,
                            language = language,
                            localizedRes = localizedRes
                        )
                    }.getOrElse {
                        gpsFallbackLocation(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            localizedRes
                        )
                    }
                }
                prefs.updateLocation(relocalized)
            }
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
            isArabic = settings.value.language.resolveIsArabic(getApplication())
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
