package com.prayertimes.widget.glance

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prayertimes.MainActivity
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.cities.CityDatabase
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.WidgetCustomizationSettings
import com.prayertimes.data.models.WidgetHeroTimeMode
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.util.LocalizedStrings
import com.prayertimes.widget.PrayerAppWidgetProvider
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class LayoutFamily {
    MINIMAL, HORIZONTAL, COMPACT, TWO_COLUMN, LARGE_RIBBON, VERTICAL_SCHEDULE, SCHEDULE
}

/**
 * A launcher-independent description derived only from the exact dp rectangle supplied by the
 * widget host. The families represent genuinely different information structures; measurements
 * inside each family remain fluid, and increasing width within a height band only adds content.
 */
private data class AdaptiveLayout(
    val family: LayoutFamily,
    val paddingDp: Float,
    val gapDp: Float,
    val fontScale: Float,
    val widthDp: Float,
    val heightDp: Float,
    val denseRows: Boolean,
    val sideBySideSchedule: Boolean,
    val supportsDualHero: Boolean,
    val stackHeader: Boolean,
    val showBarCountdown: Boolean,
    val showBarMetadata: Boolean,
    val showBarPrayerRibbon: Boolean
)

private fun layoutForSize(widthDp: Float, heightDp: Float): AdaptiveLayout {
    val width = widthDp.coerceAtLeast(1f)
    val height = heightDp.coerceAtLeast(1f)
    val shortestSide = minOf(width, height)
    val aspectRatio = width / height
    val padding = (shortestSide * 0.07f).coerceIn(5f, 11f)
    val gap = (shortestSide * 0.045f).coerceIn(3f, 8f)

    val family = when {
        width < 80f || height < 48f -> LayoutFamily.MINIMAL
        height < 120f -> LayoutFamily.HORIZONTAL
        height >= 270f && aspectRatio < 0.85f -> LayoutFamily.VERTICAL_SCHEDULE
        height >= 270f -> LayoutFamily.SCHEDULE
        width >= 240f -> LayoutFamily.LARGE_RIBBON
        height >= 220f && aspectRatio < 0.85f -> LayoutFamily.VERTICAL_SCHEDULE
        height >= 220f -> LayoutFamily.SCHEDULE
        aspectRatio < 1.35f -> LayoutFamily.COMPACT
        else -> LayoutFamily.TWO_COLUMN
    }

    val fontScale = when (family) {
        LayoutFamily.MINIMAL -> (0.82f + (shortestSide - 40f) / 160f).coerceIn(0.82f, 0.95f)
        LayoutFamily.HORIZONTAL -> (0.88f + (height - 48f) / 400f).coerceIn(0.88f, 1.05f)
        LayoutFamily.COMPACT, LayoutFamily.TWO_COLUMN ->
            (0.94f + (shortestSide - 100f) / 420f).coerceIn(0.94f, 1.18f)
        LayoutFamily.LARGE_RIBBON ->
            (1.05f + (shortestSide - 140f) / 320f).coerceIn(1.05f, 1.30f)
        LayoutFamily.VERTICAL_SCHEDULE, LayoutFamily.SCHEDULE ->
            (1.05f + (shortestSide - 160f) / 400f).coerceIn(1.05f, 1.35f)
    }

    return AdaptiveLayout(
        family = family,
        paddingDp = padding,
        gapDp = gap,
        fontScale = fontScale,
        widthDp = width,
        heightDp = height,
        denseRows = height < 360f,
        sideBySideSchedule = family == LayoutFamily.SCHEDULE && width >= 340f && aspectRatio >= 1.45f,
        supportsDualHero = width >= 220f,
        stackHeader = width < 300f,
        showBarCountdown = family == LayoutFamily.HORIZONTAL && width >= 200f,
        showBarMetadata = family == LayoutFamily.HORIZONTAL && width >= 280f,
        showBarPrayerRibbon = family == LayoutFamily.HORIZONTAL && width >= 430f
    )
}

/**
 * Glance port of the RemoteViews widget. Exact sizing lets one composable adapt to the real
 * launcher allocation instead of relying on grid-cell names or a fixed candidate canvas.
 */
class PrayerGlanceWidget : GlanceAppWidget() {

    companion object {
        fun refreshAll(context: Context) {
            PrayerApplication.instance.applicationScope.launch {
                runCatching { PrayerGlanceWidget().updateAll(context) }
            }
        }
    }

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = PrayerPreferences.getInitialSettings(context)
        val isArabic = settings.language.resolveIsArabic()
        val localizedRes = LocalizedStrings.forLanguage(context, isArabic)
        val zoneId = runCatching { ZoneId.of(settings.location.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()

        fun schedule(date: LocalDate) = PrayerTimesCalculator.calculateDailySchedule(
            date = date,
            latitude = settings.location.latitude,
            longitude = settings.location.longitude,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustments = settings.adjustments,
            hijriAdjustmentDays = settings.hijriAdjustmentDays,
            now = now
        )

        val todaySchedule = schedule(today)
        val nextToday = todaySchedule.prayerItems.firstOrNull {
            it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(now)
        }
        val isTomorrowFajr = nextToday == null
        val next = nextToday ?: schedule(today.plusDays(1)).prayerItems.first { it.type == PrayerType.FAJR }

        val previous = todaySchedule.prayerItems
            .filter { it.type != PrayerType.SUNRISE && it.zonedDateTime.isBefore(next.zonedDateTime) }
            .lastOrNull()
        val previousType = previous?.type ?: PrayerType.ISHA
        val previousTime = previous?.zonedDateTime ?: today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId)

        val untilSeconds = Duration.between(now, next.zonedDateTime).seconds.coerceAtLeast(0)
        val sinceSeconds = Duration.between(previousTime, now).seconds.coerceAtLeast(0)

        val timeFormatter = if (settings.is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }
        val widgetSettings = settings.widgetSettings
        val showingPrevious = widgetSettings.heroTimeMode == WidgetHeroTimeMode.PREVIOUS
        val colors = PrayerAppWidgetProvider.resolveWidgetColors(context, settings)
        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }

        fun slot(type: PrayerType) = MiniSlot(
            name = prayerName(type, settings.language),
            time = prayerMap[type]?.time?.format(timeFormatter) ?: "--:--",
            isActive = !isTomorrowFajr && type == next.type
        )

        // All six prayers (Sunrise optionally hidden) - matches populatePrayerRibbon /
        // buildVerticalWidget's own bindRow calls for every PrayerType, not a truncated subset.
        val allSlots = listOf(PrayerType.FAJR, PrayerType.SUNRISE, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)
            .filter { it != PrayerType.SUNRISE || widgetSettings.showSunrise }
            .map(::slot)
        // Medium's 3-slot split is a fixed Dhuhr/Asr/Maghrib selection (matches
        // buildMediumWidget's own hardcoded prayerList), not "the first 3 prayers of the day".
        val mediumSlots = listOf(PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB).map(::slot)

        val data = GlanceWidgetData(
            prayerName = prayerName(if (showingPrevious) previousType else next.type, settings.language),
            prayerTime = (if (showingPrevious) previousTime else next.zonedDateTime).format(timeFormatter),
            countdown = if (showingPrevious) sinceText(context, sinceSeconds, isArabic) else countdownText(context, untilSeconds, isArabic),
            previousName = prayerName(previousType, settings.language),
            previousTime = previousTime.format(timeFormatter),
            since = sinceText(context, sinceSeconds, isArabic),
            nextName = prayerName(next.type, settings.language),
            nextTime = next.zonedDateTime.format(timeFormatter),
            until = countdownText(context, untilSeconds, isArabic),
            locationText = CityDatabase.localizedName(localizedRes, settings.location)
                .ifBlank { CityDatabase.localizedCountry(localizedRes, settings.location) },
            hijriText = if (isArabic) {
                todaySchedule.hijriDate?.formattedAr ?: todaySchedule.hijriDateString
            } else {
                todaySchedule.hijriDate?.formattedEn ?: todaySchedule.hijriDateString
            },
            allSlots = allSlots,
            mediumSlots = mediumSlots,
            widgetSettings = widgetSettings,
            fontScale = colors.fontScale,
            rootBg = Color(colors.rootBgColor),
            rootBorder = Color(colors.rootBorderColor),
            heroBg = Color(colors.heroBgColor),
            accent = Color(colors.accentColor),
            textPrimary = Color(colors.textPrimaryColor),
            textSecondary = Color(colors.textSecondaryColor),
            textOnAccent = Color(colors.textOnAccentColor),
            inactivePrayerBg = Color(colors.inactivePrayerBgColor),
            activePrayerBg = Color(colors.activePrayerBgColor)
        )

        provideContent { WidgetContent(data) }
    }

    private fun prayerName(type: PrayerType, language: AppLanguage): String {
        val arabicNames = mapOf(
            PrayerType.FAJR to "الفجر", PrayerType.SUNRISE to "الشروق", PrayerType.DHUHR to "الظهر",
            PrayerType.ASR to "العصر", PrayerType.MAGHRIB to "المغرب", PrayerType.ISHA to "العشاء"
        )
        val useArabic = language == AppLanguage.ARABIC ||
            (language == AppLanguage.SYSTEM && Locale.getDefault().language.equals("ar", ignoreCase = true))
        return if (useArabic) arabicNames.getValue(type) else type.title
    }

    private fun countdownText(context: Context, seconds: Long, isArabic: Boolean): String {
        val res = LocalizedStrings.forLanguage(context, isArabic)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            seconds <= 0 -> res.getString(R.string.widget_countdown_now)
            hours > 0 -> res.getString(R.string.widget_countdown_hours_minutes, hours, minutes)
            minutes > 0 -> res.getString(R.string.widget_countdown_minutes_only, minutes)
            else -> res.getString(R.string.widget_countdown_less_than_min)
        }
    }

    private fun sinceText(context: Context, seconds: Long, isArabic: Boolean): String {
        val res = LocalizedStrings.forLanguage(context, isArabic)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            seconds <= 60 -> res.getString(R.string.widget_since_just_now)
            hours > 0 -> res.getString(R.string.widget_since_hours_minutes, hours, minutes)
            else -> res.getString(R.string.widget_since_minutes_only, minutes)
        }
    }
}

/** Tapping the small refresh icon re-renders just this widget instance with fresh data. */
class RefreshGlanceWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PrayerGlanceWidget().update(context, glanceId)
    }
}

// internal (not private) so the debug-only @Preview functions in PrayerGlanceWidgetPreviews.kt
// (app/src/debug/) can build sample data and call WidgetContent directly.
internal data class MiniSlot(val name: String, val time: String, val isActive: Boolean)

internal data class GlanceWidgetData(
    val prayerName: String,
    val prayerTime: String,
    val countdown: String,
    val previousName: String,
    val previousTime: String,
    val since: String,
    val nextName: String,
    val nextTime: String,
    val until: String,
    val locationText: String,
    val hijriText: String,
    val allSlots: List<MiniSlot>,
    val mediumSlots: List<MiniSlot>,
    val widgetSettings: WidgetCustomizationSettings,
    val fontScale: Float,
    val rootBg: Color,
    val rootBorder: Color,
    val heroBg: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnAccent: Color,
    val inactivePrayerBg: Color,
    val activePrayerBg: Color
)

private fun scaledSp(baseSp: Float, data: GlanceWidgetData): TextUnit = (baseSp * data.fontScale).sp

@Composable
internal fun WidgetContent(data: GlanceWidgetData) {
    val size = LocalSize.current
    val layout = layoutForSize(size.width.value, size.height.value)
    val fittedData = data.copy(fontScale = layout.fontScale)
    val context = LocalContext.current
    // Glance doesn't mirror Row child order for RTL locales the way native RemoteViews/View
    // layoutDirection does (confirmed on-device: Arabic text shapes correctly within each Text,
    // but multi-child Rows stayed in LTR visual order) - so child order is swapped manually
    // below, the same fix PrayerAppWidgetProvider already applies via setLayoutDirection().
    val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    CardSurface(fittedData.rootBg, fittedData.rootBorder) {
        when (layout.family) {
            LayoutFamily.MINIMAL -> MicroContent(fittedData)
            LayoutFamily.HORIZONTAL -> SlimContent(fittedData, isRtl, layout)
            LayoutFamily.VERTICAL_SCHEDULE -> VerticalContent(fittedData, isRtl, layout)
            LayoutFamily.COMPACT -> SmallContent(fittedData, isRtl, layout)
            LayoutFamily.TWO_COLUMN -> MediumContent(fittedData, isRtl, layout)
            LayoutFamily.LARGE_RIBBON -> LargeRibbonContent(fittedData, isRtl, layout)
            LayoutFamily.SCHEDULE -> LargeContent(fittedData, isRtl, layout)
        }
    }
}

/** Two-layer Box trick for a stroked card - Glance has no native border() modifier. */
@Composable
private fun CardSurface(rootBg: Color, borderColor: Color, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val clickModifier = GlanceModifier
        .fillMaxSize()
        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))

    if (borderColor != Color.Transparent) {
        Box(clickModifier.background(ColorProvider(borderColor)).cornerRadius(20.dp)) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(ColorProvider(rootBg))
                    .cornerRadius(19.dp)
            ) {
                content()
            }
        }
    } else {
        Box(clickModifier.background(ColorProvider(rootBg)).cornerRadius(20.dp)) {
            content()
        }
    }
}

@Composable
private fun MicroContent(data: GlanceWidgetData) {
    Box(GlanceModifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
        Text(
            data.prayerTime,
            style = TextStyle(color = ColorProvider(data.textPrimary), fontSize = scaledSp(13f, data), fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable
private fun SlimContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heroText = @Composable {
            Text(
                "${data.prayerName}  ${data.prayerTime}",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(data.textPrimary),
                    fontSize = scaledSp(13f, data),
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                ),
                maxLines = 1
            )
        }
        if (isRtl) {
            if (data.widgetSettings.showCountdown && layout.showBarCountdown) CountdownPill(data.countdown, data)
            Spacer(GlanceModifier.width(layout.gapDp.dp))
            heroText()
        } else {
            heroText()
            Spacer(GlanceModifier.width(layout.gapDp.dp))
            if (data.widgetSettings.showCountdown && layout.showBarCountdown) CountdownPill(data.countdown, data)
        }
        if (layout.showBarMetadata && (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate)) {
            Spacer(GlanceModifier.width(layout.gapDp.dp))
            val metadata = buildString {
                if (data.widgetSettings.showLocation) append(data.locationText)
                if (data.widgetSettings.showHijriDate) {
                    if (isNotEmpty()) append(" • ")
                    append(data.hijriText)
                }
            }
            Text(
                metadata,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(data.textSecondary),
                    fontSize = scaledSp(9f, data),
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                ),
                maxLines = 1
            )
        }
        if (layout.showBarPrayerRibbon && data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.width(layout.gapDp.dp))
            Box(GlanceModifier.defaultWeight()) {
                PrayerRibbon(data.mediumSlots, data, isRtl, chipGapDp = 1f)
            }
        }
        RefreshButton(data)
    }
}

private fun fittedScheduleSlots(data: GlanceWidgetData, layout: AdaptiveLayout): List<MiniSlot> {
    val slots = data.allSlots
    if (slots.size <= 3) return slots

    val hasHeader = data.widgetSettings.showLocation || data.widgetSettings.showHijriDate
    val stackedHeader = layout.stackHeader &&
        data.widgetSettings.showLocation && data.widgetSettings.showHijriDate
    val headerHeight = when {
        !hasHeader -> 0f
        stackedHeader -> 40f
        else -> 40f
    }
    val heroHeight = when {
        !data.widgetSettings.showHeroCard -> 0f
        // Reserve the largest hero that this rectangle supports. Row count must remain stable
        // when the user switches NEXT/PREVIOUS/BOTH without resizing the widget itself.
        layout.supportsDualHero -> 105f
        else -> 74f
    }
    val sectionGapCount = (if (data.widgetSettings.showHeroCard) 1 else 0) +
        (if (data.widgetSettings.showAllPrayersList) 1 else 0)
    val availableForRows = layout.heightDp -
        (layout.paddingDp * 2f) - headerHeight - heroHeight - (layout.gapDp * sectionGapCount)
    val rowHeight = availableForRows / slots.size
    val minimumReadableRowHeight = (18f + 6f * layout.fontScale).coerceIn(24f, 27f)
    if (rowHeight >= minimumReadableRowHeight) return slots

    val firstUpcoming = slots.indexOfFirst { it.isActive }.takeIf { it >= 0 } ?: 0
    val visibleCount = (availableForRows / minimumReadableRowHeight)
        .toInt()
        .coerceIn(2, slots.size)
    return List(visibleCount) { offset -> slots[(firstUpcoming + offset) % slots.size] }
}

@Composable
private fun VerticalContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Column(GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate) {
            LocationHeader(data, isRtl, showHijri = true, stacked = layout.stackHeader)
        }
        if (data.widgetSettings.showHeroCard) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            if (data.widgetSettings.heroTimeMode == WidgetHeroTimeMode.BOTH && layout.supportsDualHero) {
                DualHeroCard(data, isRtl)
            } else {
                HeroCard(data, compact = true, isRtl = isRtl)
            }
        }
        if (data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            val slots = fittedScheduleSlots(data, layout)
            // Nested Column for the same RemoteViews 10-direct-children reason as LargeContent's
            // expanded branch - cheap insurance even though 6 slots is under the limit today.
            Column(GlanceModifier.defaultWeight()) {
                slots.forEach {
                    Box(GlanceModifier.defaultWeight()) {
                        PrayerListRow(
                            it,
                            data,
                            dense = layout.denseRows && slots.size > 3,
                            isRtl = isRtl,
                            fillAvailable = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Column(GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp)) {
        if (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate) {
            LocationHeader(data, isRtl, showHijri = true, stacked = layout.stackHeader)
        }
        if (data.widgetSettings.showHeroCard) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            Box(GlanceModifier.defaultWeight()) {
                HeroCard(data, compact = true, isRtl = isRtl, fillAvailable = true)
            }
        }
    }
}

@Composable
private fun MediumContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Row(GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp), verticalAlignment = Alignment.CenterVertically) {
        if (data.widgetSettings.showHeroCard) {
            Box(GlanceModifier.defaultWeight()) {
                HeroCard(data, compact = true, isRtl = isRtl, fillAvailable = true)
            }
        }
        Spacer(GlanceModifier.width(layout.gapDp.dp))
        Column(GlanceModifier.defaultWeight()) {
            if (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate) {
                LocationHeader(data, isRtl, showHijri = true, stacked = layout.stackHeader)
            }
            if (data.widgetSettings.showAllPrayersList) {
                Spacer(GlanceModifier.height(layout.gapDp.dp))
                PrayerRibbon(data.mediumSlots, data, isRtl)
            }
        }
    }
}

@Composable
private fun LargeRibbonContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Column(GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp)) {
        if (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate) {
            LocationHeader(data, isRtl, showHijri = true, stacked = layout.stackHeader)
        }
        val showDualHero = data.widgetSettings.heroTimeMode == WidgetHeroTimeMode.BOTH
        if (data.widgetSettings.showHeroCard) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            if (showDualHero) {
                DualHeroCard(data, isRtl, compact = true)
            } else {
                HeroCard(data, compact = false, isRtl = isRtl)
            }
        }
        if (data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            Box(GlanceModifier.defaultWeight()) {
                PrayerRibbon(data.allSlots, data, isRtl, fillAvailable = true)
            }
        }
    }
}

@Composable
private fun LargeContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Column(GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp)) {
        if (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate) {
            LocationHeader(data, isRtl, showHijri = true, stacked = layout.stackHeader)
        }
        val showDualHero = data.widgetSettings.heroTimeMode == WidgetHeroTimeMode.BOTH
        val heroSection = @Composable {
            if (data.widgetSettings.showHeroCard) {
                if (showDualHero) DualHeroCard(data, isRtl) else HeroCard(data, compact = false, isRtl = isRtl)
            }
        }
        val schedule = @Composable {
            if (data.widgetSettings.showAllPrayersList) {
                val slots = fittedScheduleSlots(data, layout)
                // Keep prayer rows in one nested container: RemoteViews limits a container to ten
                // direct children, and settings may enable all six rows plus the other sections.
                Column(GlanceModifier.fillMaxSize()) {
                    slots.forEach {
                        Box(GlanceModifier.defaultWeight()) {
                            PrayerListRow(
                                it,
                                data,
                                dense = layout.denseRows && slots.size > 3,
                                isRtl = isRtl,
                                fillAvailable = true
                            )
                        }
                    }
                }
            }
        }

        if (data.widgetSettings.showHeroCard || data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
        }
        if (layout.sideBySideSchedule && data.widgetSettings.showHeroCard && data.widgetSettings.showAllPrayersList) {
            Row(
                GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) { heroSection() }
                Spacer(GlanceModifier.width(layout.gapDp.dp))
                Box(GlanceModifier.defaultWeight()) { schedule() }
            }
        } else {
            heroSection()
            if (data.widgetSettings.showHeroCard && data.widgetSettings.showAllPrayersList) {
                Spacer(GlanceModifier.height(layout.gapDp.dp))
            }
            if (data.widgetSettings.showAllPrayersList) {
                Box(GlanceModifier.defaultWeight()) { schedule() }
            }
        }
    }
}

@Composable
private fun LocationHeader(
    data: GlanceWidgetData,
    isRtl: Boolean,
    showHijri: Boolean = false,
    stacked: Boolean = false
) {
    val label = buildString {
        if (data.widgetSettings.showLocation) append(data.locationText)
        if (showHijri && data.widgetSettings.showHijriDate) {
            if (isNotEmpty()) append(" • ")
            append(data.hijriText)
        }
    }
    val labelText = @Composable { text: String, modifier: GlanceModifier ->
        Text(
            text,
            modifier = modifier,
            style = TextStyle(
                color = ColorProvider(data.textSecondary),
                fontSize = scaledSp(10f, data),
                textAlign = if (isRtl) TextAlign.End else TextAlign.Start
            ),
            maxLines = 1
        )
    }

    if (stacked && data.widgetSettings.showLocation && showHijri && data.widgetSettings.showHijriDate) {
        val stackedLabels = @Composable { modifier: GlanceModifier ->
            Column(modifier) {
                Text(
                    data.locationText,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(data.textSecondary),
                        fontSize = scaledSp(10f, data),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    maxLines = 1
                )
                Text(
                    data.hijriText,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(data.textSecondary),
                        fontSize = scaledSp(9f, data),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    maxLines = 1
                )
            }
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (isRtl) {
                RefreshButton(data)
                Spacer(GlanceModifier.width(5.dp))
                stackedLabels(GlanceModifier.defaultWeight())
            } else {
                stackedLabels(GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(5.dp))
                RefreshButton(data)
            }
        }
    } else {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (isRtl) {
                RefreshButton(data)
                Spacer(GlanceModifier.width(5.dp))
                labelText(label, GlanceModifier.defaultWeight())
            } else {
                labelText(label, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(5.dp))
                RefreshButton(data)
            }
        }
    }
}

@Composable
private fun RefreshButton(data: GlanceWidgetData) {
    Box(
        modifier = GlanceModifier.size(40.dp).clickable(actionRunCallback<RefreshGlanceWidgetAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(ColorProvider(data.textSecondary))
        )
    }
}

@Composable
private fun HeroCard(
    data: GlanceWidgetData,
    compact: Boolean,
    isRtl: Boolean,
    fillAvailable: Boolean = false
) {
    if (compact) {
        // Matches PrayerAppWidgetProvider's vertical-tier XML: name, time, and the countdown
        // pill each get their own centered line. The small tier's XML instead puts the pill
        // beside a weighted name+time column, but at ~110dp wide that leaves the pill and the
        // name fighting over the same few dp - a long countdown string ("in 7h 35m") squeezed
        // the name down to a single ellipsized letter. Stacking avoids that regardless of tier.
        Box(
            modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
                .background(ColorProvider(data.heroBg))
                .cornerRadius(13.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    data.prayerName,
                    style = TextStyle(color = ColorProvider(data.accent), fontSize = scaledSp(11f, data), fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    data.prayerTime,
                    style = TextStyle(color = ColorProvider(data.textPrimary), fontSize = scaledSp(17f, data), fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (data.widgetSettings.showCountdown) {
                    Spacer(GlanceModifier.height(4.dp))
                    CountdownPill(data.countdown, data)
                }
            }
        }
        return
    }

    // Matches PrayerAppWidgetProvider's large/expanded XML hero layout: name+time are grouped
    // in one weighted column and the pill sits beside that whole block.
    Row(
        modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
            .background(ColorProvider(data.heroBg))
            .cornerRadius(13.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val nameAndTime = @Composable {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
            ) {
                Text(
                    data.prayerName,
                    style = TextStyle(color = ColorProvider(data.accent), fontSize = scaledSp(13f, data), fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    data.prayerTime,
                    style = TextStyle(color = ColorProvider(data.textPrimary), fontSize = scaledSp(23f, data), fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
        if (isRtl) {
            if (data.widgetSettings.showCountdown) CountdownPill(data.countdown, data)
            Spacer(GlanceModifier.width(6.dp))
            nameAndTime()
        } else {
            nameAndTime()
            Spacer(GlanceModifier.width(6.dp))
            if (data.widgetSettings.showCountdown) CountdownPill(data.countdown, data)
        }
    }
}

@Composable
private fun DualHeroCard(data: GlanceWidgetData, isRtl: Boolean, compact: Boolean = false) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(data.heroBg))
            .cornerRadius(13.dp)
            .padding(if (compact) 5.dp else 9.dp)
    ) {
        val previousHalf = @Composable {
            HeroHalf(data.previousName, data.previousTime, data.since, data, GlanceModifier.defaultWeight(), isRtl, compact)
        }
        val nextHalf = @Composable {
            HeroHalf(data.nextName, data.nextTime, data.until, data, GlanceModifier.defaultWeight(), isRtl, compact)
        }
        if (isRtl) {
            nextHalf(); Spacer(GlanceModifier.width(if (compact) 6.dp else 10.dp)); previousHalf()
        } else {
            previousHalf(); Spacer(GlanceModifier.width(if (compact) 6.dp else 10.dp)); nextHalf()
        }
    }
}

@Composable
private fun HeroHalf(
    name: String,
    time: String,
    detail: String,
    data: GlanceWidgetData,
    modifier: GlanceModifier,
    isRtl: Boolean,
    compact: Boolean
) {
    Column(modifier, horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start) {
        Text(name, style = TextStyle(color = ColorProvider(data.accent), fontSize = scaledSp(if (compact) 10f else 11f, data), fontWeight = FontWeight.Medium), maxLines = 1)
        Text(time, style = TextStyle(color = ColorProvider(data.textPrimary), fontSize = scaledSp(if (compact) 16f else 18f, data), fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.height(if (compact) 2.dp else 3.dp))
        CountdownPill(detail, data)
    }
}

@Composable
private fun CountdownPill(text: String, data: GlanceWidgetData) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(data.activePrayerBg))
            .cornerRadius(10.dp)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, style = TextStyle(color = ColorProvider(data.textOnAccent), fontSize = scaledSp(9f, data), fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun PrayerRibbon(
    slots: List<MiniSlot>,
    data: GlanceWidgetData,
    isRtl: Boolean,
    chipGapDp: Float = 3f,
    fillAvailable: Boolean = false
) {
    Row(if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth()) {
        (if (isRtl) slots.reversed() else slots).forEach { slot ->
            // A single chained .padding().background() doesn't reliably inset the background in
            // Glance (confirmed on-device: the chips rendered edge-to-edge with zero gap despite
            // horizontal padding before .background()) - unlike CardSurface's border trick, which
            // works because it uses two separate nested containers. Same fix here: an outer,
            // uncolored Box carries the weight and the gap-via-padding; the inner Column carries
            // the background and is what actually gets tinted.
            val outerModifier = if (fillAvailable) {
                GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = chipGapDp.dp)
            } else {
                GlanceModifier.defaultWeight().padding(horizontal = chipGapDp.dp)
            }
            Box(modifier = outerModifier) {
                Box(
                    modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
                        .background(ColorProvider(if (slot.isActive) data.activePrayerBg else data.inactivePrayerBg))
                        .cornerRadius(8.dp)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val baseFontSize = if (fillAvailable) 10f else 8f
                        Text(slot.name, style = TextStyle(color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary), fontSize = scaledSp(baseFontSize, data)), maxLines = 1)
                        Text(slot.time, style = TextStyle(color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textPrimary), fontSize = scaledSp(baseFontSize, data), fontWeight = FontWeight.Medium), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerListRow(
    slot: MiniSlot,
    data: GlanceWidgetData,
    dense: Boolean,
    isRtl: Boolean,
    fillAvailable: Boolean = false
) {
    // Same fix as PrayerRibbon: the vertical gap has to come from an outer, uncolored container's
    // padding, not a padding chained before .background() on the same Row.
    Box(
        modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
            .padding(vertical = 1.dp)
    ) {
        Row(
            modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
                .background(ColorProvider(if (slot.isActive) data.activePrayerBg else data.inactivePrayerBg))
                .cornerRadius(7.dp)
                .padding(horizontal = 7.dp, vertical = if (dense) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val nameText = @Composable {
                Text(
                    slot.name,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary),
                        fontSize = scaledSp(if (dense) 8f else 10f, data),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    maxLines = 1
                )
            }
            val timeText = @Composable {
                Text(
                    slot.time,
                    style = TextStyle(color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textPrimary), fontSize = scaledSp(if (dense) 8f else 10f, data), fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
            if (isRtl) {
                timeText(); Spacer(GlanceModifier.width(6.dp)); nameText()
            } else {
                nameText(); timeText()
            }
        }
    }
}
