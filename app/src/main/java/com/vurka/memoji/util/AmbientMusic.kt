package com.vurka.memoji.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic ambient music renderer.
 *
 * Each world family gets a calm 64-second loop built from slow chord pads, a low drone and sparse
 * pentatonic bells. Everything is generated from maths, so the app ships no third-party audio, no
 * OGG encoder is needed, and the same seed always produces byte-identical audio. The loops are
 * rendered once into the cache by [MusicManager] and then played with `MediaPlayer`.
 *
 * The renderer deliberately uses 22.05 kHz mono: plenty for soft pads, half the memory of CD audio,
 * and small enough to render comfortably on a low-end phone.
 */
object AmbientMusic {

    const val SAMPLE_RATE = 22_050
    const val LOOP_SECONDS = 64.0

    private const val CHORD_EVERY_SEC = 8.0
    private const val CHORD_LENGTH_SEC = 7.4
    private const val FADE_IN_SEC = 1.2
    private const val FADE_OUT_SEC = 1.8
    private const val PEAK = 0.55

    internal enum class Wave { SINE, TRIANGLE, BELL }

    private data class Soundscape(
        val rootHz: Double,
        val scale: IntArray,
        val padWave: Wave,
        val bellWave: Wave,
        val chordDegrees: IntArray,
        val melodyOctave: Int,
        val melodySpacingSec: Double,
        val bellGain: Double,
        val droneGain: Double,
        val seed: Int,
    )

    private val soundscapes: Map<MusicGroup, Soundscape> = mapOf(
        // Warm, woody, slow - jungles, meadows and animal worlds.
        MusicGroup.ORGANIC to Soundscape(
            rootHz = 146.83, // D3
            scale = intArrayOf(0, 2, 4, 7, 9),
            padWave = Wave.TRIANGLE,
            bellWave = Wave.BELL,
            chordDegrees = intArrayOf(0, 3, 1, 4),
            melodyOctave = 1,
            melodySpacingSec = 2.1,
            bellGain = 0.075,
            droneGain = 0.10,
            seed = 101,
        ),
        // Brighter bells and a little more bounce - candy towns and carnivals.
        MusicGroup.FESTIVE to Soundscape(
            rootHz = 196.00, // G3
            scale = intArrayOf(0, 2, 4, 7, 9),
            padWave = Wave.BELL,
            bellWave = Wave.BELL,
            chordDegrees = intArrayOf(0, 2, 4, 1),
            melodyOctave = 1,
            melodySpacingSec = 1.7,
            bellGain = 0.065,
            droneGain = 0.07,
            seed = 211,
        ),
        // High, breathy and sparse - sky and cloud worlds.
        MusicGroup.AIRY to Soundscape(
            rootHz = 220.00, // A3
            scale = intArrayOf(0, 2, 4, 7, 9),
            padWave = Wave.SINE,
            bellWave = Wave.SINE,
            chordDegrees = intArrayOf(1, 4, 0, 3),
            melodyOctave = 1,
            melodySpacingSec = 2.5,
            bellGain = 0.058,
            droneGain = 0.055,
            seed = 307,
        ),
        // Minor, deep and slow - space harbours and the finale.
        MusicGroup.COSMIC to Soundscape(
            rootHz = 130.81, // C3
            scale = intArrayOf(0, 3, 5, 7, 10),
            padWave = Wave.SINE,
            bellWave = Wave.TRIANGLE,
            chordDegrees = intArrayOf(0, 2, 0, 3),
            melodyOctave = 1,
            melodySpacingSec = 2.8,
            bellGain = 0.05,
            droneGain = 0.12,
            seed = 401,
        ),
    )

    internal fun renderSamples(group: MusicGroup, seconds: Double = LOOP_SECONDS): ShortArray {
        val safeSeconds = seconds.coerceIn(0.25, LOOP_SECONDS)
        val spec = soundscapes.getValue(group)
        val buffer = FloatArray((safeSeconds * SAMPLE_RATE).toInt())

        addDrone(buffer, spec)
        addChords(buffer, spec)
        addBells(buffer, spec)
        applyLoopFades(buffer)
        return toPcm16(buffer)
    }

    private fun addDrone(buffer: FloatArray, spec: Soundscape) {
        val freq = spec.rootHz * 0.5
        val tremolo = 0.05
        for (i in buffer.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val lfo = 0.72 + 0.28 * sin(2.0 * PI * tremolo * t)
            buffer[i] += (sin(2.0 * PI * freq * t) * spec.droneGain * lfo).toFloat()
        }
    }

    private fun addChords(buffer: FloatArray, spec: Soundscape) {
        var startSec = 0.0
        var chordIndex = 0
        val loopSec = buffer.size.toDouble() / SAMPLE_RATE
        while (startSec < loopSec - 1.0) {
            val rootDegree = spec.chordDegrees[chordIndex % spec.chordDegrees.size]
            // Stacked pentatonic degrees: a warm open chord rather than a dense triad.
            val degrees = intArrayOf(rootDegree, rootDegree + 2, rootDegree + 4)
            degrees.forEachIndexed { voice, degree ->
                val freq = scaleFrequency(spec.rootHz, spec.scale, degree, octave = 0)
                addVoice(
                    buffer = buffer,
                    startSec = startSec,
                    durationSec = CHORD_LENGTH_SEC,
                    freq = freq,
                    amplitude = 0.105 - voice * 0.016,
                    wave = spec.padWave,
                    attackSec = 2.0,
                    decayTauSec = 4.8,
                    releaseSec = 0.9,
                )
            }
            startSec += CHORD_EVERY_SEC
            chordIndex++
        }
    }

    private fun addBells(buffer: FloatArray, spec: Soundscape) {
        val random = Random(spec.seed)
        val loopSec = buffer.size.toDouble() / SAMPLE_RATE
        var time = 2.6
        while (time < loopSec - 1.6) {
            val degree = random.nextInt(0, 7)
            val freq = scaleFrequency(spec.rootHz, spec.scale, degree, spec.melodyOctave)
            val duration = 0.9 + random.nextDouble(0.0, 0.9)
            addVoice(
                buffer = buffer,
                startSec = time,
                durationSec = duration,
                freq = freq,
                amplitude = spec.bellGain * (0.75 + random.nextDouble(0.0, 0.5)),
                wave = spec.bellWave,
                attackSec = 0.012,
                decayTauSec = duration * 0.42,
                releaseSec = 0.25,
            )
            time += spec.melodySpacingSec * (0.8 + random.nextDouble(0.0, 0.7))
        }
    }

    /**
     * Adds one voice with a soft attack, a musical decay and a short release. The release avoids
     * clicks when a note ends inside the loop.
     */
    private fun addVoice(
        buffer: FloatArray,
        startSec: Double,
        durationSec: Double,
        freq: Double,
        amplitude: Double,
        wave: Wave,
        attackSec: Double,
        decayTauSec: Double,
        releaseSec: Double,
    ) {
        val start = (startSec * SAMPLE_RATE).toInt().coerceAtLeast(0)
        val count = (durationSec * SAMPLE_RATE).toInt()
        if (count <= 0) return
        val releaseStart = (durationSec - releaseSec).coerceAtLeast(0.0)

        for (i in 0 until count) {
            val index = start + i
            if (index >= buffer.size) break
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (attackSec <= 0.0) 1.0 else min(1.0, t / attackSec)
            val release = if (t <= releaseStart) 1.0 else ((durationSec - t) / releaseSec).coerceIn(0.0, 1.0)
            val envelope = attack * release * exp(-t / decayTauSec)
            val angle = 2.0 * PI * freq * t
            val sample = when (wave) {
                Wave.SINE -> sin(angle)
                Wave.TRIANGLE -> 2.0 / PI * kotlin.math.asin(sin(angle))
                Wave.BELL -> sin(angle) + 0.28 * sin(2.0 * angle) + 0.10 * sin(3.0 * angle)
            }
            buffer[index] += (sample * amplitude * envelope).toFloat()
        }
    }

    /** Fades the loop in and out so restarting it never clicks. */
    private fun applyLoopFades(buffer: FloatArray) {
        val fadeIn = min(buffer.size, (FADE_IN_SEC * SAMPLE_RATE).toInt())
        val fadeOut = min(buffer.size, (FADE_OUT_SEC * SAMPLE_RATE).toInt())
        for (i in 0 until fadeIn) {
            buffer[i] *= i.toFloat() / fadeIn
        }
        for (i in 0 until fadeOut) {
            buffer[buffer.size - 1 - i] *= i.toFloat() / fadeOut
        }
    }

    private fun scaleFrequency(rootHz: Double, scale: IntArray, degree: Int, octave: Int): Double {
        val size = scale.size
        val wrapped = ((degree % size) + size) % size
        val extraOctaves = Math.floorDiv(degree, size)
        val semitones = scale[wrapped] + 12 * (extraOctaves + octave)
        return rootHz * 2.0.pow(semitones / 12.0)
    }

    private fun toPcm16(buffer: FloatArray): ShortArray {
        var peak = 0f
        for (value in buffer) peak = max(peak, abs(value))
        val gain = if (peak > 0.0001f) (PEAK / peak).toFloat() else 1f

        val out = ShortArray(buffer.size)
        for (i in buffer.indices) {
            val clamped = (buffer[i] * gain).coerceIn(-1f, 1f)
            out[i] = (clamped * Short.MAX_VALUE.toFloat()).toInt().toShort()
        }
        return out
    }

    internal fun renderWav(group: MusicGroup, seconds: Double = LOOP_SECONDS): ByteArray {
        val samples = renderSamples(group, seconds)
        val dataSize = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)                          // PCM chunk size
            putShort(1)                         // PCM format
            putShort(1)                         // mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)             // byte rate
            putShort(2)                         // block align
            putShort(16)                        // bits per sample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            samples.forEach { putShort(it) }
        }
        return header.array() + body.array()
    }

    // ---- Test hooks ------------------------------------------------------------------------

    internal fun renderedDuration(group: MusicGroup, seconds: Double = LOOP_SECONDS): Double =
        renderSamples(group, seconds).size.toDouble() / SAMPLE_RATE

    internal fun renderedPeak(group: MusicGroup, seconds: Double = LOOP_SECONDS): Int =
        renderSamples(group, seconds).maxOfOrNull { abs(it.toInt()) } ?: 0

    /** Root-mean-square level in 0..1; catches a loop that is mostly silence with one click. */
    internal fun renderedRms(group: MusicGroup, seconds: Double = LOOP_SECONDS): Double {
        val samples = renderSamples(group, seconds)
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for (sample in samples) {
            val normalised = sample.toDouble() / Short.MAX_VALUE
            sum += normalised * normalised
        }
        return kotlin.math.sqrt(sum / samples.size)
    }

    internal fun renderedChecksum(group: MusicGroup, seconds: Double = 2.0): Long {
        var hash = 1_125_899_906_842_597L
        for (sample in renderSamples(group, seconds)) {
            hash = 31L * hash + sample
        }
        return hash
    }
}
