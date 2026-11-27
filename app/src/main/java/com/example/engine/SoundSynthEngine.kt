package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.MascotType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Native zero-asset PCM waveform synthesizer for AllenaMente.
 * Implements 7 distinct Mascot harmonic signatures + UI tones + automatic
 * Night Calm (-3dB + 1-pole low-pass filter) when Obsidian Dark theme is active.
 */
object SoundSynthEngine {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun playTap(enabled: Boolean, nightCalm: Boolean) {
        if (!enabled) return
        playSequence(
            notes = listOf(ToneNote(520.0, 45, 0.22)),
            nightCalm = nightCalm
        )
    }

    fun playCorrect(enabled: Boolean, nightCalm: Boolean) {
        if (!enabled) return
        playSequence(
            notes = listOf(
                ToneNote(587.33, 75, 0.28),
                ToneNote(880.0, 110, 0.30)
            ),
            nightCalm = nightCalm
        )
    }

    fun playError(enabled: Boolean, nightCalm: Boolean) {
        if (!enabled) return
        playSequence(
            notes = listOf(
                ToneNote(230.0, 110, 0.26),
                ToneNote(185.0, 140, 0.22)
            ),
            nightCalm = nightCalm
        )
    }

    fun playMascotSignature(mascot: MascotType, enabled: Boolean, nightCalm: Boolean) {
        if (!enabled) return
        val notes = when (mascot) {
            // Panda Zen: calm pentatonic bell
            MascotType.PANDA -> listOf(
                ToneNote(392.0, 130, 0.28),
                ToneNote(523.25, 140, 0.26),
                ToneNote(659.25, 220, 0.24)
            )
            // Fox: crisp bright arpeggio
            MascotType.FOX -> listOf(
                ToneNote(587.33, 65, 0.30),
                ToneNote(739.99, 65, 0.30),
                ToneNote(880.0, 75, 0.30),
                ToneNote(1174.66, 140, 0.26)
            )
            // Owl: crystalline high chord
            MascotType.OWL -> listOf(
                ToneNote(523.25, 110, 0.26),
                ToneNote(783.99, 120, 0.26),
                ToneNote(1046.50, 210, 0.22)
            )
            // Otter: playful bouncing droplets
            MascotType.OTTER -> listOf(
                ToneNote(440.0, 70, 0.28),
                ToneNote(554.37, 70, 0.28),
                ToneNote(493.88, 70, 0.26),
                ToneNote(659.25, 140, 0.28)
            )
            // Cat: silky gentle chime
            MascotType.CAT -> listOf(
                ToneNote(659.25, 95, 0.25),
                ToneNote(783.99, 105, 0.24),
                ToneNote(987.77, 170, 0.22)
            )
            // Deer: warm forest fifth
            MascotType.DEER -> listOf(
                ToneNote(329.63, 140, 0.30),
                ToneNote(493.88, 160, 0.28),
                ToneNote(659.25, 200, 0.24)
            )
            // Wolf: deep resonant harmonic
            MascotType.WOLF -> listOf(
                ToneNote(261.63, 140, 0.32),
                ToneNote(392.0, 150, 0.30),
                ToneNote(523.25, 220, 0.26)
            )
        }
        playSequence(notes, nightCalm)
    }

    private data class ToneNote(
        val freqHz: Double,
        val durationMs: Int,
        val gain: Double
    )

    private fun playSequence(notes: List<ToneNote>, nightCalm: Boolean) {
        scope.launch {
            try {
                val totalSamples = notes.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
                if (totalSamples <= 0) return@launch
                val pcm = ShortArray(totalSamples)
                val volumeFactor = if (nightCalm) 0.70 else 1.0 // -3dB in Obsidian Night Calm
                val lpAlpha = if (nightCalm) 0.22 else 0.75 // Gentle low-pass smoothing
                var prevSample = 0.0
                var offset = 0

                for (note in notes) {
                    val count = (SAMPLE_RATE * note.durationMs) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val envAttack = (i.toDouble() / (SAMPLE_RATE * 0.008)).coerceAtMost(1.0)
                        val envDecay = exp(-4.5 * i.toDouble() / count.coerceAtLeast(1))
                        val fundamental = sin(2.0 * PI * note.freqHz * t)
                        val overtone = 0.22 * sin(2.0 * PI * note.freqHz * 2.0 * t)
                        val raw = (fundamental + overtone) * note.gain * volumeFactor * envAttack * envDecay
                        val filtered = prevSample + lpAlpha * (raw - prevSample)
                        prevSample = filtered
                        val idx = offset + i
                        if (idx < pcm.size) {
                            pcm[idx] = (filtered * Short.MAX_VALUE).toInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        }
                    }
                    offset += count
                }

                val bufferBytes = pcm.size * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(bufferBytes)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                try {
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    val sleepMs = notes.sumOf { it.durationMs }.toLong() + 80L
                    Thread.sleep(sleepMs)
                } finally {
                    try {
                        track.stop()
                    } catch (_: Exception) {
                    }
                    track.release()
                }
            } catch (_: Exception) {
                // Ignore audio hardware unavailability on headless test devices
            }
        }
    }
}
