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

data class AdhanVerse(
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val durationSeconds: Float
)

data class AdhanPlaybackState(
    val isPlaying: Boolean = false,
    val currentPrayer: PrayerType = PrayerType.FAJR,
    val currentVerseIndex: Int = 0,
    val currentVerse: AdhanVerse? = null,
    val progress: Float = 0f,
    val title: String = "Makkah Adhan"
)

object AthanAudioEngine {

    private const val TAG = "AthanAudioEngine"

    private val STANDARD_ADHAN_VERSES = listOf(
        AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ", "Allahu Akbar, Allahu Akbar", "Allah is the Greatest, Allah is the Greatest", 5.0f),
        AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ", "Allahu Akbar, Allahu Akbar", "Allah is the Greatest, Allah is the Greatest", 5.0f),
        AdhanVerse("أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللَّهُ", "Ash-hadu an la ilaha illallah", "I bear witness that there is no god but Allah", 5.5f),
        AdhanVerse("أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللَّهُ", "Ash-hadu an la ilaha illallah", "I bear witness that there is no god but Allah", 5.5f),
        AdhanVerse("أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ", "Ash-hadu anna Muhammadan Rasulullah", "I bear witness that Muhammad is the Messenger of Allah", 5.5f),
        AdhanVerse("أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ", "Ash-hadu anna Muhammadan Rasulullah", "I bear witness that Muhammad is the Messenger of Allah", 5.5f),
        AdhanVerse("حَيَّ عَلَى الصَّلَاةِ", "Hayya 'ala as-Salah", "Hasten to prayer", 5.0f),
        AdhanVerse("حَيَّ عَلَى الصَّلَاةِ", "Hayya 'ala as-Salah", "Hasten to prayer", 5.0f),
        AdhanVerse("حَيَّ عَلَى الْفَلَاحِ", "Hayya 'ala al-Falah", "Hasten to success", 5.0f),
        AdhanVerse("حَيَّ عَلَى الْفَلَاحِ", "Hayya 'ala al-Falah", "Hasten to success", 5.0f),
        AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ", "Allahu Akbar, Allahu Akbar", "Allah is the Greatest, Allah is the Greatest", 5.0f),
        AdhanVerse("لَا إِلٰهَ إِلَّا اللَّهُ", "La ilaha illallah", "There is no god but Allah", 6.0f)
    )

    private val FAJR_EXTRA_VERSE = AdhanVerse(
        "الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ",
        "As-Salatu Khayrun Minan-Nawm",
        "Prayer is better than sleep",
        5.5f
    )

    fun getVersesForPrayer(prayerType: PrayerType): List<AdhanVerse> {
        return if (prayerType == PrayerType.FAJR) {
            val list = STANDARD_ADHAN_VERSES.toMutableList()
            list.add(10, FAJR_EXTRA_VERSE)
            list.add(11, FAJR_EXTRA_VERSE)
            list
        } else {
            STANDARD_ADHAN_VERSES
        }
    }

    private val _playbackState = MutableStateFlow(AdhanPlaybackState())
    val playbackState: StateFlow<AdhanPlaybackState> = _playbackState.asStateFlow()

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var trackingJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Main)

    fun getRawResourceForSoundType(soundType: NotificationSoundType): Int {
        return when (soundType) {
            NotificationSoundType.FULL_ATHAN -> R.raw.athan_makkah
            NotificationSoundType.ATHAN_MADINAH -> R.raw.athan_madinah
            NotificationSoundType.ATHAN_AL_AQSA -> R.raw.athan_al_aqsa
            NotificationSoundType.ATHAN_CAIRO -> R.raw.athan_cairo
            NotificationSoundType.SHORT_TAKBEER -> R.raw.takbeer
            NotificationSoundType.MELODIC_TONE -> R.raw.chime
            else -> R.raw.athan_makkah
        }
    }

    /**
     * Plays authentic Athan audio using Android's native MediaPlayer with progress & lyrics synchronization.
     */
    fun playAthan(
        context: Context,
        prayerType: PrayerType = PrayerType.DHUHR,
        soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN,
        onVerseChange: ((AdhanVerse, Int, Float) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        stop()

        val rawResId = getRawResourceForSoundType(soundType)
        val verses = getVersesForPrayer(prayerType)
        val styleTitle = "${soundType.displayName} • ${prayerType.title}"

        try {
            val player = MediaPlayer.create(context.applicationContext, rawResId)
            if (player == null) {
                Log.e(TAG, "Failed to create MediaPlayer for resource: $rawResId")
                onFinished?.invoke()
                return
            }

            mediaPlayer = player

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            player.setAudioAttributes(audioAttributes)
            player.setVolume(1.0f, 1.0f)

            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                currentPrayer = prayerType,
                currentVerseIndex = 0,
                currentVerse = verses.firstOrNull(),
                progress = 0f,
                title = styleTitle
            )
            verses.firstOrNull()?.let { onVerseChange?.invoke(it, 0, 0f) }

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

            // Synchronize lyrics/verses with audio position
            trackingJob = audioScope.launch {
                val duration = player.duration.coerceAtLeast(1)
                val totalVerseSeconds = verses.sumOf { it.durationSeconds.toDouble() }.toFloat()

                while (isActive && mediaPlayer?.isPlaying == true) {
                    val currentPosMs = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }
                    val currentProgress = (currentPosMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                    // Map current audio progress to current verse
                    val scaledSeconds = currentProgress * totalVerseSeconds
                    var accumulatedSec = 0f
                    var matchedVerseIndex = 0
                    for ((idx, verse) in verses.withIndex()) {
                        accumulatedSec += verse.durationSeconds
                        if (scaledSeconds <= accumulatedSec || idx == verses.lastIndex) {
                            matchedVerseIndex = idx
                            break
                        }
                    }

                    val activeVerse = verses.getOrNull(matchedVerseIndex) ?: verses.first()

                    _playbackState.value = _playbackState.value.copy(
                        currentVerseIndex = matchedVerseIndex,
                        currentVerse = activeVerse,
                        progress = currentProgress
                    )
                    onVerseChange?.invoke(activeVerse, matchedVerseIndex, currentProgress)

                    delay(150)
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
        onVerseChange: ((AdhanVerse, Int, Float) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        stop()
        when (soundType) {
            NotificationSoundType.FULL_ATHAN,
            NotificationSoundType.ATHAN_MADINAH,
            NotificationSoundType.ATHAN_AL_AQSA,
            NotificationSoundType.ATHAN_CAIRO -> {
                playAthan(context, prayerType, soundType, onVerseChange, onFinished)
            }
            NotificationSoundType.SHORT_TAKBEER -> {
                playTakbeer(context, onFinished)
            }
            NotificationSoundType.MELODIC_TONE -> {
                playGentleChime(context, onFinished)
            }
            NotificationSoundType.VIBRATE_ONLY -> {
                vibrateDevice(context)
                onFinished?.invoke()
            }
            NotificationSoundType.SILENT -> {
                onFinished?.invoke()
            }
        }
    }

    /**
     * Plays authentic Mishari Alafasy Takbeerat.
     */
    fun playTakbeer(context: Context, onFinished: (() -> Unit)? = null) {
        stop()
        try {
            val player = MediaPlayer.create(context.applicationContext, R.raw.takbeer) ?: run {
                onFinished?.invoke()
                return
            }
            mediaPlayer = player
            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                title = "Takbeerat",
                currentVerse = AdhanVerse(
                    "اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ ، لَا إِلٰهَ إِلَّا اللَّهُ",
                    "Allahu Akbar, Allahu Akbar, La ilaha illallah",
                    "Allah is the Greatest, Allah is the Greatest, there is no god but Allah",
                    4.0f
                )
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
    fun playGentleChime(context: Context? = null, onFinished: (() -> Unit)? = null) {
        stop()
        if (context == null) {
            onFinished?.invoke()
            return
        }
        try {
            val player = MediaPlayer.create(context.applicationContext, R.raw.chime) ?: run {
                onFinished?.invoke()
                return
            }
            mediaPlayer = player
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
