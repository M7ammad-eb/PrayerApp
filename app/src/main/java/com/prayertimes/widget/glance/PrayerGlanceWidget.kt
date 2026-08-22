package com.prayertimes.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.content.Intent
import com.prayertimes.MainActivity
import com.prayertimes.PrayerApplication
import com.prayertimes.R
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.cities.CityDatabase
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.PrayerType
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

/**
 * Proof-of-concept Jetpack Glance widget, added alongside (not replacing) the existing
 * RemoteViews-based PrayerAppWidgetProvider, to evaluate replacing the 7 hand-maintained XML
 * layouts with SizeMode.Responsive compositions that Glance itself picks by real dp size instead
 * of us pre-building one layout per size bucket. Reuses
 * PrayerAppWidgetProvider.resolveWidgetColors() for visual parity with the shipped widget's
 * theming rather than reimplementing the 9-preset/Material-You/app-theme color logic twice.
 */
class PrayerGlanceWidget : GlanceAppWidget() {

    companion object {
        private val COMPACT = DpSize(110.dp, 100.dp)
        private val TALL = DpSize(130.dp, 190.dp)
        private val COMFORTABLE = DpSize(230.dp, 150.dp)

        /**
         * Single entry point for every trigger that should refresh this widget (prayer alarms,
         * boot, time/date/timezone changes, settings changes) - all of those already funnel
         * through PrayerAppWidgetProvider.updateAllWidgets(), which calls this too, so there's no
         * separate broadcast plumbing to maintain for the Glance widget.
         */
        fun refreshAll(context: Context) {
            PrayerApplication.instance.applicationScope.launch {
                try {
                    PrayerGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, TALL, COMFORTABLE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = PrayerPreferences.getInitialSettings(context)
        val isArabic = settings.language.resolveIsArabic()
        val zoneId = try {
            ZoneId.of(settings.location.timeZoneId)
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
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
        val nextItem = todaySchedule.prayerItems.firstOrNull {
            it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(now)
        }
        val (nextPrayerType, nextPrayerZoned) = if (nextItem != null) {
            nextItem.type to nextItem.zonedDateTime
        } else {
            val fajrItem = schedule(today.plusDays(1)).prayerItems.first { it.type == PrayerType.FAJR }
            fajrItem.type to fajrItem.zonedDateTime
        }

        val timeFormatter = if (settings.is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }
        val diffSeconds = Duration.between(now, nextPrayerZoned).seconds
        val localizedRes = LocalizedStrings.forLanguage(context, isArabic)
        val locationText = CityDatabase.localizedName(localizedRes, settings.location)
            .ifBlank { CityDatabase.localizedCountry(localizedRes, settings.location) }

        val colors = PrayerAppWidgetProvider.resolveWidgetColors(context, settings)
        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        val miniRow = listOf(PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB).map { type ->
            MiniSlot(
                name = prayerName(type, settings.language),
                time = prayerMap[type]?.time?.format(timeFormatter) ?: "--:--",
                isActive = type == nextPrayerType
            )
        }

        // Same "elapsed since previous prayer, out of the full previous->next span" math as
        // PrayerAppWidgetProvider's progress bar, so the two widgets agree on-screen.
        val previousItem = todaySchedule.prayerItems
            .filter { it.type != PrayerType.SUNRISE && it.zonedDateTime.isBefore(nextPrayerZoned) }
            .lastOrNull()
        val totalSpanSeconds = if (previousItem != null) {
            Duration.between(previousItem.zonedDateTime, nextPrayerZoned).seconds.coerceAtLeast(1)
        } else {
            Duration.between(today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId), nextPrayerZoned).seconds.coerceAtLeast(1)
        }
        val elapsed = (totalSpanSeconds - diffSeconds).coerceAtLeast(0)
        val progress = (elapsed.toFloat() / totalSpanSeconds).coerceIn(0f, 1f)

        val data = GlanceWidgetData(
            prayerName = prayerName(nextPrayerType, settings.language),
            prayerTime = nextPrayerZoned.format(timeFormatter),
            countdown = formatCountdown(context, diffSeconds, isArabic),
            locationText = locationText,
            progress = progress,
            rootBg = Color(colors.rootBgColor),
            rootBorder = Color(colors.rootBorderColor),
            heroBg = Color(colors.heroBgColor),
            accent = Color(colors.accentColor),
            textPrimary = Color(colors.textPrimaryColor),
            textSecondary = Color(colors.textSecondaryColor),
            textOnAccent = Color(colors.textOnAccentColor),
            inactivePrayerBg = Color(colors.inactivePrayerBgColor),
            activePrayerBg = Color(colors.activePrayerBgColor),
            miniRow = miniRow
        )

        provideContent {
            WidgetContent(data)
        }
    }

    private fun prayerName(type: PrayerType, language: AppLanguage): String {
        val arabicNames = mapOf(
            PrayerType.FAJR to "الفجر", PrayerType.SUNRISE to "الشروق", PrayerType.DHUHR to "الظهر",
            PrayerType.ASR to "العصر", PrayerType.MAGHRIB to "المغرب", PrayerType.ISHA to "العشاء"
        )
        return when (language) {
            AppLanguage.ARABIC -> arabicNames.getValue(type)
            AppLanguage.ENGLISH -> type.title
            AppLanguage.SYSTEM ->
                if (Locale.getDefault().language.equals("ar", ignoreCase = true)) arabicNames.getValue(type) else type.title
        }
    }

    private fun formatCountdown(context: Context, seconds: Long, isArabic: Boolean): String {
        val res = LocalizedStrings.forLanguage(context, isArabic)
        if (seconds <= 0) return res.getString(R.string.widget_countdown_now)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> res.getString(R.string.widget_countdown_hours_minutes, hours, minutes)
            minutes > 0 -> res.getString(R.string.widget_countdown_minutes_only, minutes)
            else -> res.getString(R.string.widget_countdown_less_than_min)
        }
    }
}

private data class MiniSlot(val name: String, val time: String, val isActive: Boolean)

private data class GlanceWidgetData(
    val prayerName: String,
    val prayerTime: String,
    val countdown: String,
    val locationText: String,
    val progress: Float,
    val rootBg: Color,
    val rootBorder: Color,
    val heroBg: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnAccent: Color,
    val inactivePrayerBg: Color,
    val activePrayerBg: Color,
    val miniRow: List<MiniSlot>
)

/** Two-layer Box trick for a stroked card - Glance has no native border() modifier. */
@Composable
private fun CardSurface(
    rootBg: Color,
    borderColor: Color,
    modifier: GlanceModifier,
    content: @Composable () -> Unit
) {
    if (borderColor != Color.Transparent) {
        Box(modifier = modifier.background(ColorProvider(borderColor)).cornerRadius(20.dp)) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(1.5.dp)
                    .background(ColorProvider(rootBg))
                    .cornerRadius(19.dp)
            ) {
                content()
            }
        }
    } else {
        Box(modifier = modifier.background(ColorProvider(rootBg)).cornerRadius(20.dp)) {
            content()
        }
    }
}

private enum class Tier { COMPACT, TALL, COMFORTABLE }

@Composable
private fun WidgetContent(data: GlanceWidgetData) {
    val size = LocalSize.current
    val tier = when {
        size.width >= 200.dp -> Tier.COMFORTABLE
        size.height >= 170.dp -> Tier.TALL
        else -> Tier.COMPACT
    }
    val context = LocalContext.current
    // Glance doesn't mirror Row child order for RTL locales the way native RemoteViews/View
    // layoutDirection does (confirmed on-device: Arabic text shaped correctly within each Text,
    // but multi-child Rows stayed in LTR visual order) - so child order is swapped manually here,
    // the same fix PrayerAppWidgetProvider already applies via setLayoutDirection().
    val isRtl = context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
    val openAppIntent = Intent(context, MainActivity::class.java)

    CardSurface(
        rootBg = data.rootBg,
        borderColor = data.rootBorder,
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(openAppIntent))
    ) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
            if (tier != Tier.COMPACT) {
                HeaderLine(data.locationText, data.accent, data.textSecondary, isRtl)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            HeroCard(data, isRtl, isLarge = tier == Tier.COMFORTABLE)

            when (tier) {
                Tier.COMFORTABLE -> {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = data.progress,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                        color = ColorProvider(data.accent),
                        backgroundColor = ColorProvider(data.inactivePrayerBg)
                    )
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    MiniRowHorizontal(data, isRtl)
                }
                Tier.TALL -> {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    MiniListVertical(data, isRtl)
                }
                Tier.COMPACT -> Unit
            }
        }
    }
}

@Composable
private fun HeaderLine(locationText: String, accent: Color, textSecondary: Color, isRtl: Boolean) {
    val icon = @Composable {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_mosque),
            contentDescription = null,
            modifier = GlanceModifier.size(14.dp),
            colorFilter = ColorFilter.tint(ColorProvider(accent))
        )
    }
    val label = @Composable {
        Text(locationText, style = TextStyle(color = ColorProvider(textSecondary), fontSize = 11.sp), maxLines = 1)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isRtl) {
            label(); Spacer(modifier = GlanceModifier.width(4.dp)); icon()
        } else {
            icon(); Spacer(modifier = GlanceModifier.width(4.dp)); label()
        }
    }
}

@Composable
private fun CountdownPill(countdown: String, activePrayerBg: Color, textOnAccent: Color) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(activePrayerBg))
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(countdown, style = TextStyle(color = ColorProvider(textOnAccent), fontSize = 10.sp))
    }
}

@Composable
private fun HeroCard(data: GlanceWidgetData, isRtl: Boolean, isLarge: Boolean) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(data.heroBg))
            .cornerRadius(14.dp)
            .padding(10.dp)
    ) {
        Column {
            if (isRtl) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CountdownPill(data.countdown, data.activePrayerBg, data.textOnAccent)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        data.prayerName,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(color = ColorProvider(data.accent), fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        maxLines = 1
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        data.prayerName,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(color = ColorProvider(data.accent), fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        maxLines = 1
                    )
                    CountdownPill(data.countdown, data.activePrayerBg, data.textOnAccent)
                }
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                data.prayerTime,
                style = TextStyle(
                    color = ColorProvider(data.textPrimary),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLarge) 24.sp else 18.sp
                )
            )
        }
    }
}

@Composable
private fun MiniRowHorizontal(data: GlanceWidgetData, isRtl: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        val slots = if (isRtl) data.miniRow.reversed() else data.miniRow
        slots.forEach { slot ->
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 3.dp)
                    .background(ColorProvider(if (slot.isActive) data.activePrayerBg else data.inactivePrayerBg))
                    .cornerRadius(10.dp)
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        slot.name,
                        style = TextStyle(
                            color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary),
                            fontSize = 9.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        slot.time,
                        style = TextStyle(
                            color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textPrimary),
                            fontSize = 9.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniListVertical(data: GlanceWidgetData, isRtl: Boolean) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        data.miniRow.forEachIndexed { index, slot ->
            if (index > 0) Spacer(modifier = GlanceModifier.height(4.dp))
            val rowModifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(if (slot.isActive) data.activePrayerBg else data.inactivePrayerBg))
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp)
            if (isRtl) {
                Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        slot.time,
                        style = TextStyle(
                            color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textPrimary),
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        slot.name,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary),
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                }
            } else {
                Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        slot.name,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textSecondary),
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        slot.time,
                        style = TextStyle(
                            color = ColorProvider(if (slot.isActive) data.textOnAccent else data.textPrimary),
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
