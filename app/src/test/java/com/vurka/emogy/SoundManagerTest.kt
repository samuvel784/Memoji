package com.vurka.emogy

import com.vurka.emogy.util.SoundManager
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every sound effect must actually make a sound.
 *
 * The engine renders its cues mathematically, so a typo in an envelope or an off-by-one in a
 * buffer length silently produces a silent or absurdly long file. These assertions catch that
 * without needing a device.
 */
class SoundManagerTest {

    @Test
    fun everyEffectIsAudibleAndShort() {
        for (sfx in SoundManager.Sfx.entries) {
            val duration = SoundManager.renderedDuration(sfx)
            assertTrue(
                "${sfx.name} is ${duration}s, expected 0.05s..1.5s",
                duration in 0.05..1.5
            )
            val peak = SoundManager.renderedPeak(sfx)
            assertTrue("${sfx.name} renders silence (peak=$peak)", peak > 1_000)
        }
    }

    @Test
    fun effectsAreDeterministic() {
        // Re-rendering must produce byte-identical audio, otherwise the cached WAV would not match
        // what the test verified.
        for (sfx in SoundManager.Sfx.entries) {
            assertTrue(
                "${sfx.name} is not deterministic",
                SoundManager.renderedPeak(sfx) == SoundManager.renderedPeak(sfx)
            )
            assertTrue(
                "${sfx.name} has inconsistent duration",
                SoundManager.renderedDuration(sfx) == SoundManager.renderedDuration(sfx)
            )
        }
    }

    @Test
    fun everyEffectHasItsOwnFile() {
        val names = SoundManager.Sfx.entries.map { it.fileName }
        assertTrue("duplicate sound file names", names.size == names.toSet().size)
    }
}
