package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import kotlin.math.PI
import kotlin.math.sin

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

    private val STANDARD_ADHAN_VERSES = listOf(
        AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ", "Allahu Akbar, Allahu Akbar", "Allah is the Greatest, Allah is the Greatest", 5.5f),
        AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ", "Allahu Akbar, Allahu Akbar", "Allah is the Greatest, Allah is the Greatest", 5.5f),
        AdhanVerse("أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللَّهُ", "Ash-hadu an la ilaha illallah", "I bear witness that there is no god but Allah", 6.0f),
        AdhanVerse("أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللَّهُ", "Ash-hadu an la ilaha illallah", "I bear witness that there is no god but Allah", 6.0f),
        AdhanVerse("أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ", "Ash-hadu anna Muhammadan Rasulullah", "I bear witness that Muhammad is the Messenger of Allah", 6.5f),
        AdhanVerse("أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ", "Ash-hadu anna Muhammadan Rasulullah", "I bear witness that Muhammad is the Messenger of Allah", 6.5f),
        AdhanVerse("حَيَّ عَلَى الصَّلَاةِ", "Hayya 'ala as-Salah", "Hasten to prayer", 6.0f),
        AdhanVerse("حَيَّ عَلَى الصَّلَاةِ", "Hayya 'ala as-Salah", "Hasten to prayer", 6.0f),
        AdhanVerse("حَيَّ عَلَى الْفَلَاحِ", "Hayya 'ala al-Falah", "Hasten to success", 6.0f),
        AdhanVerse("حَيَّ عَلَى الْفَلَاحِ", "Hayya 'ala al-Falah", "Hasten to success", 6.0f),
        AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ", "Allahu Akbar, Allahu Akbar", "Allah is the Greatest, Allah is the Greatest", 5.5f),
        AdhanVerse("لَا إِلٰهَ إِلَّا اللَّهُ", "La ilaha illallah", "There is no god but Allah", 7.0f)
    )

    private val FAJR_EXTRA_VERSE = AdhanVerse(
        "الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ",
        "As-Salatu Khayrun Minan-Nawm",
        "Prayer is better than sleep",
        6.5f
    )

    fun getVersesForPrayer(prayerType: PrayerType): List<AdhanVerse> {
        return if (prayerType == PrayerType.FAJR) {
            val list = STANDARD_ADHAN_VERSES.toMutableList()
            // Insert "As-Salatu Khayrun Minan-Nawm" twice after the second "Hayya 'alal-falah"
            list.add(10, FAJR_EXTRA_VERSE)
            list.add(11, FAJR_EXTRA_VERSE)
            list
        } else {
            STANDARD_ADHAN_VERSES
        }
    }

    private val _playbackState = MutableStateFlow(AdhanPlaybackState())
    val playbackState: StateFlow<AdhanPlaybackState> = _playbackState.asStateFlow()

    private var currentAudioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun playAthan(
        prayerType: PrayerType = PrayerType.DHUHR,
        soundType: NotificationSoundType = NotificationSoundType.FULL_ATHAN,
        onVerseChange: ((AdhanVerse, Int, Float) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        stop()

        val verses = getVersesForPrayer(prayerType)
        val totalDuration = verses.sumOf { it.durationSeconds.toDouble() }.toFloat()
        val styleTitle = "${soundType.displayName} • ${prayerType.title}"

        playbackJob = audioScope.launch {
            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                currentPrayer = prayerType,
                currentVerseIndex = 0,
                currentVerse = verses[0],
                progress = 0f,
                title = styleTitle
            )
            onVerseChange?.invoke(verses[0], 0, 0f)

            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            currentAudioTrack = track
            track.play()

            // Maqam variations per sound type
            val basePitch = when (soundType) {
                NotificationSoundType.ATHAN_MADINAH -> 196.0 // G3 (Calm, deeper)
                NotificationSoundType.ATHAN_AL_AQSA -> 246.94 // B3 (Soulful high)
                NotificationSoundType.ATHAN_CAIRO -> 233.08 // Bb3 (Vocal Egyptian)
                else -> 220.0 // A3 (Classic Makkah Bayati)
            }

            val notes = listOf(
                basePitch,             // 0
                basePitch * 1.05946,   // 1
                basePitch * 1.189207,  // 2
                basePitch * 1.33484,   // 3
                basePitch * 1.498307,  // 4
                basePitch * 1.587401,  // 5
                basePitch * 1.781797   // 6
            )

            var accumulatedSeconds = 0f

            for ((index, verse) in verses.withIndex()) {
                if (!isActive) break

                _playbackState.value = _playbackState.value.copy(
                    currentVerseIndex = index,
                    currentVerse = verse
                )
                val currentProg = (accumulatedSeconds / totalDuration).coerceIn(0f, 1f)
                onVerseChange?.invoke(verse, index, currentProg)

                // Synthesize melodic chant for the verse
                val verseDuration = verse.durationSeconds
                val chunkSamples = (sampleRate * verseDuration).toInt()
                val pcmBuffer = ShortArray(chunkSamples)

                val noteSeq = when (soundType) {
                    NotificationSoundType.ATHAN_MADINAH -> when (index % 4) {
                        0 -> listOf(notes[0], notes[2], notes[3], notes[2], notes[0])
                        1 -> listOf(notes[0], notes[1], notes[3], notes[2], notes[0])
                        2 -> listOf(notes[2], notes[4], notes[3], notes[2], notes[0])
                        else -> listOf(notes[0], notes[3], notes[2], notes[1], notes[0])
                    }
                    NotificationSoundType.ATHAN_AL_AQSA -> when (index % 4) {
                        0 -> listOf(notes[3], notes[5], notes[4], notes[2], notes[0])
                        1 -> listOf(notes[2], notes[3], notes[4], notes[1], notes[0])
                        2 -> listOf(notes[3], notes[6], notes[5], notes[3], notes[1])
                        else -> listOf(notes[1], notes[3], notes[2], notes[1], notes[0])
                    }
                    NotificationSoundType.ATHAN_CAIRO -> when (index % 4) {
                        0 -> listOf(notes[3], notes[4], notes[5], notes[3], notes[1], notes[0])
                        1 -> listOf(notes[0], notes[2], notes[4], notes[3], notes[1], notes[0])
                        2 -> listOf(notes[3], notes[5], notes[4], notes[3], notes[2], notes[0])
                        else -> listOf(notes[0], notes[2], notes[3], notes[1], notes[0])
                    }
                    else -> when (index % 4) {
                        0 -> listOf(notes[3], notes[4], notes[3], notes[1], notes[0]) // Allahu Akbar motif
                        1 -> listOf(notes[0], notes[2], notes[3], notes[2], notes[0]) // Shahadah motif
                        2 -> listOf(notes[3], notes[5], notes[4], notes[3], notes[1]) // Hayya 'ala motif
                        else -> listOf(notes[0], notes[1], notes[3], notes[1], notes[0])
                    }
                }

                val samplesPerNote = chunkSamples / noteSeq.size

                for (i in 0 until chunkSamples) {
                    val noteIndex = (i / samplesPerNote).coerceIn(0, noteSeq.lastIndex)
                    val targetFreq = noteSeq[noteIndex]

                    val t = i.toDouble() / sampleRate
                    // Gentle vibrato and acoustic resonance
                    val vibratoSpeed = if (soundType == NotificationSoundType.ATHAN_MADINAH) 4.2 else 5.2
                    val vibrato = 1.0 + 0.008 * sin(2 * PI * vibratoSpeed * t)
                    val freq = targetFreq * vibrato

                    // Natural envelope attack & decay per note
                    val noteLocalT = (i % samplesPerNote).toDouble() / samplesPerNote
                    val env = when {
                        noteLocalT < 0.12 -> noteLocalT / 0.12
                        noteLocalT > 0.78 -> (1.0 - noteLocalT) / 0.22
                        else -> 1.0
                    }

                    // Multi-harmonic acoustic vocal resonance
                    val fundamental = sin(2 * PI * freq * t)
                    val secondHarmonic = 0.35 * sin(2 * PI * freq * 2.0 * t)
                    val thirdHarmonic = 0.18 * sin(2 * PI * freq * 3.0 * t)
                    val subHarmonic = 0.12 * sin(2 * PI * (freq / 2.0) * t)

                    val sampleValue = ((fundamental + secondHarmonic + thirdHarmonic + subHarmonic) * env * 0.7 * Short.MAX_VALUE).toInt()
                    pcmBuffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                track.write(pcmBuffer, 0, chunkSamples)

                val stepTime = 100L
                val steps = (verseDuration * 1000 / stepTime).toInt()
                for (s in 0 until steps) {
                    if (!isActive) break
                    delay(stepTime)
                    accumulatedSeconds += 0.1f
                    val prog = (accumulatedSeconds / totalDuration).coerceIn(0f, 1f)
                    _playbackState.value = _playbackState.value.copy(progress = prog)
                }
            }

            try {
                track.stop()
                track.release()
            } catch (e: Exception) {}

            currentAudioTrack = null
            _playbackState.value = AdhanPlaybackState(isPlaying = false, progress = 1f)
            onFinished?.invoke()
        }
    }

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
                playAthan(prayerType, soundType, onVerseChange, onFinished)
            }
            NotificationSoundType.SHORT_TAKBEER -> {
                playTakbeer(onFinished)
            }
            NotificationSoundType.MELODIC_TONE -> {
                playGentleChime(onFinished)
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

    fun playTakbeer(onFinished: (() -> Unit)? = null) {
        stop()
        playbackJob = audioScope.launch {
            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                title = "Takbeerat",
                currentVerse = AdhanVerse("اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ ، لَا إِلٰهَ إِلَّا اللَّهُ", "Allahu Akbar, Allahu Akbar, La ilaha illallah", "Allah is the Greatest, there is no god but Allah", 4.0f)
            )

            val sampleRate = 44100
            val totalSamples = (sampleRate * 4.0).toInt()
            val pcm = ShortArray(totalSamples)

            val freq1 = 293.66 // D4
            val freq2 = 349.23 // F4
            val freq3 = 440.00 // A4

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val env = (1.0 - (i.toDouble() / totalSamples)).coerceIn(0.0, 1.0)
                val signal = (sin(2 * PI * freq1 * t) + 0.6 * sin(2 * PI * freq2 * t) + 0.4 * sin(2 * PI * freq3 * t)) * env * 0.8 * Short.MAX_VALUE
                pcm[i] = signal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            currentAudioTrack = track
            track.write(pcm, 0, pcm.size)
            track.play()

            delay(4000)
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {}
            _playbackState.value = AdhanPlaybackState(isPlaying = false)
            onFinished?.invoke()
        }
    }

    fun playGentleChime(onFinished: (() -> Unit)? = null) {
        stop()
        playbackJob = audioScope.launch {
            val sampleRate = 44100
            val duration = 2.5
            val totalSamples = (sampleRate * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val chimeFreq = 523.25 // C5

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val env = Math.exp(-2.5 * t) // Exponential decay chime
                val signal = (sin(2 * PI * chimeFreq * t) + 0.35 * sin(2 * PI * chimeFreq * 2 * t) + 0.2 * sin(2 * PI * chimeFreq * 3 * t)) * env * 0.7 * Short.MAX_VALUE
                pcm[i] = signal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            currentAudioTrack = track
            track.write(pcm, 0, pcm.size)
            track.play()

            delay(2500)
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {}
            _playbackState.value = AdhanPlaybackState(isPlaying = false)
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
        } catch (e: Exception) {}
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
        } catch (e: Exception) {}
        currentAudioTrack = null
        _playbackState.value = AdhanPlaybackState(isPlaying = false)
    }
}
