package com.prayertimes.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.prayertimes.R
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import com.prayertimes.audio.AthanAudioEngine
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.AthanAudioStream
import com.prayertimes.data.models.CalculationMethod
import com.prayertimes.data.models.HighLatitudeRule
import com.prayertimes.data.models.JuristicMethod
import com.prayertimes.data.models.NotificationPrayerConfig
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.UserLocation
import com.prayertimes.data.places.PlaceEntity
import com.prayertimes.data.places.PlaceRepository
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.locale.LocalAppStrings
import kotlinx.coroutines.delay
import java.util.Locale

enum class SettingsSubScreen {
    MAIN,
    THEME,
    WIDGETS,
    CALCULATION,
    LOCATION,
    NOTIFICATIONS,
    LANGUAGE,
    HIJRI_DISPLAY,
    ADJUSTMENTS,
    ABOUT
}

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
    onUpdateHijriOffset: (Int) -> Unit,
    onToggle24Hour: () -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (com.prayertimes.data.models.AppColorPreset) -> Unit = {},
    onUpdateFollowSystemColors: (Boolean) -> Unit = {},
    onUpdateWidgetSettings: (com.prayertimes.data.models.WidgetCustomizationSettings) -> Unit = {},
    onRefreshAllWidgets: () -> Unit = {},
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
    onPreviewFullScreenAlarm: (PrayerType) -> Unit = {},
    onResetOnboarding: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var currentSubScreen by remember(resetKey) { mutableStateOf(SettingsSubScreen.MAIN) }

    // Hardware back button support
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.MAIN) {
                (slideInHorizontally { width -> if (strings.isArabic) -width else width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> if (strings.isArabic) width else -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> if (strings.isArabic) width else -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> if (strings.isArabic) -width else width } + fadeOut()
                )
            }
        },
        label = "SettingsSubScreenAnimation"
    ) { screen ->
        when (screen) {
            SettingsSubScreen.MAIN -> {
                SettingsMainHub(
                    settings = settings,
                    onNavigateTo = { currentSubScreen = it },
                    onResetOnboarding = onResetOnboarding
                )
            }
            SettingsSubScreen.THEME -> {
                SettingsThemeSubScreen(
                    settings = settings,
                    onUpdateThemeMode = onUpdateThemeMode,
                    onUpdateColorPreset = onUpdateColorPreset,
                    onUpdateFollowSystemColors = onUpdateFollowSystemColors,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.WIDGETS -> {
                SettingsWidgetSubScreen(
                    settings = settings,
                    onUpdateWidgetSettings = onUpdateWidgetSettings,
                    onRefreshAllWidgets = onRefreshAllWidgets,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.CALCULATION -> {
                SettingsCalculationSubScreen(
                    settings = settings,
                    onUpdateCalculationMethod = onUpdateCalculationMethod,
                    onUpdateJuristicMethod = onUpdateJuristicMethod,
                    onUpdateHighLatitudeRule = onUpdateHighLatitudeRule,
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
                    onPreviewFullScreenAlarm = onPreviewFullScreenAlarm,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.LANGUAGE -> {
                SettingsLanguageSubScreen(
                    settings = settings,
                    onUpdateLanguage = onUpdateLanguage,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.HIJRI_DISPLAY -> {
                SettingsHijriDisplaySubScreen(
                    settings = settings,
                    onUpdateHijriOffset = onUpdateHijriOffset,
                    onToggle24Hour = onToggle24Hour,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN }
                )
            }
            SettingsSubScreen.ADJUSTMENTS -> {
                SettingsAdjustmentsSubScreen(
                    settings = settings,
                    onUpdatePrayerAdjustment = onUpdatePrayerAdjustment,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Theme
        SettingsHubCategoryCard(
            title = strings.themeSection,
            subtitle = strings.themeModeName(settings.themeMode),
            icon = Icons.Default.Palette,
            testTag = "settings_cat_theme",
            onClick = { onNavigateTo(SettingsSubScreen.THEME) }
        )

        // Widgets (التطبيقات المصغرة)
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

        // Calculation
        SettingsHubCategoryCard(
            title = strings.calcMethodSection,
            subtitle = "${strings.calcMethodName(settings.calculationMethod)} • ${if (settings.juristicMethod == JuristicMethod.STANDARD) strings.standardJuristic else strings.hanafiJuristic}",
            icon = Icons.Default.Calculate,
            testTag = "settings_cat_calc",
            onClick = { onNavigateTo(SettingsSubScreen.CALCULATION) }
        )

        // Location
        val locationHubName = com.prayertimes.data.cities.CityDatabase.localizedName(LocalContext.current.resources, settings.location)
        val locationHubCountry = com.prayertimes.data.cities.CityDatabase.localizedCountry(LocalContext.current.resources, settings.location)
        SettingsHubCategoryCard(
            title = strings.locationSection,
            subtitle = "$locationHubName${if (locationHubCountry.isNotEmpty()) ", $locationHubCountry" else ""} ${if (settings.location.isGps) "(GPS)" else ""}",
            icon = Icons.Default.LocationOn,
            testTag = "settings_cat_location",
            onClick = { onNavigateTo(SettingsSubScreen.LOCATION) }
        )

        // Notifications
        val enabledNotifsCount = settings.prayerConfigs.values.count { it.enabled }
        SettingsHubCategoryCard(
            title = strings.notifSectionTitle,
            subtitle = strings.notifCountActive(enabledNotifsCount),
            icon = Icons.Default.NotificationsActive,
            testTag = "settings_cat_notifications",
            onClick = { onNavigateTo(SettingsSubScreen.NOTIFICATIONS) }
        )

        // Language
        SettingsHubCategoryCard(
            title = strings.languageSection,
            subtitle = settings.language.getDisplayName(strings.isArabic),
            icon = Icons.Default.Language,
            testTag = "settings_cat_language",
            onClick = { onNavigateTo(SettingsSubScreen.LANGUAGE) }
        )

        // Hijri & Display
        SettingsHubCategoryCard(
            title = strings.displayHijriSection,
            subtitle = "${if (settings.is24HourFormat) "24-Hour" else "12-Hour"} • ${if (settings.hijriAdjustmentDays == 0) "0 " + strings.days else (if (settings.hijriAdjustmentDays > 0) "+" else "") + settings.hijriAdjustmentDays + " " + strings.days}",
            icon = Icons.Default.CalendarMonth,
            testTag = "settings_cat_hijri",
            onClick = { onNavigateTo(SettingsSubScreen.HIJRI_DISPLAY) }
        )

        // Adjustments
        val hasAdjustments = settings.adjustments.let { it.fajr != 0 || it.sunrise != 0 || it.dhuhr != 0 || it.asr != 0 || it.maghrib != 0 || it.isha != 0 }
        SettingsHubCategoryCard(
            title = strings.minuteAdjustmentsTitle,
            subtitle = if (hasAdjustments) strings.adjustmentsCustomActive else strings.adjustmentsStandard,
            icon = Icons.Default.Tune,
            testTag = "settings_cat_adjustments",
            onClick = { onNavigateTo(SettingsSubScreen.ADJUSTMENTS) }
        )

        // About
        SettingsHubCategoryCard(
            title = strings.aboutHubTitle,
            subtitle = strings.aboutHubSubtitle,
            icon = Icons.Default.Info,
            testTag = "settings_cat_about",
            onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
        )

        // Re-run Setup Wizard
        SettingsHubCategoryCard(
            title = strings.rerunSetupTitle,
            subtitle = strings.rerunSetupSubtitle,
            icon = Icons.Default.AutoAwesome,
            testTag = "settings_cat_rerun_setup",
            onClick = onResetOnboarding
        )

        Spacer(modifier = Modifier.height(40.dp))
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
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
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
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

// 1. THEME
@Composable
private fun SettingsThemeSubScreen(
    settings: AppPrayerSettings,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateColorPreset: (com.prayertimes.data.models.AppColorPreset) -> Unit,
    onUpdateFollowSystemColors: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("theme_subscreen")
    ) {
        SubScreenHeader(title = strings.themeSection, onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.colorPaletteSection,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Follow System Colors Card (App Theming)
            Card(
                onClick = { onUpdateFollowSystemColors(!settings.followSystemColors) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.followSystemColors) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                border = if (settings.followSystemColors) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
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
                                .background(if (settings.followSystemColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (settings.followSystemColors) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = strings.followSystemColorsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.followSystemColorsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.followSystemColors,
                        onCheckedChange = { onUpdateFollowSystemColors(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.presetColorsTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preset Colors list
            com.prayertimes.data.models.AppColorPreset.values().filter { it != com.prayertimes.data.models.AppColorPreset.SYSTEM_DYNAMIC }.forEach { preset ->
                val isSelected = !settings.followSystemColors && settings.colorPreset == preset
                val isDark = settings.themeMode == AppThemeMode.DARK || (settings.themeMode == AppThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
                val primaryColorLong = if (isDark) preset.primaryDark else preset.primaryLight
                val secondaryColorLong = if (isDark) preset.secondaryDark else preset.secondaryLight

                Card(
                    onClick = {
                        if (settings.followSystemColors) {
                            onUpdateFollowSystemColors(false)
                        }
                        onUpdateColorPreset(preset)
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Dual Color Preview Swatch
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(primaryColorLong)),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(androidx.compose.ui.graphics.Color(secondaryColorLong))
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = strings.colorPresetName(preset),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.colorPresetName(preset),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (settings.followSystemColors) {
                                    onUpdateFollowSystemColors(false)
                                }
                                onUpdateColorPreset(preset)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
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
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
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
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("calc_subscreen")
    ) {
        SubScreenHeader(title = strings.calcMethodSection, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = strings.juristicMethodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
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
                                onClick = { onUpdateJuristicMethod(JuristicMethod.STANDARD) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
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
                                onClick = { onUpdateJuristicMethod(JuristicMethod.HANAFI) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.calcMethodSection,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(CalculationMethod.values()) { method ->
                val isSelected = settings.calculationMethod == method
                Card(
                    onClick = { onUpdateCalculationMethod(method) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
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
                                text = strings.calcMethodName(method),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val ishaDesc = if ((method.ishaMinutesAfterMaghrib ?: 0) > 0) "${method.ishaMinutesAfterMaghrib}m" else "${method.ishaAngle}°"
                            Text(
                                text = "${strings.prayerName(PrayerType.FAJR)}: ${method.fajrAngle}° • ${strings.prayerName(PrayerType.ISHA)}: $ishaDesc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onUpdateCalculationMethod(method) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
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

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
        } else {
            delay(200)
            searchResults = PlaceRepository.search(context, searchQuery)
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
            shape = RoundedCornerShape(20.dp),
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

        Spacer(modifier = Modifier.height(14.dp))

        // GPS Action Button
        Card(
            shape = RoundedCornerShape(18.dp),
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
                    shape = RoundedCornerShape(12.dp)
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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.locationSearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isNotBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                        shape = RoundedCornerShape(14.dp),
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
    onPreviewFullScreenAlarm: (PrayerType) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = LocalAppStrings.current
    var activeSoundDialogPrayer by remember { mutableStateOf<PrayerType?>(null) }
    var testAlarmScheduledText by remember { mutableStateOf<String?>(null) }
    var previewPrayerArtwork by remember { mutableStateOf(PrayerType.FAJR) }

    val notificationsEnabled = remember {
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification Permission Status Banner
            item {
                if (!notificationsEnabled) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
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
                                shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(20.dp),
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
                                shape = RoundedCornerShape(14.dp),
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
                                        onClick = { onUpdateAudioStream(stream) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Wake Screen & Full-Screen Alarm Artwork Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                PrayerType.FAJR,
                                PrayerType.SUNRISE,
                                PrayerType.DHUHR,
                                PrayerType.ASR,
                                PrayerType.MAGHRIB,
                                PrayerType.ISHA
                            ).forEach { p ->
                                val isSelected = previewPrayerArtwork == p
                                FilledTonalButton(
                                    onClick = { previewPrayerArtwork = p },
                                    shape = RoundedCornerShape(10.dp),
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onPreviewFullScreenAlarm(previewPrayerArtwork) },
                            shape = RoundedCornerShape(12.dp),
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
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(5, 10, 15, 20, 25, 30).forEach { mins ->
                                    val isSelected = settings.liveCountdownMinutesBefore == mins
                                    FilledTonalButton(
                                        onClick = { onUpdateLiveCountdownSettings(true, mins) },
                                        shape = RoundedCornerShape(10.dp),
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
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onTestLiveCountdown,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.live_countdown_test_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(PrayerType.values()) { prayer ->
                val cfg = settings.prayerConfigs[prayer] ?: NotificationPrayerConfig()
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                onCheckedChange = { isChecked ->
                                    onUpdateNotificationConfig(prayer, isChecked, cfg.soundType, cfg.preReminderMinutes)
                                }
                            )
                        }

                        if (cfg.enabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
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
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(strings.change, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))

                // Test instant notification
                Button(
                    onClick = { onTestNotification(PrayerType.DHUHR, NotificationSoundType.FULL_ATHAN) },
                    shape = RoundedCornerShape(14.dp),
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
                        testAlarmScheduledText = "Alarm scheduled in 5 seconds! Lock screen or leave app to test."
                    },
                    shape = RoundedCornerShape(14.dp),
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

                Spacer(modifier = Modifier.height(8.dp))

                // Reschedule 7-Day Alarms
                FilledTonalButton(
                    onClick = {
                        onRescheduleAlarms()
                        testAlarmScheduledText = "All 7-day prayer alarms synced and scheduled in AlarmManager!"
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.reschedule7Days, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// 5. LANGUAGE
@Composable
private fun SettingsLanguageSubScreen(
    settings: AppPrayerSettings,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("language_subscreen")
    ) {
        SubScreenHeader(title = strings.languageSection, onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(AppLanguage.values()) { lang ->
                val isSelected = settings.language == lang
                Card(
                    onClick = { onUpdateLanguage(lang) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lang.getDisplayName(strings.isArabic),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onUpdateLanguage(lang) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

// 6. HIJRI & DISPLAY
@Composable
private fun SettingsHijriDisplaySubScreen(
    settings: AppPrayerSettings,
    onUpdateHijriOffset: (Int) -> Unit,
    onToggle24Hour: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("hijri_subscreen")
    ) {
        SubScreenHeader(title = strings.displayHijriSection, onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 24-Hour Switch Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            text = if (settings.is24HourFormat) "13:30 (24-Hour)" else "1:30 PM (12-Hour AM/PM)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.is24HourFormat,
                        onCheckedChange = { onToggle24Hour() }
                    )
                }
            }

            // Hijri Offset Stepper Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.hijriOffsetTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.hijriOffsetDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onUpdateHijriOffset(settings.hijriAdjustmentDays - 1) },
                            modifier = Modifier.size(44.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Text(
                            text = "${if (settings.hijriAdjustmentDays > 0) "+" else ""}${settings.hijriAdjustmentDays} ${strings.days}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        FilledTonalButton(
                            onClick = { onUpdateHijriOffset(settings.hijriAdjustmentDays + 1) },
                            modifier = Modifier.size(44.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Umm al Qura info card
            Card(
                shape = RoundedCornerShape(18.dp),
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
}

// 7. ADJUSTMENTS
@Composable
private fun SettingsAdjustmentsSubScreen(
    settings: AppPrayerSettings,
    onUpdatePrayerAdjustment: (PrayerType, Int) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("adjustments_subscreen")
    ) {
        SubScreenHeader(title = strings.minuteAdjustmentsTitle, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PrayerType.values()) { prayer ->
                val currentOffset = when (prayer) {
                    PrayerType.FAJR -> settings.adjustments.fajr
                    PrayerType.SUNRISE -> settings.adjustments.sunrise
                    PrayerType.DHUHR -> settings.adjustments.dhuhr
                    PrayerType.ASR -> settings.adjustments.asr
                    PrayerType.MAGHRIB -> settings.adjustments.maghrib
                    PrayerType.ISHA -> settings.adjustments.isha
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            text = strings.prayerName(prayer),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { onUpdatePrayerAdjustment(prayer, currentOffset - 1) },
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "${if (currentOffset > 0) "+" else ""}${currentOffset}m",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            FilledTonalButton(
                                onClick = { onUpdatePrayerAdjustment(prayer, currentOffset + 1) },
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        PrayerType.values().forEach { onUpdatePrayerAdjustment(it, 0) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.resetAllAdjustments)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
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
                        text = strings.versionEngineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.compassCalibTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.compassCalibText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
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

    Dialog(onDismissRequest = {
        AthanAudioEngine.stop()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
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
                    items(NotificationSoundType.values()) { sound ->
                        val isSelected = sound == currentSound
                        Card(
                            onClick = { onSelectSound(sound) },
                            shape = RoundedCornerShape(14.dp),
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
                                        onClick = { onSelectSound(sound) }
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
                                            if (playbackState.isPlaying) {
                                                AthanAudioEngine.stop()
                                            } else {
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
                    TextButton(onClick = onDismiss) {
                        Text(strings.close)
                    }
                }
            }
        }
    }
}
