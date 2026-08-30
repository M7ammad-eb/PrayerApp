package com.prayertimes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppThemeMode

fun getPresetColorScheme(
    preset: AppColorPreset,
    isDark: Boolean,
    customColorSeed: Long = AppColorPreset.CUSTOM.previewColor
): ColorScheme {
    val primary = if (preset == AppColorPreset.CUSTOM) {
        customTonalColor(customColorSeed, isDark, hueOffset = 0f, saturationFactor = 1f)
    } else {
        Color(if (isDark) preset.primaryDark else preset.primaryLight)
    }
    val secondary = if (preset == AppColorPreset.CUSTOM) {
        customTonalColor(customColorSeed, isDark, hueOffset = 24f, saturationFactor = 0.72f)
    } else {
        Color(if (isDark) preset.secondaryDark else preset.secondaryLight)
    }
    val schemeBackground = if (isDark) Color(0xFF0F1513) else Color(0xFFF7FAF7)
    // Material container roles are opaque tonal colors. Keeping an accent's alpha here caused
    // callers using copy(alpha = …) to replace the intended tint strength and produce harsh,
    // saturated selections. Composite once at the scheme boundary, like dynamic color does.
    val containerPrimary = primary
        .copy(alpha = if (isDark) 0.28f else 0.14f)
        .compositeOver(schemeBackground)
    val containerSecondary = secondary
        .copy(alpha = if (isDark) 0.20f else 0.15f)
        .compositeOver(schemeBackground)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = containerPrimary,
            onPrimaryContainer = Color(0xFFE8F5E9),
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = containerSecondary,
            onSecondaryContainer = Color(0xFFFFF8E1),
            tertiary = Color(0xFFF3D27C),
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFF493B0C),
            onTertiaryContainer = Color(0xFFFFE08A),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = schemeBackground,
            onBackground = Color(0xFFE8EFEA),
            surface = Color(0xFF151D1A),
            onSurface = Color(0xFFE8EFEA),
            surfaceVariant = Color(0xFF222C29),
            onSurfaceVariant = Color(0xFFC0CEC5),
            outline = Color(0xFF71807B),
            outlineVariant = Color(0xFF34413D),
            inverseSurface = Color(0xFFDDE5E0),
            inverseOnSurface = Color(0xFF2A322F),
            inversePrimary = Color(if (preset.primaryLight != 0L) preset.primaryLight else 0xFF165B33),
            surfaceTint = primary,
            surfaceContainerLowest = Color(0xFF0A100E),
            surfaceContainerLow = Color(0xFF131B18),
            surfaceContainer = Color(0xFF18211E),
            surfaceContainerHigh = Color(0xFF1E2824),
            surfaceContainerHighest = Color(0xFF25302C)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = containerPrimary,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = containerSecondary,
            onSecondaryContainer = Color(0xFF3E2723),
            tertiary = Color(0xFFB8860B),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFE08A),
            onTertiaryContainer = Color(0xFF3A2F00),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = schemeBackground,
            onBackground = Color(0xFF191C1B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191C1B),
            surfaceVariant = Color(0xFFE1E9E5),
            onSurfaceVariant = Color(0xFF434E48),
            outline = Color(0xFF6E7977),
            outlineVariant = Color(0xFFC3CCC7),
            inverseSurface = Color(0xFF2D312F),
            inverseOnSurface = Color(0xFFF0F1EE),
            inversePrimary = Color(if (preset.primaryDark != 0L) preset.primaryDark else 0xFF4ADE80),
            surfaceTint = primary,
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEEF5F3),
            surfaceContainer = Color(0xFFE8EFED),
            surfaceContainerHigh = Color(0xFFE3EAE8),
            surfaceContainerHighest = Color(0xFFDDE4E2)
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorPreset: AppColorPreset = AppColorPreset.SYSTEM_DYNAMIC,
    followSystemColors: Boolean = true,
    customColorSeed: Long = AppColorPreset.CUSTOM.previewColor,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        followSystemColors && colorPreset == AppColorPreset.SYSTEM_DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                getPresetColorScheme(AppColorPreset.EMERALD_GOLD, isDark)
            }
        }
        colorPreset == AppColorPreset.SYSTEM_DYNAMIC -> {
            getPresetColorScheme(AppColorPreset.EMERALD_GOLD, isDark)
        }
        else -> {
            getPresetColorScheme(colorPreset, isDark, customColorSeed)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SalatiShapes,
        content = content
    )
}

private fun customTonalColor(
    seed: Long,
    isDark: Boolean,
    hueOffset: Float,
    saturationFactor: Float
): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toInt(), hsv)
    hsv[0] = (hsv[0] + hueOffset) % 360f
    hsv[1] = (hsv[1].coerceAtLeast(0.45f) * saturationFactor).coerceIn(0.28f, 0.78f)
    hsv[2] = if (isDark) 0.86f else 0.48f
    return Color(android.graphics.Color.HSVToColor(hsv))
}
