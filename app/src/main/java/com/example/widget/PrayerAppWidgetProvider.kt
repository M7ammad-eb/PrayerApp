package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.example.MainActivity
import com.example.R
import com.example.data.calculator.PrayerTimesCalculator
import com.example.data.models.AppLanguage
import com.example.data.models.DailyPrayerSchedule
import com.example.data.models.PrayerTimeItem
import com.example.data.models.PrayerType
import com.example.data.models.WidgetBackgroundStyle
import com.example.data.models.WidgetThemeMode
import com.example.data.preferences.AppPrayerSettings
import com.example.data.preferences.PrayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetColorScheme(
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

class PrayerAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"
        private const val REQUEST_CODE_REFRESH = 4001
        private const val REQUEST_CODE_ALARM_UPDATE = 4002

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PrayerAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, PrayerAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
                val provider = PrayerAppWidgetProvider()
                provider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }

        fun resolveWidgetColors(context: Context, settings: AppPrayerSettings): WidgetColorScheme {
            val wSet = settings.widgetSettings
            val opacityFrac = (wSet.opacityPercent / 100f).coerceIn(0f, 1f)

            data class ColorPalette(val primaryAccent: Int, val bgCardColor: Int, val textPrimary: Int, val textSecondary: Int)

            // Exact match with SettingsWidgetSubScreen.kt resolveWidgetPreviewTheme
            val palette = when (wSet.themeMode) {
                WidgetThemeMode.APP_THEME -> ColorPalette(
                    primaryAccent = 0xFF165B33.toInt(),
                    bgCardColor = 0xFF1E293B.toInt(),
                    textPrimary = 0xFFF8FAFC.toInt(),
                    textSecondary = 0xFF94A3B8.toInt()
                )
                WidgetThemeMode.MATERIAL_YOU -> ColorPalette(
                    primaryAccent = 0xFF3F51B5.toInt(),
                    bgCardColor = 0xFF1C1B1F.toInt(),
                    textPrimary = 0xFFE6E1E5.toInt(),
                    textSecondary = 0xFFCAC4D0.toInt()
                )
                WidgetThemeMode.DARK_ELEGANT -> ColorPalette(
                    primaryAccent = 0xFF10B981.toInt(),
                    bgCardColor = 0xFF121820.toInt(),
                    textPrimary = 0xFFF1F5F9.toInt(),
                    textSecondary = 0xFF94A3B8.toInt()
                )
                WidgetThemeMode.LIGHT_CLEAN -> ColorPalette(
                    primaryAccent = 0xFF059669.toInt(),
                    bgCardColor = 0xFFFFFFFF.toInt(),
                    textPrimary = 0xFF0F172A.toInt(),
                    textSecondary = 0xFF64748B.toInt()
                )
                WidgetThemeMode.OLED_BLACK -> ColorPalette(
                    primaryAccent = 0xFF34D399.toInt(),
                    bgCardColor = 0xFF000000.toInt(),
                    textPrimary = 0xFFFFFFFF.toInt(),
                    textSecondary = 0xFFA1A1AA.toInt()
                )
                WidgetThemeMode.EMERALD_ISLAMIC -> ColorPalette(
                    primaryAccent = 0xFFF59E0B.toInt(),
                    bgCardColor = 0xFF064E3B.toInt(),
                    textPrimary = 0xFFECFDF5.toInt(),
                    textSecondary = 0xFFA7F3D0.toInt()
                )
                WidgetThemeMode.GOLDEN_HOUR -> ColorPalette(
                    primaryAccent = 0xFFF59E0B.toInt(),
                    bgCardColor = 0xFF451A03.toInt(),
                    textPrimary = 0xFFFEF3C7.toInt(),
                    textSecondary = 0xFFFDE68A.toInt()
                )
                WidgetThemeMode.ROYAL_BLUE -> ColorPalette(
                    primaryAccent = 0xFF38BDF8.toInt(),
                    bgCardColor = 0xFF0F172A.toInt(),
                    textPrimary = 0xFFF0F9FF.toInt(),
                    textSecondary = 0xFFBAE6FD.toInt()
                )
                WidgetThemeMode.MONOCHROME -> ColorPalette(
                    primaryAccent = 0xFFFAFAFA.toInt(),
                    bgCardColor = 0xFF18181B.toInt(),
                    textPrimary = 0xFFFAFAFA.toInt(),
                    textSecondary = 0xFFA1A1AA.toInt()
                )
            }

            val finalRootBg = when (wSet.bgStyle) {
                WidgetBackgroundStyle.TRANSPARENT_CLEAN -> Color.TRANSPARENT
                WidgetBackgroundStyle.MINIMAL_BORDER -> ColorUtils.setAlphaComponent(0xFF000000.toInt(), (0.15f * opacityFrac * 255).toInt())
                else -> ColorUtils.setAlphaComponent(palette.bgCardColor, (opacityFrac * 255).toInt())
            }

            val rootBorder = when (wSet.bgStyle) {
                WidgetBackgroundStyle.TRANSPARENT_CLEAN -> Color.TRANSPARENT
                WidgetBackgroundStyle.MINIMAL_BORDER -> ColorUtils.setAlphaComponent(palette.primaryAccent, (0.80f * 255).toInt())
                else -> ColorUtils.setAlphaComponent(palette.primaryAccent, (0.25f * 255).toInt())
            }

            val heroBg = when (wSet.bgStyle) {
                WidgetBackgroundStyle.TRANSPARENT_CLEAN -> ColorUtils.setAlphaComponent(palette.textPrimary, (0.05f * 255).toInt())
                else -> ColorUtils.setAlphaComponent(palette.textPrimary, (0.08f * 255).toInt())
            }

            val heroStroke = ColorUtils.setAlphaComponent(palette.primaryAccent, (0.15f * 255).toInt())

            val activePrayerBg = palette.primaryAccent
            val inactivePrayerBg = ColorUtils.setAlphaComponent(palette.textPrimary, (0.06f * 255).toInt())
            val countdownBg = palette.primaryAccent

            val textOnAccent = if (ColorUtils.calculateLuminance(palette.primaryAccent) > 0.60) {
                0xFF0F172A.toInt()
            } else {
                Color.WHITE
            }

            return WidgetColorScheme(
                rootBgColor = finalRootBg,
                rootBorderColor = rootBorder,
                heroBgColor = heroBg,
                heroStrokeColor = heroStroke,
                accentColor = palette.primaryAccent,
                textPrimaryColor = palette.textPrimary,
                textSecondaryColor = palette.textSecondary,
                textOnAccentColor = textOnAccent,
                activePrayerBgColor = activePrayerBg,
                inactivePrayerBgColor = inactivePrayerBg,
                countdownBgColor = countdownBg,
                fontScale = wSet.fontSize.scaleFactor
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, intArrayOf(appWidgetId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            "com.example.ACTION_PRAYER_ALARM" -> {
                updateAllWidgets(context)
            }
        }
    }

    private suspend fun updateWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val settings = PrayerPreferences.getInitialSettings(context)
        val isArabic = settings.language == AppLanguage.ARABIC
        val zoneId = try {
            ZoneId.of(settings.location.timeZoneId)
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()

        val timeFormatter = if (settings.is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }

        val todaySchedule = PrayerTimesCalculator.calculateDailySchedule(
            date = today,
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

        // Determine next prayer
        val nextItem = todaySchedule.prayerItems.firstOrNull {
            it.type != PrayerType.SUNRISE && it.zonedDateTime.isAfter(now)
        }

        val (nextPrayerType, nextPrayerZoned, isTomorrowFajr) = if (nextItem != null) {
            Triple(nextItem.type, nextItem.zonedDateTime, false)
        } else {
            val tomorrow = today.plusDays(1)
            val tomorrowSchedule = PrayerTimesCalculator.calculateDailySchedule(
                date = tomorrow,
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
            val fajrItem = tomorrowSchedule.prayerItems.first { it.type == PrayerType.FAJR }
            Triple(PrayerType.FAJR, fajrItem.zonedDateTime, true)
        }

        val diffSeconds = Duration.between(now, nextPrayerZoned).seconds
        val countdownFormatted = formatCountdown(diffSeconds, isArabic)

        val previousItem = todaySchedule.prayerItems
            .filter { it.type != PrayerType.SUNRISE && it.zonedDateTime.isBefore(nextPrayerZoned) }
            .lastOrNull()
        val totalSpanSeconds = if (previousItem != null) {
            Duration.between(previousItem.zonedDateTime, nextPrayerZoned).seconds.coerceAtLeast(1)
        } else {
            Duration.between(today.minusDays(1).atTime(todaySchedule.isha).atZone(zoneId), nextPrayerZoned).seconds.coerceAtLeast(1)
        }
        val elapsed = totalSpanSeconds - diffSeconds
        val progressPercent = ((elapsed.toFloat() / totalSpanSeconds) * 100).toInt().coerceIn(0, 100)

        // Location & Hijri display strings
        val locationFormatted = formatLocationString(settings.location.name, settings.location.country, isArabic)
        val hijriFormatted = if (isArabic) {
            todaySchedule.hijriDate?.formattedAr ?: todaySchedule.hijriDateString
        } else {
            todaySchedule.hijriDate?.formattedEn ?: todaySchedule.hijriDateString
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refreshIntent = Intent(context, PrayerAppWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REFRESH,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prayerDisplayName = getPrayerName(nextPrayerType, settings.language)
        val formattedNextTime = nextPrayerZoned.format(timeFormatter)
        val colors = resolveWidgetColors(context, settings)

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

            val verticalViews = buildVerticalWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                isTomorrowFajr = isTomorrowFajr,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent
            )

            val smallViews = buildSmallWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent
            )

            val slimViews = buildSlimWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent
            )

            val mediumViews = buildMediumWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                progressPercent = progressPercent,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent
            )

            val largeViews = buildLargeWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                hijriText = hijriFormatted,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                isTomorrowFajr = isTomorrowFajr,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                diffSeconds = diffSeconds,
                progressPercent = progressPercent,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent
            )

            val expandedViews = buildExpandedWidget(
                context = context,
                settings = settings,
                colors = colors,
                locationText = locationFormatted,
                hijriText = hijriFormatted,
                todaySchedule = todaySchedule,
                nextPrayerType = nextPrayerType,
                isTomorrowFajr = isTomorrowFajr,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                diffSeconds = diffSeconds,
                progressPercent = progressPercent,
                timeFormatter = timeFormatter,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent
            )

            val widgetViews: RemoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(
                    mapOf(
                        SizeF(50f, 110f) to verticalViews,
                        SizeF(70f, 40f) to smallViews,
                        SizeF(150f, 40f) to slimViews,
                        SizeF(140f, 80f) to mediumViews,
                        SizeF(210f, 80f) to largeViews,
                        SizeF(220f, 250f) to expandedViews
                    )
                )
            } else {
                when {
                    minWidth < 130 && minHeight >= 110 -> verticalViews
                    minHeight < 75 && minWidth >= 150 -> slimViews
                    minHeight < 80 && minWidth < 150 -> smallViews
                    minHeight >= 250 -> expandedViews
                    minWidth >= 210 -> largeViews
                    else -> mediumViews
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, widgetViews)
        }

        scheduleNextWidgetUpdate(context, nextPrayerZoned)
    }

    private fun buildVerticalWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        isTomorrowFajr: Boolean,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_vertical)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_vert_root_bg_img, "setColorFilter", colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_vert_root_border_img, View.VISIBLE)
            views.setInt(R.id.widget_vert_root_border_img, "setColorFilter", colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_vert_root_border_img, View.GONE)
        }

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_vert_header, View.VISIBLE)
            views.setTextViewText(R.id.widget_vert_location, locationText)
            views.setTextColor(R.id.widget_vert_location, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_vert_location, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_vert_header, View.GONE)
        }

        if (wSet.showHeroCard) {
            views.setViewVisibility(R.id.widget_vert_hero, View.VISIBLE)
            views.setInt(R.id.widget_vert_hero_bg_img, "setColorFilter", colors.heroBgColor)

            views.setTextViewText(R.id.widget_vert_next_name, prayerName)
            views.setTextColor(R.id.widget_vert_next_name, colors.accentColor)
            views.setTextViewTextSize(R.id.widget_vert_next_name, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

            views.setTextViewText(R.id.widget_vert_next_time, prayerTime)
            views.setTextColor(R.id.widget_vert_next_time, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_vert_next_time, TypedValue.COMPLEX_UNIT_SP, 14f * scale)

            if (wSet.showCountdown) {
                views.setViewVisibility(R.id.widget_vert_countdown_container, View.VISIBLE)
                views.setInt(R.id.widget_vert_countdown_bg_img, "setColorFilter", colors.countdownBgColor)
                views.setTextViewText(R.id.widget_vert_countdown, countdown)
                views.setTextColor(R.id.widget_vert_countdown, colors.textOnAccentColor)
                views.setTextViewTextSize(R.id.widget_vert_countdown, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            } else {
                views.setViewVisibility(R.id.widget_vert_countdown_container, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_vert_hero, View.GONE)
        }

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        fun bindRow(rowId: Int, bgId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            if (type == PrayerType.SUNRISE && !wSet.showSunrise) {
                views.setViewVisibility(rowId, View.GONE)
                return
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")

            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 10f * scale)

            val isHighlighted = !isTomorrowFajr && nextPrayerType == type
            if (isHighlighted) {
                views.setInt(bgId, "setColorFilter", colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                views.setInt(bgId, "setColorFilter", colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        bindRow(R.id.widget_vert_fajr_row, R.id.widget_vert_fajr_bg_img, R.id.widget_vert_fajr_name, R.id.widget_vert_fajr_time, PrayerType.FAJR)
        bindRow(R.id.widget_vert_sunrise_row, R.id.widget_vert_sunrise_bg_img, R.id.widget_vert_sunrise_name, R.id.widget_vert_sunrise_time, PrayerType.SUNRISE)
        bindRow(R.id.widget_vert_dhuhr_row, R.id.widget_vert_dhuhr_bg_img, R.id.widget_vert_dhuhr_name, R.id.widget_vert_dhuhr_time, PrayerType.DHUHR)
        bindRow(R.id.widget_vert_asr_row, R.id.widget_vert_asr_bg_img, R.id.widget_vert_asr_name, R.id.widget_vert_asr_time, PrayerType.ASR)
        bindRow(R.id.widget_vert_maghrib_row, R.id.widget_vert_maghrib_bg_img, R.id.widget_vert_maghrib_name, R.id.widget_vert_maghrib_time, PrayerType.MAGHRIB)
        bindRow(R.id.widget_vert_isha_row, R.id.widget_vert_isha_bg_img, R.id.widget_vert_isha_name, R.id.widget_vert_isha_time, PrayerType.ISHA)

        views.setOnClickPendingIntent(R.id.widget_root_vert, mainIntent)
        return views
    }

    private fun buildSlimWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_slim)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_slim_root_bg_img, "setColorFilter", colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_slim_root_border_img, View.VISIBLE)
            views.setInt(R.id.widget_slim_root_border_img, "setColorFilter", colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_slim_root_border_img, View.GONE)
        }

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_slim_location, View.VISIBLE)
            views.setTextViewText(R.id.widget_slim_location, locationText)
            views.setTextColor(R.id.widget_slim_location, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_slim_location, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_slim_location, View.GONE)
        }

        views.setTextViewText(R.id.widget_slim_prayer_name, prayerName)
        views.setTextColor(R.id.widget_slim_prayer_name, colors.accentColor)
        views.setTextViewTextSize(R.id.widget_slim_prayer_name, TypedValue.COMPLEX_UNIT_SP, 13f * scale)

        views.setTextViewText(R.id.widget_slim_prayer_time, prayerTime)
        views.setTextColor(R.id.widget_slim_prayer_time, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_slim_prayer_time, TypedValue.COMPLEX_UNIT_SP, 14f * scale)

        if (wSet.showCountdown) {
            views.setViewVisibility(R.id.widget_slim_countdown_container, View.VISIBLE)
            views.setInt(R.id.widget_slim_countdown_bg_img, "setColorFilter", colors.countdownBgColor)
            views.setTextViewText(R.id.widget_slim_countdown, countdown)
            views.setTextColor(R.id.widget_slim_countdown, colors.textOnAccentColor)
            views.setTextViewTextSize(R.id.widget_slim_countdown, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
        } else {
            views.setViewVisibility(R.id.widget_slim_countdown_container, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root_slim, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_slim_refresh_btn, refreshIntent)
        return views
    }

    private fun buildSmallWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_small)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_small_root_bg_img, "setColorFilter", colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_small_root_border_img, View.VISIBLE)
            views.setInt(R.id.widget_small_root_border_img, "setColorFilter", colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_small_root_border_img, View.GONE)
        }

        views.setInt(R.id.widget_small_hero_bg_img, "setColorFilter", colors.heroBgColor)

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_small_location, View.VISIBLE)
            views.setTextViewText(R.id.widget_small_location, locationText)
            views.setTextColor(R.id.widget_small_location, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_small_location, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
        } else {
            views.setViewVisibility(R.id.widget_small_location, View.GONE)
        }

        views.setTextViewText(R.id.widget_small_prayer_name, prayerName)
        views.setTextColor(R.id.widget_small_prayer_name, colors.accentColor)
        views.setTextViewTextSize(R.id.widget_small_prayer_name, TypedValue.COMPLEX_UNIT_SP, 13f * scale)

        views.setTextViewText(R.id.widget_small_prayer_time, prayerTime)
        views.setTextColor(R.id.widget_small_prayer_time, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_small_prayer_time, TypedValue.COMPLEX_UNIT_SP, 18f * scale)

        if (wSet.showCountdown) {
            views.setViewVisibility(R.id.widget_small_countdown_container, View.VISIBLE)
            views.setInt(R.id.widget_small_countdown_bg_img, "setColorFilter", colors.countdownBgColor)
            views.setTextViewText(R.id.widget_small_countdown, countdown)
            views.setTextColor(R.id.widget_small_countdown, colors.textOnAccentColor)
            views.setTextViewTextSize(R.id.widget_small_countdown, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_small_countdown_container, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root_small, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_small_refresh_btn, refreshIntent)
        return views
    }

    private fun buildMediumWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        progressPercent: Int,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_medium)
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_med_root_bg_img, "setColorFilter", colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_med_root_border_img, View.VISIBLE)
            views.setInt(R.id.widget_med_root_border_img, "setColorFilter", colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_med_root_border_img, View.GONE)
        }

        views.setInt(R.id.widget_med_hero_bg_img, "setColorFilter", colors.heroBgColor)
        views.setInt(R.id.widget_med_hero_border_img, "setColorFilter", colors.heroStrokeColor)

        views.setTextViewText(R.id.widget_med_location, locationText)
        views.setTextColor(R.id.widget_med_location, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_med_location, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

        views.setTextViewText(R.id.widget_med_next_prayer_name, prayerName)
        views.setTextColor(R.id.widget_med_next_prayer_name, colors.accentColor)
        views.setTextViewTextSize(R.id.widget_med_next_prayer_name, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

        views.setTextViewText(R.id.widget_med_next_prayer_time, prayerTime)
        views.setTextColor(R.id.widget_med_next_prayer_time, colors.textPrimaryColor)
        views.setTextViewTextSize(R.id.widget_med_next_prayer_time, TypedValue.COMPLEX_UNIT_SP, 19f * scale)

        if (wSet.showCountdown) {
            views.setViewVisibility(R.id.widget_med_countdown_container, View.VISIBLE)
            views.setInt(R.id.widget_med_countdown_bg_img, "setColorFilter", colors.countdownBgColor)
            views.setTextViewText(R.id.widget_med_countdown, countdown)
            views.setTextColor(R.id.widget_med_countdown, colors.textOnAccentColor)
            views.setTextViewTextSize(R.id.widget_med_countdown, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_med_countdown_container, View.GONE)
        }

        if (wSet.showProgressBar) {
            views.setViewVisibility(R.id.widget_med_prayer_progress, View.VISIBLE)
            views.setProgressBar(R.id.widget_med_prayer_progress, 100, progressPercent, false)
        } else {
            views.setViewVisibility(R.id.widget_med_prayer_progress, View.GONE)
        }

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        val prayerList = listOf(PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB)

        fun bindSlot(bgId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")
            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)

            if (nextPrayerType == type) {
                views.setInt(bgId, "setColorFilter", colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                views.setInt(bgId, "setColorFilter", colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        bindSlot(R.id.widget_med_slot1_bg_img, R.id.widget_med_slot1_name, R.id.widget_med_slot1_time, prayerList[0])
        bindSlot(R.id.widget_med_slot2_bg_img, R.id.widget_med_slot2_name, R.id.widget_med_slot2_time, prayerList[1])
        bindSlot(R.id.widget_med_slot3_bg_img, R.id.widget_med_slot3_name, R.id.widget_med_slot3_time, prayerList[2])

        views.setOnClickPendingIntent(R.id.widget_root_medium, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_med_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_med_refresh_btn, refreshIntent)
        return views
    }

    private fun buildLargeWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        hijriText: String,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        isTomorrowFajr: Boolean,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        diffSeconds: Long,
        progressPercent: Int,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_layout)
        val isArabic = settings.language == AppLanguage.ARABIC
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_root_bg_img, "setColorFilter", colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_root_border_img, View.VISIBLE)
            views.setInt(R.id.widget_root_border_img, "setColorFilter", colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_root_border_img, View.GONE)
        }

        views.setInt(R.id.widget_mosque_icon, "setColorFilter", colors.accentColor)
        views.setInt(R.id.widget_refresh_button, "setColorFilter", colors.textSecondaryColor)

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_location_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_location_text, locationText)
            views.setTextColor(R.id.widget_location_text, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_location_text, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
        } else {
            views.setViewVisibility(R.id.widget_location_text, View.GONE)
        }

        if (wSet.showHijriDate) {
            views.setViewVisibility(R.id.widget_hijri_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_hijri_text, hijriText)
            views.setTextColor(R.id.widget_hijri_text, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_hijri_text, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        } else {
            views.setViewVisibility(R.id.widget_hijri_text, View.GONE)
        }

        if (wSet.showHeroCard) {
            views.setViewVisibility(R.id.widget_hero_card, View.VISIBLE)
            views.setInt(R.id.widget_hero_bg_img, "setColorFilter", colors.heroBgColor)
            views.setInt(R.id.widget_hero_border_img, "setColorFilter", colors.heroStrokeColor)

            views.setTextViewText(R.id.widget_next_prayer_name, prayerName)
            views.setTextColor(R.id.widget_next_prayer_name, colors.accentColor)
            views.setTextViewTextSize(R.id.widget_next_prayer_name, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

            views.setTextViewText(R.id.widget_next_prayer_time, prayerTime)
            views.setTextColor(R.id.widget_next_prayer_time, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_next_prayer_time, TypedValue.COMPLEX_UNIT_SP, 20f * scale)

            if (wSet.showCountdown) {
                views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                views.setInt(R.id.widget_countdown_bg_img, "setColorFilter", colors.countdownBgColor)
                views.setTextViewText(R.id.widget_countdown_text, countdown)
                views.setTextColor(R.id.widget_countdown_text, colors.textOnAccentColor)
                views.setTextViewTextSize(R.id.widget_countdown_text, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

                views.setTextViewText(
                    R.id.widget_status_text,
                    if (diffSeconds <= 60) {
                        if (isArabic) "حان وقت الصلاة" else "Prayer Time!"
                    } else {
                        if (isArabic) "متبقي" else "Remaining"
                    }
                )
                views.setTextColor(R.id.widget_status_text, colors.textOnAccentColor)
                views.setTextViewTextSize(R.id.widget_status_text, TypedValue.COMPLEX_UNIT_SP, 8f * scale)
            } else {
                views.setViewVisibility(R.id.widget_countdown_container, View.GONE)
            }

            if (wSet.showProgressBar) {
                views.setViewVisibility(R.id.widget_prayer_progress, View.VISIBLE)
                views.setProgressBar(R.id.widget_prayer_progress, 100, progressPercent, false)
            } else {
                views.setViewVisibility(R.id.widget_prayer_progress, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_hero_card, View.GONE)
        }

        if (wSet.showAllPrayersList) {
            views.setViewVisibility(R.id.widget_prayer_ribbon, View.VISIBLE)
            val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
            populatePrayerRibbon(context, views, colors, prayerMap, nextPrayerType, isTomorrowFajr, timeFormatter, settings.language, wSet.showSunrise)
        } else {
            views.setViewVisibility(R.id.widget_prayer_ribbon, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshIntent)
        return views
    }

    private fun buildExpandedWidget(
        context: Context,
        settings: AppPrayerSettings,
        colors: WidgetColorScheme,
        locationText: String,
        hijriText: String,
        todaySchedule: DailyPrayerSchedule,
        nextPrayerType: PrayerType,
        isTomorrowFajr: Boolean,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        diffSeconds: Long,
        progressPercent: Int,
        timeFormatter: DateTimeFormatter,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_expanded)
        val isArabic = settings.language == AppLanguage.ARABIC
        val wSet = settings.widgetSettings
        val scale = colors.fontScale

        views.setInt(R.id.widget_exp_root_bg_img, "setColorFilter", colors.rootBgColor)
        if (colors.rootBorderColor != Color.TRANSPARENT) {
            views.setViewVisibility(R.id.widget_exp_root_border_img, View.VISIBLE)
            views.setInt(R.id.widget_exp_root_border_img, "setColorFilter", colors.rootBorderColor)
        } else {
            views.setViewVisibility(R.id.widget_exp_root_border_img, View.GONE)
        }

        views.setInt(R.id.widget_exp_mosque_icon, "setColorFilter", colors.accentColor)
        views.setInt(R.id.widget_exp_refresh_btn, "setColorFilter", colors.textSecondaryColor)

        if (wSet.showLocation) {
            views.setViewVisibility(R.id.widget_exp_location, View.VISIBLE)
            views.setTextViewText(R.id.widget_exp_location, locationText)
            views.setTextColor(R.id.widget_exp_location, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_exp_location, TypedValue.COMPLEX_UNIT_SP, 13f * scale)
        } else {
            views.setViewVisibility(R.id.widget_exp_location, View.GONE)
        }

        if (wSet.showHijriDate) {
            views.setViewVisibility(R.id.widget_exp_hijri, View.VISIBLE)
            views.setTextViewText(R.id.widget_exp_hijri, hijriText)
            views.setTextColor(R.id.widget_exp_hijri, colors.textSecondaryColor)
            views.setTextViewTextSize(R.id.widget_exp_hijri, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
        } else {
            views.setViewVisibility(R.id.widget_exp_hijri, View.GONE)
        }

        if (wSet.showHeroCard) {
            views.setViewVisibility(R.id.widget_exp_hero_card, View.VISIBLE)
            views.setInt(R.id.widget_exp_hero_bg_img, "setColorFilter", colors.heroBgColor)
            views.setInt(R.id.widget_exp_hero_border_img, "setColorFilter", colors.heroStrokeColor)

            views.setTextViewText(R.id.widget_exp_next_prayer_name, prayerName)
            views.setTextColor(R.id.widget_exp_next_prayer_name, colors.accentColor)
            views.setTextViewTextSize(R.id.widget_exp_next_prayer_name, TypedValue.COMPLEX_UNIT_SP, 13f * scale)

            views.setTextViewText(R.id.widget_exp_next_prayer_time, prayerTime)
            views.setTextColor(R.id.widget_exp_next_prayer_time, colors.textPrimaryColor)
            views.setTextViewTextSize(R.id.widget_exp_next_prayer_time, TypedValue.COMPLEX_UNIT_SP, 24f * scale)

            if (wSet.showCountdown) {
                views.setViewVisibility(R.id.widget_exp_countdown_container, View.VISIBLE)
                views.setInt(R.id.widget_exp_countdown_bg_img, "setColorFilter", colors.countdownBgColor)
                views.setTextViewText(R.id.widget_exp_countdown_text, countdown)
                views.setTextColor(R.id.widget_exp_countdown_text, colors.textOnAccentColor)
                views.setTextViewTextSize(R.id.widget_exp_countdown_text, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

                views.setTextViewText(
                    R.id.widget_exp_status_text,
                    if (diffSeconds <= 60) {
                        if (isArabic) "حان وقت الصلاة" else "Prayer Time!"
                    } else {
                        if (isArabic) "متبقي" else "Remaining"
                    }
                )
                views.setTextColor(R.id.widget_exp_status_text, colors.textOnAccentColor)
                views.setTextViewTextSize(R.id.widget_exp_status_text, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            } else {
                views.setViewVisibility(R.id.widget_exp_countdown_container, View.GONE)
            }

            if (wSet.showProgressBar) {
                views.setViewVisibility(R.id.widget_exp_prayer_progress, View.VISIBLE)
                views.setProgressBar(R.id.widget_exp_prayer_progress, 100, progressPercent, false)
            } else {
                views.setViewVisibility(R.id.widget_exp_prayer_progress, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_exp_hero_card, View.GONE)
        }

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        fun bindExpRow(rowId: Int, bgId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            if (type == PrayerType.SUNRISE && !wSet.showSunrise) {
                views.setViewVisibility(rowId, View.GONE)
                return
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")

            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 12f * scale)

            val isHighlighted = !isTomorrowFajr && nextPrayerType == type
            if (isHighlighted) {
                views.setInt(bgId, "setColorFilter", colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                views.setInt(bgId, "setColorFilter", colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        if (wSet.showAllPrayersList) {
            views.setViewVisibility(R.id.widget_exp_schedule_list, View.VISIBLE)
            bindExpRow(R.id.widget_exp_fajr_row, R.id.widget_exp_fajr_bg_img, R.id.widget_exp_fajr_name, R.id.widget_exp_fajr_time, PrayerType.FAJR)
            bindExpRow(R.id.widget_exp_sunrise_row, R.id.widget_exp_sunrise_bg_img, R.id.widget_exp_sunrise_name, R.id.widget_exp_sunrise_time, PrayerType.SUNRISE)
            bindExpRow(R.id.widget_exp_dhuhr_row, R.id.widget_exp_dhuhr_bg_img, R.id.widget_exp_dhuhr_name, R.id.widget_exp_dhuhr_time, PrayerType.DHUHR)
            bindExpRow(R.id.widget_exp_asr_row, R.id.widget_exp_asr_bg_img, R.id.widget_exp_asr_name, R.id.widget_exp_asr_time, PrayerType.ASR)
            bindExpRow(R.id.widget_exp_maghrib_row, R.id.widget_exp_maghrib_bg_img, R.id.widget_exp_maghrib_name, R.id.widget_exp_maghrib_time, PrayerType.MAGHRIB)
            bindExpRow(R.id.widget_exp_isha_row, R.id.widget_exp_isha_bg_img, R.id.widget_exp_isha_name, R.id.widget_exp_isha_time, PrayerType.ISHA)
        } else {
            views.setViewVisibility(R.id.widget_exp_schedule_list, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widget_root_expanded, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_exp_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_exp_refresh_btn, refreshIntent)
        return views
    }

    private fun populatePrayerRibbon(
        context: Context,
        views: RemoteViews,
        colors: WidgetColorScheme,
        prayerMap: Map<PrayerType, PrayerTimeItem>,
        nextType: PrayerType,
        isTomorrowFajr: Boolean,
        formatter: DateTimeFormatter,
        language: AppLanguage,
        showSunrise: Boolean
    ) {
        val scale = colors.fontScale
        fun updateSlot(
            containerId: Int,
            bgId: Int,
            nameId: Int,
            timeId: Int,
            type: PrayerType
        ) {
            if (type == PrayerType.SUNRISE && !showSunrise) {
                views.setViewVisibility(containerId, View.GONE)
                return
            }
            views.setViewVisibility(containerId, View.VISIBLE)
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, language))
            views.setTextViewText(timeId, item?.time?.format(formatter) ?: "--:--")

            views.setTextViewTextSize(nameId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, 9f * scale)

            val isHighlighted = !isTomorrowFajr && nextType == type
            if (isHighlighted) {
                views.setInt(bgId, "setColorFilter", colors.activePrayerBgColor)
                views.setTextColor(nameId, colors.textOnAccentColor)
                views.setTextColor(timeId, colors.textOnAccentColor)
            } else {
                views.setInt(bgId, "setColorFilter", colors.inactivePrayerBgColor)
                views.setTextColor(nameId, colors.textSecondaryColor)
                views.setTextColor(timeId, colors.textPrimaryColor)
            }
        }

        updateSlot(R.id.widget_fajr_container, R.id.widget_fajr_bg_img, R.id.widget_fajr_name, R.id.widget_fajr_time, PrayerType.FAJR)
        updateSlot(R.id.widget_sunrise_container, R.id.widget_sunrise_bg_img, R.id.widget_sunrise_name, R.id.widget_sunrise_time, PrayerType.SUNRISE)
        updateSlot(R.id.widget_dhuhr_container, R.id.widget_dhuhr_bg_img, R.id.widget_dhuhr_name, R.id.widget_dhuhr_time, PrayerType.DHUHR)
        updateSlot(R.id.widget_asr_container, R.id.widget_asr_bg_img, R.id.widget_asr_name, R.id.widget_asr_time, PrayerType.ASR)
        updateSlot(R.id.widget_maghrib_container, R.id.widget_maghrib_bg_img, R.id.widget_maghrib_name, R.id.widget_maghrib_time, PrayerType.MAGHRIB)
        updateSlot(R.id.widget_isha_container, R.id.widget_isha_bg_img, R.id.widget_isha_name, R.id.widget_isha_time, PrayerType.ISHA)
    }

    private fun formatLocationString(city: String, country: String, isArabic: Boolean): String {
        if (city.isBlank()) return country
        if (country.isBlank()) return city
        return if (isArabic) {
            "$city، $country"
        } else {
            "$city, $country"
        }
    }

    private fun getPrayerName(type: PrayerType, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ARABIC -> when (type) {
                PrayerType.FAJR -> "الفجر"
                PrayerType.SUNRISE -> "الشروق"
                PrayerType.DHUHR -> "الظهر"
                PrayerType.ASR -> "العصر"
                PrayerType.MAGHRIB -> "المغرب"
                PrayerType.ISHA -> "العشاء"
            }
            AppLanguage.ENGLISH -> type.title
            AppLanguage.SYSTEM -> {
                val isAr = Locale.getDefault().language.equals("ar", ignoreCase = true)
                if (isAr) {
                    when (type) {
                        PrayerType.FAJR -> "الفجر"
                        PrayerType.SUNRISE -> "الشروق"
                        PrayerType.DHUHR -> "الظهر"
                        PrayerType.ASR -> "العصر"
                        PrayerType.MAGHRIB -> "المغرب"
                        PrayerType.ISHA -> "العشاء"
                    }
                } else {
                    type.title
                }
            }
        }
    }

    private fun formatCountdown(seconds: Long, isArabic: Boolean): String {
        if (seconds <= 0) return if (isArabic) "الآن" else "Now"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (isArabic) {
            when {
                hours > 0 -> "خلال $hours س و $minutes د"
                minutes > 0 -> "خلال $minutes د"
                else -> "أقل من دقيقة"
            }
        } else {
            when {
                hours > 0 -> "In ${hours}h ${minutes}m"
                minutes > 0 -> "In ${minutes}m"
                else -> "In < 1 min"
            }
        }
    }

    private fun scheduleNextWidgetUpdate(context: Context, nextPrayerTime: ZonedDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAppWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nowMillis = System.currentTimeMillis()
        val prayerMillis = nextPrayerTime.toInstant().toEpochMilli()
        val fifteenMinMillis = nowMillis + 15 * 60 * 1000L
        val triggerMillis = if (prayerMillis in (nowMillis + 1000)..fifteenMinMillis) {
            prayerMillis
        } else {
            fifteenMinMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            // Fallback
        }
    }
}
