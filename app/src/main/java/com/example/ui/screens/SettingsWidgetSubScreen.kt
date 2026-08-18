package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.StayCurrentPortrait
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.WidgetBackgroundStyle
import com.example.data.models.WidgetCustomizationSettings
import com.example.data.models.WidgetFontSize
import com.example.data.models.WidgetHeroTimeMode
import com.example.data.models.WidgetTextStyle
import com.example.data.models.WidgetThemeMode
import com.example.data.preferences.AppPrayerSettings
import com.example.ui.locale.LocalAppStrings

enum class WidgetPreviewType(val labelEn: String, val labelAr: String) {
    STANDARD_4X2("4x2 Ribbon", "شريط 4×2"),
    EXPANDED_MAX("Expanded (Max)", "موسع كامل (أقصى حجم)"),
    VERTICAL_1_COL("1-Col (1x4 / 1x6)", "عمود نحيف 1×6"),
    SLIM_BAR("Slim (4x1 / 6x1)", "شريط أفقي 4×1"),
    COMPACT_2X1("Compact (2x1)", "مصغر 2×1")
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
                        contentDescription = "Back",
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
                        text = if (strings.isArabic)
                            "خصص شكل وحجم التطبيق المصغر على شاشتك الرئيسية ليتناسب تماماً مع أي عدد من الأعمدة والصفوف (من عمود واحد نحيف إلى الشاشة الكاملة)."
                        else
                            "Customize how the widget looks on your home screen to perfectly fit any grid layout (from 1-column slim to full max expanded).",
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
                                text = if (strings.isArabic) type.labelAr else type.labelEn,
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
                    text = if (strings.isArabic) "✓ تم تحديث جميع الودجات على الشاشة الرئيسية بنجاح!" else "✓ All home screen widgets refreshed!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // 1. Theme Mode & Palette Selection
            Text(
                text = strings.widgetThemeModeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            WidgetThemeSelector(
                currentTheme = wSet.themeMode,
                isArabic = strings.isArabic,
                onSelectTheme = {
                    onUpdateWidgetSettings(wSet.copy(themeMode = it))
                    onRefreshAllWidgets()
                }
            )

            HorizontalDivider()

            // 2. Background Style & Transparency
            Text(
                text = strings.widgetBgStyleTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            WidgetBackgroundStyleSelector(
                currentStyle = wSet.bgStyle,
                isArabic = strings.isArabic,
                onSelectStyle = {
                    onUpdateWidgetSettings(wSet.copy(bgStyle = it))
                    onRefreshAllWidgets()
                }
            )

            // Opacity Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                modifier = Modifier.size(20.dp)
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

                    Spacer(modifier = Modifier.height(8.dp))

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
                            text = if (strings.isArabic) "شفاف (0%)" else "Transparent (0%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (strings.isArabic) "معتم كامل (100%)" else "Solid (100%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // 3. Font Size Selection
            Text(
                text = strings.widgetFontSizeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            WidgetFontSizeSelector(
                currentSize = wSet.fontSize,
                isArabic = strings.isArabic,
                onSelectSize = {
                    onUpdateWidgetSettings(wSet.copy(fontSize = it))
                    onRefreshAllWidgets()
                }
            )

            HorizontalDivider()

            // Text Color Style - a manual escape hatch for when the theme's own text colors
            // can't be trusted for contrast, since a transparent/near-transparent widget has no
            // way to know what's actually behind it (see PrayerAppWidgetProvider.resolveWidgetColors).
            Text(
                text = if (strings.isArabic) "لون النص" else "Text Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            WidgetTextStyleSelector(
                currentStyle = wSet.textStyle,
                isArabic = strings.isArabic,
                onSelectStyle = {
                    onUpdateWidgetSettings(wSet.copy(textStyle = it))
                    onRefreshAllWidgets()
                }
            )

            HorizontalDivider()

            // Hero Card Time Mode - "In 2h 41m" (next prayer) vs "Since 2h 10m" (current/last
            // prayer). Only one at a time, per user preference.
            Text(
                text = if (strings.isArabic) "توقيت البطاقة الرئيسية" else "Hero Card Timing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            WidgetHeroTimeModeSelector(
                currentMode = wSet.heroTimeMode,
                isArabic = strings.isArabic,
                onSelectMode = {
                    onUpdateWidgetSettings(wSet.copy(heroTimeMode = it))
                    onRefreshAllWidgets()
                }
            )

            HorizontalDivider()

            // 4. Content & Toggles
            Text(
                text = strings.widgetContentTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

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
                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار الموقع واسم المدينة" else "Show Location & City",
                        checked = wSet.showLocation,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showLocation = it))
                            onRefreshAllWidgets()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار التاريخ الهجري" else "Show Hijri Date",
                        checked = wSet.showHijriDate,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showHijriDate = it))
                            onRefreshAllWidgets()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار العداد التنازلي المتبقي" else "Show Countdown Timer",
                        checked = wSet.showCountdown,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showCountdown = it))
                            onRefreshAllWidgets()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار شريط التقدم للوقت المنقضي" else "Show Interval Progress Bar",
                        checked = wSet.showProgressBar,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showProgressBar = it))
                            onRefreshAllWidgets()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار وقت الشروق في القائمة" else "Show Sunrise in Schedule",
                        checked = wSet.showSunrise,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showSunrise = it))
                            onRefreshAllWidgets()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار بطاقة الصلاة القادمة (Hero Card)" else "Show Next Prayer Hero Card",
                        checked = wSet.showHeroCard,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showHeroCard = it))
                            onRefreshAllWidgets()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    WidgetToggleRow(
                        title = if (strings.isArabic) "إظهار قائمة كافة الصلوات" else "Show All Prayers Schedule",
                        checked = wSet.showAllPrayersList,
                        onCheckedChange = {
                            onUpdateWidgetSettings(wSet.copy(showAllPrayersList = it))
                            onRefreshAllWidgets()
                        }
                    )
                }
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
    isArabic: Boolean,
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
                                text = if (isArabic) mode.titleAr else mode.titleEn,
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
    isArabic: Boolean,
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
                        text = if (isArabic) style.titleAr else style.titleEn,
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
// FONT SIZE SELECTOR
// ---------------------------------------------------------------------------
@Composable
private fun WidgetFontSizeSelector(
    currentSize: WidgetFontSize,
    isArabic: Boolean,
    onSelectSize: (WidgetFontSize) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WidgetFontSize.values().forEach { size ->
            val isSelected = currentSize == size
            FilledTonalButton(
                onClick = { onSelectSize(size) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (isArabic) size.titleAr else size.titleEn,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
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
    isArabic: Boolean,
    onSelectStyle: (WidgetTextStyle) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WidgetTextStyle.values().forEach { style ->
            val isSelected = currentStyle == style
            FilledTonalButton(
                onClick = { onSelectStyle(style) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (isArabic) style.titleAr else style.titleEn,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HERO TIME MODE SELECTOR
// ---------------------------------------------------------------------------
@Composable
private fun WidgetHeroTimeModeSelector(
    currentMode: WidgetHeroTimeMode,
    isArabic: Boolean,
    onSelectMode: (WidgetHeroTimeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WidgetHeroTimeMode.values().forEach { mode ->
            val isSelected = currentMode == mode
            FilledTonalButton(
                onClick = { onSelectMode(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (isArabic) mode.titleAr else mode.titleEn,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TOGGLE ROW
// ---------------------------------------------------------------------------
@Composable
private fun WidgetToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
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
    isArabic: Boolean
) {
    val scale = wSet.fontSize.scaleFactor
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
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = settings.location.name,
                            fontSize = 11.sp * scale,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    if (wSet.showHijriDate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• 14 Safar 1448 AH",
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
                    val (heroName, heroTime, heroCountdown) = previewHeroMock(wSet.heroTimeMode, isArabic)
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
                                    color = Color.White,
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
            val prayers = getPreviewPrayers(isArabic, wSet.showSunrise)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                prayers.forEach { (name, time) ->
                    val isActive = name == (if (isArabic) "العصر" else "Asr")
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
                                color = if (isActive) Color.White else textSecondary
                            )
                            Text(
                                text = time,
                                fontSize = 8.sp * scale,
                                maxLines = 1,
                                color = if (isActive) Color.White else textPrimary
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
    isArabic: Boolean
) {
    val scale = wSet.fontSize.scaleFactor
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
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = settings.location.name,
                            fontSize = 13.sp * scale,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    if (wSet.showHijriDate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "• ١٤ صفر ١٤٤٨ هـ" else "• 14 Safar 1448 AH",
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
                    val (heroName, heroTime, heroCountdown) = previewHeroMock(wSet.heroTimeMode, isArabic)
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
                                    color = Color.White,
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
            val prayers = getPreviewPrayers(isArabic, wSet.showSunrise)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                prayers.forEach { (name, time) ->
                    val isActive = name == (if (isArabic) "العصر" else "Asr")
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
                                color = if (isActive) Color.White else textSecondary
                            )
                            Text(
                                text = time,
                                fontSize = 12.sp * scale,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) Color.White else textPrimary
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
    isArabic: Boolean
) {
    val scale = wSet.fontSize.scaleFactor
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (wSet.showLocation) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Mosque, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = settings.location.name, fontSize = 9.sp * scale, fontWeight = FontWeight.Bold, color = textPrimary)
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
                    Text(text = if (isArabic) (if (isPrevious) "الظهر" else "العصر") else (if (isPrevious) "Dhuhr" else "Asr"), fontSize = 10.sp * scale, fontWeight = FontWeight.Bold, color = primaryAccent)
                    Text(text = if (isPrevious) "12:20 PM" else "3:45 PM", fontSize = 12.sp * scale, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                    if (wSet.showCountdown) {
                        Surface(color = primaryAccent, shape = RoundedCornerShape(6.dp)) {
                            Text(text = if (isPrevious) (if (isArabic) "منذ 45د" else "45m ago") else (if (isArabic) "خلال 45د" else "In 45m"), color = Color.White, fontSize = 8.sp * scale, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        val prayers = getPreviewPrayers(isArabic, wSet.showSunrise)

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            prayers.forEach { (name, time) ->
                val isActive = name == (if (isArabic) "العصر" else "Asr")
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
                        Text(text = name, fontSize = 8.sp * scale, fontWeight = FontWeight.Bold, color = if (isActive) Color.White else textSecondary)
                        Text(text = time, fontSize = 8.sp * scale, color = if (isActive) Color.White else textPrimary)
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
    isArabic: Boolean
) {
    val scale = wSet.fontSize.scaleFactor
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Mosque, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                if (wSet.showLocation) {
                    Text(text = settings.location.name, fontSize = 9.sp * scale, fontWeight = FontWeight.Bold, color = textSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
                    Text(text = if (isArabic) (if (isPrevious) "الظهر" else "العصر") else (if (isPrevious) "Dhuhr" else "Asr"), fontSize = 12.sp * scale, fontWeight = FontWeight.Bold, color = primaryAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isPrevious) "12:20 PM" else "3:45 PM", fontSize = 13.sp * scale, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (wSet.showCountdown) {
                val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
                Surface(color = primaryAccent, shape = RoundedCornerShape(8.dp)) {
                    Text(text = if (isPrevious) (if (isArabic) "منذ 45د" else "45m ago") else (if (isArabic) "خلال 45د" else "In 45m"), color = Color.White, fontSize = 10.sp * scale, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
    isArabic: Boolean
) {
    val scale = wSet.fontSize.scaleFactor
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isPrevious = wSet.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
        Column {
            if (wSet.showLocation) {
                Text(text = settings.location.name, fontSize = 9.sp * scale, fontWeight = FontWeight.Bold, color = textSecondary)
            }
            Text(text = if (isArabic) (if (isPrevious) "الظهر" else "العصر") else (if (isPrevious) "Dhuhr Prayer" else "Asr Prayer"), fontSize = 11.sp * scale, fontWeight = FontWeight.Bold, color = primaryAccent)
            Text(text = if (isPrevious) "12:20 PM" else "3:45 PM", fontSize = 18.sp * scale, fontWeight = FontWeight.ExtraBold, color = textPrimary)
        }

        if (wSet.showCountdown) {
            Surface(color = primaryAccent, shape = RoundedCornerShape(8.dp)) {
                Text(text = if (isPrevious) (if (isArabic) "منذ 45د" else "45m ago") else (if (isArabic) "45د" else "45m"), color = Color.White, fontSize = 10.sp * scale, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// Mock hero card content for the previews - name, time, and the countdown/elapsed label -
// swapped based on heroTimeMode so the preview reflects the setting it's demonstrating.
private fun previewHeroMock(heroTimeMode: WidgetHeroTimeMode, isArabic: Boolean): Triple<String, String, String> {
    return if (heroTimeMode == WidgetHeroTimeMode.PREVIOUS) {
        Triple(
            if (isArabic) "الظهر" else "Dhuhr",
            "12:20 PM",
            if (isArabic) "منذ 2س 10د" else "2h 10m ago"
        )
    } else {
        Triple(
            if (isArabic) "العصر" else "Asr",
            "3:45 PM",
            if (isArabic) "خلال 1س 45د" else "In 1h 45m"
        )
    }
}

private fun getPreviewPrayers(isArabic: Boolean, showSunrise: Boolean): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    list.add((if (isArabic) "الفجر" else "Fajr") to "05:12")
    if (showSunrise) {
        list.add((if (isArabic) "الشروق" else "Sunrise") to "06:34")
    }
    list.add((if (isArabic) "الظهر" else "Dhuhr") to "12:20")
    list.add((if (isArabic) "العصر" else "Asr") to "15:45")
    list.add((if (isArabic) "المغرب" else "Maghrib") to "18:05")
    list.add((if (isArabic) "العشاء" else "Isha") to "19:35")
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
