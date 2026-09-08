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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.style.TextOverflow
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
                    onNavigateTo = { currentSubScreen = it }
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
                    onResetOnboarding = onResetOnboarding,
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
    onNavigateTo: (SettingsSubScreen) -> Unit
) {
    val strings = LocalAppStrings.current
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.navSettings,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                onClick = {
                    if (searchExpanded) searchQuery = ""
                    searchExpanded = !searchExpanded
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = stringResource(
                            if (searchExpanded) R.string.settings_cancel else R.string.settings_search_hint
                        ),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = searchExpanded) {
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
                    .padding(top = 8.dp)
                    .testTag("settings_search")
            )
        }

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
                SettingsHubGroup {
                    matches.forEachIndexed { index, entry ->
                        SettingsHubRow(
                            title = entry.title,
                            subtitle = stringResource(R.string.settings_search_result_in, entry.section),
                            icon = entry.icon,
                            testTag = "settings_search_result_$index",
                            showDivider = index > 0,
                            onClick = { onNavigateTo(entry.destination) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(112.dp))
            return@Column
        }

        val enabledNotifsCount = settings.prayerConfigs.values.count { it.enabled }
        SettingsHubSectionLabel(stringResource(R.string.settings_group_essentials))
        SettingsHubGroup {
            SettingsHubRow(
                title = strings.locationSection,
                subtitle = "$locationHubName${if (settings.location.isGps) " • ${strings.locationSourceGps}" else if (locationHubCountry.isNotEmpty()) " • $locationHubCountry" else ""}",
                icon = Icons.Default.LocationOn,
                testTag = "settings_cat_location",
                onClick = { onNavigateTo(SettingsSubScreen.LOCATION) }
            )
            SettingsHubRow(
                title = strings.notifSectionTitle,
                subtitle = strings.notifCountActive(enabledNotifsCount),
                icon = Icons.Default.NotificationsActive,
                testTag = "settings_cat_notifications",
                showDivider = true,
                onClick = { onNavigateTo(SettingsSubScreen.NOTIFICATIONS) }
            )
        }

        SettingsHubSectionLabel(stringResource(R.string.settings_group_personalization))
        SettingsHubGroup {
            SettingsHubRow(
                title = strings.appearanceLanguageTitle,
                subtitle = "${strings.themeModeName(settings.themeMode)} • ${settings.language.getDisplayName(strings.isArabic)}",
                icon = Icons.Default.Palette,
                testTag = "settings_cat_appearance",
                onClick = { onNavigateTo(SettingsSubScreen.APPEARANCE) }
            )
            SettingsHubRow(
                title = strings.widgetsSection,
                subtitle = stringResource(settings.widgetSettings.themeMode.titleRes),
                icon = Icons.Default.StayCurrentPortrait,
                testTag = "settings_cat_widgets",
                showDivider = true,
                onClick = { onNavigateTo(SettingsSubScreen.WIDGETS) }
            )
        }

        SettingsHubSectionLabel(stringResource(R.string.settings_group_advanced))
        SettingsHubGroup {
            SettingsHubRow(
                title = strings.prayerCalculationTimeTitle,
                subtitle = "${strings.calcMethodName(settings.calculationMethod)} • ${if (settings.juristicMethod == JuristicMethod.HANAFI) strings.hanafiJuristic else strings.standardJuristic}",
                icon = Icons.Default.Calculate,
                testTag = "settings_cat_calc",
                onClick = { onNavigateTo(SettingsSubScreen.CALCULATION) }
            )
        }

        SettingsHubSectionLabel(stringResource(R.string.settings_group_information))
        SettingsHubGroup {
            SettingsHubRow(
                title = strings.aboutHubTitle,
                subtitle = strings.aboutHubSubtitle,
                icon = Icons.Default.Info,
                testTag = "settings_cat_about",
                onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
            )
        }

        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Composable
private fun SettingsHubSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsHubGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsHubRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    showDivider: Boolean = false,
    onClick: () -> Unit
) {
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 64.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
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
            fontWeight = FontWeight.SemiBold,
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                leading?.let {
                    it(selected)
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = optionLabel(selected),
                    style = MaterialTheme.typography.bodyLarge,
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
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_advanced_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
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
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.themeModeTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeSelectionCard(
                    title = strings.themeModeName(AppThemeMode.SYSTEM),
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = settings.themeMode == AppThemeMode.SYSTEM,
                    onClick = { onUpdateThemeMode(AppThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                ThemeSelectionCard(
                    title = strings.themeModeName(AppThemeMode.LIGHT),
                    icon = Icons.Default.LightMode,
                    isSelected = settings.themeMode == AppThemeMode.LIGHT,
                    onClick = { onUpdateThemeMode(AppThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeSelectionCard(
                    title = strings.themeModeName(AppThemeMode.DARK),
                    icon = Icons.Default.DarkMode,
                    isSelected = settings.themeMode == AppThemeMode.DARK,
                    onClick = { onUpdateThemeMode(AppThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }

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
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.custom_color_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(selectedColor)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
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
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.height(108.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
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
                    modifier = Modifier.size(19.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
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
    var calculationMethodExpanded by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = strings.calcMethodSection,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        onClick = { calculationMethodExpanded = true },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.calcMethodName(settings.calculationMethod),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = calculationMethodExpanded,
                        onDismissRequest = { calculationMethodExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        CalculationMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.calcMethodName(method),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                trailingIcon = {
                                    if (method == settings.calculationMethod) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    calculationMethodExpanded = false
                                    if (method != settings.calculationMethod) onUpdateCalculationMethod(method)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.juristicMethodTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateJuristicMethod(JuristicMethod.STANDARD) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.standardJuristic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
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

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateJuristicMethod(JuristicMethod.HANAFI) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.hanafiJuristic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
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
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle24Hour() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.timeFormatTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.minuteAdjustmentsTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.minuteAdjustmentsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                        Spacer(modifier = Modifier.height(4.dp))
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
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prayerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("−", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_minute_value, currentOffset),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", fontSize = 18.sp)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth().testTag("current_location_card")
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (settings.location.isGps) Icons.Default.MyLocation else Icons.Default.LocationCity,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.currentLocationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = settings.location.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = listOfNotNull(
                                settings.location.country.takeIf { it.isNotEmpty() },
                                (if (settings.location.isGps) strings.locationSourceGps else strings.locationSourceManual)
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestGps,
                        enabled = !isGpsLoading,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (isGpsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.gpsButtonLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (settings.location.isGps) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { locationDetailsExpanded = !locationDetailsExpanded }
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_location_details),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (locationDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(if (locationDetailsExpanded) R.string.settings_hide_advanced else R.string.settings_show_advanced),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AnimatedVisibility(visible = locationDetailsExpanded) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.settings_choose_another_city),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.locationSearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

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
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = placeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
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
    var audioStreamExpanded by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                }
            }

            // Keep this first: it determines which physical volume control affects Athan.
            item {
                Text(
                    text = strings.audioStreamSectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
                Text(
                    text = strings.audioStreamDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        onClick = { audioStreamExpanded = true },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (settings.audioStream) {
                                        AthanAudioStream.ALARM -> strings.audioStreamAlarmTitle
                                        AthanAudioStream.MEDIA -> strings.audioStreamMediaTitle
                                        AthanAudioStream.RINGTONE -> strings.audioStreamRingtoneTitle
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (settings.audioStream) {
                                        AthanAudioStream.ALARM -> strings.audioStreamAlarmDesc
                                        AthanAudioStream.MEDIA -> strings.audioStreamMediaDesc
                                        AthanAudioStream.RINGTONE -> strings.audioStreamRingtoneDesc
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = audioStreamExpanded,
                        onDismissRequest = { audioStreamExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        listOf(
                            AthanAudioStream.ALARM to strings.audioStreamAlarmTitle,
                            AthanAudioStream.MEDIA to strings.audioStreamMediaTitle,
                            AthanAudioStream.RINGTONE to strings.audioStreamRingtoneTitle
                        ).forEach { (stream, title) ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (stream) {
                                            AthanAudioStream.ALARM -> Icons.Default.Alarm
                                            AthanAudioStream.MEDIA -> Icons.Default.MusicNote
                                            AthanAudioStream.RINGTONE -> Icons.Default.PhoneInTalk
                                        },
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (settings.audioStream == stream) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    audioStreamExpanded = false
                                    onUpdateAudioStream(stream)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_prayer_alerts_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
                Text(
                    text = stringResource(R.string.settings_prayer_alerts_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        PrayerType.values().forEachIndexed { index, prayer ->
                            val cfg = settings.prayerConfigs[prayer] ?: NotificationPrayerConfig()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = cfg.enabled) { activeSoundDialogPrayer = prayer }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.prayerName(prayer),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (cfg.enabled) strings.soundTypeName(cfg.soundType)
                                        else stringResource(R.string.settings_off),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (cfg.enabled) {
                                    TextButton(onClick = { activeSoundDialogPrayer = prayer }) {
                                        Text(strings.change, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Switch(
                                    checked = cfg.enabled,
                                    onCheckedChange = {
                                        onUpdateNotificationConfig(prayer, it, cfg.soundType, cfg.preReminderMinutes)
                                    }
                                )
                            }
                            if (index != PrayerType.values().lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
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
private fun SettingsAboutSubScreen(
    onResetOnboarding: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showResetConfirmation by remember { mutableStateOf(false) }
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

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
