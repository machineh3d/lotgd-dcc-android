package ca.machineh3d.lotgddcc.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural DSP RPG sound effects synthesizer (22.05 kHz 16-bit PCM, zero external assets).
 * Generates distinct sword slashes, shield clangs, air whooshes, demolition booms,
 * arcane spell shimmers, impacts, and Syndicate UI chimes.
 */
object SoundFxManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val SAMPLE_RATE = 22050

    /** Crisp blade slice: high-pass air whoosh + metallic steel transient + impact thud. */
    fun playSwordSlash() {
        playPcm(durationMs = 210) { i, total, rng ->
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / total
            // Swept blade air-slice noise
            val whooshEnv = sin(PI * (progress / 0.55).coerceIn(0.0, 1.0)) * exp(-t * 14.0)
            val noise = (rng.nextDouble() * 2.0 - 1.0)
            // Metallic steel ring (inharmonic partials)
            val steelEnv = exp(-t * 28.0)
            val steel = (sin(2.0 * PI * 1850.0 * t) * 0.45 +
                sin(2.0 * PI * 2790.0 * t) * 0.35 +
                sin(2.0 * PI * 4120.0 * t) * 0.20) * steelEnv
            // Punchy contact thud at 45ms
            val hitT = (t - 0.045).coerceAtLeast(0.0)
            val thudEnv = if (t >= 0.045) exp(-hitT * 32.0) else 0.0
            val thudFreq = 145.0 * exp(-hitT * 12.0) + 55.0
            val thud = sin(2.0 * PI * thudFreq * hitT) * thudEnv

            (noise * whooshEnv * 0.36 + steel * 0.28 + thud * 0.38).coerceIn(-1.0, 1.0)
        }
    }

    /** Heavy critical strike: double-sharp steel ring + deep crunch impact. */
    fun playCritSlash() {
        playPcm(durationMs = 310) { i, total, rng ->
            val t = i.toDouble() / SAMPLE_RATE
            val noise = rng.nextDouble() * 2.0 - 1.0
            val sliceEnv = exp(-t * 12.0)
            val steelEnv = exp(-t * 16.0)
            val steel = (sin(2.0 * PI * 2150.0 * t) * 0.4 +
                sin(2.0 * PI * 3260.0 * t) * 0.35 +
                sin(2.0 * PI * 4680.0 * t) * 0.25) * steelEnv
            val thudFreq = 180.0 * exp(-t * 10.0) + 50.0
            val thud = sin(2.0 * PI * thudFreq * t) * exp(-t * 14.0)
            (noise * sliceEnv * 0.32 + steel * 0.36 + thud * 0.42).coerceIn(-1.0, 1.0)
        }
    }

    /** Metallic shield block / parry clang with inharmonic steel-bell resonance. */
    fun playShieldBlock() {
        playPcm(durationMs = 340) { i, _, rng ->
            val t = i.toDouble() / SAMPLE_RATE
            val clickEnv = exp(-t * 95.0)
            val click = (rng.nextDouble() * 2.0 - 1.0) * clickEnv
            // Classic steel-plate inharmonic clang partials
            val clang = (
                sin(2.0 * PI * 640.0 * t) * exp(-t * 11.0) * 0.34 +
                    sin(2.0 * PI * 1075.0 * t) * exp(-t * 13.0) * 0.30 +
                    sin(2.0 * PI * 1685.0 * t) * exp(-t * 16.0) * 0.24 +
                    sin(2.0 * PI * 2490.0 * t) * exp(-t * 20.0) * 0.18 +
                    sin(2.0 * PI * 3420.0 * t) * exp(-t * 25.0) * 0.12
                )
            (click * 0.40 + clang * 0.65).coerceIn(-1.0, 1.0)
        }
    }

    /** Weapon miss / dodge air whoosh (filtered smooth noise sweep). */
    fun playMissWhoosh() {
        var lp1 = 0.0
        var lp2 = 0.0
        playPcm(durationMs = 210) { i, total, rng ->
            val p = i.toDouble() / total
            val env = sin(PI * p) * sin(PI * p)
            val raw = rng.nextDouble() * 2.0 - 1.0
            // Sweeping low-pass alpha creates a realistic air-whoosh pitch arc
            val alpha = 0.08 + 0.32 * sin(PI * p)
            lp1 += alpha * (raw - lp1)
            lp2 += (alpha * 0.7) * (lp1 - lp2)
            ((lp1 - lp2 * 0.55) * env * 1.45).coerceIn(-1.0, 1.0)
        }
    }

    /** Demolition / Hoblobber / Jug O' Boom / Bomb explosion: deep sub-bass drop + filtered rumble. */
    fun playExplosionBoom() {
        var rumble = 0.0
        playPcm(durationMs = 480) { i, _, rng ->
            val t = i.toDouble() / SAMPLE_RATE
            val raw = rng.nextDouble() * 2.0 - 1.0
            // Heavy low-pass rumble filter
            val alpha = (0.24 * exp(-t * 4.0) + 0.05).coerceIn(0.04, 0.30)
            rumble += alpha * (raw - rumble)
            val rumbleEnv = exp(-t * 5.2)
            // Initial shockwave crack
            val crack = raw * exp(-t * 45.0) * 0.45
            // Sub-bass boom pitch drop (125 Hz -> 30 Hz)
            val subFreq = 95.0 * exp(-t * 9.0) + 30.0
            val sub = sin(2.0 * PI * subFreq * t) * exp(-t * 6.5) * 0.55
            (rumble * rumbleEnv * 1.35 + crack + sub).coerceIn(-1.0, 1.0)
        }
    }

    /** Magic / Arcane / Healing spell cast: crystalline rising harmonic shimmer with vibrato. */
    fun playMagicSpell() {
        playPcm(durationMs = 380) { i, total, rng ->
            val t = i.toDouble() / SAMPLE_RATE
            val p = i.toDouble() / total
            val env = sin(PI * p) * exp(-t * 2.2)
            val vibrato = 1.0 + 0.018 * sin(2.0 * PI * 14.0 * t)
            val baseFreq = (420.0 + 520.0 * p) * vibrato
            val shimmer = (
                sin(2.0 * PI * baseFreq * t) * 0.34 +
                    sin(2.0 * PI * baseFreq * 1.5 * t) * 0.26 +
                    sin(2.0 * PI * baseFreq * 2.0 * t) * 0.22 +
                    sin(2.0 * PI * (baseFreq * 3.01) * t) * 0.14
                )
            val sparkle = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 16.0) * 0.08
            ((shimmer + sparkle) * env * 0.85).coerceIn(-1.0, 1.0)
        }
    }

    /** Enemy hit on Crawler: dull armor/body impact thud + brief crunch. */
    fun playDamageTaken() {
        var lp = 0.0
        playPcm(durationMs = 200) { i, _, rng ->
            val t = i.toDouble() / SAMPLE_RATE
            val raw = rng.nextDouble() * 2.0 - 1.0
            lp += 0.22 * (raw - lp)
            val freq = 120.0 * exp(-t * 14.0) + 42.0
            val thud = sin(2.0 * PI * freq * t) * exp(-t * 15.0) * 0.55
            val crunch = lp * exp(-t * 24.0) * 0.55
            (thud + crunch).coerceIn(-1.0, 1.0)
        }
    }

    /** Hostile Eliminated victory triad. */
    fun playVictoryFanfare() {
        playNotes(
            listOf(
                Note(392.00, 90, 0.25),  // G4
                Note(523.25, 95, 0.27),  // C5
                Note(659.25, 190, 0.30), // E5
            )
        )
    }

    /** System AI Achievement / Sponsor Loot Box celebratory chime. */
    fun playAchievementOrLoot() {
        playNotes(
            listOf(
                Note(523.25, 75, 0.24),  // C5
                Note(659.25, 75, 0.25),  // E5
                Note(783.99, 90, 0.27),  // G5
                Note(1046.50, 180, 0.30), // C6
            )
        )
    }

    /** Crisp Syndicate Comms incoming mail chirp. */
    fun playMailPing() {
        playNotes(
            listOf(
                Note(880.00, 65, 0.22),
                Note(1174.66, 115, 0.22),
            )
        )
    }

    private data class Note(val freqHz: Double, val durationMs: Int, val volume: Double)

    private fun playNotes(notes: List<Note>) {
        scope.launch {
            runCatching {
                val totalSamples = notes.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for (note in notes) {
                    val count = (SAMPLE_RATE * note.durationMs) / 1000
                    for (i in 0 until count) {
                        val env = when {
                            i < 120 -> i / 120.0
                            i > count - 260 -> ((count - i).coerceAtLeast(0)) / 260.0
                            else -> 1.0
                        }
                        val t = i.toDouble() / SAMPLE_RATE
                        // Warm fundamental + soft octave harmonic
                        val sample = (sin(2.0 * PI * note.freqHz * t) * 0.78 +
                            sin(2.0 * PI * note.freqHz * 2.0 * t) * 0.22) * note.volume * env
                        buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
                    }
                    offset += count
                }
                emitBuffer(buffer, notes.sumOf { it.durationMs }.toLong())
            }
        }
    }

    private fun playPcm(
        durationMs: Int,
        generator: (index: Int, totalSamples: Int, rng: Random) -> Double,
    ) {
        scope.launch {
            runCatching {
                val totalSamples = (SAMPLE_RATE * durationMs) / 1000
                if (totalSamples <= 0) return@runCatching
                val rng = Random(System.nanoTime())
                val buffer = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val fadeIn = if (i < 80) i / 80.0 else 1.0
                    val fadeOut = if (i > totalSamples - 160) ((totalSamples - i).coerceAtLeast(0)) / 160.0 else 1.0
                    val s = generator(i, totalSamples, rng) * fadeIn * fadeOut * 0.65
                    buffer[i] = (s * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
                }
                emitBuffer(buffer, durationMs.toLong())
            }
        }
    }

    private fun emitBuffer(buffer: ShortArray, durationMs: Long) {
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
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs + 45L)
        track.stop()
        track.release()
    }
}
