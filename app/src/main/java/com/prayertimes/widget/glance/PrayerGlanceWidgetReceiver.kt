package com.prayertimes.widget.glance

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for the PrayerGlanceWidget proof-of-concept, added alongside (not replacing)
 * PrayerAppWidgetProvider so the two can be compared side by side on a home screen.
 * Deliberately minimal for now - no custom refresh triggers (prayer alarms, boot, time/date
 * changes) - to isolate whether the widget renders at all before layering that back on.
 */
class PrayerGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerGlanceWidget()
}
