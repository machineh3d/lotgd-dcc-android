package ca.machineh3d.lotgddcc.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight, zero-asset synthesized cyberpunk audio cues for combat hits, crits,
 * loot boxes, achievements, and casino spins.
 */
object SoundFxManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val SAMPLE_RATE = 22050

    fun playHit() {
        playSequence(
            listOf(
                Tone(freqHz = 280.0, durationMs = 55, volume = 0.28),
                Tone(freqHz = 190.0, durationMs = 60, volume = 0.22),
            )
        )
    }

    fun playCrit() {
        playSequence(
            listOf(
                Tone(freqHz = 520.0, durationMs = 55, volume = 0.30),
                Tone(freqHz = 780.0, durationMs = 70, volume = 0.32),
                Tone(freqHz = 1040.0, durationMs = 110, volume = 0.28),
            )
        )
    }

    fun playDamageTaken() {
        playSequence(
            listOf(
                Tone(freqHz = 175.0, durationMs = 80, volume = 0.34),
                Tone(freqHz = 115.0, durationMs = 110, volume = 0.30),
            )
        )
    }

    fun playAchievementOrLoot() {
        playSequence(
            listOf(
                Tone(freqHz = 523.25, durationMs = 80, volume = 0.26), // C5
                Tone(freqHz = 659.25, durationMs = 80, volume = 0.26), // E5
                Tone(freqHz = 783.99, durationMs = 95, volume = 0.28), // G5
                Tone(freqHz = 1046.5, durationMs = 160, volume = 0.30), // C6
            )
        )
    }

    fun playMailPing() {
        playSequence(
            listOf(
                Tone(freqHz = 880.0, durationMs = 65, volume = 0.22),
                Tone(freqHz = 1174.66, durationMs = 110, volume = 0.22),
            )
        )
    }

    private data class Tone(
        val freqHz: Double,
        val durationMs: Int,
        val volume: Double,
    )

    private fun playSequence(tones: List<Tone>) {
        scope.launch {
            runCatching {
                val totalSamples = tones.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for (tone in tones) {
                    val count = (SAMPLE_RATE * tone.durationMs) / 1000
                    for (i in 0 until count) {
                        val env = when {
                            i < 120 -> i / 120.0
                            i > count - 200 -> ((count - i).coerceAtLeast(0)) / 200.0
                            else -> 1.0
                        }
                        val sample = sin(2.0 * PI * tone.freqHz * i / SAMPLE_RATE) * tone.volume * env
                        buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    offset += count
                }

                val byteCount = buffer.size * 2
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
                    .setBufferSizeInBytes(byteCount)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                val sleepMs = tones.sumOf { it.durationMs }.toLong() + 40L
                Thread.sleep(sleepMs)
                track.stop()
                track.release()
            }
        }
    }
}
