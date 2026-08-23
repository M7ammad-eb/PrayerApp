package com.prayertimes.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertimes.data.models.WidgetBackgroundStyle
import com.prayertimes.data.models.WidgetCustomizationSettings
import com.prayertimes.data.models.WidgetHeroTimeMode
import com.prayertimes.data.models.WidgetTextStyle
import com.prayertimes.data.models.WidgetThemeMode
import android.content.res.Resources
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.locale.LocalAppStrings
import com.prayertimes.util.LocalizedStrings
import com.prayertimes.R

enum class WidgetPreviewType(@androidx.annotation.StringRes val labelRes: Int) {
    STANDARD_4X2(R.string.widget_preview_type_standard_4x2),
    EXPANDED_MAX(R.string.widget_preview_type_expanded_max),
    VERTICAL_1_COL(R.string.widget_preview_type_vertical_1col),
    SLIM_BAR(R.string.widget_preview_type_slim_bar),
    COMPACT_2X1(R.string.widget_preview_type_compact_2x1)
}

@Composable
fun SettingsWidgetSubScreen(
    settings: AppPrayerSettings,
    onUpdateWidgetSettings: (WidgetCustomizationSettings) -> Unit,
    onRefreshAllWidgets: () -> Unit = {},
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val wSet = settings.widgetSettings
    var selectedPreviewType by remember { mutableStateOf(WidgetPreviewType.STANDARD_4X2) }
    var showAppliedNotice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("widget_settings_subscreen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("widget_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.widgetsSection,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = {
                    onUpdateWidgetSettings(WidgetCustomizationSettings())
                    onRefreshAllWidgets()
                },
                modifier = Modifier.testTag("widget_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = strings.widgetResetDefaults,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.StayCurrentPortrait,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.widget_settings_intro_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Live Preview Card Header
            Text(
                text = strings.widgetPreviewTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Preview Type Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WidgetPreviewType.values().forEach { type ->
                    FilterChip(
                        selected = selectedPreviewType == type,
                        onClick = { selectedPreviewType = type },
                        label = {
                            Text(
                                text = stringResource(type.labelRes),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Live Widget Render Box
            WidgetCanvasPreview(
                settings = settings,
                widgetSettings = wSet,
                previewType = selectedPreviewType,
                isArabic = strings.isArabic
            )

            // Primary Apply Button
            Button(
                onClick = {
                    onRefreshAllWidgets()
                    showAppliedNotice = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_widget_settings_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.widgetRefreshAll,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (showAppliedNotice) {
                Text(
                    text = stringResource(R.string.widget_settings_applied_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ===== Appearance: theme, background, opacity, font size, text color =====
            WidgetSettingsSectionCard(
                icon = Icons.Default.Palette,
                title = strings.widgetAppearanceSectionTitle,
                subtitle = strings.widgetAppearanceSectionSubtitle
            ) {
                WidgetSettingsSubLabel(strings.widgetThemeModeTitle)
                WidgetThemeSelector(
                    currentTheme = wSet.themeMode,
                    onSelectTheme = {
                        onUpdateWidgetSettings(wSet.copy(themeMode = it))
                        onRefreshAllWidgets()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                WidgetSettingsSubLabel(strings.widgetBgStyleTitle)
                WidgetBackgroundStyleSelector(
                    currentStyle = wSet.bgStyle,
                    onSelectStyle = {
                        onUpdateWidgetSettings(wSet.copy(bgStyle = it))
                        onRefreshAllWidgets()
                    }
                )

                // Opacity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.widgetOpacityTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${wSet.opacityPercent}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Slider(
                        value = wSet.opacityPercent.toFloat(),
                        onValueChange = {
                            onUpdateWidgetSettings(wSet.copy(opacityPercent = it.toInt()))
                        },
                        onValueChangeFinished = {
                            onRefreshAllWidgets()
                        },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.widget_settings_opacity_transparent),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.widget_settings_opacity_solid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Text Color Style - a manual escape hatch for when the theme's own text colors
                // can't be trusted for contrast, since a transparent/near-transparent widget has no
                // way to know what's actually behind it (see PrayerAppWidgetProvider.resolveWidgetColors).
                WidgetSettingsSubLabel(stringResource(R.string.widget_settings_text_color_title))
                WidgetTextStyleSelector(
                    currentStyle = wSet.textStyle,
                    onSelectStyle = {
                        onUpdateWidgetSettings(wSet.copy(textStyle = it))
                        onRefreshAllWidgets()
                    }
                )
            }

            // ===== Behavior: hero card timing mode =====
            WidgetSettingsSectionCard(
                icon = Icons.Default.Schedule,
                title = strings.widgetBehaviorSectionTitle,
                subtitle = strings.widgetBehaviorSectionSubtitle
            ) {
                // "In 2h 41m" (next prayer) vs "Since 2h 10m" (current/last prayer). Only one at
                // a time, per user preference.
                WidgetHeroTimeModeSelector(
                    currentMode = wSet.heroTimeMode,
                    onSelectMode = {
                        onUpdateWidgetSettings(wSet.copy(heroTimeMode = it))
                        onRefreshAllWidgets()
                    }
                )
            }

            // ===== Content: what shows on the widget, with dependent toggles dimmed when their
            // parent is off (e.g. Countdown/Progress do nothing while the Hero Card is hidden) =====
            WidgetSettingsSectionCard(
                icon = Icons.Default.Tune,
                title = strings.widgetContentTitle,
                subtitle = strings.widgetContentSectionSubtitle
            ) {
                WidgetToggleRow(
                    icon = Icons.Default.Layers,
                    title = stringResource(R.string.widget_settings_toggle_show_hero),
                    subtitle = strings.widgetToggleShowHeroDesc,
                    checked = wSet.showHeroCard,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showHeroCard = it))
                        onRefreshAllWidgets()
                    }
                )

                Box(modifier = Modifier.padding(start = 28.dp)) {
                    WidgetToggleRow(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.widget_settings_toggle_show_countdown),
                        subtitle = strings.widgetToggleShowCountdownDesc,
                        checked = wSet.showCountdown,
                        enabled = wSet.showHeroCard,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showCountdown = it))
                            onRefreshAllWidgets()
                        }
                    )
                }

                Box(modifier = Modifier.padding(start = 28.dp)) {
                    WidgetToggleRow(
                        icon = Icons.Default.ShowChart,
                        title = stringResource(R.string.widget_settings_toggle_show_progress),
                        subtitle = strings.widgetToggleShowProgressDesc,
                        checked = wSet.showProgressBar,
                        enabled = wSet.showHeroCard,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showProgressBar = it))
                            onRefreshAllWidgets()
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                WidgetToggleRow(
                    icon = Icons.Default.FormatListBulleted,
                    title = stringResource(R.string.widget_settings_toggle_show_all_prayers),
                    subtitle = strings.widgetToggleShowAllPrayersDesc,
                    checked = wSet.showAllPrayersList,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showAllPrayersList = it))
                        onRefreshAllWidgets()
                    }
                )

                Box(modifier = Modifier.padding(start = 28.dp)) {
                    WidgetToggleRow(
                        icon = Icons.Default.WbSunny,
                        title = stringResource(R.string.widget_settings_toggle_show_sunrise),
                        subtitle = strings.widgetToggleShowSunriseDesc,
                        checked = wSet.showSunrise,
                        enabled = wSet.showAllPrayersList,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showSunrise = it))
                            onRefreshAllWidgets()
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                WidgetToggleRow(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.widget_settings_toggle_show_location),
                    subtitle = strings.widgetToggleShowLocationDesc,
                    checked = wSet.showLocation,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showLocation = it))
                        onRefreshAllWidgets()
                    }
                )

                WidgetToggleRow(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.widget_settings_toggle_show_hijri),
                    subtitle = strings.widgetToggleShowHijriDesc,
                    checked = wSet.showHijriDate,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showHijriDate = it))
                        onRefreshAllWidgets()
                    }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// THEME SELECTOR
// ---------------------------------------------------------------------------
@Composable
private fun WidgetThemeSelector(
    currentTheme: WidgetThemeMode,
    onSelectTheme: (WidgetThemeMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WidgetThemeMode.values().forEach { mode ->
            val isSelected = currentTheme == mode
            OutlinedCard(
                onClick = { onSelectTheme(mode) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        if (isSelected) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                        else listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Palette preview circles
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            val (c1, c2, c3) = getThemePreviewColors(mode)
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c1).border(1.dp, Color.White, CircleShape))
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c2).border(1.dp, Color.White, CircleShape))
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c3).border(1.dp, Color.White, CircleShape))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = stringResource(mode.titleRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getThemePreviewColors(theme: WidgetThemeMode): Triple<Color, Color, Color> {
    return when (theme) {
        WidgetThemeMode.APP_THEME -> Triple(Color(0xFF165B33), Color(0xFF81C784), Color(0xFFF1F5F9))
        WidgetThemeMode.MATERIAL_YOU -> {
            val dynamic = resolveSystemDynamicPreviewPalette()
            if (dynamic != null) Triple(dynamic.first, dynamic.third, dynamic.second)
            else Triple(Color(0xFF3F51B5), Color(0xFF9FA8DA), Color(0xFFE8EAF6))
        }
        WidgetThemeMode.DARK_ELEGANT -> Triple(Color(0xFF10B981), Color(0xFF1E293B), Color(0xFF0F172A))
        WidgetThemeMode.LIGHT_CLEAN -> Triple(Color(0xFF059669), Color(0xFFF8FAFC), Color(0xFFE2E8F0))
        WidgetThemeMode.OLED_BLACK -> Triple(Color(0xFF34D399), Color(0xFF18181B), Color(0xFF000000))
        WidgetThemeMode.EMERALD_ISLAMIC -> Triple(Color(0xFF064E3B), Color(0xFFF59E0B), Color(0xFFECFDF5))
        WidgetThemeMode.GOLDEN_HOUR -> Triple(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFFEF3C7))
        WidgetThemeMode.ROYAL_BLUE -> Triple(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0F172A))
        WidgetThemeMode.MONOCHROME -> Triple(Color(0xFFFAFAFA), Color(0xFF71717A), Color(0xFF18181B))
    }
}

// ---------------------------------------------------------------------------
// BACKGROUND STYLE SELECTOR
// ---------------------------------------------------------------------------
@Composable
private fun WidgetBackgroundStyleSelector(
    currentStyle: WidgetBackgroundStyle,
    onSelectStyle: (WidgetBackgroundStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WidgetBackgroundStyle.values().forEach { style ->
            val isSelected = currentStyle == style
            OutlinedCard(
                onClick = { onSelectStyle(style) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        if (isSelected) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                        else listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                    )
                ),
                modifier = Modifier.width(150.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(style.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// OPTION CHIP ROW - shared by Text Color / Hero Time Mode. Labels here can run
// longer than a single line comfortably fits at equal 1/3-1/4 width (e.g. "Both (Wide Widgets)",
// "Extra Large"), so this wraps to two lines with an ellipsis fallback instead of the raw
// mid-word clipping a hard maxLines=1 produces.
// ---------------------------------------------------------------------------
@Composable
private fun <T> WidgetOptionChipRow(
    options: Array<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            FilledTonalButton(
                onClick = { onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(label(option)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TEXT STYLE SELECTOR
// ---------------------------------------------------------------------------
@Composable
private fun WidgetTextStyleSelector(
    currentStyle: WidgetTextStyle,
    onSelectStyle: (WidgetTextStyle) -> Unit
) {
    WidgetOptionChipRow(
        options = WidgetTextStyle.values(),
        selected = currentStyle,
        label = { it.titleRes },
        onSelect = onSelectStyle
    )
}

// ---------------------------------------------------------------------------
// HERO TIME MODE SELECTOR
// ---------------------------------------------------------------------------
@Composable
private fun WidgetHeroTimeModeSelector(
    currentMode: WidgetHeroTimeMode,
    onSelectMode: (WidgetHeroTimeMode) -> Unit
) {
    WidgetOptionChipRow(
        options = WidgetHeroTimeMode.values(),
        selected = currentMode,
        label = { it.titleRes },
        onSelect = onSelectMode
    )
}

// ---------------------------------------------------------------------------
// TOGGLE ROW
// ---------------------------------------------------------------------------
@Composable
private fun WidgetToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

// ---------------------------------------------------------------------------
// SECTION CARD - groups related widget settings under one titled, icon-headed card
// ---------------------------------------------------------------------------
@Composable
private fun WidgetSettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun WidgetSettingsSubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ---------------------------------------------------------------------------
// LIVE CANVAS PREVIEW
// ---------------------------------------------------------------------------
@Composable
private fun WidgetCanvasPreview(
    settings: AppPrayerSettings,
    widgetSettings: WidgetCustomizationSettings,
    previewType: WidgetPreviewType,
    isArabic: Boolean
) {
    val (primaryAccent, bgCardColor, themeTextPrimary, themeTextSecondary) = resolveWidgetPreviewTheme(widgetSettings.themeMode)
    // Mirrors PrayerAppWidgetProvider.resolveWidgetColors' textStyle override exactly.
    val (textPrimary, textSecondary) = when (widgetSettings.textStyle) {
        WidgetTextStyle.AUTO -> themeTextPrimary to themeTextSecondary
        WidgetTextStyle.LIGHT -> Color(0xFFFFFFFF) to Color(0xFFE2E8F0)
        WidgetTextStyle.DARK -> Color(0xFF0F172A) to Color(0xFF334155)
    }

    // Mirrors PrayerAppWidgetProvider.resolveWidgetColors' textOnAccent exactly (same 0.42
    // threshold) - the preview previously hardcoded white pill/badge text regardless of theme.
    val textOnAccent = if (primaryAccent.luminance() > 0.42f) Color(0xFF0F172A) else Color.White

    val alpha = (widgetSettings.opacityPercent / 100f).coerceIn(0f, 1f)
    // Mirrors PrayerAppWidgetProvider.resolveWidgetColors exactly so the preview matches the
    // real widget for every background style, not just the default TRANSLUCENT one.
    val finalBgColor = when (widgetSettings.bgStyle) {
        WidgetBackgroundStyle.TRANSPARENT_CLEAN -> Color.Transparent
        WidgetBackgroundStyle.MINIMAL_BORDER -> Color.Black.copy(alpha = 0.15f * alpha)
        WidgetBackgroundStyle.SOLID_SURFACE -> bgCardColor.copy(alpha = 1f)
        WidgetBackgroundStyle.FROSTED_GLASS -> lerp(bgCardColor, Color.White, 0.25f).copy(alpha = alpha.coerceAtLeast(0.55f))
        WidgetBackgroundStyle.TRANSLUCENT -> bgCardColor.copy(alpha = alpha)
    }
    val borderWidth = if (widgetSettings.bgStyle == WidgetBackgroundStyle.MINIMAL_BORDER) 1.5.dp else 0.5.dp
    val borderColor = when (widgetSettings.bgStyle) {
        WidgetBackgroundStyle.MINIMAL_BORDER -> primaryAccent.copy(alpha = 0.8f)
        WidgetBackgroundStyle.SOLID_SURFACE -> primaryAccent.copy(alpha = 0.35f)
        WidgetBackgroundStyle.FROSTED_GLASS -> Color.White.copy(alpha = 0.20f)
        else -> primaryAccent.copy(alpha = 0.25f)
    }

    // Simulated Wallpaper Background container - deliberately bright & busy rather than an
    // idealized dark gradient, since a faint translucent overlay reads very differently on
    // a dark backdrop vs. a real (often bright, photographic) home screen wallpaper.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFB8C6D9),
                        Color(0xFFE8DCC4),
                        Color(0xFF9CAF88),
                        Color(0xFFD9C4A8)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Widget Card Box - a fixed height per preview type, independent of font size or which
        // toggles are on. A real widget's footprint is fixed by its home screen grid placement;
        // it never grows or shrinks to fit its content, so the mockup shouldn't either.
        val cardHeight = when (previewType) {
            WidgetPreviewType.STANDARD_4X2 -> 210.dp
            WidgetPreviewType.EXPANDED_MAX -> 340.dp
            WidgetPreviewType.VERTICAL_1_COL -> 320.dp
            WidgetPreviewType.SLIM_BAR -> 72.dp
            WidgetPreviewType.COMPACT_2X1 -> 110.dp
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(if (previewType == WidgetPreviewType.VERTICAL_1_COL) 0.52f else 1f)
                .height(cardHeight)
                .clip(RoundedCornerShape(24.dp))
                .background(finalBgColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(14.dp)
        ) {
            when (previewType) {
                WidgetPreviewType.STANDARD_4X2 -> {
                    PreviewStandard4x2(
                        settings = settings,
                        wSet = widgetSettings,
                        primaryAccent = primaryAccent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textOnAccent = textOnAccent,
                        isArabic = isArabic
                    )
                }
                WidgetPreviewType.EXPANDED_MAX -> {
                    PreviewExpandedMax(
                        settings = settings,
                        wSet = widgetSettings,
                        primaryAccent = primaryAccent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textOnAccent = textOnAccent,
                        isArabic = isArabic
                    )
                }
                WidgetPreviewType.VERTICAL_1_COL -> {
                    PreviewVertical1Col(
                        settings = settings,
                        wSet = widgetSettings,
                        primaryAccent = primaryAccent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textOnAccent = textOnAccent,
                        isArabic = isArabic
                    )
                }
                WidgetPreviewType.SLIM_BAR -> {
                    PreviewSlimBar(
                        settings = settings,
                        wSet = widgetSettings,
                        primaryAccent = primaryAccent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textOnAccent = textOnAccent,
                        isArabic = isArabic
                    )
                }
                WidgetPreviewType.COMPACT_2X1 -> {
                    PreviewCompact2x1(
                        settings = settings,
                        wSet = widgetSettings,
                        primaryAccent = primaryAccent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textOnAccent = textOnAccent,
                        isArabic = isArabic
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewStandard4x2(
    settings: AppPrayerSettings,
    wSet: WidgetCustomizationSettings,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    textOnAccent: Color,
    isArabic: Boolean
) {
    val scale = 1.02f
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header - at 4+ columns the real widget shows Hijri date inline next to location
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (wSet.showLocation || wSet.showHijriDate) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (wSet.showLocation) {
                        Text(
                            text = com.prayertimes.data.cities.CityDatabase.localizedName(LocalizedStrings.forLanguage(LocalContext.current, isArabic), settings.location),
                            fontSize = 11.sp * scale,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    if (wSet.showHijriDate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.widget_preview_mock_hijri_date),
                            fontSize = 9.sp * scale,
                            color = textSecondary
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }

        // Hero Card
        if (wSet.showHeroCard) {
            Surface(
                color = textPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    val previewRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
                    val (heroName, heroTime, heroCountdown) = previewHeroMock(wSet.heroTimeMode, previewRes)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = heroName,
                                fontSize = 12.sp * scale,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                            Text(
                                text = heroTime,
                                fontSize = 20.sp * scale,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                        }

                        if (wSet.showCountdown) {
                            Surface(
                                color = primaryAccent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = heroCountdown,
                                    color = textOnAccent,
                                    fontSize = 11.sp * scale,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (wSet.showProgressBar) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.65f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = primaryAccent,
                            trackColor = textSecondary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // 6-Prayer Ribbon
        if (wSet.showAllPrayersList) {
            val ribbonRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
            val prayers = getPreviewPrayers(ribbonRes, wSet.showSunrise)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                prayers.forEach { (name, time) ->
                    val isActive = name == ribbonRes.getString(R.string.prayer_name_asr)
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (isActive) primaryAccent else textPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = name,
                                fontSize = 8.sp * scale,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = if (isActive) textOnAccent else textSecondary
                            )
                            Text(
                                text = time,
                                fontSize = 8.sp * scale,
                                maxLines = 1,
                                color = if (isActive) textOnAccent else textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewExpandedMax(
    settings: AppPrayerSettings,
    wSet: WidgetCustomizationSettings,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    textOnAccent: Color,
    isArabic: Boolean
) {
    val scale = 1.10f
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header - at 4+ columns the real widget shows Hijri date inline next to location
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (wSet.showLocation || wSet.showHijriDate) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (wSet.showLocation) {
                        Text(
                            text = com.prayertimes.data.cities.CityDatabase.localizedName(LocalizedStrings.forLanguage(LocalContext.current, isArabic), settings.location),
                            fontSize = 13.sp * scale,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    if (wSet.showHijriDate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.widget_preview_mock_hijri_date),
                            fontSize = 11.sp * scale,
                            color = textSecondary
                        )
                    }
                }
            }
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
        }

        // Expanded Hero Card
        if (wSet.showHeroCard) {
            Surface(
                color = textPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val previewRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
                    val (heroName, heroTime, heroCountdown) = previewHeroMock(wSet.heroTimeMode, previewRes)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = heroName,
                                fontSize = 13.sp * scale,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                            Text(
                                text = heroTime,
                                fontSize = 24.sp * scale,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                        }

                        if (wSet.showCountdown) {
                            Surface(
                                color = primaryAccent,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = heroCountdown,
                                    color = textOnAccent,
                                    fontSize = 12.sp * scale,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (wSet.showProgressBar) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.65f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = primaryAccent,
                            trackColor = textSecondary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Expanded Prayer Rows List (Evenly distributed)
        if (wSet.showAllPrayersList) {
            val ribbonRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
            val prayers = getPreviewPrayers(ribbonRes, wSet.showSunrise)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                prayers.forEach { (name, time) ->
                    val isActive = name == ribbonRes.getString(R.string.prayer_name_asr)
                    Surface(
                        color = if (isActive) primaryAccent else textPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp * scale,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) textOnAccent else textSecondary
                            )
                            Text(
                                text = time,
                                fontSize = 12.sp * scale,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) textOnAccent else textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewVertical1Col(
    settings: AppPrayerSettings,
    wSet: WidgetCustomizationSettings,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    textOnAccent: Color,
    isArabic: Boolean
) {
    val scale = 0.98f
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val vertRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
        if (wSet.showLocation) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = com.prayertimes.data.cities.CityDatabase.localizedName(LocalizedStrings.forLanguage(LocalContext.current, isArabic), settings.location), fontSize = 9.sp * scale, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        }

        if (wSet.showHeroCard) {
            Surface(
                color = textPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
                    Text(text = vertRes.getString(if (isPrevious) R.string.prayer_name_dhuhr else R.string.prayer_name_asr), fontSize = 10.sp * scale, fontWeight = FontWeight.Bold, color = primaryAccent)
                    Text(text = if (isPrevious) "12:20 PM" else "3:45 PM", fontSize = 12.sp * scale, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                    if (wSet.showCountdown) {
                        Surface(color = primaryAccent, shape = RoundedCornerShape(6.dp)) {
                            Text(text = if (isPrevious) vertRes.getString(R.string.widget_since_minutes_only, 45) else vertRes.getString(R.string.widget_countdown_minutes_only, 45), color = textOnAccent, fontSize = 8.sp * scale, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        val prayers = getPreviewPrayers(vertRes, wSet.showSunrise)

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            prayers.forEach { (name, time) ->
                val isActive = name == vertRes.getString(R.string.prayer_name_asr)
                Surface(
                    color = if (isActive) primaryAccent else textPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = name, fontSize = 8.sp * scale, fontWeight = FontWeight.Bold, color = if (isActive) textOnAccent else textSecondary)
                        Text(text = time, fontSize = 8.sp * scale, color = if (isActive) textOnAccent else textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSlimBar(
    settings: AppPrayerSettings,
    wSet: WidgetCustomizationSettings,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    textOnAccent: Color,
    isArabic: Boolean
) {
    val scale = 0.92f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val slimRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                if (wSet.showLocation) {
                    Text(text = com.prayertimes.data.cities.CityDatabase.localizedName(LocalizedStrings.forLanguage(LocalContext.current, isArabic), settings.location), fontSize = 9.sp * scale, fontWeight = FontWeight.Bold, color = textSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
                    Text(text = slimRes.getString(if (isPrevious) R.string.prayer_name_dhuhr else R.string.prayer_name_asr), fontSize = 12.sp * scale, fontWeight = FontWeight.Bold, color = primaryAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isPrevious) "12:20 PM" else "3:45 PM", fontSize = 13.sp * scale, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (wSet.showCountdown) {
                val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
                Surface(color = primaryAccent, shape = RoundedCornerShape(8.dp)) {
                    Text(text = if (isPrevious) slimRes.getString(R.string.widget_since_minutes_only, 45) else slimRes.getString(R.string.widget_countdown_minutes_only, 45), color = textOnAccent, fontSize = 10.sp * scale, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun PreviewCompact2x1(
    settings: AppPrayerSettings,
    wSet: WidgetCustomizationSettings,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    textOnAccent: Color,
    isArabic: Boolean
) {
    val scale = 0.95f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
        val compactRes = LocalizedStrings.forLanguage(LocalContext.current, isArabic)
        Column {
            if (wSet.showLocation) {
                Text(text = com.prayertimes.data.cities.CityDatabase.localizedName(LocalizedStrings.forLanguage(LocalContext.current, isArabic), settings.location), fontSize = 9.sp * scale, fontWeight = FontWeight.Bold, color = textSecondary)
            }
            Text(text = compactRes.getString(if (isPrevious) R.string.widget_preview_compact_prev_label else R.string.widget_preview_compact_next_label), fontSize = 11.sp * scale, fontWeight = FontWeight.Bold, color = primaryAccent)
            Text(text = if (isPrevious) "12:20 PM" else "3:45 PM", fontSize = 18.sp * scale, fontWeight = FontWeight.ExtraBold, color = textPrimary)
        }

        if (wSet.showCountdown) {
            Surface(color = primaryAccent, shape = RoundedCornerShape(8.dp)) {
                Text(text = if (isPrevious) compactRes.getString(R.string.widget_since_minutes_only, 45) else compactRes.getString(R.string.widget_preview_mock_minutes_bare, 45), color = textOnAccent, fontSize = 10.sp * scale, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// Mock hero card content for the previews - name, time, and the countdown/elapsed label -
// swapped based on heroTimeMode so the preview reflects the setting it's demonstrating.
private fun previewHeroMock(heroTimeMode: WidgetHeroTimeMode, res: Resources): Triple<String, String, String> {
    return if (heroTimeMode == WidgetHeroTimeMode.PREVIOUS) {
        Triple(
            res.getString(R.string.prayer_name_dhuhr),
            "12:20 PM",
            res.getString(R.string.widget_since_hours_minutes, 2, 10)
        )
    } else {
        Triple(
            res.getString(R.string.prayer_name_asr),
            "3:45 PM",
            res.getString(R.string.widget_countdown_hours_minutes, 1, 45)
        )
    }
}

private fun getPreviewPrayers(res: Resources, showSunrise: Boolean): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    list.add(res.getString(R.string.prayer_name_fajr) to "05:12")
    if (showSunrise) {
        list.add(res.getString(R.string.prayer_name_sunrise) to "06:34")
    }
    list.add(res.getString(R.string.prayer_name_dhuhr) to "12:20")
    list.add(res.getString(R.string.prayer_name_asr) to "15:45")
    list.add(res.getString(R.string.prayer_name_maghrib) to "18:05")
    list.add(res.getString(R.string.prayer_name_isha) to "19:35")
    return list
}

// Mirrors PrayerAppWidgetProvider.resolveSystemDynamicPalette exactly (same dynamicDarkColorScheme
// derivation) so the preview shows the same colors "Material You Dynamic" will actually render with.
@Composable
private fun resolveSystemDynamicPreviewPalette(): Quadruple<Color, Color, Color, Color>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return try {
        val scheme = androidx.compose.material3.dynamicDarkColorScheme(context)
        Quadruple(scheme.primary, scheme.surfaceContainerHigh, scheme.onSurface, scheme.onSurfaceVariant)
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun resolveWidgetPreviewTheme(theme: WidgetThemeMode): Quadruple<Color, Color, Color, Color> {
    return when (theme) {
        // MaterialTheme.colorScheme here is already exactly what "Match App Theme" should
        // mirror - the app is already composed inside MyApplicationTheme, so this always tracks
        // the user's actual current theme/color preset/light-dark mode with no separate logic.
        WidgetThemeMode.APP_THEME -> Quadruple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        WidgetThemeMode.MATERIAL_YOU -> resolveSystemDynamicPreviewPalette()
            ?: Quadruple(Color(0xFF3F51B5), Color(0xFF1C1B1F), Color(0xFFE6E1E5), Color(0xFFCAC4D0))
        WidgetThemeMode.DARK_ELEGANT -> Quadruple(Color(0xFF10B981), Color(0xFF121820), Color(0xFFF1F5F9), Color(0xFF94A3B8))
        WidgetThemeMode.LIGHT_CLEAN -> Quadruple(Color(0xFF059669), Color(0xFFFFFFFF), Color(0xFF0F172A), Color(0xFF64748B))
        WidgetThemeMode.OLED_BLACK -> Quadruple(Color(0xFF34D399), Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFA1A1AA))
        WidgetThemeMode.EMERALD_ISLAMIC -> Quadruple(Color(0xFFF59E0B), Color(0xFF064E3B), Color(0xFFECFDF5), Color(0xFFA7F3D0))
        WidgetThemeMode.GOLDEN_HOUR -> Quadruple(Color(0xFFF59E0B), Color(0xFF451A03), Color(0xFFFEF3C7), Color(0xFFFDE68A))
        WidgetThemeMode.ROYAL_BLUE -> Quadruple(Color(0xFF38BDF8), Color(0xFF0F172A), Color(0xFFF0F9FF), Color(0xFFBAE6FD))
        WidgetThemeMode.MONOCHROME -> Quadruple(Color(0xFFFAFAFA), Color(0xFF18181B), Color(0xFFFAFAFA), Color(0xFFA1A1AA))
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
