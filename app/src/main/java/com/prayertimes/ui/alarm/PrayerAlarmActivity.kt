package com.prayertimes.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.prayertimes.MainActivity
import com.prayertimes.audio.AthanAudioEngine
import com.prayertimes.audio.AthanAudioService
import com.prayertimes.data.models.AthanAudioStream
import com.prayertimes.data.models.NotificationSoundType
import com.prayertimes.data.models.PrayerType
import com.prayertimes.data.preferences.PrayerPreferences
import com.prayertimes.notifications.PrayerNotificationScheduler
import com.prayertimes.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PrayerAlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"

        fun createIntent(
            context: Context,
            prayerType: PrayerType,
            prayerTime: String,
            locationName: String,
            soundType: NotificationSoundType
        ): Intent {
            return Intent(context, PrayerAlarmActivity::class.java).apply {
                putExtra(EXTRA_PRAYER_NAME, prayerType.name)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
                putExtra(EXTRA_LOCATION_NAME, locationName)
                putExtra(EXTRA_SOUND_TYPE, soundType.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        }
    }

    private var prayerType: PrayerType = PrayerType.DHUHR
    private var prayerTime: String = ""
    private var locationName: String = ""
    private var soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn on screen and show when device is locked/idle
        turnScreenOnAndShowWhenLocked()

        parseIntentExtras(intent)

        // If audio isn't already playing from background service, ensure it plays according to user's sound & stream preferences
        ensureAudioPlaying()

        setContent {
            val prefs = remember { com.prayertimes.data.preferences.PrayerPreferences(this@PrayerAlarmActivity) }
            val settings by prefs.settingsFlow.collectAsState(initial = null)
            
            MyApplicationTheme(
                themeMode = settings?.themeMode ?: com.prayertimes.data.models.AppThemeMode.SYSTEM,
                colorPreset = settings?.colorPreset ?: com.prayertimes.data.models.AppColorPreset.SYSTEM_DYNAMIC,
                followSystemColors = settings?.followSystemColors ?: true,
                customColorSeed = settings?.customColorSeed ?: com.prayertimes.data.models.AppColorPreset.CUSTOM.previewColor
            ) {
                val playbackState by AthanAudioEngine.playbackState.collectAsState()

                PrayerAlarmScreen(
                    prayerType = prayerType,
                    prayerTimeFormatted = prayerTime,
                    locationName = locationName,
                    soundType = soundType,
                    playbackState = playbackState,
                    onStopAthan = {
                        AthanAudioService.stopAthan(this@PrayerAlarmActivity)
                        AthanAudioEngine.stop()
                        finish()
                    },
                    onSnooze = {
                        AthanAudioService.stopAthan(this@PrayerAlarmActivity)
                        AthanAudioEngine.stop()
                        // Schedule quick 5 minute snooze alarm
                        PrayerNotificationScheduler.scheduleSnoozeAlarm(
                            context = this@PrayerAlarmActivity,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            locationName = locationName,
                            soundType = soundType,
                            delaySeconds = 5 * 60
                        )
                        finish()
                    },
                    onOpenApp = {
                        val mainIntent = Intent(this@PrayerAlarmActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntentExtras(intent)
        turnScreenOnAndShowWhenLocked()
    }

    private fun parseIntentExtras(intent: Intent?) {
        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: PrayerType.DHUHR.name
        prayerType = try {
            PrayerType.valueOf(prayerName)
        } catch (e: Exception) {
            PrayerType.DHUHR
        }
        prayerTime = intent?.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        locationName = intent?.getStringExtra(EXTRA_LOCATION_NAME) ?: ""
        val soundTypeName = intent?.getStringExtra(EXTRA_SOUND_TYPE) ?: NotificationSoundType.FULL_ATHAN.name
        soundType = try {
            NotificationSoundType.valueOf(soundTypeName)
        } catch (e: Exception) {
            NotificationSoundType.FULL_ATHAN
        }
    }

    private fun ensureAudioPlaying() {
        if (soundType == NotificationSoundType.SILENT || soundType == NotificationSoundType.VIBRATE_ONLY) {
            return
        }

        lifecycleScope.launch {
            val prefs = PrayerPreferences(this@PrayerAlarmActivity)
            val settings = prefs.settingsFlow.firstOrNull()
            val audioStream = settings?.audioStream ?: AthanAudioStream.ALARM

            if (!AthanAudioEngine.playbackState.value.isPlaying) {
                try {
                    AthanAudioService.startAthan(
                        context = this@PrayerAlarmActivity,
                        prayerType = prayerType,
                        soundType = soundType,
                        locationName = locationName,
                        audioStream = audioStream
                    )
                } catch (e: Exception) {
                    AthanAudioEngine.playSoundType(
                        context = this@PrayerAlarmActivity,
                        soundType = soundType,
                        prayerType = prayerType,
                        audioStream = audioStream,
                        isArabic = (settings?.language ?: com.prayertimes.data.models.AppLanguage.SYSTEM).resolveIsArabic(this@PrayerAlarmActivity)
                    )
                }
            }
        }
    }

    private fun turnScreenOnAndShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
