package com.prayertimes.widget.glance

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.layout.ContentScale
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
import com.prayertimes.data.calculator.CurrentPrayerResolver
import com.prayertimes.data.cities.CityDatabase
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.models.WidgetCustomizationSettings
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.util.LocalizedStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    val systemFontScale: Float,
    val widthDp: Float,
    val heightDp: Float,
    val denseRows: Boolean,
    val sideBySideSchedule: Boolean,
    val stackHeader: Boolean,
    val showBarCountdown: Boolean,
    val showBarMetadata: Boolean,
    val showBarPrayerRibbon: Boolean
)

private fun layoutForSize(widthDp: Float, heightDp: Float, systemFontScale: Float): AdaptiveLayout {
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
        LayoutFamily.MINIMAL -> (0.95f + (shortestSide - 40f) / 160f).coerceIn(0.95f, 1.08f)
        LayoutFamily.HORIZONTAL -> (1.0f + (height - 48f) / 320f).coerceIn(1.0f, 1.15f)
        LayoutFamily.COMPACT, LayoutFamily.TWO_COLUMN ->
            (1.0f + (shortestSide - 100f) / 360f).coerceIn(1.0f, 1.22f)
        LayoutFamily.LARGE_RIBBON ->
            (1.08f + (shortestSide - 140f) / 300f).coerceIn(1.08f, 1.32f)
        LayoutFamily.VERTICAL_SCHEDULE, LayoutFamily.SCHEDULE ->
            (1.10f + (shortestSide - 160f) / 360f).coerceIn(1.10f, 1.38f)
    }

    return AdaptiveLayout(
        family = family,
        paddingDp = padding,
        gapDp = gap,
        fontScale = fontScale,
        systemFontScale = systemFontScale.coerceIn(0.85f, 2f),
        widthDp = width,
        heightDp = height,
        denseRows = height < 360f,
        sideBySideSchedule = family == LayoutFamily.SCHEDULE && width >= 340f && aspectRatio >= 1.45f,
        stackHeader = width < 300f,
        showBarCountdown = family == LayoutFamily.HORIZONTAL && width >= 200f,
        showBarMetadata = family == LayoutFamily.HORIZONTAL && width >= 280f,
        showBarPrayerRibbon = family == LayoutFamily.HORIZONTAL && width >= 430f
    )
}

/**
 * Exact sizing lets one composable adapt to the real launcher allocation instead of relying on
 * grid-cell names or a fixed candidate canvas.
 */
class PrayerGlanceWidget : GlanceAppWidget() {

    companion object {
        private val refreshGeneration = MutableStateFlow(0L)

        internal fun signalRefresh() {
            refreshGeneration.update { it + 1L }
        }

        fun refreshAll(context: Context) {
            signalRefresh()
            PrayerApplication.instance.applicationScope.launch {
                runCatching { PrayerGlanceWidget().updateAll(context) }
            }
        }
    }

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialSettings = PrayerPreferences.getInitialSettings(context)
        val initialData = buildGlanceWidgetData(context, initialSettings)
        val dataFlow = PrayerPreferences(context).settingsFlow
            .combine(refreshGeneration) { settings, _ ->
                buildGlanceWidgetData(context, settings)
            }
            .flowOn(Dispatchers.Default)

        // A Glance composition remains alive for roughly 45 seconds. Observing both persisted
        // settings and explicit refresh requests keeps that active composition current instead of
        // reusing the immutable snapshot captured when the session first started.
        provideContent {
            val data by dataFlow.collectAsState(initialData)
            WidgetContent(data)
        }
    }

    /**
     * Builds the exact same [GlanceWidgetData] the real widget renders, from an arbitrary
     * [AppPrayerSettings] snapshot rather than always reading persisted preferences.
     * [provideGlance] calls this with the persisted settings; the settings screen's live preview
     * calls it with the screen's own in-memory (possibly unsaved) settings, then renders the
     * result through the identical [WidgetContent] composable - so the preview can never visually
     * drift from the real widget the way a hand-mirrored mockup can.
     */
    internal fun buildGlanceWidgetData(context: Context, settings: AppPrayerSettings): GlanceWidgetData {
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

        val yesterdaySchedule = schedule(today.minusDays(1))
        val todaySchedule = schedule(today)
        val tomorrowSchedule = schedule(today.plusDays(1))
        val currentPeriod = CurrentPrayerResolver.resolve(now, yesterdaySchedule, todaySchedule, tomorrowSchedule)
        val remainingSeconds = if (currentPeriod.isPrayerTimeEnded) 0L
        else Duration.between(now, currentPeriod.endsAt).seconds.coerceAtLeast(0)

        val timeFormatter = if (settings.is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }
        val widgetSettings = settings.widgetSettings
        val colors = WidgetColorResolver.resolve(context, settings)
        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }

        fun slot(type: PrayerType) = MiniSlot(
            name = prayerName(type, settings.language),
            time = prayerMap[type]?.time?.format(timeFormatter) ?: "--:--",
            isActive = !currentPeriod.isPrayerTimeEnded && type == currentPeriod.prayerItem.type
        )

        // All six prayers (Sunrise optionally hidden) - matches populatePrayerRibbon /
        // buildVerticalWidget's own bindRow calls for every PrayerType, not a truncated subset.
        val allSlots = listOf(PrayerType.FAJR, PrayerType.SUNRISE, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)
            .filter { it != PrayerType.SUNRISE || widgetSettings.showSunrise }
            .map(::slot)
        // Medium's 3-slot split is a fixed Dhuhr/Asr/Maghrib selection (matches
        // buildMediumWidget's own hardcoded prayerList), not "the first 3 prayers of the day".
        val mediumSlots = listOf(PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB).map(::slot)

        return GlanceWidgetData(
            prayerName = prayerName(currentPeriod.prayerItem.type, settings.language),
            prayerTime = currentPeriod.prayerItem.zonedDateTime.format(timeFormatter),
            countdown = if (currentPeriod.isPrayerTimeEnded) {
                localizedRes.getString(R.string.widget_fajr_time_ended)
            } else {
                remainingText(context, remainingSeconds, isArabic)
            },
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
            activePrayerBg = Color(colors.activePrayerBgColor),
            isRtl = isArabic
        )
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

    private fun remainingText(context: Context, seconds: Long, isArabic: Boolean): String {
        val res = LocalizedStrings.forLanguage(context, isArabic)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            seconds <= 0 -> res.getString(R.string.widget_time_ending_now)
            hours > 0 -> res.getString(R.string.widget_remaining_hours_minutes, hours, minutes)
            minutes > 0 -> res.getString(R.string.widget_remaining_minutes_only, minutes)
            else -> res.getString(R.string.widget_remaining_less_than_min)
        }
    }
}

/** Tapping the small refresh icon re-renders just this widget instance with fresh data. */
class RefreshGlanceWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PrayerGlanceWidget.signalRefresh()
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
    val activePrayerBg: Color,
    val isRtl: Boolean = false
)

private fun scaledSp(baseSp: Float, data: GlanceWidgetData): TextUnit = (baseSp * data.fontScale).sp

private data class TimeParts(val clock: String, val suffix: String?)

private fun splitTime(text: String): TimeParts {
    val separator = text.lastIndexOf(' ')
    if (separator <= 0 || separator == text.lastIndex) return TimeParts(text, null)
    val suffix = text.substring(separator + 1)
    return if (suffix in setOf("ص", "م", "AM", "PM", "am", "pm")) {
        TimeParts(text.substring(0, separator), suffix)
    } else {
        TimeParts(text, null)
    }
}

/**
 * Keeps the clock and its period in a stable physical order. Leaving them in one bidi Text lets
 * Arabic context move ص/م to different sides, and gives the one-letter suffix the Hero's huge
 * number font metrics. A smaller separate suffix fixes both issues.
 */
@Composable
private fun TimeText(
    text: String,
    baseSp: Float,
    data: GlanceWidgetData,
    color: Color,
    fontWeight: FontWeight = FontWeight.Medium,
    modifier: GlanceModifier = GlanceModifier,
    suffixScale: Float = 0.72f
) {
    val parts = splitTime(text)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val clockText = @Composable {
            Text(
                parts.clock,
                style = TextStyle(
                    color = ColorProvider(color),
                    fontSize = scaledSp(baseSp, data),
                    fontWeight = fontWeight
                ),
                maxLines = 1
            )
        }
        val suffixText = @Composable { suffix: String ->
            Text(
                suffix,
                style = TextStyle(
                    color = ColorProvider(color),
                    fontSize = scaledSp(baseSp * suffixScale, data),
                    fontWeight = fontWeight
                ),
                maxLines = 1
            )
        }
        val suffix = parts.suffix
        if (suffix == "ص" || suffix == "م") {
            // Glance Rows keep physical LTR child order. The Arabic suffix belongs visually on
            // the left of the clock (after it in RTL reading order).
            suffixText(suffix)
            Spacer(GlanceModifier.width(3.dp))
            clockText()
        } else {
            clockText()
            suffix?.let {
                Spacer(GlanceModifier.width(3.dp))
                suffixText(it)
            }
        }
    }
}

@Composable
internal fun WidgetContent(data: GlanceWidgetData) {
    val size = LocalSize.current
    val systemFontScale = LocalContext.current.resources.configuration.fontScale
    val layout = layoutForSize(size.width.value, size.height.value, systemFontScale)
    // The saved option is deliberately relative: adaptive sizing remains the baseline and the
    // user's preference nudges every layout up or down from that device-appropriate result.
    val fittedData = data.copy(fontScale = layout.fontScale * data.fontScale)
    // Glance doesn't mirror Row child order for RTL locales the way native RemoteViews/View
    // layoutDirection does. Use the app/widget language carried with the data rather than the
    // RemoteViews host context: the launcher or preview host may remain LTR while the app is Arabic.
    val isRtl = data.isRtl

    CardSurface(
        rootBg = fittedData.rootBg,
        borderColor = fittedData.rootBorder,
        widthDp = size.width.value,
        heightDp = size.height.value
    ) {
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

/**
 * Draws fill and stroke into one bitmap because Glance/RemoteViews has no native border modifier.
 * A nested transparent Box cannot form a border: transparency reveals the colored parent and
 * therefore fills the whole widget. Keeping both layers in one ARGB bitmap preserves a truly
 * transparent center when only the accent outline is enabled.
 */
@Composable
private fun CardSurface(
    rootBg: Color,
    borderColor: Color,
    widthDp: Float,
    heightDp: Float,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val surface = remember(rootBg, borderColor, widthDp, heightDp, density) {
        createWidgetSurfaceBitmap(
            widthPx = (widthDp * density).roundToInt(),
            heightPx = (heightDp * density).roundToInt(),
            fillColor = rootBg.toArgb(),
            borderColor = borderColor.toArgb(),
            cornerRadiusPx = 20f * density,
            borderWidthPx = 1.5f * density
        )
    }
    val clickModifier = GlanceModifier
        .fillMaxSize()
        .background(ImageProvider(surface), contentScale = ContentScale.FillBounds)
        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))

    Box(clickModifier) {
        content()
    }
}

internal fun createWidgetSurfaceBitmap(
    widthPx: Int,
    heightPx: Int,
    fillColor: Int,
    borderColor: Int,
    cornerRadiusPx: Float,
    borderWidthPx: Float
): Bitmap {
    val width = widthPx.coerceAtLeast(1)
    val height = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (android.graphics.Color.alpha(borderColor) > 0 && borderWidthPx > 0f) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            cornerRadiusPx,
            cornerRadiusPx,
            borderPaint
        )

        // SRC replaces (rather than blends with) the border layer, so a transparent fill really
        // clears the center and a translucent fill keeps its intended unpolluted color/opacity.
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        val inset = borderWidthPx
        canvas.drawRoundRect(
            RectF(inset, inset, width - inset, height - inset),
            (cornerRadiusPx - inset).coerceAtLeast(0f),
            (cornerRadiusPx - inset).coerceAtLeast(0f),
            innerPaint
        )
    } else {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            cornerRadiusPx,
            cornerRadiusPx,
            fillPaint
        )
    }
    return bitmap
}

@Composable
private fun MicroContent(data: GlanceWidgetData) {
    Box(GlanceModifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
        TimeText(data.prayerTime, 14f, data, data.textPrimary, FontWeight.Bold)
    }
}

@Composable
private fun SlimContent(data: GlanceWidgetData, isRtl: Boolean, layout: AdaptiveLayout) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(layout.paddingDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val prayerLabel = @Composable {
            Text(
                data.prayerName,
                style = TextStyle(color = ColorProvider(data.textPrimary), fontSize = scaledSp(14f, data), fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        val heroText = @Composable {
            Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                if (isRtl) {
                    TimeText(data.prayerTime, 14f, data, data.textPrimary, FontWeight.Bold)
                    Spacer(GlanceModifier.width(5.dp))
                    prayerLabel()
                } else {
                    prayerLabel()
                    Spacer(GlanceModifier.width(5.dp))
                    TimeText(data.prayerTime, 14f, data, data.textPrimary, FontWeight.Bold)
                }
            }
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
                    fontSize = scaledSp(10f, data),
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                ),
                maxLines = 1
            )
        }
        if (layout.showBarPrayerRibbon && data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.width(layout.gapDp.dp))
            Box(GlanceModifier.defaultWeight()) {
                val ribbonSlots = fittedRibbonSlots(
                    slots = data.mediumSlots,
                    availableWidthDp = layout.widthDp * 0.30f,
                    data = data,
                    layout = layout
                )
                PrayerRibbon(ribbonSlots, data, isRtl, chipGapDp = 1f)
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
        else -> 74f
    }
    val sectionGapCount = (if (data.widgetSettings.showHeroCard) 1 else 0) +
        (if (data.widgetSettings.showAllPrayersList) 1 else 0)
    val availableForRows = layout.heightDp -
        (layout.paddingDp * 2f) - headerHeight - heroHeight - (layout.gapDp * sectionGapCount)
    val rowHeight = availableForRows / slots.size
    // Glance Text uses sp, so the host font scale affects its real height even though LocalSize is
    // reported in dp. Reduce row count when necessary instead of letting RemoteViews clip text.
    val renderedScale = data.fontScale * layout.systemFontScale
    val minimumReadableRowHeight = (11f * renderedScale * 1.35f + 8f).coerceAtLeast(25f)
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
            HeroCard(data, compact = true, isRtl = isRtl)
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
                PrayerRibbon(
                    fittedRibbonSlots(data.mediumSlots, layout.widthDp * 0.48f, data, layout),
                    data,
                    isRtl
                )
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
        if (data.widgetSettings.showHeroCard) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            HeroCard(data, compact = false, isRtl = isRtl)
        }
        if (data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.height(layout.gapDp.dp))
            Box(GlanceModifier.defaultWeight()) {
                val ribbonSlots = fittedRibbonSlots(
                    data.allSlots,
                    layout.widthDp - layout.paddingDp * 2f,
                    data,
                    layout
                )
                PrayerRibbon(
                    ribbonSlots,
                    data,
                    isRtl,
                    fillAvailable = true
                )
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
        val heroSection = @Composable {
            if (data.widgetSettings.showHeroCard) {
                HeroCard(data, compact = false, isRtl = isRtl)
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
                fontSize = scaledSp(11f, data),
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
                        fontSize = scaledSp(11f, data),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    maxLines = 1
                )
                Text(
                    data.hijriText,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(data.textSecondary),
                        fontSize = scaledSp(10f, data),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    maxLines = 1
                )
            }
        }
        Row(
            GlanceModifier.fillMaxWidth().height(20.dp).clickable(actionRunCallback<RefreshGlanceWidgetAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRtl) {
                RefreshButton(data, touchSizeDp = 20f)
                Spacer(GlanceModifier.width(5.dp))
                stackedLabels(GlanceModifier.defaultWeight())
            } else {
                stackedLabels(GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(5.dp))
                RefreshButton(data, touchSizeDp = 20f)
            }
        }
    } else {
        Row(
            GlanceModifier.fillMaxWidth().height(20.dp).clickable(actionRunCallback<RefreshGlanceWidgetAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRtl) {
                RefreshButton(data, touchSizeDp = 20f)
                Spacer(GlanceModifier.width(5.dp))
                labelText(label, GlanceModifier.defaultWeight())
            } else {
                labelText(label, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(5.dp))
                RefreshButton(data, touchSizeDp = 20f)
            }
        }
    }
}

@Composable
private fun RefreshButton(data: GlanceWidgetData, touchSizeDp: Float = 40f) {
    Box(
        modifier = GlanceModifier.size(touchSizeDp.dp).clickable(actionRunCallback<RefreshGlanceWidgetAction>()),
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
        // Keep name, time, and countdown visually grouped in the vertical tier. Each gets a
        // centered line so long countdown strings cannot squeeze the prayer name at narrow sizes.
        Box(
            modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
                .background(ColorProvider(data.heroBg))
                .cornerRadius(13.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    data.prayerName,
                    style = TextStyle(color = ColorProvider(data.accent), fontSize = scaledSp(11f, data), fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(1.dp))
                TimeText(data.prayerTime, 17f, data, data.textPrimary, FontWeight.Bold, suffixScale = 0.68f)
                if (data.widgetSettings.showCountdown) {
                    Spacer(GlanceModifier.height(2.dp))
                    CountdownPill(data.countdown, data)
                }
            }
        }
        return
    }

    // In the large hero layout, name and time are grouped
    // in one weighted column and the pill sits beside that whole block.
    Row(
        modifier = (if (fillAvailable) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth())
            .background(ColorProvider(data.heroBg))
            .cornerRadius(13.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
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
                Spacer(GlanceModifier.height(1.dp))
                TimeText(data.prayerTime, 23f, data, data.textPrimary, FontWeight.Bold, suffixScale = 0.58f)
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
private fun CountdownPill(text: String, data: GlanceWidgetData) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(data.activePrayerBg))
            .cornerRadius(10.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = TextStyle(color = ColorProvider(data.textOnAccent), fontSize = scaledSp(10f, data), fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

private fun fittedRibbonSlots(
    slots: List<MiniSlot>,
    availableWidthDp: Float,
    data: GlanceWidgetData,
    layout: AdaptiveLayout
): List<MiniSlot> {
    if (slots.size <= 2) return slots
    val renderedScale = data.fontScale * layout.systemFontScale
    val minimumCellWidth = (43f * renderedScale).coerceAtLeast(46f)
    val visibleCount = (availableWidthDp / minimumCellWidth).toInt().coerceIn(2, slots.size)
    if (visibleCount >= slots.size) return slots
    val firstUpcoming = slots.indexOfFirst { it.isActive }.takeIf { it >= 0 } ?: 0
    return List(visibleCount) { offset -> slots[(firstUpcoming + offset) % slots.size] }
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
                        val baseFontSize = if (fillAvailable) 11f else 10f
                        Text(slot.name, style = TextStyle(color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary), fontSize = scaledSp(baseFontSize, data)), maxLines = 1)
                        TimeText(slot.time, baseFontSize, data, if (slot.isActive) data.textOnAccent else data.textPrimary)
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
                        fontSize = scaledSp(if (dense) 10f else 11f, data),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    maxLines = 1
                )
            }
            val timeText = @Composable {
                TimeText(
                    slot.time,
                    if (dense) 10f else 11f,
                    data,
                    if (slot.isActive) data.textOnAccent else data.textPrimary
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
