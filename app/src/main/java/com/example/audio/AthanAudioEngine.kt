package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import com.example.data.models.AthanAudioStream
import com.example.data.models.NotificationSoundType
import com.example.data.models.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdhanPlaybackState(
    val isPlaying: Boolean = false,
    val currentPrayer: PrayerType = PrayerType.FAJR,
    val progress: Float = 0f,
    val title: String = "Makkah Adhan"
)

object AthanAudioEngine {

    private const val TAG = "AthanAudioEngine"

    private val _playbackState = MutableStateFlow(AdhanPlaybackState())
    val playbackState: StateFlow<AdhanPlaybackState> = _playbackState.asStateFlow()

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var trackingJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Main)

    fun getRawResourceForSoundType(soundType: NotificationSoundType): Int {
        return when (soundType) {
            NotificationSoundType.ATHAN_MAKKAH_MULLA -> R.raw.athan_makkah_ali_bin_ahmad_mula
            NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY -> R.raw.athan_fajr1_kwait_mashary_alafasy
            NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA -> R.raw.athan_fajr2_jordan_kamel_allala
            NotificationSoundType.ATHAN_RIYADH_QATAMI -> R.raw.athan_riyadh_naser_alqatami
            NotificationSoundType.ATHAN_QATAR_NABET -> R.raw.athan_qatar_saleh_alnabet
            NotificationSoundType.ATHAN_QUDS_QAZAZ_1 -> R.raw.athan_palastine_quds_nagi_qazaz_1
            NotificationSoundType.ATHAN_QUDS_QAZAZ_2 -> R.raw.athan_palastine_quds_nagi_qazaz_2
            NotificationSoundType.ATHAN_EGYPT_DAWOD -> R.raw.athan_egypt_ahmad_dawod
            NotificationSoundType.ATHAN_EGYPT_ALALFI -> R.raw.athan_egypt_salah_alalfi
            NotificationSoundType.ATHAN_EGYPT_ABDULAATI -> R.raw.athan_egypt_alhusain_sayed_abdulaati
            NotificationSoundType.ATHAN_IRAQ_ALAMOURI -> R.raw.athan_iraq_abu_omar_alamouri
            NotificationSoundType.ATHAN_GEORGIA -> R.raw.athan_georgia
            NotificationSoundType.SHORT_TAKBEER -> R.raw.takbeer
            NotificationSoundType.MELODIC_TONE -> R.raw.chime
            else -> R.raw.athan_riyadh_naser_alqatami
        }
    }

    private fun createPreparedPlayer(context: Context, rawResId: Int, audioStream: AthanAudioStream): MediaPlayer? {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(audioStream.audioUsage)
            .setContentType(
                if (audioStream == AthanAudioStream.ALARM) AudioAttributes.CONTENT_TYPE_SONIFICATION
                else AudioAttributes.CONTENT_TYPE_MUSIC
            )
            .build()

        // 1. Direct MediaPlayer with setDataSource & openRawResourceFd (Sets attributes in IDLE state)
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(audioAttributes)
            val afd = context.resources.openRawResourceFd(rawResId)
            if (afd != null) {
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.prepare()
                player.setVolume(1.0f, 1.0f)
                return player
            }
        } catch (e: Exception) {
            Log.w(TAG, "Method 1 (openRawResourceFd) failed: ${e.message}, trying Method 2")
        }

        // 2. MediaPlayer.create with AudioAttributes (API 21+)
        try {
            val player = MediaPlayer.create(context.applicationContext, rawResId, audioAttributes, 0)
            if (player != null) {
                player.setVolume(1.0f, 1.0f)
                return player
            }
        } catch (e: Exception) {
            Log.w(TAG, "Method 2 (create with audioAttributes) failed: ${e.message}, trying Method 3")
        }

        // 3. Fallback MediaPlayer.create without post-preparation attribute modification
        try {
            val player = MediaPlayer.create(context.applicationContext, rawResId)
            if (player != null) {
                player.setVolume(1.0f, 1.0f)
                return player
            }
        } catch (e: Exception) {
            Log.e(TAG, "Method 3 (standard create) failed: ${e.message}")
        }

        return null
    }

    /**
     * Plays authentic Athan audio using Android's native MediaPlayer with progress tracking.
     */
    fun playAthan(
        context: Context,
        prayerType: PrayerType = PrayerType.DHUHR,
        soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN,
        audioStream: AthanAudioStream = AthanAudioStream.ALARM,
        onFinished: (() -> Unit)? = null
    ) {
        stop()

        val rawResId = getRawResourceForSoundType(soundType)
        val styleTitle = "${soundType.displayName} • ${prayerType.title}"

        try {
            val player = createPreparedPlayer(context, rawResId, audioStream)
            if (player == null) {
                Log.e(TAG, "Failed to create MediaPlayer for resource: $rawResId")
                onFinished?.invoke()
                return
            }

            mediaPlayer = player
            player.setVolume(1.0f, 1.0f)

            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                currentPrayer = prayerType,
                progress = 0f,
                title = styleTitle
            )

            player.setOnCompletionListener {
                stop()
                onFinished?.invoke()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stop()
                onFinished?.invoke()
                true
            }

            player.start()

            // Track audio progress smoothly
            trackingJob = audioScope.launch {
                val duration = player.duration.coerceAtLeast(1)

                while (isActive && mediaPlayer?.isPlaying == true) {
                    val currentPosMs = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }
                    val currentProgress = (currentPosMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                    _playbackState.value = _playbackState.value.copy(
                        progress = currentProgress
                    )

                    delay(200)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
            stop()
            onFinished?.invoke()
        }
    }

    /**
     * Plays the requested sound type using high-fidelity native audio.
     */
    fun playSoundType(
        context: Context,
        soundType: NotificationSoundType,
        prayerType: PrayerType = PrayerType.DHUHR,
        audioStream: AthanAudioStream = AthanAudioStream.ALARM,
        onFinished: (() -> Unit)? = null
    ) {
        stop()
        if (soundType.isFullAthan) {
            playAthan(context, prayerType, soundType, audioStream, onFinished)
            return
        }

        when (soundType) {
            NotificationSoundType.SHORT_TAKBEER -> {
                playTakbeer(context, audioStream, onFinished)
            }
            NotificationSoundType.MELODIC_TONE -> {
                playGentleChime(context, audioStream, onFinished)
            }
            NotificationSoundType.VIBRATE_ONLY -> {
                vibrateDevice(context)
                onFinished?.invoke()
            }
            NotificationSoundType.SILENT -> {
                onFinished?.invoke()
            }
            else -> {
                playAthan(context, prayerType, soundType, audioStream, onFinished)
            }
        }
    }

    /**
     * Plays authentic Mishari Alafasy Takbeerat.
     */
    fun playTakbeer(
        context: Context,
        audioStream: AthanAudioStream = AthanAudioStream.ALARM,
        onFinished: (() -> Unit)? = null
    ) {
        stop()
        try {
            val player = createPreparedPlayer(context, R.raw.takbeer, audioStream) ?: run {
                onFinished?.invoke()
                return
            }
            mediaPlayer = player
            player.setVolume(1.0f, 1.0f)

            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                title = "Takbeerat",
                progress = 0f
            )

            player.setOnCompletionListener {
                stop()
                onFinished?.invoke()
            }
            player.setOnErrorListener { _, _, _ ->
                stop()
                onFinished?.invoke()
                true
            }
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Takbeer playback failed", e)
            stop()
            onFinished?.invoke()
        }
    }

    /**
     * Plays gentle crystal chime tone.
     */
    fun playGentleChime(
        context: Context? = null,
        audioStream: AthanAudioStream = AthanAudioStream.ALARM,
        onFinished: (() -> Unit)? = null
    ) {
        stop()
        if (context == null) {
            onFinished?.invoke()
            return
        }
        try {
            val player = createPreparedPlayer(context, R.raw.chime, audioStream) ?: run {
                onFinished?.invoke()
                return
            }
            mediaPlayer = player
            player.setVolume(1.0f, 1.0f)

            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                title = "Prayer Reminder Chime"
            )
            player.setOnCompletionListener {
                stop()
                onFinished?.invoke()
            }
            player.setOnErrorListener { _, _, _ ->
                stop()
                onFinished?.invoke()
                true
            }
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Chime playback failed", e)
            stop()
            onFinished?.invoke()
        }
    }

    fun vibrateDevice(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 400, 200, 400, 200, 600)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    fun stop() {
        trackingJob?.cancel()
        trackingJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _playbackState.value = AdhanPlaybackState(isPlaying = false)
        }
    }
}
