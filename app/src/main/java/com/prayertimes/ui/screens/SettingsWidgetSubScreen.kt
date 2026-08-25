package com.prayertimes.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

@Composable
fun SettingsWidgetSubScreen(
    settings: AppPrayerSettings,
    onUpdateWidgetSettings: (WidgetCustomizationSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val wSet = settings.widgetSettings

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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("widget_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.widgetsSection,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.widget_settings_intro_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    onUpdateWidgetSettings(WidgetCustomizationSettings())
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

        // Live Widget Render Box - fixed above the scrollable settings list so it stays visible
        // while flipping through styles below, instead of scrolling out of view.
        WidgetCanvasPreview(
            settings = settings,
            widgetSettings = wSet,
            isArabic = strings.isArabic
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                WidgetSettingsSubLabel(strings.widgetBgStyleTitle)
                WidgetBackgroundStyleSelector(
                    currentStyle = wSet.bgStyle,
                    onSelectStyle = {
                        onUpdateWidgetSettings(wSet.copy(bgStyle = it))
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
                // way to know what's actually behind it.
                WidgetSettingsSubLabel(stringResource(R.string.widget_settings_text_color_title))
                WidgetTextStyleSelector(
                    currentStyle = wSet.textStyle,
                    onSelectStyle = {
                        onUpdateWidgetSettings(wSet.copy(textStyle = it))
                    }
                )
            }

            // Hero controls belong together: display, timing, then the dependent countdown.
            WidgetSettingsSectionCard(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.widget_settings_hero_section_title),
                subtitle = stringResource(R.string.widget_settings_hero_section_subtitle)
            ) {
                WidgetHeroTimeModeSelector(
                    currentMode = wSet.heroTimeMode,
                    onSelectMode = {
                        onUpdateWidgetSettings(wSet.copy(heroTimeMode = it))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                WidgetToggleRow(
                    icon = Icons.Default.Layers,
                    title = stringResource(R.string.widget_settings_toggle_show_hero),
                    subtitle = strings.widgetToggleShowHeroDesc,
                    checked = wSet.showHeroCard,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showHeroCard = it))
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
                        }
                    )
                }

            }

            // Schedule and header metadata form the second independent content group.
            WidgetSettingsSectionCard(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.widget_settings_schedule_section_title),
                subtitle = stringResource(R.string.widget_settings_schedule_section_subtitle)
            ) {
                WidgetToggleRow(
                    icon = Icons.Default.FormatListBulleted,
                    title = stringResource(R.string.widget_settings_toggle_show_all_prayers),
                    subtitle = strings.widgetToggleShowAllPrayersDesc,
                    checked = wSet.showAllPrayersList,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showAllPrayersList = it))
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
                    }
                )

                WidgetToggleRow(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.widget_settings_toggle_show_hijri),
                    subtitle = strings.widgetToggleShowHijriDesc,
                    checked = wSet.showHijriDate,
                    onCheckedChange = {
                        onUpdateWidgetSettings(wSet.copy(showHijriDate = it))
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
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WidgetThemeOptionRow(currentTheme, showArrow = true)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            WidgetThemeMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { WidgetThemeOptionRow(mode, showArrow = false) },
                    onClick = {
                        expanded = false
                        onSelectTheme(mode)
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetThemeOptionRow(mode: WidgetThemeMode, showArrow: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (showArrow) 14.dp else 0.dp, vertical = if (showArrow) 10.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                val (c1, c2, c3) = getThemePreviewColors(mode)
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c1).border(1.dp, Color.White, CircleShape))
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c2).border(1.dp, Color.White, CircleShape))
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c3).border(1.dp, Color.White, CircleShape))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(mode.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WidgetBackgroundOptionRow(currentStyle, showArrow = true)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            WidgetBackgroundStyle.values().forEach { style ->
                DropdownMenuItem(
                    text = { WidgetBackgroundOptionRow(style, showArrow = false) },
                    onClick = {
                        expanded = false
                        onSelectStyle(style)
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetBackgroundOptionRow(style: WidgetBackgroundStyle, showArrow: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (showArrow) 14.dp else 0.dp, vertical = if (showArrow) 10.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(style.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    isArabic: Boolean
) {
    val (primaryAccent, bgCardColor, themeTextPrimary, themeTextSecondary) = resolveWidgetPreviewTheme(widgetSettings.themeMode)
    // Mirrors the Glance widget color resolver's text-style override.
    val (textPrimary, textSecondary) = when (widgetSettings.textStyle) {
        WidgetTextStyle.AUTO -> themeTextPrimary to themeTextSecondary
        WidgetTextStyle.LIGHT -> Color(0xFFFFFFFF) to Color(0xFFE2E8F0)
        WidgetTextStyle.DARK -> Color(0xFF0F172A) to Color(0xFF334155)
    }

    // Mirrors the Glance widget color resolver's textOnAccent threshold (0.42
    // threshold) - the preview previously hardcoded white pill/badge text regardless of theme.
    val textOnAccent = if (primaryAccent.luminance() > 0.42f) Color(0xFF0F172A) else Color.White

    val alpha = (widgetSettings.opacityPercent / 100f).coerceIn(0f, 1f)
    // Mirrors the Glance widget color resolver so the preview matches the
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

    // Simulated Wallpaper Background container - split diagonally into a bright half and a
    // dark half so a single preview shows how each background style (especially translucent/
    // frosted ones) reads against both a light, photographic wallpaper and a dark one at once,
    // rather than only ever testing against one extreme.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .drawWithCache {
                val brightTriangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    close()
                }
                val darkTriangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                val brightBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE8DCC4), Color(0xFFB8C6D9)),
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height)
                )
                val darkBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0B1220)),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                onDrawBehind {
                    drawPath(brightTriangle, brightBrush)
                    drawPath(darkTriangle, darkBrush)
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Widget Card Box - a fixed height, independent of font size or which toggles are on.
        // A real widget's footprint is fixed by its home screen grid placement; it never grows
        // or shrinks to fit its content, so the mockup shouldn't either.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(finalBgColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(14.dp)
        ) {
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

// Mock hero card content for the preview - name, time, and the countdown/elapsed label -
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

// Mirrors the Glance widget dynamic palette (same dynamicDarkColorScheme
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
