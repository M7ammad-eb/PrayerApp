package com.prayertimes.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.prayertimes.R
import com.prayertimes.ui.components.LightweightPageEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import com.prayertimes.audio.AthanAudioEngine
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.AthanAudioStream
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.JuristicMethod
import com.prayertimes.data.models.NotificationPrayerConfig
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.notifications.PrayerLiveCountdownManager
import com.prayertimes.data.models.UserLocation
import com.prayertimes.data.places.PlaceEntity
import com.prayertimes.data.places.PlaceRepository
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.locale.LocalAppStrings
import kotlinx.coroutines.delay
import java.util.Locale

enum class SettingsSubScreen {
    MAIN,
    APPEARANCE,
    WIDGETS,
    CALCULATION,
    LOCATION,
    NOTIFICATIONS,
    ABOUT
}

private data class SettingsSearchEntry(
    val title: String,
    val section: String,
    val keywords: String,
    val icon: ImageVector,
    val destination: SettingsSubScreen
)

@Composable
fun SettingsScreen(
    resetKey: Any = Unit,
    settings: AppPrayerSettings,
    isGpsLoading: Boolean,
    onSelectCity: (UserLocation) -> Unit,
    onRequestGps: () -> Unit,
    onUpdateCalculationMethod: (CalculationMethod) -> Unit,
    onUpdateJuristicMethod: (JuristicMethod) -> Unit,
    onUpdateHighLatitudeRule: (HighLatitudeRule) -> Unit,
    onToggle24Hour: () -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (com.prayertimes.data.models.AppColorPreset) -> Unit = {},
    onUpdateCustomColorSeed: (Long) -> Unit = {},
    onUpdateFollowSystemColors: (Boolean) -> Unit = {},
    onUpdateWidgetSettings: (com.prayertimes.data.models.WidgetCustomizationSettings) -> Unit = {},
    onUpdatePrayerAdjustment: (PrayerType, Int) -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onTestNotification: (PrayerType, NotificationSoundType) -> Unit,
    onTestAlarmInSeconds: (PrayerType, NotificationSoundType, Int) -> Unit = { _, _, _ -> },
    onRescheduleAlarms: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit,
    onUpdateAudioStream: (AthanAudioStream) -> Unit = {},
    onUpdateWakeScreen: (Boolean) -> Unit = {},
    onUpdateLiveCountdownSettings: (Boolean, Int) -> Unit = { _, _ -> },
    onTestLiveCountdown: () -> Unit = {},
    onTestScheduledLiveCountdown: () -> Unit = {},
    onPreviewFullScreenAlarm: (PrayerType) -> Unit = {},
    onResetOnboarding: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var currentSubScreen by remember(resetKey) { mutableStateOf(SettingsSubScreen.MAIN) }

    // Hardware back button support
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    LightweightPageEntry(animationKey = currentSubScreen) {
        when (currentSubScreen) {
            SettingsSubScreen.MAIN -> {
                SettingsMainHub(
                    settings = settings,
                    onNavigateTo = { currentSubScreen = it },
                    onResetOnboarding = onResetOnboarding
                )
            }
            SettingsSubScreen.APPEARANCE -> {
                SettingsAppearanceSubScreen(
                    settings = settings,
                    onUpdateLanguage = onUpdateLanguage,
                    onUpdateThemeMode = onUpdateThemeMode,
                    onUpdateColorPreset = onUpdateColorPreset,
                    onUpdateCustomColorSeed = onUpdateCustomColorSeed,
                    onUpdateFollowSystemColors = onUpdateFollowSystemColors,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.WIDGETS -> {
                SettingsWidgetSubScreen(
                    settings = settings,
                    onUpdateWidgetSettings = onUpdateWidgetSettings,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.CALCULATION -> {
                SettingsCalculationSubScreen(
                    settings = settings,
                    onUpdateCalculationMethod = onUpdateCalculationMethod,
                    onUpdateJuristicMethod = onUpdateJuristicMethod,
                    onUpdateHighLatitudeRule = onUpdateHighLatitudeRule,
                    onToggle24Hour = onToggle24Hour,
                    onUpdatePrayerAdjustment = onUpdatePrayerAdjustment,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.LOCATION -> {
                SettingsLocationSubScreen(
                    settings = settings,
                    isGpsLoading = isGpsLoading,
                    onSelectCity = onSelectCity,
                    onRequestGps = onRequestGps,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.NOTIFICATIONS -> {
                SettingsNotificationsSubScreen(
                    settings = settings,
                    onUpdateNotificationConfig = onUpdateNotificationConfig,
                    onTestNotification = onTestNotification,
                    onTestAlarmInSeconds = onTestAlarmInSeconds,
                    onRescheduleAlarms = onRescheduleAlarms,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onPreviewSound = onPreviewSound,
                    onUpdateAudioStream = onUpdateAudioStream,
                    onUpdateWakeScreen = onUpdateWakeScreen,
                    onUpdateLiveCountdownSettings = onUpdateLiveCountdownSettings,
                    onTestLiveCountdown = onTestLiveCountdown,
                    onTestScheduledLiveCountdown = onTestScheduledLiveCountdown,
                    onPreviewFullScreenAlarm = onPreviewFullScreenAlarm,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.ABOUT -> {
                SettingsAboutSubScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
        }
    }
}

// ============================================================================
// MAIN HUB (Categorized overview)
// ============================================================================
@Composable
private fun SettingsMainHub(
    settings: AppPrayerSettings,
    onNavigateTo: (SettingsSubScreen) -> Unit,
    onResetOnboarding: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showResetConfirmation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onResetOnboarding()
                    }
                ) {
                    Text(stringResource(R.string.settings_reset_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.settings_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_cancel))
                    }
                }
            } else null,
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_search")
        )
        Spacer(modifier = Modifier.height(4.dp))

        val locationHubName = com.prayertimes.data.cities.CityDatabase.localizedName(LocalContext.current.resources, settings.location)
        val locationHubCountry = com.prayertimes.data.cities.CityDatabase.localizedCountry(LocalContext.current.resources, settings.location)
        val searchEntries = listOf(
            SettingsSearchEntry(strings.currentLocationLabel, strings.locationSection, "${strings.useGps} ${strings.locationSearchHint} ${strings.locationSourceGps}", Icons.Default.LocationOn, SettingsSubScreen.LOCATION),
            SettingsSearchEntry(strings.calcMethodSection, strings.prayerCalculationTimeTitle, strings.calcMethodName(settings.calculationMethod), Icons.Default.Calculate, SettingsSubScreen.CALCULATION),
            SettingsSearchEntry(strings.juristicMethodTitle, strings.prayerCalculationTimeTitle, "${strings.standardJuristic} ${strings.hanafiJuristic}", Icons.Default.Calculate, SettingsSubScreen.CALCULATION),
            SettingsSearchEntry(strings.timeFormatTitle, strings.prayerCalculationTimeTitle, "12 24 ${strings.minuteAdjustmentsTitle}", Icons.Default.Calculate, SettingsSubScreen.CALCULATION),
            SettingsSearchEntry(strings.highLatitudeSection, strings.prayerCalculationTimeTitle, strings.minuteAdjustmentsTitle, Icons.Default.Calculate, SettingsSubScreen.CALCULATION),
            SettingsSearchEntry(strings.audioStreamSectionTitle, strings.notifSectionTitle, "${strings.athanSound} ${strings.audioStreamAlarmTitle} ${strings.audioStreamMediaTitle}", Icons.Default.NotificationsActive, SettingsSubScreen.NOTIFICATIONS),
            SettingsSearchEntry(stringResource(R.string.settings_prayer_alerts_title), strings.notifSectionTitle, "${strings.wakeScreenTitle} ${stringResource(R.string.live_countdown_section_title)}", Icons.Default.NotificationsActive, SettingsSubScreen.NOTIFICATIONS),
            SettingsSearchEntry(strings.themeModeTitle, strings.appearanceLanguageTitle, "${strings.themeSection} ${strings.colorPaletteSection} ${strings.languageSection}", Icons.Default.Palette, SettingsSubScreen.APPEARANCE),
            SettingsSearchEntry(strings.widgetsSection, strings.widgetsSection, "${strings.widgetAppearanceSectionTitle} ${strings.widgetFontSizeTitle}", Icons.Default.StayCurrentPortrait, SettingsSubScreen.WIDGETS),
            SettingsSearchEntry(strings.aboutHubTitle, strings.aboutHubTitle, strings.aboutHubSubtitle, Icons.Default.Info, SettingsSubScreen.ABOUT)
        )
        val normalizedQuery = searchQuery.trim()

        if (normalizedQuery.isNotEmpty()) {
            val matches = searchEntries.filter { entry ->
                listOf(entry.title, entry.section, entry.keywords)
                    .any { it.contains(normalizedQuery, ignoreCase = true) }
            }
            if (matches.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 20.dp)
                )
            } else {
                matches.forEachIndexed { index, entry ->
                    SettingsHubCategoryCard(
                        title = entry.title,
                        subtitle = stringResource(R.string.settings_search_result_in, entry.section),
                        icon = entry.icon,
                        testTag = "settings_search_result_$index",
                        onClick = { onNavigateTo(entry.destination) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(112.dp))
            return@Column
        }

        SettingsHubCategoryCard(
            title = strings.locationSection,
            subtitle = "$locationHubName${if (locationHubCountry.isNotEmpty()) ", $locationHubCountry" else ""}${if (settings.location.isGps) " • ${strings.locationSourceGps}" else ""}",
            icon = Icons.Default.LocationOn,
            testTag = "settings_cat_location",
            onClick = { onNavigateTo(SettingsSubScreen.LOCATION) }
        )

        val hasAdjustments = settings.adjustments.let { it.fajr != 0 || it.sunrise != 0 || it.dhuhr != 0 || it.asr != 0 || it.maghrib != 0 || it.isha != 0 }
        SettingsHubCategoryCard(
            title = strings.prayerCalculationTimeTitle,
            subtitle = "${strings.calcMethodName(settings.calculationMethod)} • ${stringResource(if (settings.is24HourFormat) R.string.time_format_24_summary else R.string.time_format_12_summary)} • ${if (hasAdjustments) strings.adjustmentsCustomActive else strings.adjustmentsStandard}",
            icon = Icons.Default.Calculate,
            testTag = "settings_cat_calc",
            onClick = { onNavigateTo(SettingsSubScreen.CALCULATION) }
        )

        val enabledNotifsCount = settings.prayerConfigs.values.count { it.enabled }
        SettingsHubCategoryCard(
            title = strings.notifSectionTitle,
            subtitle = strings.notifCountActive(enabledNotifsCount),
            icon = Icons.Default.NotificationsActive,
            testTag = "settings_cat_notifications",
            onClick = { onNavigateTo(SettingsSubScreen.NOTIFICATIONS) }
        )

        SettingsHubCategoryCard(
            title = strings.appearanceLanguageTitle,
            subtitle = "${settings.language.getDisplayName(strings.isArabic)} • ${strings.themeModeName(settings.themeMode)} • ${strings.colorPresetName(if (settings.followSystemColors) com.prayertimes.data.models.AppColorPreset.SYSTEM_DYNAMIC else settings.colorPreset)}",
            icon = Icons.Default.Palette,
            testTag = "settings_cat_appearance",
            onClick = { onNavigateTo(SettingsSubScreen.APPEARANCE) }
        )

        SettingsHubCategoryCard(
            title = strings.widgetsSection,
            subtitle = stringResource(
                R.string.settings_widget_subtitle_format,
                stringResource(settings.widgetSettings.themeMode.titleRes),
                settings.widgetSettings.opacityPercent
            ),
            icon = Icons.Default.StayCurrentPortrait,
            testTag = "settings_cat_widgets",
            onClick = { onNavigateTo(SettingsSubScreen.WIDGETS) }
        )

        SettingsHubCategoryCard(
            title = strings.aboutHubTitle,
            subtitle = strings.aboutHubSubtitle,
            icon = Icons.Default.Info,
            testTag = "settings_cat_about",
            onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        TextButton(
            onClick = { showResetConfirmation = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_cat_rerun_setup")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_help_reset_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_help_reset_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Composable
private fun SettingsHubCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_back_button")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.back,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun <T> SettingsDropdownSelector(
    title: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    leading: (@Composable (T) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                leading?.let {
                    it(selected)
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = optionLabel(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                leading?.let {
                                    it(option)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(optionLabel(option), modifier = Modifier.weight(1f))
                                if (option == selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            if (option != selected) onSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    description: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_advanced_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.settings_hide_advanced else R.string.settings_show_advanced
                    )
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsAppearanceSubScreen(
    settings: AppPrayerSettings,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (com.prayertimes.data.models.AppColorPreset) -> Unit,
    onUpdateCustomColorSeed: (Long) -> Unit,
    onUpdateFollowSystemColors: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val selectedPalette = if (settings.followSystemColors) {
        com.prayertimes.data.models.AppColorPreset.SYSTEM_DYNAMIC
    } else {
        settings.colorPreset
    }
    val darkPalette = settings.themeMode == AppThemeMode.DARK ||
        (settings.themeMode == AppThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    var customHue by remember(settings.customColorSeed) {
        mutableStateOf(hueFromColor(settings.customColorSeed))
    }
    var customBrightness by remember(settings.customColorSeed) {
        mutableStateOf(brightnessFromColor(settings.customColorSeed))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("appearance_subscreen")
    ) {
        SubScreenHeader(title = strings.appearanceLanguageTitle, onBack = onBack)
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = strings.themeModeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ThemeSelectionCard(
                title = strings.themeModeName(AppThemeMode.SYSTEM),
                description = strings.systemThemeDesc,
                icon = Icons.Default.BrightnessAuto,
                isSelected = settings.themeMode == AppThemeMode.SYSTEM,
                onClick = { onUpdateThemeMode(AppThemeMode.SYSTEM) }
            )
            ThemeSelectionCard(
                title = strings.themeModeName(AppThemeMode.LIGHT),
                description = strings.lightThemeDesc,
                icon = Icons.Default.LightMode,
                isSelected = settings.themeMode == AppThemeMode.LIGHT,
                onClick = { onUpdateThemeMode(AppThemeMode.LIGHT) }
            )
            ThemeSelectionCard(
                title = strings.themeModeName(AppThemeMode.DARK),
                description = strings.darkThemeDesc,
                icon = Icons.Default.DarkMode,
                isSelected = settings.themeMode == AppThemeMode.DARK,
                onClick = { onUpdateThemeMode(AppThemeMode.DARK) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsDropdownSelector(
                title = strings.appLanguage,
                selected = settings.language,
                options = AppLanguage.values().toList(),
                optionLabel = { it.getDisplayName(strings.isArabic) },
                onSelected = onUpdateLanguage
            )

            SettingsDropdownSelector(
                title = strings.colorPaletteSection,
                selected = selectedPalette,
                options = com.prayertimes.data.models.AppColorPreset.values().toList(),
                optionLabel = strings::colorPresetName,
                onSelected = { preset ->
                    if (preset == com.prayertimes.data.models.AppColorPreset.SYSTEM_DYNAMIC) {
                        onUpdateFollowSystemColors(true)
                    } else {
                        onUpdateColorPreset(preset)
                    }
                },
                leading = { preset ->
                    val primary = if (preset == com.prayertimes.data.models.AppColorPreset.CUSTOM) {
                        settings.customColorSeed
                    } else if (darkPalette) preset.primaryDark else preset.primaryLight
                    val secondary = if (preset == com.prayertimes.data.models.AppColorPreset.CUSTOM) {
                        colorFromHueAndBrightness((customHue + 24f) % 360f, customBrightness)
                    } else if (darkPalette) preset.secondaryDark else preset.secondaryLight
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(primary)),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(secondary))
                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }
            )

            AnimatedVisibility(visible = selectedPalette == com.prayertimes.data.models.AppColorPreset.CUSTOM) {
                val selectedColor = androidx.compose.ui.graphics.Color(
                    colorFromHueAndBrightness(customHue, customBrightness)
                )
                val saveCustomColor = {
                    onUpdateCustomColorSeed(colorFromHueAndBrightness(customHue, customBrightness))
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.custom_color_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(selectedColor)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.custom_color_hue),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Red,
                                            androidx.compose.ui.graphics.Color.Yellow,
                                            androidx.compose.ui.graphics.Color.Green,
                                            androidx.compose.ui.graphics.Color.Cyan,
                                            androidx.compose.ui.graphics.Color.Blue,
                                            androidx.compose.ui.graphics.Color.Magenta,
                                            androidx.compose.ui.graphics.Color.Red
                                        )
                                    )
                                )
                        )
                        Slider(
                            value = customHue,
                            onValueChange = { customHue = it },
                            onValueChangeFinished = saveCustomColor,
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = selectedColor,
                                activeTrackColor = selectedColor,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                        Text(
                            text = stringResource(R.string.custom_color_brightness),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Black,
                                            androidx.compose.ui.graphics.Color(
                                                colorFromHueAndBrightness(customHue, 1f)
                                            )
                                        )
                                    )
                                )
                        )
                        Slider(
                            value = customBrightness,
                            onValueChange = { customBrightness = it },
                            onValueChangeFinished = saveCustomColor,
                            valueRange = 0.2f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = selectedColor,
                                activeTrackColor = selectedColor,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun hueFromColor(color: Long): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toInt(), hsv)
    return hsv[0]
}

private fun brightnessFromColor(color: Long): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toInt(), hsv)
    return hsv[2].coerceIn(0.2f, 1f)
}

private fun colorFromHueAndBrightness(hue: Float, brightness: Float): Long =
    android.graphics.Color.HSVToColor(
        floatArrayOf(hue, 0.72f, brightness.coerceIn(0.2f, 1f))
    ).toLong() and 0xFFFFFFFFL

@Composable
private fun ThemeSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

// 2. CALCULATION
@Composable
private fun SettingsCalculationSubScreen(
    settings: AppPrayerSettings,
    onUpdateCalculationMethod: (CalculationMethod) -> Unit,
    onUpdateJuristicMethod: (JuristicMethod) -> Unit,
    onUpdateHighLatitudeRule: (HighLatitudeRule) -> Unit,
    onToggle24Hour: () -> Unit,
    onUpdatePrayerAdjustment: (PrayerType, Int) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    var advancedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("calc_subscreen")
    ) {
        SubScreenHeader(title = strings.prayerCalculationTimeTitle, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsDropdownSelector(
                    title = strings.calcMethodSection,
                    selected = settings.calculationMethod,
                    options = CalculationMethod.values().toList(),
                    optionLabel = strings::calcMethodName,
                    onSelected = onUpdateCalculationMethod
                )
            }

            item {
                Text(
                    text = strings.juristicMethodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onUpdateJuristicMethod(JuristicMethod.STANDARD) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.standardJuristic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = strings.juristicStandardDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = settings.juristicMethod == JuristicMethod.STANDARD,
                                onClick = null
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onUpdateJuristicMethod(JuristicMethod.HANAFI) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.hanafiJuristic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = strings.juristicHanafiDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = settings.juristicMethod == JuristicMethod.HANAFI,
                                onClick = null
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle24Hour() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.timeFormatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(if (settings.is24HourFormat) R.string.time_format_24_summary else R.string.time_format_12_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.is24HourFormat,
                            onCheckedChange = null
                        )
                    }
                }
            }

            item {
                AdvancedSettingsSection(
                    expanded = advancedExpanded,
                    onExpandedChange = { advancedExpanded = it },
                    description = stringResource(R.string.settings_advanced_calculation_desc)
                ) {
                    SettingsDropdownSelector(
                        title = strings.highLatitudeSection,
                        selected = settings.highLatitudeRule,
                        options = HighLatitudeRule.values().toList(),
                        optionLabel = strings::highLatitudeName,
                        onSelected = onUpdateHighLatitudeRule
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.minuteAdjustmentsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.minuteAdjustmentsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrayerType.values().forEach { prayer ->
                        val currentOffset = when (prayer) {
                            PrayerType.FAJR -> settings.adjustments.fajr
                            PrayerType.SUNRISE -> settings.adjustments.sunrise
                            PrayerType.DHUHR -> settings.adjustments.dhuhr
                            PrayerType.ASR -> settings.adjustments.asr
                            PrayerType.MAGHRIB -> settings.adjustments.maghrib
                            PrayerType.ISHA -> settings.adjustments.isha
                        }
                        PrayerAdjustmentCard(
                            prayerName = strings.prayerName(prayer),
                            currentOffset = currentOffset,
                            onDecrease = { onUpdatePrayerAdjustment(prayer, currentOffset - 1) },
                            onIncrease = { onUpdatePrayerAdjustment(prayer, currentOffset + 1) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = { PrayerType.values().forEach { onUpdatePrayerAdjustment(it, 0) } },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.resetAllAdjustments)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.aboutUmmAlQuraTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.aboutUmmAlQuraDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun PrayerAdjustmentCard(
    prayerName: String,
    currentOffset: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prayerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_minute_value, currentOffset),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilledTonalButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 3. LOCATION
@Composable
private fun SettingsLocationSubScreen(
    settings: AppPrayerSettings,
    isGpsLoading: Boolean,
    onSelectCity: (UserLocation) -> Unit,
    onRequestGps: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PlaceEntity>>(emptyList()) }
    var locationDetailsExpanded by remember { mutableStateOf(false) }
    var searchCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            searchCompleted = false
        } else {
            searchCompleted = false
            delay(200)
            searchResults = PlaceRepository.search(context, searchQuery)
            searchCompleted = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("location_subscreen")
    ) {
        SubScreenHeader(title = strings.locationSection, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

        // Current Location - always reflects settings.location (persisted state), so it stays
        // visible regardless of the search field's transient state, unlike relying on a search
        // result row happening to be on screen with a checkmark.
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("current_location_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (settings.location.isGps) Icons.Default.MyLocation else Icons.Default.LocationCity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.currentLocationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = settings.location.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (settings.location.country.isNotEmpty()) {
                        Text(
                            text = settings.location.country,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (settings.location.isGps) strings.locationSourceGps else strings.locationSourceManual,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (settings.location.isGps) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraSmall)
                                .clickable { locationDetailsExpanded = !locationDetailsExpanded }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_location_details),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (locationDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = stringResource(if (locationDetailsExpanded) R.string.settings_hide_advanced else R.string.settings_show_advanced)
                            )
                        }
                        AnimatedVisibility(visible = locationDetailsExpanded) {
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.4f°, %.4f°", settings.location.latitude, settings.location.longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (settings.location.nearestPlaceDistanceKm != null) {
                                    Text(
                                        text = strings.gpsLocationDisclaimer,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // GPS Action Button
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.useGps,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.gpsTapToDetect,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onRequestGps,
                    enabled = !isGpsLoading,
                    shape = MaterialTheme.shapes.small
                ) {
                    if (isGpsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.gpsButtonLabel)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.settings_choose_another_city),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.locationSearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isNotBlank()) {
            if (searchCompleted && searchResults.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_city_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.geonameId }) { place ->
                    val isSelected = !settings.location.isGps &&
                        settings.location.latitude == place.latitude &&
                        settings.location.longitude == place.longitude
                    val placeName = if (strings.isArabic) place.nameAr ?: place.nameEn else place.nameEn
                    val countryName = Locale("", place.countryCode)
                        .getDisplayCountry(if (strings.isArabic) Locale("ar") else Locale.ENGLISH)
                    Card(
                        onClick = {
                            onSelectCity(
                                UserLocation(
                                    name = placeName,
                                    country = countryName,
                                    latitude = place.latitude,
                                    longitude = place.longitude,
                                    timeZoneId = place.timeZoneId,
                                    isGps = false
                                )
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = placeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$countryName • ${place.timeZoneId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

// 4. NOTIFICATIONS
@Composable
private fun SettingsNotificationsSubScreen(
    settings: AppPrayerSettings,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onTestNotification: (PrayerType, NotificationSoundType) -> Unit,
    onTestAlarmInSeconds: (PrayerType, NotificationSoundType, Int) -> Unit,
    onRescheduleAlarms: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onPreviewSound: (NotificationSoundType, PrayerType) -> Unit,
    onUpdateAudioStream: (AthanAudioStream) -> Unit,
    onUpdateWakeScreen: (Boolean) -> Unit,
    onUpdateLiveCountdownSettings: (Boolean, Int) -> Unit,
    onTestLiveCountdown: () -> Unit,
    onTestScheduledLiveCountdown: () -> Unit,
    onPreviewFullScreenAlarm: (PrayerType) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = LocalAppStrings.current
    var activeSoundDialogPrayer by remember { mutableStateOf<PrayerType?>(null) }
    var testAlarmScheduledText by remember { mutableStateOf<String?>(null) }
    var previewPrayerArtwork by remember { mutableStateOf(PrayerType.FAJR) }
    var advancedExpanded by remember { mutableStateOf(false) }

    var notificationsEnabled by remember {
        mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var canPostPromotedNotifications by remember {
        mutableStateOf(PrayerLiveCountdownManager.canPostPromotedNotifications(context))
    }
    val promotionSettingsIntent = remember(context) {
        PrayerLiveCountdownManager.promotionSettingsIntent(context)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                canPostPromotedNotifications =
                    PrayerLiveCountdownManager.canPostPromotedNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (activeSoundDialogPrayer != null) {
        val target = activeSoundDialogPrayer!!
        val cfg = settings.prayerConfigs[target] ?: NotificationPrayerConfig()
        SoundPickerDialog(
            prayerType = target,
            currentSound = cfg.soundType,
            onSelectSound = {
                onUpdateNotificationConfig(target, cfg.enabled, it, cfg.preReminderMinutes)
                activeSoundDialogPrayer = null
            },
            onPreviewSound = { sound -> onPreviewSound(sound, target) },
            onDismiss = { activeSoundDialogPrayer = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("notif_subscreen")
    ) {
        SubScreenHeader(title = strings.notifSectionTitle, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification Permission Status Banner
            item {
                if (!notificationsEnabled) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = strings.notificationsDisabledWarning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onRequestNotificationPermission()
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.enableNotificationsBtn,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.notificationsStatusActive,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Audio Stream Output Selection Card (Follows System Volume)
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.audioStreamSectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.audioStreamDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(
                            Triple(AthanAudioStream.ALARM, strings.audioStreamAlarmTitle, strings.audioStreamAlarmDesc),
                            Triple(AthanAudioStream.MEDIA, strings.audioStreamMediaTitle, strings.audioStreamMediaDesc),
                            Triple(AthanAudioStream.RINGTONE, strings.audioStreamRingtoneTitle, strings.audioStreamRingtoneDesc)
                        ).forEach { (stream, title, desc) ->
                            val isSelected = settings.audioStream == stream
                            val streamIcon = when (stream) {
                                AthanAudioStream.ALARM -> Icons.Default.Alarm
                                AthanAudioStream.MEDIA -> Icons.Default.MusicNote
                                AthanAudioStream.RINGTONE -> Icons.Default.PhoneInTalk
                            }

                            Card(
                                onClick = { onUpdateAudioStream(stream) },
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                                ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = streamIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AdvancedSettingsSection(
                    expanded = advancedExpanded,
                    onExpandedChange = { advancedExpanded = it },
                    description = stringResource(R.string.settings_advanced_notifications_desc)
                ) {}
            }

            // Wake Screen & Full-Screen Alarm Artwork Card
            if (advancedExpanded) item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StayCurrentPortrait,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = strings.wakeScreenTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.wakeScreenDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.wakeScreenOnAlarm,
                                onCheckedChange = { isChecked ->
                                    onUpdateWakeScreen(isChecked)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = strings.selectArtworkPreview,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Chips to choose prayer artwork for preview
                        listOf(
                                PrayerType.FAJR,
                                PrayerType.SUNRISE,
                                PrayerType.DHUHR,
                                PrayerType.ASR,
                                PrayerType.MAGHRIB,
                                PrayerType.ISHA
                            ).chunked(3).forEach { rowPrayers ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                              rowPrayers.forEach { p ->
                                val isSelected = previewPrayerArtwork == p
                                FilledTonalButton(
                                    onClick = { previewPrayerArtwork = p },
                                    shape = MaterialTheme.shapes.extraSmall,
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = strings.prayerName(p),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                              }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onPreviewFullScreenAlarm(previewPrayerArtwork) },
                            shape = MaterialTheme.shapes.small,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Panorama, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.previewFullScreenAlarmBtn,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            // Live Athan Countdown Card (standard Android Live Update notification)
            if (advancedExpanded) item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.live_countdown_section_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.live_countdown_section_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.liveCountdownEnabled,
                                onCheckedChange = { isChecked ->
                                    onUpdateLiveCountdownSettings(isChecked, settings.liveCountdownMinutesBefore)
                                }
                            )
                        }

                        if (settings.liveCountdownEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = stringResource(R.string.live_countdown_lead_time_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            listOf(5, 10, 15, 20, 25, 30).chunked(3).forEach { rowMinutes ->
                              Row(
                                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                                  modifier = Modifier.fillMaxWidth()
                              ) {
                                rowMinutes.forEach { mins ->
                                    val isSelected = settings.liveCountdownMinutesBefore == mins
                                    FilledTonalButton(
                                        onClick = { onUpdateLiveCountdownSettings(true, mins) },
                                        shape = MaterialTheme.shapes.extraSmall,
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.live_countdown_minutes_chip, mins),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                              }
                              Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (Build.VERSION.SDK_INT >= 36 && !canPostPromotedNotifications) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = stringResource(R.string.live_countdown_promotion_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        if (PrayerLiveCountdownManager.isSamsungDevice()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.live_countdown_samsung_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        if (promotionSettingsIntent != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TextButton(onClick = { context.startActivity(promotionSettingsIntent) }) {
                                                Text(stringResource(R.string.live_countdown_enable_live_updates))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onTestLiveCountdown,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.live_countdown_test_btn), fontWeight = FontWeight.Bold)
                        }
                        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onTestScheduledLiveCountdown,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Alarm, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.live_countdown_scheduled_test_btn),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_prayer_alerts_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.settings_prayer_alerts_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(PrayerType.values()) { prayer ->
                val cfg = settings.prayerConfigs[prayer] ?: NotificationPrayerConfig()
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraSmall)
                                .clickable {
                                    onUpdateNotificationConfig(prayer, !cfg.enabled, cfg.soundType, cfg.preReminderMinutes)
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.prayerName(prayer),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = cfg.enabled,
                                onCheckedChange = null
                            )
                        }

                        if (cfg.enabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable { activeSoundDialogPrayer = prayer }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.athanSound,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = strings.soundTypeName(cfg.soundType),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { activeSoundDialogPrayer = prayer },
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(strings.change, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            if (advancedExpanded) item {
                Spacer(modifier = Modifier.height(10.dp))

                // Test instant notification
                Button(
                    onClick = { onTestNotification(PrayerType.DHUHR, NotificationSoundType.FULL_ATHAN) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.testAlert, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test 5-second background alarm
                OutlinedButton(
                    onClick = {
                        onTestAlarmInSeconds(PrayerType.DHUHR, NotificationSoundType.FULL_ATHAN, 5)
                        testAlarmScheduledText = context.getString(R.string.settings_test_alarm_scheduled)
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.testAlarm5s, fontWeight = FontWeight.Bold)
                }

                testAlarmScheduledText?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// 8. ABOUT
@Composable
private fun SettingsAboutSubScreen(onBack: () -> Unit) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("about_subscreen")
    ) {
        SubScreenHeader(
            title = strings.aboutScreenTitle,
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = strings.appBrandName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_version_engine_label,
                            versionName
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.astroEngineTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.astroEngineDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.aboutDataAttributionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.aboutDataAttributionBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.aboutTypefaceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.aboutTypefaceBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// SOUND PICKER DIALOG
@Composable
fun SoundPickerDialog(
    prayerType: PrayerType,
    currentSound: NotificationSoundType,
    onSelectSound: (NotificationSoundType) -> Unit,
    onPreviewSound: (NotificationSoundType) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val playbackState by AthanAudioEngine.playbackState.collectAsState()
    val stopAndDismiss = {
        AthanAudioEngine.stop()
        onDismiss()
    }
    val stopAndSelect: (NotificationSoundType) -> Unit = { sound ->
        AthanAudioEngine.stop()
        onSelectSound(sound)
    }

    // Also stop if the picker leaves composition because its parent screen navigates away.
    DisposableEffect(Unit) {
        onDispose { AthanAudioEngine.stop() }
    }

    Dialog(onDismissRequest = stopAndDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "${strings.athanSound} - ${strings.prayerName(prayerType)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(NotificationSoundType.selectableValues(strings.isArabic)) { sound ->
                        val isSelected = sound == currentSound
                        Card(
                            onClick = { stopAndSelect(sound) },
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { stopAndSelect(sound) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = strings.soundTypeName(sound),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        val subtitle = strings.soundTypeSubtitle(sound)
                                        if (subtitle.isNotEmpty()) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (sound != NotificationSoundType.SILENT && sound != NotificationSoundType.VIBRATE_ONLY) {
                                    val isThisPlaying = playbackState.isPlaying && playbackState.title.contains(sound.localizedDisplayName(strings.isArabic), ignoreCase = true)
                                    IconButton(
                                        onClick = {
                                            if (isThisPlaying) {
                                                AthanAudioEngine.stop()
                                            } else {
                                                // playSoundType stops any other current preview
                                                // before starting this one, so switching is one tap.
                                                onPreviewSound(sound)
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = if (isThisPlaying) "Stop" else "Preview",
                                            tint = if (isThisPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = stopAndDismiss) {
                        Text(strings.close)
                    }
                }
            }
        }
    }
}
