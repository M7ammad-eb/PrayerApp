package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.cities.CityDatabase
import com.example.data.models.AppLanguage
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
import com.example.ui.theme.GoldAccent

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
    onUpdatePrayerAdjustment: (PrayerType, Int) -> Unit,
    onUpdateNotificationConfig: (PrayerType, Boolean, NotificationSoundType, Int) -> Unit,
    onTestNotification: (PrayerType, NotificationSoundType) -> Unit,
    onPreviewSound: (NotificationSoundType) -> Unit
) {
    val strings = LocalAppStrings.current
    var showCityPicker by remember { mutableStateOf(false) }
    var showManualCoordinatesDialog by remember { mutableStateOf(false) }
    var showCalcMethodPicker by remember { mutableStateOf(false) }
    var showAdjustmentsDialog by remember { mutableStateOf(false) }
    var soundPickerPrayerType by remember { mutableStateOf<PrayerType?>(null) }

    if (soundPickerPrayerType != null) {
        val targetPrayer = soundPickerPrayerType!!
        val currentCfg = settings.prayerConfigs[targetPrayer] ?: NotificationPrayerConfig()
        SoundPickerDialog(
            prayerType = targetPrayer,
            currentSound = currentCfg.soundType,
            onSelectSound = { newSound ->
                onUpdateNotificationConfig(targetPrayer, currentCfg.enabled, newSound, currentCfg.preReminderMinutes)
                soundPickerPrayerType = null
            },
            onPreviewSound = onPreviewSound,
            onDismiss = { soundPickerPrayerType = null }
        )
    }

    if (showCityPicker) {
        CityPickerDialog(
            onSelectCity = {
                onSelectCity(it)
                showCityPicker = false
            },
            onOpenManualCoordinates = {
                showCityPicker = false
                showManualCoordinatesDialog = true
            },
            onDismiss = { showCityPicker = false }
        )
    }

    if (showManualCoordinatesDialog) {
        ManualCoordinatesDialog(
            currentLocation = settings.location,
            onSaveLocation = { newLocation ->
                onSelectCity(newLocation)
                showManualCoordinatesDialog = false
            },
            onDismiss = { showManualCoordinatesDialog = false }
        )
    }

    if (showCalcMethodPicker) {
        CalculationMethodPickerDialog(
            currentMethod = settings.calculationMethod,
            onSelectMethod = {
                onUpdateCalculationMethod(it)
                showCalcMethodPicker = false
            },
            onDismiss = { showCalcMethodPicker = false }
        )
    }

    if (showAdjustmentsDialog) {
        ManualAdjustmentsDialog(
            settings = settings,
            onUpdateAdjustment = onUpdatePrayerAdjustment,
            onDismiss = { showAdjustmentsDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 0. Language & Localization Section
        SettingsSectionHeader(title = strings.languageSection, icon = Icons.Default.Language)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("language_settings_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.appLanguage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = settings.language == lang
                        Surface(
                            onClick = { onUpdateLanguage(lang) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.weight(1f).testTag("lang_opt_${lang.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (strings.isArabic) lang.displayNameAr else lang.displayNameEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = when (lang) {
                                        AppLanguage.SYSTEM -> if (strings.isArabic) "تلقائي (123)" else "Default (123)"
                                        AppLanguage.ENGLISH -> "English (123)"
                                        AppLanguage.ARABIC -> "العربية (123)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Location Settings Section
        SettingsSectionHeader(title = strings.locationSection, icon = Icons.Default.LocationOn)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = settings.location.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (settings.location.country.isNotEmpty()) settings.location.country + " • " else ""}Lat: ${String.format("%.2f", settings.location.latitude)}, Lon: ${String.format("%.2f", settings.location.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (settings.location.isGps) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("GPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestGps,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("use_gps_button"),
                        enabled = !isGpsLoading
                    ) {
                        if (isGpsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use GPS", maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = { showCityPicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choose_city_button")
                    ) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("World Cities", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = { showManualCoordinatesDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_coordinates_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.manualCoordinates, maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 2. Calculation Methods Section
        SettingsSectionHeader(title = strings.calcMethodSection, icon = Icons.Default.Calculate)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Method Selector Clickable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCalcMethodPicker = true }
                        .padding(vertical = 8.dp)
                        .testTag("calculation_method_selector"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.calculationMethod.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = settings.calculationMethod.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Juristic Method (Asr Shadow)
                Text(
                    text = strings.juristicMethodTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    JuristicMethod.values().forEach { juristic ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateJuristicMethod(juristic) }
                        ) {
                            RadioButton(
                                selected = settings.juristicMethod == juristic,
                                onClick = { onUpdateJuristicMethod(juristic) }
                            )
                            Text(
                                text = if (juristic == JuristicMethod.STANDARD) strings.standardJuristic else strings.hanafiJuristic,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // High Latitude Rule Selector
                Text(
                    text = strings.highLatitudeSection,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                HighLatitudeRule.values().forEach { rule ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateHighLatitudeRule(rule) }
                    ) {
                        RadioButton(
                            selected = settings.highLatitudeRule == rule,
                            onClick = { onUpdateHighLatitudeRule(rule) }
                        )
                        Text(text = rule.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 3. Calendar & Time Display
        SettingsSectionHeader(title = if (strings.isArabic) "العرض والتقويم الهجري" else "Display & Hijri Adjustments", icon = Icons.Default.FormatListNumbered)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 24-Hour Time Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = strings.timeFormatTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = if (settings.is24HourFormat) "13:30" else "1:30 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.is24HourFormat,
                        onCheckedChange = { onToggle24Hour() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Hijri Date Adjustment (Umm al-Qura)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.hijriOffsetTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (strings.isArabic) "المعيار: أم القرى (مكة) • التعديل: ${if (settings.hijriAdjustmentDays > 0) "+" else ""}${settings.hijriAdjustmentDays} ${strings.days}" else "Standard: Umm al-Qura (Makkah) • Offset: ${if (settings.hijriAdjustmentDays > 0) "+" else ""}${settings.hijriAdjustmentDays} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(-2, -1, 0, 1, 2).forEach { offset ->
                            FilledTonalButton(
                                onClick = { onUpdateHijriOffset(offset) },
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (offset > 0) "+$offset" else "$offset",
                                    fontSize = 11.sp,
                                    fontWeight = if (settings.hijriAdjustmentDays == offset) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Manual Mosque Time Adjustments Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdjustmentsDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = strings.minuteAdjustmentsTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = strings.minuteAdjustmentsSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        // 4. Notifications & Athan Per Prayer
        SettingsSectionHeader(title = strings.notifSectionTitle, icon = Icons.Default.NotificationsActive)
        PrayerType.values().forEach { prayerType ->
            val config = settings.prayerConfigs[prayerType] ?: NotificationPrayerConfig()
            PrayerNotificationConfigCard(
                prayerType = prayerType,
                config = config,
                onUpdate = { enabled, sound, reminder ->
                    onUpdateNotificationConfig(prayerType, enabled, sound, reminder)
                },
                onOpenSoundPicker = { soundPickerPrayerType = prayerType },
                onTest = { onTestNotification(prayerType, config.soundType) },
                onPreviewSound = { onPreviewSound(config.soundType) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PrayerNotificationConfigCard(
    prayerType: PrayerType,
    config: NotificationPrayerConfig,
    onUpdate: (Boolean, NotificationSoundType, Int) -> Unit,
    onOpenSoundPicker: () -> Unit,
    onTest: () -> Unit,
    onPreviewSound: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_config_${prayerType.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Prayer Name & Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prayerType.title.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "${prayerType.title} (${prayerType.arabicName})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (config.enabled) "${config.soundType.displayName}${if (config.preReminderMinutes > 0) " + ${config.preReminderMinutes}m reminder" else ""}" else "Notifications Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (config.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onUpdate(it, config.soundType, config.preReminderMinutes) }
                )
            }

            if (config.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Sound Selector Button
                Surface(
                    onClick = onOpenSoundPicker,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().testTag("sound_picker_trigger_${prayerType.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (config.soundType == NotificationSoundType.SILENT) Icons.Default.Notifications
                                else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = config.soundType.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (config.soundType.subtitle.isNotBlank()) {
                                    Text(
                                        text = config.soundType.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change sound",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pre-reminder & Test Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Pre-reminder: ", style = MaterialTheme.typography.bodySmall)
                        listOf(0, 10, 15).forEach { mins ->
                            val isSel = config.preReminderMinutes == mins
                            Text(
                                text = if (mins == 0) "None" else "${mins}m",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onUpdate(config.enabled, config.soundType, mins) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onPreviewSound, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Preview Sound", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onTest, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Test Notification", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityPickerDialog(
    onSelectCity: (UserLocation) -> Unit,
    onOpenManualCoordinates: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) CityDatabase.PRESET_CITIES
        else CityDatabase.PRESET_CITIES.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.country.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("city_picker_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Offline World City",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city or country...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Coordinates shortcut button in dialog
                FilledTonalButton(
                    onClick = onOpenManualCoordinates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_manual_coordinates_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Can't find city? Enter Lat / Lon", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCities) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCity(city) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = city.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(text = city.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(imageVector = Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CalculationMethodPickerDialog(
    currentMethod: CalculationMethod,
    onSelectMethod: (CalculationMethod) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("calculation_method_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calculation Methods",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(CalculationMethod.values()) { method ->
                        val isSelected = method == currentMethod
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectMethod(method) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = method.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = method.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ManualAdjustmentsDialog(
    settings: AppPrayerSettings,
    onUpdateAdjustment: (PrayerType, Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("manual_adjustments_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Manual Minute Adjustments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Adjust calculated times (-30 to +30 min) to match your local mosque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrayerType.values().forEach { prayer ->
                    val currentVal = when (prayer) {
                        PrayerType.FAJR -> settings.adjustments.fajr
                        PrayerType.SUNRISE -> settings.adjustments.sunrise
                        PrayerType.DHUHR -> settings.adjustments.dhuhr
                        PrayerType.ASR -> settings.adjustments.asr
                        PrayerType.MAGHRIB -> settings.adjustments.maghrib
                        PrayerType.ISHA -> settings.adjustments.isha
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = prayer.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { onUpdateAdjustment(prayer, currentVal - 1) },
                                modifier = Modifier.size(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-")
                            }
                            Text(
                                text = "${if (currentVal > 0) "+" else ""}$currentVal m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            FilledTonalButton(
                                onClick = { onUpdateAdjustment(prayer, currentVal + 1) },
                                modifier = Modifier.size(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun SoundPickerDialog(
    prayerType: PrayerType,
    currentSound: NotificationSoundType,
    onSelectSound: (NotificationSoundType) -> Unit,
    onPreviewSound: (NotificationSoundType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("sound_picker_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "${prayerType.title} Alert Sound",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Select custom Athan style, melodic chime, or vibrate alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category: Full Athan Chants
                    item {
                        Text(
                            text = "GRAND MOSQUES ADHAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val athanList = listOf(
                        NotificationSoundType.FULL_ATHAN,
                        NotificationSoundType.ATHAN_MADINAH,
                        NotificationSoundType.ATHAN_AL_AQSA,
                        NotificationSoundType.ATHAN_CAIRO
                    )

                    items(athanList) { sound ->
                        SoundOptionRow(
                            sound = sound,
                            isSelected = sound == currentSound,
                            onSelect = { onSelectSound(sound) },
                            onPreview = { onPreviewSound(sound) }
                        )
                    }

                    // Category: Short Alerts & Chimes
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SHORT ALERTS & CHIMES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val shortList = listOf(
                        NotificationSoundType.SHORT_TAKBEER,
                        NotificationSoundType.MELODIC_TONE
                    )

                    items(shortList) { sound ->
                        SoundOptionRow(
                            sound = sound,
                            isSelected = sound == currentSound,
                            onSelect = { onSelectSound(sound) },
                            onPreview = { onPreviewSound(sound) }
                        )
                    }

                    // Category: Haptics & Silent
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HAPTIC & SILENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val silentList = listOf(
                        NotificationSoundType.VIBRATE_ONLY,
                        NotificationSoundType.SILENT
                    )

                    items(silentList) { sound ->
                        SoundOptionRow(
                            sound = sound,
                            isSelected = sound == currentSound,
                            onSelect = { onSelectSound(sound) },
                            onPreview = { onPreviewSound(sound) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun SoundOptionRow(
    sound: NotificationSoundType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().testTag("sound_option_${sound.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = sound.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (sound.subtitle.isNotBlank()) {
                        Text(
                            text = sound.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sound != NotificationSoundType.SILENT) {
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier.size(36.dp).testTag("preview_btn_${sound.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Preview ${sound.displayName}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
