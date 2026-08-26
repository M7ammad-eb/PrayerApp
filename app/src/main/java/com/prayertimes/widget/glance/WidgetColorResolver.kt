package com.prayertimes.widget.glance

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppThemeMode
import com.prayertimes.data.models.WidgetTextStyle
import com.prayertimes.data.models.WidgetThemeMode
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.ui.theme.getPresetColorScheme

internal data class WidgetColorScheme(
    val rootBgColor: Int,
    val rootBorderColor: Int,
    val heroBgColor: Int,
    val heroStrokeColor: Int,
    val accentColor: Int,
    val textPrimaryColor: Int,
    val textSecondaryColor: Int,
    val textOnAccentColor: Int,
    val activePrayerBgColor: Int,
    val inactivePrayerBgColor: Int,
    val countdownBgColor: Int,
    val fontScale: Float
)

internal object WidgetColorResolver {
    private data class Palette(
        val accent: Int,
        val background: Int,
        val primaryText: Int,
        val secondaryText: Int
    )

    fun resolve(context: Context, settings: AppPrayerSettings): WidgetColorScheme {
        val widget = settings.widgetSettings
        val opacity = (widget.opacityPercent / 100f).coerceIn(0f, 1f)
        val palette = resolvePalette(context, settings).let { base ->
            when (widget.textStyle) {
                WidgetTextStyle.AUTO -> base
                WidgetTextStyle.LIGHT -> base.copy(
                    primaryText = Color.WHITE,
                    secondaryText = 0xFFE2E8F0.toInt()
                )
                WidgetTextStyle.DARK -> base.copy(
                    primaryText = 0xFF0F172A.toInt(),
                    secondaryText = 0xFF334155.toInt()
                )
            }
        }

        val rootBackground = if (widget.showBackground) {
            ColorUtils.setAlphaComponent(palette.background, (opacity * 255).toInt())
        } else {
            Color.TRANSPARENT
        }
        // The outline deliberately uses the exact same resolved accent as the Hero countdown pill.
        val rootBorder = if (widget.showBorder) palette.accent else Color.TRANSPARENT
        val hasFloatingContent = !widget.showBackground
        val heroBackground = if (hasFloatingContent) {
            Color.TRANSPARENT
        } else {
            ColorUtils.setAlphaComponent(palette.primaryText, 41)
        }
        val inactivePrayerBackground = if (hasFloatingContent) {
            Color.TRANSPARENT
        } else {
            ColorUtils.setAlphaComponent(palette.primaryText, 36)
        }
        val textOnAccent = if (ColorUtils.calculateLuminance(palette.accent) > 0.42) {
            0xFF0F172A.toInt()
        } else {
            Color.WHITE
        }

        return WidgetColorScheme(
            rootBgColor = rootBackground,
            rootBorderColor = rootBorder,
            heroBgColor = heroBackground,
            heroStrokeColor = ColorUtils.setAlphaComponent(palette.accent, 38),
            accentColor = palette.accent,
            textPrimaryColor = palette.primaryText,
            textSecondaryColor = palette.secondaryText,
            textOnAccentColor = textOnAccent,
            activePrayerBgColor = palette.accent,
            inactivePrayerBgColor = inactivePrayerBackground,
            countdownBgColor = palette.accent,
            fontScale = 1f
        )
    }

    private fun resolvePalette(context: Context, settings: AppPrayerSettings): Palette {
        val widget = settings.widgetSettings
        return when (widget.themeMode) {
            WidgetThemeMode.APP_THEME -> appPalette(context, settings)
            WidgetThemeMode.MATERIAL_YOU -> dynamicPalette(context) ?: Palette(
                accent = 0xFF3F51B5.toInt(),
                background = 0xFF1C1B1F.toInt(),
                primaryText = 0xFFE6E1E5.toInt(),
                secondaryText = 0xFFCAC4D0.toInt()
            )
            WidgetThemeMode.DARK_ELEGANT -> Palette(0xFF10B981.toInt(), 0xFF121820.toInt(), 0xFFF1F5F9.toInt(), 0xFF94A3B8.toInt())
            WidgetThemeMode.LIGHT_CLEAN -> Palette(0xFF059669.toInt(), Color.WHITE, 0xFF0F172A.toInt(), 0xFF64748B.toInt())
            WidgetThemeMode.OLED_BLACK -> Palette(0xFF34D399.toInt(), Color.BLACK, Color.WHITE, 0xFFA1A1AA.toInt())
            WidgetThemeMode.EMERALD_ISLAMIC -> Palette(0xFFF59E0B.toInt(), 0xFF064E3B.toInt(), 0xFFECFDF5.toInt(), 0xFFA7F3D0.toInt())
            WidgetThemeMode.GOLDEN_HOUR -> Palette(0xFFF59E0B.toInt(), 0xFF451A03.toInt(), 0xFFFEF3C7.toInt(), 0xFFFDE68A.toInt())
            WidgetThemeMode.ROYAL_BLUE -> Palette(0xFF38BDF8.toInt(), 0xFF0F172A.toInt(), 0xFFF0F9FF.toInt(), 0xFFBAE6FD.toInt())
            WidgetThemeMode.MONOCHROME -> Palette(0xFFFAFAFA.toInt(), 0xFF18181B.toInt(), 0xFFFAFAFA.toInt(), 0xFFA1A1AA.toInt())
        }
    }

    private fun dynamicPalette(context: Context): Palette? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return runCatching {
            val scheme = if (context.isDarkMode()) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            Palette(
                accent = scheme.primary.toArgb(),
                background = scheme.surfaceColorAtElevation(8.dp).toArgb(),
                primaryText = scheme.onSurface.toArgb(),
                secondaryText = scheme.onSurfaceVariant.toArgb()
            )
        }.getOrNull()
    }

    private fun appPalette(context: Context, settings: AppPrayerSettings): Palette {
        val dark = when (settings.themeMode) {
            AppThemeMode.SYSTEM -> context.isDarkMode()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
        if (
            settings.followSystemColors &&
            settings.colorPreset == AppColorPreset.SYSTEM_DYNAMIC
        ) {
            dynamicPalette(context)?.let { return it }
        }

        val preset = settings.colorPreset.takeUnless { it == AppColorPreset.SYSTEM_DYNAMIC }
            ?: AppColorPreset.EMERALD_GOLD
        val scheme = getPresetColorScheme(preset, dark)
        return Palette(
            accent = (if (dark) preset.primaryDark else preset.primaryLight).toInt(),
            background = scheme.surfaceColorAtElevation(8.dp).toArgb(),
            primaryText = scheme.onSurface.toArgb(),
            secondaryText = scheme.onSurfaceVariant.toArgb()
        )
    }

    private fun Context.isDarkMode(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
