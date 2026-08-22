package com.prayertimes.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import android.content.Intent
import com.prayertimes.MainActivity
import com.prayertimes.R
import com.prayertimes.data.calculator.PrayerTimesCalculator
import com.prayertimes.data.cities.CityDatabase
import com.prayertimes.data.models.AppLanguage
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.util.LocalizedStrings
import com.prayertimes.widget.PrayerAppWidgetProvider
import java.time.Duration
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
        private val COMFORTABLE = DpSize(230.dp, 120.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, COMFORTABLE))

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

        fun schedule(date: java.time.LocalDate) = PrayerTimesCalculator.calculateDailySchedule(
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
            Triple(
                prayerName(type, settings.language),
                prayerMap[type]?.time?.format(timeFormatter) ?: "--:--",
                type == nextPrayerType
            )
        }

        provideContent {
            WidgetContent(
                prayerName = prayerName(nextPrayerType, settings.language),
                prayerTime = nextPrayerZoned.format(timeFormatter),
                countdown = formatCountdown(context, diffSeconds, isArabic),
                locationText = locationText,
                rootBg = Color(colors.rootBgColor),
                accent = Color(colors.accentColor),
                textPrimary = Color(colors.textPrimaryColor),
                textSecondary = Color(colors.textSecondaryColor),
                textOnAccent = Color(colors.textOnAccentColor),
                miniRow = miniRow
            )
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

@Composable
private fun WidgetContent(
    prayerName: String,
    prayerTime: String,
    countdown: String,
    locationText: String,
    rootBg: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    textOnAccent: Color,
    miniRow: List<Triple<String, String, Boolean>>
) {
    val size = LocalSize.current
    val isComfortable = size.width >= 200.dp
    val context = LocalContext.current
    val openAppIntent = Intent(context, MainActivity::class.java)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(rootBg))
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(openAppIntent))
    ) {
        if (isComfortable) {
            Text(
                text = locationText,
                style = TextStyle(color = ColorProvider(textSecondary), fontSize = 11.sp)
            )
        }
        Text(
            text = prayerName,
            style = TextStyle(color = ColorProvider(accent), fontWeight = FontWeight.Medium, fontSize = 13.sp)
        )
        Text(
            text = prayerTime,
            style = TextStyle(
                color = ColorProvider(textPrimary),
                fontWeight = FontWeight.Bold,
                fontSize = if (isComfortable) 20.sp else 16.sp
            )
        )
        Text(
            text = countdown,
            style = TextStyle(color = ColorProvider(textOnAccent), fontSize = 11.sp)
        )

        if (isComfortable) {
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                miniRow.forEach { (name, time, isNext) ->
                    Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp)) {
                        Text(
                            text = name,
                            style = TextStyle(
                                color = ColorProvider(if (isNext) accent else textSecondary),
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = time,
                            style = TextStyle(
                                color = ColorProvider(if (isNext) accent else textPrimary),
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
