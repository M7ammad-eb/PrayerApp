package com.example.audio

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.PrayerApplication
import com.example.R
import com.example.data.models.AthanAudioStream
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import com.example.data.preferences.PrayerPreferences
import com.example.util.LocalizedStrings

class AthanAudioService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val ACTION_PLAY_ATHAN = "com.example.ACTION_PLAY_ATHAN"
        const val ACTION_STOP_ATHAN = "com.example.ACTION_STOP_ATHAN"

        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_AUDIO_STREAM = "extra_audio_stream"
        const val EXTRA_VOLUME_PERCENT = "extra_volume_percent"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_SHOW_FULL_SCREEN = "extra_show_full_screen"

        const val NOTIFICATION_ID = 9999

        fun startAthan(
            context: Context,
            prayerType: PrayerType,
            soundType: NotificationSoundType,
            locationName: String = "",
            audioStream: AthanAudioStream = AthanAudioStream.ALARM,
            prayerTime: String = "",
            showFullScreenAlarm: Boolean = false
        ) {
            val intent = Intent(context, AthanAudioService::class.java).apply {
                action = ACTION_PLAY_ATHAN
                putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
                putExtra(EXTRA_SOUND_TYPE, soundType.name)
                putExtra(EXTRA_LOCATION_NAME, locationName)
                putExtra(EXTRA_AUDIO_STREAM, audioStream.name)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
                putExtra(EXTRA_SHOW_FULL_SCREEN, showFullScreenAlarm)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAthan(context: Context) {
            val intent = Intent(context, AthanAudioService::class.java).apply {
                action = ACTION_STOP_ATHAN
            }
            context.startService(intent)
        }
    }

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentPrayerType: PrayerType = PrayerType.DHUHR
    private var currentSoundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN
    private var currentAudioStream: AthanAudioStream = AthanAudioStream.ALARM
    private var locationName: String = ""

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_PLAY_ATHAN

        if (action == ACTION_STOP_ATHAN) {
            stopAudioAndService()
            return START_NOT_STICKY
        }

        val prayerTypeName = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: PrayerType.DHUHR.name
        val soundTypeName = intent?.getStringExtra(EXTRA_SOUND_TYPE) ?: NotificationSoundType.FULL_ATHAN.name
        val audioStreamName = intent?.getStringExtra(EXTRA_AUDIO_STREAM) ?: AthanAudioStream.ALARM.name
        val prayerTimeStr = intent?.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val showFullScreenAlarm = intent?.getBooleanExtra(EXTRA_SHOW_FULL_SCREEN, false) ?: false
        locationName = intent?.getStringExtra(EXTRA_LOCATION_NAME) ?: ""

        currentPrayerType = try {
            PrayerType.valueOf(prayerTypeName)
        } catch (e: Exception) {
            PrayerType.DHUHR
        }

        currentSoundType = try {
            NotificationSoundType.valueOf(soundTypeName)
        } catch (e: Exception) {
            NotificationSoundType.FULL_ATHAN
        }

        currentAudioStream = try {
            AthanAudioStream.valueOf(audioStreamName)
        } catch (e: Exception) {
            AthanAudioStream.ALARM
        }

        // 1. Start as foreground immediately with initial notification to satisfy Android OS requirements
        val isArabic = PrayerPreferences.getInitialSettings(this).language.resolveIsArabic()
        val localizedRes = LocalizedStrings.forLanguage(this, isArabic)
        val localizedPrayerName = LocalizedStrings.prayerName(localizedRes, currentPrayerType)
        val initialNotification = buildNotification(
            title = localizedRes.getString(R.string.notif_athan_playing_title, localizedPrayerName),
            verseText = "${currentSoundType.localizedDisplayName(isArabic)}${if (locationName.isNotBlank()) " • $locationName" else ""}",
            subText = currentSoundType.localizedDisplayName(isArabic),
            localizedRes = localizedRes,
            prayerTime = prayerTimeStr,
            showFullScreenAlarm = showFullScreenAlarm
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    initialNotification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // 2. Request Audio Focus
        requestAudioFocus()

        // 3. Play sound engine
        AthanAudioEngine.playSoundType(
            context = this,
            soundType = currentSoundType,
            prayerType = currentPrayerType,
            audioStream = currentAudioStream,
            isArabic = isArabic,
            onFinished = {
                stopAudioAndService()
            }
        )

        return START_NOT_STICKY
    }

    private fun buildNotification(
        title: String,
        verseText: String,
        subText: String? = null,
        localizedRes: android.content.res.Resources = resources,
        prayerTime: String = "",
        showFullScreenAlarm: Boolean = false
    ): Notification {
        // Tapping (or the full-screen intent) opens the actual full-screen alarm UI rather than
        // just the main app, matching what the (now-suppressed) receiver-level notification used to do.
        val alarmViewIntent = com.example.ui.alarm.PrayerAlarmActivity.createIntent(
            context = this,
            prayerType = currentPrayerType,
            prayerTime = prayerTime,
            locationName = locationName,
            soundType = currentSoundType
        )
        val alarmViewPendingIntent = PendingIntent.getActivity(
            this,
            2,
            alarmViewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AthanAudioService::class.java).apply {
            action = ACTION_STOP_ATHAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, PrayerApplication.CHANNEL_ATHAN_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(verseText)
            .setSubText(subText ?: currentSoundType.localizedDisplayName(isArabic = false))
            .setStyle(NotificationCompat.BigTextStyle().bigText(verseText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // Not ongoing, so swiping the notification away is possible - paired with the delete
            // intent below so a swipe actually stops the athan instead of just hiding the
            // notification while it keeps playing.
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setVibrate(null)
            .setContentIntent(alarmViewPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, localizedRes.getString(R.string.alarm_stop_athan_btn), stopPendingIntent)
            .addAction(android.R.drawable.ic_input_get, localizedRes.getString(R.string.notif_alarm_open_view_action), alarmViewPendingIntent)

        if (showFullScreenAlarm) {
            builder.setFullScreenIntent(alarmViewPendingIntent, true)
        }

        return builder.build()
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(currentAudioStream.audioUsage)
            .setContentType(
                if (currentAudioStream == AthanAudioStream.ALARM) AudioAttributes.CONTENT_TYPE_SONIFICATION
                else AudioAttributes.CONTENT_TYPE_MUSIC
            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(this, currentAudioStream.streamType, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Another app took full focus: stop playback
                stopAudioAndService()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient loss
                stopAudioAndService()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Continue or duck volume
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PrayerApp:AthanAudioWakeLock")?.apply {
                // Safety timeout of 5 minutes max
                acquire(5 * 60 * 1000L)
            }
        } catch (e: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}
        wakeLock = null
    }

    private fun stopAudioAndService() {
        AthanAudioEngine.stop()
        abandonAudioFocus()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioAndService()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
