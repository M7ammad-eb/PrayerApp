package com.prayertimes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.prayertimes.ui.components.CityPickerDialog
import com.prayertimes.ui.locale.LocalAppStrings
import com.prayertimes.ui.locale.ProvideAppLocale
import com.prayertimes.ui.screens.MonthlyCalendarScreen
import com.prayertimes.ui.screens.PrayerHomeScreen
import com.prayertimes.ui.screens.QiblaScreen
import com.prayertimes.ui.screens.SettingsScreen
import com.prayertimes.ui.viewmodel.PrayerViewModel
import kotlinx.coroutines.launch

enum class AppNavTab(val icon: ImageVector) {
    PRAYER_TIMES(Icons.Default.Schedule),
    QIBLA(Icons.Default.Explore),
    MONTHLY(Icons.Default.CalendarMonth),
    SETTINGS(Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: PrayerViewModel,
    onRequestLocationPermission: (() -> Unit)? = null,
    onRequestNotificationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(AppNavTab.PRAYER_TIMES.ordinal) }
    // SettingsScreen keeps its own sub-screen navigation state internally, so simply re-selecting
    // an already-active Settings tab doesn't recompose it back to the main hub on its own -
    // bumping this on every tap of the Settings tab forces that reset.
    var settingsResetKey by remember { mutableIntStateOf(0) }
    // Distinguishes "Re-run Setup Wizard" tapped from Settings (skippable, in case of a mis-tap)
    // from true first-run onboarding (not skippable - initial setup should be completed).
    var onboardingReopenedFromSettings by remember { mutableStateOf(false) }
    var showCityPickerFromHeader by remember { mutableStateOf(false) }

    val settings by viewModel.settings.collectAsState()
    val isGpsLoading by viewModel.isGpsLoading.collectAsState()
    val locationErrorMessage by viewModel.locationErrorMessage.collectAsState()

    val prayerTabVisible = settings.onboardingCompleted &&
        selectedTab == AppNavTab.PRAYER_TIMES.ordinal
    DisposableEffect(prayerTabVisible) {
        viewModel.setPrayerTabVisible(prayerTabVisible)
        onDispose {
            if (prayerTabVisible) viewModel.setPrayerTabVisible(false)
        }
    }

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.requestGpsLocation(context)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission was denied. You can select your city manually.")
            }
        }
    }

    // Fallback launcher for the contextual onRequestNotificationPermission call sites below (used
    // only if MainActivity doesn't supply its own callback, e.g. in a preview). There used to also
    // be an unconditional LaunchedEffect(Unit) here firing this same POST_NOTIFICATIONS request on
    // every cold start regardless of context - duplicating, and racing, MainActivity's own
    // onCreate()-time request through a second independent launcher. Permission requests now only
    // happen contextually: from the "notifications disabled" banner in PrayerHomeScreen, or when
    // the user actually enables a prayer notification in SettingsScreen.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Notification permission is required for Athan & prayer alerts.")
            }
        }
    }

    LaunchedEffect(locationErrorMessage) {
        locationErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLocationError()
        }
    }

    ProvideAppLocale(appLanguage = settings.language) {
        val strings = LocalAppStrings.current

        if (!settings.onboardingCompleted) {
            com.prayertimes.ui.screens.OnboardingScreen(
                settings = settings,
                isGpsLoading = isGpsLoading,
                onUpdateLanguage = { viewModel.updateLanguage(it) },
                onSelectCity = { viewModel.selectCity(it) },
                onRequestGps = {
                    if (onRequestLocationPermission != null) {
                        onRequestLocationPermission()
                    } else {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            viewModel.requestGpsLocation(context)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    }
                },
                onUpdateCalculationMethod = { viewModel.updateCalculationMethod(it) },
                onUpdateNotificationConfig = { prayer, enabled, sound, reminder ->
                    viewModel.updatePrayerNotification(prayer, enabled, sound, reminder)
                },
                onPreviewSound = { sound, prayer ->
                    viewModel.previewNotificationSound(sound, prayer)
                },
                onUpdateLiveCountdownSettings = { enabled, minutes ->
                    viewModel.updateLiveCountdownSettings(enabled, minutes)
                },
                onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                onUpdateColorPreset = { viewModel.updateColorPreset(it) },
                onUpdateFollowSystemColors = { viewModel.updateFollowSystemColors(it) },
                onCompleteOnboarding = {
                    viewModel.completeOnboarding()
                    viewModel.rescheduleAllAlarms()
                    onboardingReopenedFromSettings = false
                },
                onSkip = if (onboardingReopenedFromSettings) {
                    {
                        viewModel.completeOnboarding()
                        viewModel.rescheduleAllAlarms()
                        onboardingReopenedFromSettings = false
                    }
                } else null
            )
        } else {
            if (showCityPickerFromHeader) {
                CityPickerDialog(
                    currentLocation = settings.location,
                    onSelectCity = {
                        viewModel.selectCity(it)
                        showCityPickerFromHeader = false
                    },
                    onDismiss = { showCityPickerFromHeader = false }
                )
            }

            fun tabTitle(tab: AppNavTab): String = when (tab) {
                AppNavTab.PRAYER_TIMES -> strings.navPrayerTimes
                AppNavTab.QIBLA -> strings.navQibla
                AppNavTab.MONTHLY -> strings.navCalendar
                AppNavTab.SETTINGS -> strings.navSettings
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    // Expressive Floating Header Toolbar
                    ExpressiveTopHeader(
                        currentTab = AppNavTab.values()[selectedTab],
                        settings = settings,
                        strings = strings,
                        onLocationClick = { showCityPickerFromHeader = true },
                        onUpdateHijriAdjustment = { viewModel.updateHijriOffset(it) }
                    )
                },
                bottomBar = {
                    // Expressive Floating Bottom Navigation Bar
                    ExpressiveFloatingBottomBar(
                        selectedTab = selectedTab,
                        onSelectTab = { tab ->
                            if (tab == AppNavTab.SETTINGS.ordinal) settingsResetKey++
                            selectedTab = tab
                        },
                        tabTitle = { tabTitle(it) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (AppNavTab.values()[selectedTab]) {
                        AppNavTab.PRAYER_TIMES -> {
                            val selectedDate by viewModel.selectedDate.collectAsState()
                            val dailySchedule by viewModel.dailySchedule.collectAsState()
                            val currentPrayerInfo by viewModel.currentPrayerInfo.collectAsState()
                            PrayerHomeScreen(
                                dailySchedule = dailySchedule,
                                currentPrayerInfo = currentPrayerInfo,
                                settings = settings,
                                selectedDate = selectedDate,
                                onPreviousDay = { viewModel.previousDay() },
                                onNextDay = { viewModel.nextDay() },
                                onSelectToday = { viewModel.selectToday() },
                                onDatePicked = { viewModel.setSelectedDate(it) },
                                onPreviewSound = { sound, prayer -> viewModel.previewNotificationSound(sound, prayer) },
                                onUpdateNotificationConfig = { prayer, enabled, sound, reminder ->
                                    viewModel.updatePrayerNotification(prayer, enabled, sound, reminder)
                                },
                                onRequestNotificationPermission = {
                                    if (onRequestNotificationPermission != null) {
                                        onRequestNotificationPermission()
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                        AppNavTab.QIBLA -> {
                            val compassState by viewModel.compassState.collectAsState()
                            QiblaScreen(
                                compassSensorManager = viewModel.compassManager,
                                compassState = compassState,
                                location = settings.location
                            )
                        }
                        AppNavTab.MONTHLY -> {
                            val selectedDate by viewModel.selectedDate.collectAsState()
                            MonthlyCalendarScreen(
                                selectedDate = selectedDate,
                                hijriAdjustmentDays = settings.hijriAdjustmentDays,
                                onViewPrayerTimes = { date ->
                                    viewModel.setSelectedDate(date)
                                    selectedTab = AppNavTab.PRAYER_TIMES.ordinal
                                }
                            )
                        }
                        AppNavTab.SETTINGS -> {
                            SettingsScreen(
                                resetKey = settingsResetKey,
                                settings = settings,
                                isGpsLoading = isGpsLoading,
                                onSelectCity = { viewModel.selectCity(it) },
                                onRequestGps = {
                                    if (onRequestLocationPermission != null) {
                                        onRequestLocationPermission()
                                    } else {
                                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (fine || coarse) {
                                            viewModel.requestGpsLocation(context)
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    }
                                },
                                onUpdateCalculationMethod = { viewModel.updateCalculationMethod(it) },
                                onUpdateJuristicMethod = { viewModel.updateJuristicMethod(it) },
                                onUpdateHighLatitudeRule = { viewModel.updateHighLatitudeRule(it) },
                                onToggle24Hour = { viewModel.toggle24HourFormat() },
                                onUpdateLanguage = { viewModel.updateLanguage(it) },
                                onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                                onUpdateColorPreset = { viewModel.updateColorPreset(it) },
                                onUpdateFollowSystemColors = { viewModel.updateFollowSystemColors(it) },
                                onUpdateWidgetSettings = { viewModel.updateWidgetSettings(it) },
                                onUpdatePrayerAdjustment = { prayer, mins ->
                                    viewModel.updatePrayerAdjustment(prayer, mins)
                                },
                                onUpdateNotificationConfig = { prayer, enabled, sound, reminder ->
                                    viewModel.updatePrayerNotification(prayer, enabled, sound, reminder)
                                },
                                onTestNotification = { prayer, sound ->
                                    viewModel.testNotification(prayer, sound)
                                },
                                onTestAlarmInSeconds = { prayer, sound, seconds ->
                                    viewModel.testAlarmInSeconds(prayer, sound, seconds)
                                },
                                onRescheduleAlarms = {
                                    viewModel.rescheduleAllAlarms()
                                },
                                onRequestNotificationPermission = {
                                    if (onRequestNotificationPermission != null) {
                                        onRequestNotificationPermission()
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onPreviewSound = { sound, prayer ->
                                    viewModel.previewNotificationSound(sound, prayer)
                                },
                                onUpdateAudioStream = { stream ->
                                    viewModel.updateAudioStream(stream)
                                },
                                onUpdateWakeScreen = { wake ->
                                    viewModel.updateWakeScreenOnAlarm(wake)
                                },
                                onUpdateLiveCountdownSettings = { enabled, minutes ->
                                    viewModel.updateLiveCountdownSettings(enabled, minutes)
                                },
                                onTestLiveCountdown = {
                                    viewModel.testLiveCountdown()
                                },
                                onPreviewFullScreenAlarm = { prayer ->
                                    val intent = com.prayertimes.ui.alarm.PrayerAlarmActivity.createIntent(
                                        context = context,
                                        prayerType = prayer,
                                        prayerTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern(if (settings.is24HourFormat) "HH:mm" else "h:mm a")),
                                        locationName = com.prayertimes.data.cities.CityDatabase.localizedName(context.resources, settings.location).ifEmpty { strings.currentLocationFallback },
                                        soundType = settings.prayerConfigs[prayer]?.soundType ?: com.prayertimes.data.models.NotificationSoundType.FULL_ATHAN
                                    )
                                    context.startActivity(intent)
                                },
                                onResetOnboarding = {
                                    onboardingReopenedFromSettings = true
                                    viewModel.setOnboardingCompleted(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// ============================================================================
// MATERIAL 3 EXPRESSIVE FLOATING HEADER
// ============================================================================
@Composable
private fun ExpressiveTopHeader(
    currentTab: AppNavTab,
    settings: com.prayertimes.data.preferences.AppPrayerSettings,
    strings: com.prayertimes.ui.locale.AppStrings,
    onLocationClick: () -> Unit,
    onUpdateHijriAdjustment: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentTab == AppNavTab.PRAYER_TIMES) {
                // Expressive Interactive Location Pill - location only, the brand name is a
                // separate sibling below so SpaceBetween places it on the opposite side.
                Surface(
                    onClick = onLocationClick,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = run {
                                val headerRes = androidx.compose.ui.platform.LocalContext.current.resources
                                val locName = com.prayertimes.data.cities.CityDatabase.localizedName(headerRes, settings.location)
                                val locCountry = com.prayertimes.data.cities.CityDatabase.localizedCountry(headerRes, settings.location)
                                "$locName${if (locCountry.isNotEmpty() && !locCountry.contains("°")) ", $locCountry" else ""}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = strings.appBrandName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = when (currentTab) {
                        AppNavTab.PRAYER_TIMES -> strings.appTitle
                        AppNavTab.QIBLA -> strings.qiblaTitle
                        AppNavTab.MONTHLY -> strings.monthlyCalendarTitle
                        AppNavTab.SETTINGS -> strings.settingsTitle
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 7.dp)
                )
                if (currentTab == AppNavTab.MONTHLY) {
                    CompactHijriAdjustmentControl(
                        adjustmentDays = settings.hijriAdjustmentDays,
                        onUpdate = onUpdateHijriAdjustment
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactHijriAdjustmentControl(
    adjustmentDays: Int,
    onUpdate: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = { onUpdate(adjustmentDays - 1) },
            enabled = adjustmentDays > -2,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Text("−", fontWeight = FontWeight.Bold) }
        }
        Text(
            text = if (adjustmentDays > 0) "+$adjustmentDays" else adjustmentDays.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            onClick = { onUpdate(adjustmentDays + 1) },
            enabled = adjustmentDays < 2,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold) }
        }
    }
}

// ============================================================================
// MATERIAL 3 EXPRESSIVE FLOATING BOTTOM NAVIGATION BAR
// ============================================================================
@Composable
private fun ExpressiveFloatingBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    tabTitle: (AppNavTab) -> String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppNavTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    ExpressiveNavTabItem(
                        tab = tab,
                        title = tabTitle(tab),
                        isSelected = isSelected,
                        onClick = { onSelectTab(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveNavTabItem(
    tab: AppNavTab,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "TabIndicator"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabIcon"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabText"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(indicatorColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            maxLines = 1
        )
    }
}
