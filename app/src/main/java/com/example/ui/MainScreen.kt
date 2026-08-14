package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audio.AdhanPlaybackState
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.ProvideAppLocale
import com.example.ui.screens.MonthlyCalendarScreen
import com.example.ui.screens.PrayerHomeScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.PrayerViewModel
import kotlinx.coroutines.launch

enum class AppNavTab(val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PRAYER_TIMES(Icons.Default.Schedule),
    QIBLA(Icons.Default.Explore),
    MONTHLY(Icons.Default.CalendarMonth),
    SETTINGS(Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PrayerViewModel,
    onRequestLocationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(AppNavTab.PRAYER_TIMES.ordinal) }

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

        fun tabTitle(tab: AppNavTab): String = when (tab) {
            AppNavTab.PRAYER_TIMES -> strings.navPrayerTimes
            AppNavTab.QIBLA -> strings.navQibla
            AppNavTab.MONTHLY -> strings.navCalendar
            AppNavTab.SETTINGS -> strings.navSettings
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (AppNavTab.values()[selectedTab]) {
                                AppNavTab.PRAYER_TIMES -> strings.appTitle
                                AppNavTab.QIBLA -> strings.qiblaTitle
                                AppNavTab.MONTHLY -> strings.monthlyCalendarTitle
                                AppNavTab.SETTINGS -> strings.settingsTitle
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    AppNavTab.values().forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        val title = tabTitle(tab)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = title
                                )
                            },
                            label = {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
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
                            onNavigateToQibla = { selectedTab = AppNavTab.QIBLA.ordinal },
                            onNavigateToSettings = { selectedTab = AppNavTab.SETTINGS.ordinal }
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
                            onUpdatePrayerAdjustment = { prayer, mins ->
                                viewModel.updatePrayerAdjustment(prayer, mins)
                            },
                            onUpdateNotificationConfig = { prayer, enabled, sound, reminder ->
                                viewModel.updatePrayerNotification(prayer, enabled, sound, reminder)
                            },
                            onTestNotification = { prayer, sound ->
                                viewModel.testNotification(prayer, sound)
                            },
                            onPreviewSound = { sound ->
                                com.example.audio.AthanAudioEngine.playSoundType(context, sound)
                            }
                        )
                    }
                }
            }
        }
    }
}
