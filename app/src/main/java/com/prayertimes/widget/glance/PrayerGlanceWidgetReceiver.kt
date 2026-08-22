package com.prayertimes.widget.glance

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.prayertimes.PrayerApplication
import kotlinx.coroutines.launch

/**
 * Receiver for the PrayerGlanceWidget proof-of-concept, added alongside (not replacing)
 * PrayerAppWidgetProvider so the two can be compared side by side on a home screen. Mirrors
 * PrayerAppWidgetProvider's refresh triggers (prayer alarms, boot, time/date/timezone changes)
 * since those custom/system actions aren't part of GlanceAppWidgetReceiver's automatic dispatch
 * (which only auto-handles the standard AppWidgetProvider lifecycle callbacks).
 */
class PrayerGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = PrayerGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            "com.prayertimes.ACTION_PRAYER_ALARM" -> {
                val pendingResult = goAsync()
                PrayerApplication.instance.applicationScope.launch {
                    try {
                        glanceAppWidget.updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
