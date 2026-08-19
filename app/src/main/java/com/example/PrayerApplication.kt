package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper

class PrayerApplication : Application() {

    companion object {
        const val CHANNEL_ATHAN_ID = "prayer_athan_channel_v2"
        const val CHANNEL_REMINDER_ID = "prayer_reminder_channel_v2"
        const val CHANNEL_LIVE_COUNTDOWN_ID = "prayer_live_countdown_channel_v1"

        lateinit var instance: PrayerApplication
            private set
    }

    private var lastNightModeBit = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingWidgetRefresh = Runnable {
        com.example.widget.PrayerAppWidgetProvider.updateAllWidgets(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        lastNightModeBit = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    // System-wide day/night broadcasts don't reach a manifest-registered widget receiver on
    // modern Android (the same background-execution limits that apply to CONNECTIVITY_ACTION
    // etc.), so a Material You/dynamic-color widget only re-reads the new colors on its next
    // scheduled update otherwise. This callback IS delivered reliably whenever the app process
    // is alive, which covers the common case (app used recently); it's a best-effort catch, not
    // a guarantee for a fully-killed process - Google's own widgets sidestep this entirely by
    // using static day/night resource files the launcher re-resolves with no app code involved,
    // which isn't available to us since Material You's actual colors require a runtime API call.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newNightModeBit = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (newNightModeBit != lastNightModeBit) {
            lastNightModeBit = newNightModeBit
            // Delayed, not immediate: querying getAppWidgetOptions() while the launcher is still
            // mid-animation for the theme switch can catch it reporting transitional (smaller)
            // widget bounds, so the size bucket picked here would visibly shrink for a moment
            // until the next real update. Debounced too, in case Android delivers more than one
            // configuration tick for the same theme change.
            mainHandler.removeCallbacks(pendingWidgetRefresh)
            mainHandler.postDelayed(pendingWidgetRefresh, 700)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete old channels if they had default sounds attached by Android
            try {
                notificationManager.deleteNotificationChannel("prayer_athan_channel")
                notificationManager.deleteNotificationChannel("prayer_dynamic_island_channel")
                // The Dynamic Island feature actually shipped its channel under the "_v2" id below -
                // the un-suffixed one above was already dead. Delete the real one so it stops
                // showing in system notification settings now that the feature is removed.
                notificationManager.deleteNotificationChannel("prayer_dynamic_island_channel_v2")
            } catch (e: Exception) {
                // Ignore
            }

            val athanChannel = NotificationChannel(
                CHANNEL_ATHAN_ID,
                "Prayer Times & Athan Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and Athan sounds when prayer time arrives"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "Pre-Prayer Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle pre-prayer reminders prior to adhan"
                enableVibration(true)
                setShowBadge(true)
            }

            val liveCountdownChannel = NotificationChannel(
                CHANNEL_LIVE_COUNTDOWN_ID,
                "Live Athan Countdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ambient live countdown shown before Athan time"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(athanChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(liveCountdownChannel)
        }
    }
}
