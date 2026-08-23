package com.prayertimes.widget.glance

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.appwidget.LinearProgressIndicator
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

private enum class Tier { MICRO, SLIM, VERTICAL, SMALL, MEDIUM, LARGE, EXPANDED }

// Same seven SizeF breakpoints as PrayerAppWidgetProvider.updateWidgetsInternal's
// RemoteViews(mapOf(...)) call, so Glance recomposes into the matching tier instead of us
// maintaining one XML layout per size. SizeMode.Responsive always resolves LocalSize.current to
// its nearest match among exactly these values, so tier lookup below is a plain map read rather
// than re-deriving the threshold table a second time.
private val SIZE_MICRO = DpSize(40.dp, 40.dp)
private val SIZE_SLIM = DpSize(90.dp, 40.dp)
private val SIZE_VERTICAL = DpSize(40.dp, 110.dp)
private val SIZE_SMALL = DpSize(110.dp, 100.dp)
private val SIZE_MEDIUM = DpSize(230.dp, 90.dp)
private val SIZE_LARGE = DpSize(250.dp, 180.dp)
private val SIZE_EXPANDED = DpSize(250.dp, 320.dp)

private val TIER_BY_SIZE = mapOf(
    SIZE_MICRO to Tier.MICRO,
    SIZE_SLIM to Tier.SLIM,
    SIZE_VERTICAL to Tier.VERTICAL,
    SIZE_SMALL to Tier.SMALL,
    SIZE_MEDIUM to Tier.MEDIUM,
    SIZE_LARGE to Tier.LARGE,
    SIZE_EXPANDED to Tier.EXPANDED
)

/**
 * Full Glance port of the seven RemoteViews size tiers in PrayerAppWidgetProvider - one
 * SizeMode.Responsive composable instead of seven hand-maintained XML layouts.
 */
class PrayerGlanceWidget : GlanceAppWidget() {

    companion object {
        fun refreshAll(context: Context) {
            PrayerApplication.instance.applicationScope.launch {
                runCatching { PrayerGlanceWidget().updateAll(context) }
            }
        }
    }

    override val sizeMode = SizeMode.Responsive(TIER_BY_SIZE.keys)

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
        val totalSpanSeconds = Duration.between(previousTime, next.zonedDateTime).seconds.coerceAtLeast(1)

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
            progress = (sinceSeconds.toFloat() / totalSpanSeconds).coerceIn(0f, 1f),
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

private data class MiniSlot(val name: String, val time: String, val isActive: Boolean)

private data class GlanceWidgetData(
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
    val progress: Float,
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
private fun WidgetContent(data: GlanceWidgetData) {
    val tier = TIER_BY_SIZE[LocalSize.current] ?: Tier.SMALL
    val context = LocalContext.current
    // Glance doesn't mirror Row child order for RTL locales the way native RemoteViews/View
    // layoutDirection does (confirmed on-device: Arabic text shapes correctly within each Text,
    // but multi-child Rows stayed in LTR visual order) - so child order is swapped manually
    // below, the same fix PrayerAppWidgetProvider already applies via setLayoutDirection().
    val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    CardSurface(data.rootBg, data.rootBorder) {
        when (tier) {
            Tier.MICRO -> MicroContent(data)
            Tier.SLIM -> SlimContent(data, isRtl)
            Tier.VERTICAL -> VerticalContent(data, isRtl)
            Tier.SMALL -> SmallContent(data, isRtl)
            Tier.MEDIUM -> MediumContent(data, isRtl)
            Tier.LARGE -> LargeContent(data, isRtl, expanded = false)
            Tier.EXPANDED -> LargeContent(data, isRtl, expanded = true)
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
private fun SlimContent(data: GlanceWidgetData, isRtl: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
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
            if (data.widgetSettings.showCountdown) CountdownPill(data.countdown, data)
            Spacer(GlanceModifier.width(6.dp))
            heroText()
        } else {
            heroText()
            Spacer(GlanceModifier.width(6.dp))
            if (data.widgetSettings.showCountdown) CountdownPill(data.countdown, data)
        }
        RefreshButton(data)
    }
}

@Composable
private fun VerticalContent(data: GlanceWidgetData, isRtl: Boolean) {
    Column(GlanceModifier.fillMaxSize().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (data.widgetSettings.showHeroCard) HeroCard(data, compact = true, isRtl = isRtl)
        if (data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.height(5.dp))
            // Nested Column for the same RemoteViews 10-direct-children reason as LargeContent's
            // expanded branch - cheap insurance even though 6 slots is under the limit today.
            Column { data.allSlots.forEach { PrayerListRow(it, data, dense = true, isRtl = isRtl) } }
        }
    }
}

@Composable
private fun SmallContent(data: GlanceWidgetData, isRtl: Boolean) {
    Column(GlanceModifier.fillMaxSize().padding(9.dp)) {
        if (data.widgetSettings.showLocation) LocationHeader(data, isRtl)
        if (data.widgetSettings.showHeroCard) {
            Spacer(GlanceModifier.height(5.dp))
            HeroCard(data, compact = true, isRtl = isRtl)
        }
    }
}

@Composable
private fun MediumContent(data: GlanceWidgetData, isRtl: Boolean) {
    Row(GlanceModifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        if (data.widgetSettings.showHeroCard) {
            Box(GlanceModifier.defaultWeight()) { HeroCard(data, compact = true, isRtl = isRtl) }
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            if (data.widgetSettings.showLocation) LocationHeader(data, isRtl)
            if (data.widgetSettings.showAllPrayersList) {
                Spacer(GlanceModifier.height(5.dp))
                PrayerRibbon(data.mediumSlots, data, isRtl)
            }
        }
    }
}

@Composable
private fun LargeContent(data: GlanceWidgetData, isRtl: Boolean, expanded: Boolean) {
    Column(GlanceModifier.fillMaxSize().padding(11.dp)) {
        if (data.widgetSettings.showLocation || data.widgetSettings.showHijriDate) {
            LocationHeader(data, isRtl, showHijri = true)
        }
        val showDualHero = data.widgetSettings.heroTimeMode == WidgetHeroTimeMode.BOTH
        if (data.widgetSettings.showHeroCard) {
            Spacer(GlanceModifier.height(7.dp))
            if (showDualHero) DualHeroCard(data, isRtl) else HeroCard(data, compact = false, isRtl = isRtl)
        }
        if (data.widgetSettings.showProgressBar && data.widgetSettings.showHeroCard && !showDualHero) {
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = data.progress,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                color = ColorProvider(data.accent),
                backgroundColor = ColorProvider(data.inactivePrayerBg)
            )
        }
        if (data.widgetSettings.showAllPrayersList) {
            Spacer(GlanceModifier.height(8.dp))
            if (expanded) {
                // RemoteViews hard-caps a container at 10 direct children - header + hero +
                // progress bar already use several slots in the outer Column, so all 6 prayer
                // rows are nested in their own Column (one child of the outer Column) instead of
                // being emitted as 6 separate top-level children, which crashed past that limit.
                Column { data.allSlots.forEach { PrayerListRow(it, data, dense = false, isRtl = isRtl) } }
            } else {
                PrayerRibbon(data.allSlots, data, isRtl)
            }
        }
    }
}

@Composable
private fun LocationHeader(data: GlanceWidgetData, isRtl: Boolean, showHijri: Boolean = false) {
    val label = buildString {
        if (data.widgetSettings.showLocation) append(data.locationText)
        if (showHijri && data.widgetSettings.showHijriDate) {
            if (isNotEmpty()) append(" • ")
            append(data.hijriText)
        }
    }
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val labelText = @Composable {
            Text(
                label,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(data.textSecondary),
                    fontSize = scaledSp(10f, data),
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                ),
                maxLines = 1
            )
        }
        val icon = @Composable {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_mosque),
                contentDescription = null,
                modifier = GlanceModifier.size(13.dp),
                colorFilter = ColorFilter.tint(ColorProvider(data.accent))
            )
        }
        // Mirror icon-label-refresh (LTR) to refresh-label-icon (RTL) rather than dropping the
        // icon, which the original single-branch reuse of this Row accidentally did.
        if (isRtl) {
            RefreshButton(data)
            Spacer(GlanceModifier.width(5.dp))
            labelText()
            Spacer(GlanceModifier.width(4.dp))
            icon()
        } else {
            icon()
            Spacer(GlanceModifier.width(4.dp))
            labelText()
            Spacer(GlanceModifier.width(5.dp))
            RefreshButton(data)
        }
    }
}

@Composable
private fun RefreshButton(data: GlanceWidgetData) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_refresh),
        contentDescription = null,
        modifier = GlanceModifier.size(20.dp).padding(3.dp).clickable(actionRunCallback<RefreshGlanceWidgetAction>()),
        colorFilter = ColorFilter.tint(ColorProvider(data.textSecondary))
    )
}

@Composable
private fun HeroCard(data: GlanceWidgetData, compact: Boolean, isRtl: Boolean) {
    if (compact) {
        // Matches PrayerAppWidgetProvider's vertical-tier XML: name, time, and the countdown
        // pill each get their own centered line. The small tier's XML instead puts the pill
        // beside a weighted name+time column, but at ~110dp wide that leaves the pill and the
        // name fighting over the same few dp - a long countdown string ("in 7h 35m") squeezed
        // the name down to a single ellipsized letter. Stacking avoids that regardless of tier.
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(data.heroBg))
                .cornerRadius(13.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
        return
    }

    // Matches PrayerAppWidgetProvider's large/expanded XML hero layout: name+time are grouped
    // in one weighted column and the pill sits beside that whole block.
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
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
private fun DualHeroCard(data: GlanceWidgetData, isRtl: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(data.heroBg))
            .cornerRadius(13.dp)
            .padding(9.dp)
    ) {
        val previousHalf = @Composable {
            HeroHalf(data.previousName, data.previousTime, data.since, R.string.widget_hero_prev_label, data, GlanceModifier.defaultWeight(), isRtl)
        }
        val nextHalf = @Composable {
            HeroHalf(data.nextName, data.nextTime, data.until, R.string.widget_hero_next_label, data, GlanceModifier.defaultWeight(), isRtl)
        }
        if (isRtl) {
            nextHalf(); Spacer(GlanceModifier.width(10.dp)); previousHalf()
        } else {
            previousHalf(); Spacer(GlanceModifier.width(10.dp)); nextHalf()
        }
    }
}

@Composable
private fun HeroHalf(name: String, time: String, detail: String, labelRes: Int, data: GlanceWidgetData, modifier: GlanceModifier, isRtl: Boolean) {
    val context = LocalContext.current
    Column(modifier, horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start) {
        Text(context.getString(labelRes), style = TextStyle(color = ColorProvider(data.textSecondary), fontSize = scaledSp(8f, data)), maxLines = 1)
        Text(name, style = TextStyle(color = ColorProvider(data.accent), fontSize = scaledSp(11f, data), fontWeight = FontWeight.Medium), maxLines = 1)
        Text(time, style = TextStyle(color = ColorProvider(data.textPrimary), fontSize = scaledSp(18f, data), fontWeight = FontWeight.Bold), maxLines = 1)
        Text(detail, style = TextStyle(color = ColorProvider(data.textSecondary), fontSize = scaledSp(9f, data)), maxLines = 1)
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
private fun PrayerRibbon(slots: List<MiniSlot>, data: GlanceWidgetData, isRtl: Boolean) {
    Row(GlanceModifier.fillMaxWidth()) {
        (if (isRtl) slots.reversed() else slots).forEach { slot ->
            // A single chained .padding().background() doesn't reliably inset the background in
            // Glance (confirmed on-device: the chips rendered edge-to-edge with zero gap despite
            // horizontal padding before .background()) - unlike CardSurface's border trick, which
            // works because it uses two separate nested containers. Same fix here: an outer,
            // uncolored Box carries the weight and the gap-via-padding; the inner Column carries
            // the background and is what actually gets tinted.
            Box(modifier = GlanceModifier.defaultWeight().padding(horizontal = 3.dp)) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(if (slot.isActive) data.activePrayerBg else data.inactivePrayerBg))
                        .cornerRadius(8.dp)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(slot.name, style = TextStyle(color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary), fontSize = scaledSp(8f, data)), maxLines = 1)
                    Text(slot.time, style = TextStyle(color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textPrimary), fontSize = scaledSp(8f, data), fontWeight = FontWeight.Medium), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PrayerListRow(slot: MiniSlot, data: GlanceWidgetData, dense: Boolean, isRtl: Boolean) {
    // Same fix as PrayerRibbon: the vertical gap has to come from an outer, uncolored container's
    // padding, not a padding chained before .background() on the same Row.
    Box(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
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
