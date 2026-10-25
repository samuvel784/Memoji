package com.vurka.memoji.util

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.concurrent.Executors

/**
 * Lightweight sound engine for the game.
 *
 * Uses a single reused [ToneGenerator] to avoid leaking system resources and
 * plays every tone on a background executor so the UI thread never blocks.
 */
object SoundManager {

    private const val VOLUME = 80

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MemojiSound").apply { isDaemon = true }
    }

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    private fun getToneGenerator(): ToneGenerator? {
        val existing = toneGenerator
        if (existing != null) return existing
        return synchronized(this) {
            toneGenerator ?: try {
                ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME).also { toneGenerator = it }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun tone(toneType: Int, durationMs: Int) {
        executor.execute {
            try {
                getToneGenerator()?.startTone(toneType, durationMs)
            } catch (_: Exception) {
                // Ignore: audio is a nice-to-have, never crash the game.
            }
        }
    }

    /**
     * Plays a short melody: a list of tone/duration pairs separated by [gapMs].
     */
    private fun melody(gapMs: Long, vararg notes: Pair<Int, Int>) {
        executor.execute {
            try {
                val generator = getToneGenerator() ?: return@execute
                notes.forEachIndexed { index, (toneType, durationMs) ->
                    generator.startTone(toneType, durationMs)
                    if (index != notes.lastIndex) {
                        try {
                            Thread.sleep(durationMs + gapMs)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return@execute
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun playFlip(enabled: Boolean) {
        if (!enabled) return
        tone(ToneGenerator.TONE_DTMF_2, 35)
    }

    fun playMatch(enabled: Boolean) {
        if (!enabled) return
        melody(
            15L,
            ToneGenerator.TONE_DTMF_3 to 70,
            ToneGenerator.TONE_DTMF_7 to 110
        )
    }

    fun playMismatch(enabled: Boolean) {
        if (!enabled) return
        melody(
            20L,
            ToneGenerator.TONE_DTMF_2 to 70,
            ToneGenerator.TONE_DTMF_1 to 90
        )
    }

    fun playVictory(enabled: Boolean) {
        if (!enabled) return
        melody(
            20L,
            ToneGenerator.TONE_DTMF_3 to 90,
            ToneGenerator.TONE_DTMF_5 to 90,
            ToneGenerator.TONE_DTMF_8 to 120,
            ToneGenerator.TONE_DTMF_9 to 220
        )
    }

    fun playClick(enabled: Boolean) = playFlip(enabled)

    fun playHint(enabled: Boolean) {
        if (!enabled) return
        melody(
            15L,
            ToneGenerator.TONE_DTMF_4 to 70,
            ToneGenerator.TONE_DTMF_8 to 110
        )
    }

    fun playLevelUp(enabled: Boolean) {
        if (!enabled) return
        melody(
            20L,
            ToneGenerator.TONE_DTMF_5 to 90,
            ToneGenerator.TONE_DTMF_8 to 140
        )
    }

    fun playShuffle(enabled: Boolean) {
        if (!enabled) return
        melody(
            15L,
            ToneGenerator.TONE_DTMF_2 to 50,
            ToneGenerator.TONE_DTMF_4 to 50,
            ToneGenerator.TONE_DTMF_6 to 70
        )
    }

    fun playWhoosh(enabled: Boolean) {
        if (!enabled) return
        tone(ToneGenerator.TONE_DTMF_6, 80)
    }

    fun playError(enabled: Boolean) {
        if (!enabled) return
        melody(
            20L,
            ToneGenerator.TONE_DTMF_8 to 80,
            ToneGenerator.TONE_DTMF_2 to 120
        )
    }

    fun playCoin(enabled: Boolean) {
        if (!enabled) return
        melody(
            15L,
            ToneGenerator.TONE_DTMF_5 to 70,
            ToneGenerator.TONE_DTMF_9 to 150
        )
    }

    /** Call from the Activity's onDestroy to free native audio resources. */
    fun release() {
        synchronized(this) {
            try {
                toneGenerator?.release()
            } catch (_: Exception) {
            }
            toneGenerator = null
        }
    }
}
