package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.calculator.PrayerTimesCalculator
import com.example.data.models.PrayerType
import com.example.data.preferences.AppPrayerSettings
import com.example.data.preferences.PrayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
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

                // Calculate countdown
                val diffSeconds = Duration.between(now, nextPrayerZoned).seconds
                val countdownFormatted = formatCountdown(diffSeconds)

                // Calculate progress
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

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.prayer_appwidget_layout)

                    // 1. Header info
                    views.setTextViewText(R.id.widget_location_text, "${settings.location.name}, ${settings.location.country}")
                    views.setTextViewText(R.id.widget_hijri_text, todaySchedule.hijriDateString)

                    // 2. Next Prayer Hero Card
                    val nextLabel = if (isTomorrowFajr) {
                        "NEXT: FAJR • الفجر (Tomorrow)"
                    } else {
                        "NEXT: ${nextPrayerType.title.uppercase()} • ${nextPrayerType.arabicName}"
                    }
                    views.setTextViewText(R.id.widget_next_prayer_name, nextLabel)
                    views.setTextViewText(R.id.widget_next_prayer_time, nextPrayerZoned.format(timeFormatter))
                    views.setTextViewText(R.id.widget_countdown_text, countdownFormatted)
                    views.setTextViewText(R.id.widget_status_text, if (diffSeconds <= 60) "Time for Prayer!" else "Remaining")
                    views.setProgressBar(R.id.widget_prayer_progress, 100, progressPercent, false)

                    // 3. Today's 6 Prayer Times Ribbon
                    val prayerMap = todaySchedule.prayerItems.associateBy { it.type }
                    populatePrayerRibbon(views, prayerMap, nextPrayerType, isTomorrowFajr, timeFormatter)

                    // 4. Click to open app
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val mainPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_hero_card, mainPendingIntent)

                    // 5. Click to refresh widget
                    val refreshIntent = Intent(context, PrayerAppWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET
                    }
                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_REFRESH,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

                // Schedule next widget refresh alarm at next prayer or within 15 minutes
                scheduleNextWidgetUpdate(context, nextPrayerZoned)

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

    private fun populatePrayerRibbon(
        views: RemoteViews,
        prayerMap: Map<PrayerType, com.example.data.models.PrayerTimeItem>,
        nextType: PrayerType,
        isTomorrowFajr: Boolean,
        formatter: DateTimeFormatter
    ) {
        fun updateSlot(
            containerId: Int,
            nameId: Int,
            timeId: Int,
            type: PrayerType,
            displayName: String
        ) {
            val item = prayerMap[type]
            views.setTextViewText(nameId, displayName)
            views.setTextViewText(timeId, item?.time?.format(formatter) ?: "--:--")

            val isHighlighted = !isTomorrowFajr && nextType == type
            if (isHighlighted) {
                views.setInt(containerId, "setBackgroundResource", R.drawable.widget_active_prayer_bg)
                views.setTextColor(nameId, 0xFFFFD49B.toInt())
                views.setTextColor(timeId, 0xFFFFFFFF.toInt())
            } else {
                views.setInt(containerId, "setBackgroundResource", R.drawable.widget_inactive_prayer_bg)
                views.setTextColor(nameId, 0xFFA8A4B8.toInt())
                views.setTextColor(timeId, 0xFFFFFFFF.toInt())
            }
        }

        updateSlot(R.id.widget_fajr_container, R.id.widget_fajr_name, R.id.widget_fajr_time, PrayerType.FAJR, "Fajr")
        updateSlot(R.id.widget_sunrise_container, R.id.widget_sunrise_name, R.id.widget_sunrise_time, PrayerType.SUNRISE, "Sunrise")
        updateSlot(R.id.widget_dhuhr_container, R.id.widget_dhuhr_name, R.id.widget_dhuhr_time, PrayerType.DHUHR, "Dhuhr")
        updateSlot(R.id.widget_asr_container, R.id.widget_asr_name, R.id.widget_asr_time, PrayerType.ASR, "Asr")
        updateSlot(R.id.widget_maghrib_container, R.id.widget_maghrib_name, R.id.widget_maghrib_time, PrayerType.MAGHRIB, "Maghrib")
        updateSlot(R.id.widget_isha_container, R.id.widget_isha_name, R.id.widget_isha_time, PrayerType.ISHA, "Isha")
    }

    private fun formatCountdown(seconds: Long): String {
        if (seconds <= 0) return "Now!"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "In ${hours}h ${minutes}m"
            minutes > 0 -> "In ${minutes} min"
            else -> "In < 1 min"
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

        // Schedule at the exact next prayer time or 15 minutes from now, whichever is sooner
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
            // Non-critical scheduling fallback
        }
    }
}
