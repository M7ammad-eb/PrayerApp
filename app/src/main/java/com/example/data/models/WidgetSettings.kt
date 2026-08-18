package com.example.data.models

import androidx.annotation.StringRes
import com.example.R

enum class WidgetThemeMode(
    @StringRes val titleRes: Int,
    val previewBgColor: Long,
    val previewAccentColor: Long,
    val previewTextColor: Long
) {
    APP_THEME(
        titleRes = R.string.widget_theme_app_theme,
        previewBgColor = 0xFF1E293B,
        previewAccentColor = 0xFF10B981,
        previewTextColor = 0xFFFFFFFF
    ),
    MATERIAL_YOU(
        titleRes = R.string.widget_theme_material_you,
        previewBgColor = 0xFF2A3439,
        previewAccentColor = 0xFF7DD3FC,
        previewTextColor = 0xFFFFFFFF
    ),
    DARK_ELEGANT(
        titleRes = R.string.widget_theme_dark_elegant,
        previewBgColor = 0xFF121820,
        previewAccentColor = 0xFF10B981,
        previewTextColor = 0xFFF1F5F9
    ),
    LIGHT_CLEAN(
        titleRes = R.string.widget_theme_light_clean,
        previewBgColor = 0xFFF8FAFC,
        previewAccentColor = 0xFF059669,
        previewTextColor = 0xFF0F172A
    ),
    OLED_BLACK(
        titleRes = R.string.widget_theme_oled_black,
        previewBgColor = 0xFF000000,
        previewAccentColor = 0xFF34D399,
        previewTextColor = 0xFFFFFFFF
    ),
    EMERALD_ISLAMIC(
        titleRes = R.string.widget_theme_emerald_islamic,
        previewBgColor = 0xFF064E3B,
        previewAccentColor = 0xFFFBBF24,
        previewTextColor = 0xFFECFDF5
    ),
    GOLDEN_HOUR(
        titleRes = R.string.widget_theme_golden_hour,
        previewBgColor = 0xFF451A03,
        previewAccentColor = 0xFFF59E0B,
        previewTextColor = 0xFFFEF3C7
    ),
    ROYAL_BLUE(
        titleRes = R.string.widget_theme_royal_blue,
        previewBgColor = 0xFF0F172A,
        previewAccentColor = 0xFF38BDF8,
        previewTextColor = 0xFFF0F9FF
    ),
    MONOCHROME(
        titleRes = R.string.widget_theme_monochrome,
        previewBgColor = 0xFF18181B,
        previewAccentColor = 0xFFE4E4E7,
        previewTextColor = 0xFFFAFAFA
    )
}

enum class WidgetBackgroundStyle(@StringRes val titleRes: Int) {
    TRANSLUCENT(R.string.widget_bgstyle_translucent),
    SOLID_SURFACE(R.string.widget_bgstyle_solid_surface),
    FROSTED_GLASS(R.string.widget_bgstyle_frosted_glass),
    MINIMAL_BORDER(R.string.widget_bgstyle_minimal_border),
    TRANSPARENT_CLEAN(R.string.widget_bgstyle_transparent_clean)
}

enum class WidgetFontSize(@StringRes val titleRes: Int, val scaleFactor: Float) {
    COMPACT(R.string.widget_fontsize_compact, 0.88f),
    STANDARD(R.string.widget_fontsize_standard, 1.0f),
    LARGE(R.string.widget_fontsize_large, 1.15f),
    EXTRA_LARGE(R.string.widget_fontsize_extra_large, 1.30f)
}

enum class WidgetTextStyle(@StringRes val titleRes: Int) {
    AUTO(R.string.widget_textstyle_auto),
    LIGHT(R.string.widget_textstyle_light),
    DARK(R.string.widget_textstyle_dark)
}

enum class WidgetHeroTimeMode(@StringRes val titleRes: Int) {
    NEXT(R.string.widget_herotime_next),
    PREVIOUS(R.string.widget_herotime_previous),
    BOTH(R.string.widget_herotime_both)
}

data class WidgetCustomizationSettings(
    val themeMode: WidgetThemeMode = WidgetThemeMode.APP_THEME,
    val bgStyle: WidgetBackgroundStyle = WidgetBackgroundStyle.TRANSLUCENT,
    val opacityPercent: Int = 85,
    val fontSize: WidgetFontSize = WidgetFontSize.STANDARD,
    val textStyle: WidgetTextStyle = WidgetTextStyle.AUTO,
    val heroTimeMode: WidgetHeroTimeMode = WidgetHeroTimeMode.NEXT,
    val showLocation: Boolean = true,
    val showHijriDate: Boolean = true,
    val showCountdown: Boolean = true,
    val showProgressBar: Boolean = true,
    val showSunrise: Boolean = true,
    val showAllPrayersList: Boolean = true,
    val showHeroCard: Boolean = true
)
