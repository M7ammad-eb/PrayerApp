@file:OptIn(androidx.glance.preview.ExperimentalGlancePreviewApi::class)

package com.prayertimes.widget.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.preview.Preview
import com.prayertimes.data.models.WidgetCustomizationSettings

/**
 * Debug-only previews of representative widget rectangles for Android Studio's Preview pane
 * without a full build/install/screenshot round trip. Only exists in the debug source set
 * because glance-appwidget-preview/glance-preview are debugImplementation - a release build
 * never sees this file. Sample data is hand-written (not pulled from PrayerPreferences or
 * PrayerTimesCalculator) since @Preview composables don't run suspend/Context-dependent code.
 */

private val sampleColors = SampleColors()

private class SampleColors {
    val rootBg = Color(0xFF1C1B1F)
    val rootBorder = Color.Transparent
    val heroBg = Color(0xFF2A384C)
    val accent = Color(0xFF34D399)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)
    val textOnAccent = Color(0xFF0F172A)
    val inactivePrayerBg = Color(0xFF334155)
    val activePrayerBg = Color(0xFF34D399)
}

private fun sampleData(showAllPrayersList: Boolean = true, heroTimeMode: com.prayertimes.data.models.WidgetHeroTimeMode = com.prayertimes.data.models.WidgetHeroTimeMode.NEXT) = GlanceWidgetData(
    prayerName = "العصر",
    prayerTime = "3:47 PM",
    countdown = "In 1h 12m",
    previousName = "الظهر",
    previousTime = "12:24 PM",
    since = "42m ago",
    nextName = "العصر",
    nextTime = "3:47 PM",
    until = "In 1h 12m",
    locationText = "Al Madinah",
    hijriText = "10 Rabi' I 1448",
    progress = 0.4f,
    allSlots = listOf(
        MiniSlot("الفجر", "4:38 AM", false),
        MiniSlot("الشروق", "5:58 AM", false),
        MiniSlot("الظهر", "12:24 PM", false),
        MiniSlot("العصر", "3:47 PM", true),
        MiniSlot("المغرب", "6:50 PM", false),
        MiniSlot("العشاء", "8:20 PM", false)
    ),
    mediumSlots = listOf(
        MiniSlot("الظهر", "12:24 PM", false),
        MiniSlot("العصر", "3:47 PM", true),
        MiniSlot("المغرب", "6:50 PM", false)
    ),
    widgetSettings = WidgetCustomizationSettings(showAllPrayersList = showAllPrayersList, heroTimeMode = heroTimeMode),
    fontScale = 1f,
    rootBg = sampleColors.rootBg,
    rootBorder = sampleColors.rootBorder,
    heroBg = sampleColors.heroBg,
    accent = sampleColors.accent,
    textPrimary = sampleColors.textPrimary,
    textSecondary = sampleColors.textSecondary,
    textOnAccent = sampleColors.textOnAccent,
    inactivePrayerBg = sampleColors.inactivePrayerBg,
    activePrayerBg = sampleColors.activePrayerBg
)

@Composable
@Preview(widthDp = 40, heightDp = 40)
private fun MicroPreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 90, heightDp = 40)
private fun SlimPreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 40, heightDp = 110)
private fun VerticalPreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 110, heightDp = 100)
private fun SmallPreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 230, heightDp = 90)
private fun MediumPreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 250, heightDp = 180)
private fun LargePreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 250, heightDp = 320)
private fun ExpandedPreview() {
    WidgetContent(sampleData())
}

@Composable
@Preview(widthDp = 250, heightDp = 320)
private fun ExpandedDualHeroPreview() {
    WidgetContent(sampleData(heroTimeMode = com.prayertimes.data.models.WidgetHeroTimeMode.BOTH))
}
