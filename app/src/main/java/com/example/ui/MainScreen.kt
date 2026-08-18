package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CityPickerDialog
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.ProvideAppLocale
import com.example.ui.screens.MonthlyCalendarScreen
import com.example.ui.screens.PrayerHomeScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.PrayerViewModel
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
    var showCityPickerFromHeader by remember { mutableStateOf(false) }

    val settings by viewModel.settings.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailySchedule by viewModel.dailySchedule.collectAsState()
    val nextPrayerInfo by viewModel.nextPrayerInfo.collectAsState()
    val compassState by viewModel.compassState.collectAsState()
    val audioPlaybackState by viewModel.audioPlaybackState.collectAsState()
    val monthlySchedule by viewModel.monthlySchedule.collectAsState()
    val isGpsLoading by viewModel.isGpsLoading.collectAsState()
    val locationErrorMessage by viewModel.locationErrorMessage.collectAsState()

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

    // Notification Permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Notification permission is required for Athan & prayer alerts.")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            com.example.ui.screens.OnboardingScreen(
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
                onUpdateNotificationConfig = { prayer, enabled, sound, reminder ->
                    viewModel.updatePrayerNotification(prayer, enabled, sound, reminder)
                },
                onPreviewSound = { sound, prayer ->
                    viewModel.previewNotificationSound(sound, prayer)
                },
                onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                onUpdateColorPreset = { viewModel.updateColorPreset(it) },
                onUpdateFollowSystemColors = { viewModel.updateFollowSystemColors(it) },
                onCompleteOnboarding = {
                    viewModel.completeOnboarding()
                    viewModel.rescheduleAllAlarms()
                }
            )
        } else {
            if (showCityPickerFromHeader) {
                CityPickerDialog(
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
                        onLocationClick = { showCityPickerFromHeader = true }
                    )
                },
                bottomBar = {
                    // Expressive Floating Bottom Navigation Bar
                    ExpressiveFloatingBottomBar(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
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
                            PrayerHomeScreen(
                                dailySchedule = dailySchedule,
                                nextPrayerInfo = nextPrayerInfo,
                                settings = settings,
                                selectedDate = selectedDate,
                                audioPlaybackState = audioPlaybackState,
                                onPreviousDay = { viewModel.previousDay() },
                                onNextDay = { viewModel.nextDay() },
                                onSelectToday = { viewModel.selectToday() },
                                onDatePicked = { viewModel.setSelectedDate(it) },
                                onPlayPrayerAthan = { viewModel.playAthanPreview(it) },
                                onStopAudio = { viewModel.stopAudio() },
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
                            QiblaScreen(
                                compassSensorManager = viewModel.compassManager,
                                compassState = compassState,
                                location = settings.location
                            )
                        }
                        AppNavTab.MONTHLY -> {
                            MonthlyCalendarScreen(
                                monthlySchedule = monthlySchedule,
                                selectedDate = selectedDate,
                                location = settings.location,
                                is24HourFormat = settings.is24HourFormat,
                                onPreviousMonth = { viewModel.setSelectedDate(selectedDate.minusMonths(1)) },
                                onNextMonth = { viewModel.setSelectedDate(selectedDate.plusMonths(1)) }
                            )
                        }
                        AppNavTab.SETTINGS -> {
                            SettingsScreen(
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
                                onUpdateHijriOffset = { viewModel.updateHijriOffset(it) },
                                onToggle24Hour = { viewModel.toggle24HourFormat() },
                                onUpdateLanguage = { viewModel.updateLanguage(it) },
                                onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                                onUpdateColorPreset = { viewModel.updateColorPreset(it) },
                                onUpdateFollowSystemColors = { viewModel.updateFollowSystemColors(it) },
                                onUpdateWidgetSettings = { viewModel.updateWidgetSettings(it) },
                                onRefreshAllWidgets = { viewModel.refreshAllWidgets() },
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
                                onUpdateDynamicIslandSettings = { enabled, minutes ->
                                    viewModel.updateDynamicIslandSettings(enabled, minutes)
                                },
                                onPreviewDynamicIsland = {
                                    viewModel.previewDynamicIsland()
                                },
                                onDismissDynamicIsland = {
                                    viewModel.dismissDynamicIsland()
                                },
                                onUpdateAudioStream = { stream ->
                                    viewModel.updateAudioStream(stream)
                                },
                                onUpdateWakeScreen = { wake ->
                                    viewModel.updateWakeScreenOnAlarm(wake)
                                },
                                onPreviewFullScreenAlarm = { prayer ->
                                    val intent = com.example.ui.alarm.PrayerAlarmActivity.createIntent(
                                        context = context,
                                        prayerType = prayer,
                                        prayerTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern(if (settings.is24HourFormat) "HH:mm" else "h:mm a")),
                                        locationName = com.example.data.cities.CityDatabase.localizedName(settings.location, strings.isArabic).ifEmpty { "Current Location" },
                                        soundType = settings.prayerConfigs[prayer]?.soundType ?: com.example.data.models.NotificationSoundType.FULL_ATHAN
                                    )
                                    context.startActivity(intent)
                                },
                                onResetOnboarding = {
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
    settings: com.example.data.preferences.AppPrayerSettings,
    strings: com.example.ui.locale.AppStrings,
    onLocationClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentTab == AppNavTab.PRAYER_TIMES) {
                // Expressive Interactive Location Pill
                Surface(
                    onClick = onLocationClick,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ),
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

                        Column {
                            Text(
                                text = strings.appBrandName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = run {
                                    val locName = com.example.data.cities.CityDatabase.localizedName(settings.location, strings.isArabic)
                                    val locCountry = com.example.data.cities.CityDatabase.localizedCountry(settings.location, strings.isArabic)
                                    "$locName${if (locCountry.isNotEmpty() && !locCountry.contains("°")) ", $locCountry" else ""}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = when (currentTab) {
                        AppNavTab.PRAYER_TIMES -> strings.appTitle
                        AppNavTab.QIBLA -> strings.qiblaTitle
                        AppNavTab.MONTHLY -> strings.monthlyCalendarTitle
                        AppNavTab.SETTINGS -> strings.settingsTitle
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
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
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "TabIndicator"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabIcon"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabText"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
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
