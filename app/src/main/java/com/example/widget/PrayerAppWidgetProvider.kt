package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.calculator.PrayerTimesCalculator
import com.example.data.models.AppLanguage
import com.example.data.models.DailyPrayerSchedule
import com.example.data.models.PrayerTimeItem
import com.example.data.models.PrayerType
import com.example.data.preferences.AppPrayerSettings
import com.example.data.preferences.PrayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
                val provider = PrayerAppWidgetProvider()
                provider.onUpdate(context, appWidgetManager, appWidgetIds)
            }
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
        newOptions: Bundle
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
        val prefs = PrayerPreferences(context)
        val settings = prefs.settingsFlow.first()
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
        val countdownFormatted = formatCountdown(diffSeconds, settings.language == AppLanguage.ARABIC)

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

        // Intents for opening the app and refreshing
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

        val isArabic = settings.language == AppLanguage.ARABIC
        val prayerDisplayName = getPrayerName(nextPrayerType, settings.language)
        val formattedNextTime = nextPrayerZoned.format(timeFormatter)

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

            val smallViews = buildSmallWidget(
                context = context,
                settings = settings,
                prayerName = prayerDisplayName,
                prayerTime = formattedNextTime,
                countdown = countdownFormatted,
                mainIntent = mainPendingIntent,
                refreshIntent = refreshPendingIntent
            )

            val mediumViews = buildMediumWidget(
                context = context,
                settings = settings,
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
                // Responsive multi-size layouts in Android 12+ (Material You)
                RemoteViews(
                    mapOf(
                        SizeF(110f, 60f) to smallViews,
                        SizeF(210f, 60f) to mediumViews,
                        SizeF(240f, 150f) to largeViews
                    )
                )
            } else {
                // Sizing fallback based on measured options for pre-Android 12
                when {
                    minHeight < 110 && minWidth < 220 -> smallViews
                    minHeight < 150 || minWidth < 250 -> mediumViews
                    else -> largeViews
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, widgetViews)
        }

        scheduleNextWidgetUpdate(context, nextPrayerZoned)
    }

    private fun buildSmallWidget(
        context: Context,
        settings: AppPrayerSettings,
        prayerName: String,
        prayerTime: String,
        countdown: String,
        mainIntent: PendingIntent,
        refreshIntent: PendingIntent
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_small)
        views.setTextViewText(R.id.widget_small_location, settings.location.name)
        views.setTextViewText(R.id.widget_small_prayer_name, prayerName)
        views.setTextViewText(R.id.widget_small_prayer_time, prayerTime)
        views.setTextViewText(R.id.widget_small_countdown, countdown)

        views.setOnClickPendingIntent(R.id.widget_root_small, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_small_refresh_btn, refreshIntent)
        return views
    }

    private fun buildMediumWidget(
        context: Context,
        settings: AppPrayerSettings,
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
        views.setTextViewText(R.id.widget_med_location, "${settings.location.name}")
        views.setTextViewText(R.id.widget_med_next_prayer_name, prayerName)
        views.setTextViewText(R.id.widget_med_next_prayer_time, prayerTime)
        views.setTextViewText(R.id.widget_med_countdown, countdown)
        views.setProgressBar(R.id.widget_med_prayer_progress, 100, progressPercent, false)

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        val prayerList = listOf(PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB)

        fun bindSlot(containerId: Int, nameId: Int, timeId: Int, type: PrayerType) {
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, settings.language))
            views.setTextViewText(timeId, item?.time?.format(timeFormatter) ?: "--:--")
            if (nextPrayerType == type) {
                views.setInt(containerId, "setBackgroundResource", R.drawable.widget_active_prayer_bg)
                views.setTextColor(nameId, context.getColor(R.color.widget_text_on_accent))
                views.setTextColor(timeId, context.getColor(R.color.widget_text_on_accent))
            } else {
                views.setInt(containerId, "setBackgroundResource", R.drawable.widget_inactive_prayer_bg)
                views.setTextColor(nameId, context.getColor(R.color.widget_text_secondary))
                views.setTextColor(timeId, context.getColor(R.color.widget_text_primary))
            }
        }

        bindSlot(R.id.widget_med_slot1_container, R.id.widget_med_slot1_name, R.id.widget_med_slot1_time, prayerList[0])
        bindSlot(R.id.widget_med_slot2_container, R.id.widget_med_slot2_name, R.id.widget_med_slot2_time, prayerList[1])
        bindSlot(R.id.widget_med_slot3_container, R.id.widget_med_slot3_name, R.id.widget_med_slot3_time, prayerList[2])

        views.setOnClickPendingIntent(R.id.widget_root_medium, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_med_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_med_refresh_btn, refreshIntent)
        return views
    }

    private fun buildLargeWidget(
        context: Context,
        settings: AppPrayerSettings,
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

        views.setTextViewText(R.id.widget_location_text, "${settings.location.name}, ${settings.location.country}")
        views.setTextViewText(R.id.widget_hijri_text, todaySchedule.hijriDateString)

        views.setTextViewText(R.id.widget_next_prayer_name, prayerName)
        views.setTextViewText(R.id.widget_next_prayer_time, prayerTime)
        views.setTextViewText(R.id.widget_countdown_text, countdown)
        views.setTextViewText(
            R.id.widget_status_text,
            if (diffSeconds <= 60) {
                if (isArabic) "حان وقت الصلاة" else "Prayer Time!"
            } else {
                if (isArabic) "متبقي" else "Remaining"
            }
        )
        views.setProgressBar(R.id.widget_prayer_progress, 100, progressPercent, false)

        val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
        populatePrayerRibbon(context, views, prayerMap, nextPrayerType, isTomorrowFajr, timeFormatter, settings.language)

        views.setOnClickPendingIntent(R.id.widget_root, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_hero_card, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshIntent)
        return views
    }

    private fun populatePrayerRibbon(
        context: Context,
        views: RemoteViews,
        prayerMap: Map<PrayerType, PrayerTimeItem>,
        nextType: PrayerType,
        isTomorrowFajr: Boolean,
        formatter: DateTimeFormatter,
        language: AppLanguage
    ) {
        fun updateSlot(
            containerId: Int,
            nameId: Int,
            timeId: Int,
            type: PrayerType
        ) {
            val item = prayerMap[type]
            views.setTextViewText(nameId, getPrayerName(type, language))
            views.setTextViewText(timeId, item?.time?.format(formatter) ?: "--:--")

            val isHighlighted = !isTomorrowFajr && nextType == type
            if (isHighlighted) {
                views.setInt(containerId, "setBackgroundResource", R.drawable.widget_active_prayer_bg)
                views.setTextColor(nameId, context.getColor(R.color.widget_text_on_accent))
                views.setTextColor(timeId, context.getColor(R.color.widget_text_on_accent))
            } else {
                views.setInt(containerId, "setBackgroundResource", R.drawable.widget_inactive_prayer_bg)
                views.setTextColor(nameId, context.getColor(R.color.widget_text_secondary))
                views.setTextColor(timeId, context.getColor(R.color.widget_text_primary))
            }
        }

        updateSlot(R.id.widget_fajr_container, R.id.widget_fajr_name, R.id.widget_fajr_time, PrayerType.FAJR)
        updateSlot(R.id.widget_sunrise_container, R.id.widget_sunrise_name, R.id.widget_sunrise_time, PrayerType.SUNRISE)
        updateSlot(R.id.widget_dhuhr_container, R.id.widget_dhuhr_name, R.id.widget_dhuhr_time, PrayerType.DHUHR)
        updateSlot(R.id.widget_asr_container, R.id.widget_asr_name, R.id.widget_asr_time, PrayerType.ASR)
        updateSlot(R.id.widget_maghrib_container, R.id.widget_maghrib_name, R.id.widget_maghrib_time, PrayerType.MAGHRIB)
        updateSlot(R.id.widget_isha_container, R.id.widget_isha_name, R.id.widget_isha_time, PrayerType.ISHA)
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
            AppLanguage.TURKISH -> when (type) {
                PrayerType.FAJR -> "İmsak"
                PrayerType.SUNRISE -> "Güneş"
                PrayerType.DHUHR -> "Öğle"
                PrayerType.ASR -> "İkindi"
                PrayerType.MAGHRIB -> "Akşam"
                PrayerType.ISHA -> "Yatsı"
            }
            AppLanguage.URDU -> when (type) {
                PrayerType.FAJR -> "فجر"
                PrayerType.SUNRISE -> "طلوع آفتاب"
                PrayerType.DHUHR -> "ظہر"
                PrayerType.ASR -> "عصر"
                PrayerType.MAGHRIB -> "مغرب"
                PrayerType.ISHA -> "عشاء"
            }
            AppLanguage.INDONESIAN -> when (type) {
                PrayerType.FAJR -> "Subuh"
                PrayerType.SUNRISE -> "Terbit"
                PrayerType.DHUHR -> "Dzuhur"
                PrayerType.ASR -> "Ashar"
                PrayerType.MAGHRIB -> "Maghrib"
                PrayerType.ISHA -> "Isya"
            }
            AppLanguage.FRENCH -> when (type) {
                PrayerType.FAJR -> "Fajr"
                PrayerType.SUNRISE -> "Lever du soleil"
                PrayerType.DHUHR -> "Dhuhr"
                PrayerType.ASR -> "Asr"
                PrayerType.MAGHRIB -> "Maghrib"
                PrayerType.ISHA -> "Icha"
            }
            else -> type.title
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
