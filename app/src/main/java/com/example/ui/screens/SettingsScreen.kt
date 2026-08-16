package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.cities.CityDatabase
import com.example.data.models.AppLanguage
import com.example.data.models.AppThemeMode
import com.example.data.models.CalculationMethod
import com.example.data.models.HighLatitudeRule
import com.example.data.models.JuristicMethod
import com.example.data.models.NotificationPrayerConfig
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import com.example.data.models.UserLocation
import com.example.data.preferences.AppPrayerSettings
import com.example.ui.components.ManualCoordinatesDialog
import com.example.ui.locale.LocalAppStrings

enum class SettingsSubScreen {
    MAIN,
    THEME,
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
    onUpdatePrayerAdjustment: (PrayerType, Int) -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onTestNotification: (PrayerType, NotificationSoundType) -> Unit,
    onPreviewSound: (NotificationSoundType) -> Unit,
    onUpdateDynamicIslandSettings: (Boolean, Int) -> Unit = { _, _ -> },
    onPreviewDynamicIsland: () -> Unit = {},
    onDismissDynamicIsland: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

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
                    onNavigateTo = { currentSubScreen = it }
                )
            }
            SettingsSubScreen.THEME -> {
                SettingsThemeSubScreen(
                    settings = settings,
                    onUpdateThemeMode = onUpdateThemeMode,
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
                    onPreviewSound = onPreviewSound,
                    onUpdateDynamicIslandSettings = onUpdateDynamicIslandSettings,
                    onPreviewDynamicIsland = onPreviewDynamicIsland,
                    onDismissDynamicIsland = onDismissDynamicIsland,
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
    onNavigateTo: (SettingsSubScreen) -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme
        SettingsHubCategoryCard(
            title = strings.themeSection,
            subtitle = strings.themeModeName(settings.themeMode),
            icon = Icons.Default.Palette,
            badgeColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.primary,
            testTag = "settings_cat_theme",
            onClick = { onNavigateTo(SettingsSubScreen.THEME) }
        )

        // Calculation
        SettingsHubCategoryCard(
            title = strings.calcMethodSection,
            subtitle = "${strings.calcMethodName(settings.calculationMethod)} • ${if (settings.juristicMethod == JuristicMethod.STANDARD) strings.standardJuristic else strings.hanafiJuristic}",
            icon = Icons.Default.Calculate,
            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.secondary,
            testTag = "settings_cat_calc",
            onClick = { onNavigateTo(SettingsSubScreen.CALCULATION) }
        )

        // Location
        SettingsHubCategoryCard(
            title = strings.locationSection,
            subtitle = "${settings.location.name}${if (settings.location.country.isNotEmpty()) ", " + settings.location.country else ""} ${if (settings.location.isGps) "(GPS)" else ""}",
            icon = Icons.Default.LocationOn,
            badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.tertiary,
            testTag = "settings_cat_location",
            onClick = { onNavigateTo(SettingsSubScreen.LOCATION) }
        )

        // Notifications
        val enabledNotifsCount = settings.prayerConfigs.values.count { it.enabled }
        SettingsHubCategoryCard(
            title = strings.notifSectionTitle,
            subtitle = if (strings.isArabic) "$enabledNotifsCount صلوات مفعلة" else "$enabledNotifsCount prayers active",
            icon = Icons.Default.NotificationsActive,
            badgeColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.primary,
            testTag = "settings_cat_notifications",
            onClick = { onNavigateTo(SettingsSubScreen.NOTIFICATIONS) }
        )

        // Language
        SettingsHubCategoryCard(
            title = strings.languageSection,
            subtitle = settings.language.getDisplayName(strings.isArabic),
            icon = Icons.Default.Language,
            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.secondary,
            testTag = "settings_cat_language",
            onClick = { onNavigateTo(SettingsSubScreen.LANGUAGE) }
        )

        // Hijri & Display
        SettingsHubCategoryCard(
            title = strings.displayHijriSection,
            subtitle = "${if (settings.is24HourFormat) "24-Hour" else "12-Hour"} • ${if (settings.hijriAdjustmentDays == 0) "0 " + strings.days else (if (settings.hijriAdjustmentDays > 0) "+" else "") + settings.hijriAdjustmentDays + " " + strings.days}",
            icon = Icons.Default.CalendarMonth,
            badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.tertiary,
            testTag = "settings_cat_hijri",
            onClick = { onNavigateTo(SettingsSubScreen.HIJRI_DISPLAY) }
        )

        // Adjustments
        val hasAdjustments = settings.adjustments.let { it.fajr != 0 || it.sunrise != 0 || it.dhuhr != 0 || it.asr != 0 || it.maghrib != 0 || it.isha != 0 }
        SettingsHubCategoryCard(
            title = strings.minuteAdjustmentsTitle,
            subtitle = if (hasAdjustments) (if (strings.isArabic) "تعديلات مخصصة مفعلة" else "Custom offsets active") else (if (strings.isArabic) "افتراضي (0 دقيقة)" else "Standard (0 min)"),
            icon = Icons.Default.Tune,
            badgeColor = MaterialTheme.colorScheme.surfaceVariant,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            testTag = "settings_cat_adjustments",
            onClick = { onNavigateTo(SettingsSubScreen.ADJUSTMENTS) }
        )

        // About
        SettingsHubCategoryCard(
            title = if (strings.isArabic) "حول التطبيق والحسابات الفلكية" else "About & Calculation Info",
            subtitle = if (strings.isArabic) "حسابات فلكية بدون إنترنت • الإصدار 1.0" else "Offline Astronomical Algorithms • v1.0",
            icon = Icons.Default.Info,
            badgeColor = MaterialTheme.colorScheme.surfaceVariant,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            testTag = "settings_cat_about",
            onClick = { onNavigateTo(SettingsSubScreen.ABOUT) }
        )

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
private fun SettingsHubCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    iconTint: Color,
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
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
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_back_button")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
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
                                    text = if (strings.isArabic) "ظل الشيء مثله (الشافعي، المالكي، الحنبلي)" else "Shadow equals object length (Standard)",
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
                                    text = if (strings.isArabic) "ظل الشيء مثليه (المذهب الحنفي)" else "Shadow equals twice object length (Hanafi)",
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
                                text = "Fajr: ${method.fajrAngle}° • Isha: $ishaDesc",
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
    var showManualDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showManualDialog) {
        ManualCoordinatesDialog(
            currentLocation = settings.location,
            onSaveLocation = {
                onSelectCity(it)
                showManualDialog = false
            },
            onDismiss = { showManualDialog = false }
        )
    }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) CityDatabase.popularCities
        else CityDatabase.searchCities(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("location_subscreen")
    ) {
        SubScreenHeader(title = strings.locationSection, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

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
                        text = if (settings.location.isGps) "Current: ${settings.location.name}" else "Tap to detect automatically",
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
                        Text(text = "GPS")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { showManualDialog = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = strings.manualCoordinates, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.search + " (London, Cairo, Makkah...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCities) { city ->
                val isSelected = settings.location.name.equals(city.name, ignoreCase = true)
                Card(
                    onClick = { onSelectCity(city) },
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
                                text = city.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${city.country} • ${city.timeZoneId}",
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

// 4. NOTIFICATIONS
@Composable
private fun SettingsNotificationsSubScreen(
    settings: AppPrayerSettings,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onTestNotification: (PrayerType, NotificationSoundType) -> Unit,
    onPreviewSound: (NotificationSoundType) -> Unit,
    onUpdateDynamicIslandSettings: (Boolean, Int) -> Unit,
    onPreviewDynamicIsland: () -> Unit,
    onDismissDynamicIsland: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    var activeSoundDialogPrayer by remember { mutableStateOf<PrayerType?>(null) }

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
            onPreviewSound = onPreviewSound,
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
            // Dynamic Island / Live Activity Feature Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🏝️ " + strings.dynamicIslandSectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.dynamicIslandDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = settings.dynamicIslandEnabled,
                                onCheckedChange = { isChecked ->
                                    onUpdateDynamicIslandSettings(isChecked, settings.dynamicIslandMinutesBefore)
                                }
                            )
                        }

                        if (settings.dynamicIslandEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = strings.dynamicIslandLeadTimeTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(5, 10, 15, 30).forEach { mins ->
                                    val isSelected = settings.dynamicIslandMinutesBefore == mins
                                    FilledTonalButton(
                                        onClick = { onUpdateDynamicIslandSettings(true, mins) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "$mins m",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onPreviewDynamicIsland,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = strings.previewDynamicIslandBtn,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = onDismissDynamicIsland,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = strings.dismissDynamicIslandBtn,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
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
                Button(
                    onClick = { onTestNotification(PrayerType.DHUHR, NotificationSoundType.FULL_ATHAN) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.testAlert, fontWeight = FontWeight.Bold)
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
                        text = if (strings.isArabic) "مطابقة رؤية الهلال مع الجهة الشرعية المحلية" else "Sync with official local moon sighting declarations",
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
                    Text(if (strings.isArabic) "إعادة ضبط جميع الصلوات (0 دقيقة)" else "Reset All Adjustments to 0")
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
            title = if (strings.isArabic) "حول التطبيق والحسابات" else "About & Precision Engine",
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
                        text = "Version 1.0.0 • 100% Offline Precision Calculation Engine",
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
                        text = if (strings.isArabic) "الخوارزميات الفلكية المعتمدة" else "Astronomical Calculation Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (strings.isArabic) "يعتمد التطبيق خوارزميات جان ميوس (Jean Meeus) الفلكية لحساب موقع الشمس، زاوية الميل الشمسي، ومعادلة الزمن بدقة متناهية متوافقة مع معايير رابطة العالم الإسلامي وتقويم أم القرى."
                               else "Calculates exact solar coordinates, solar declination angle, and equation of time using Jean Meeus astronomical algorithms, supporting major world Islamic authorities with zero network dependencies.",
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

    Dialog(onDismissRequest = onDismiss) {
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
                                    Text(
                                        text = strings.soundTypeName(sound),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                if (sound != NotificationSoundType.SILENT && sound != NotificationSoundType.VIBRATE_ONLY) {
                                    IconButton(
                                        onClick = { onPreviewSound(sound) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Preview",
                                            tint = MaterialTheme.colorScheme.primary
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
