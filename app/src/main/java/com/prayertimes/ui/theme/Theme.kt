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
import androidx.compose.ui.platform.LocalContext
import com.prayertimes.data.models.AppColorPreset
import com.prayertimes.data.models.AppThemeMode

fun getPresetColorScheme(preset: AppColorPreset, isDark: Boolean): ColorScheme {
    val primary = Color(if (isDark) preset.primaryDark else preset.primaryLight)
    val secondary = Color(if (isDark) preset.secondaryDark else preset.secondaryLight)
    val containerPrimary = if (isDark) primary.copy(alpha = 0.25f) else primary.copy(alpha = 0.12f)
    val onContainerPrimary = if (isDark) primary else primary

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = containerPrimary,
            onPrimaryContainer = Color(0xFFE8F5E9),
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = secondary.copy(alpha = 0.2f),
            onSecondaryContainer = Color(0xFFFFF8E1),
            tertiary = Color(0xFFF3D27C),
            onTertiary = Color.Black,
            background = Color(0xFF0F1412),
            onBackground = Color(0xFFE8EFEA),
            surface = Color(0xFF161E1A),
            onSurface = Color(0xFFE8EFEA),
            surfaceVariant = Color(0xFF222C26),
            onSurfaceVariant = Color(0xFFC0CEC5),
            outline = Color(0xFF38463E),
            outlineVariant = Color(0xFF28342D)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = containerPrimary,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFF3E2723),
            tertiary = Color(0xFFB8860B),
            onTertiary = Color.White,
            background = Color(0xFFF8FAF9),
            onBackground = Color(0xFF191C1B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191C1B),
            surfaceVariant = Color(0xFFEDF3F0),
            onSurfaceVariant = Color(0xFF434E48),
            outline = Color(0xFFD0DDD6),
            outlineVariant = Color(0xFFE4ECE8)
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorPreset: AppColorPreset = AppColorPreset.SYSTEM_DYNAMIC,
    followSystemColors: Boolean = true,
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
            getPresetColorScheme(colorPreset, isDark)
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
