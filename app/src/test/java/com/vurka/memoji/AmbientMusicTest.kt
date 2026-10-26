package com.vurka.memoji

import com.vurka.memoji.util.AmbientMusic
import com.vurka.memoji.util.MusicGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ambient tracks are generated at runtime, so these tests protect the same things the effect
 * tests do: every group must actually make sound, and the same group must always render the same
 * bytes (otherwise the cached file and the verified output could diverge).
 */
class AmbientMusicTest {

    @Test
    fun everyGroupRendersAudibleAudio() {
        for (group in MusicGroup.entries) {
            val peak = AmbientMusic.renderedPeak(group, seconds = 2.0)
            assertTrue("${group.name} renders silence (peak=$peak)", peak > 1_000)
            val rms = AmbientMusic.renderedRms(group, seconds = 2.0)
            assertTrue("${group.name} is mostly silence (rms=$rms)", rms > 0.02)
        }
    }

    @Test
    fun groupsAreDifferentSoundscapes() {
        val checksums = MusicGroup.entries.map { AmbientMusic.renderedChecksum(it, seconds = 1.0) }
        assertEquals("two world families render identical audio", checksums.size, checksums.toSet().size)
    }

    @Test
    fun renderedLengthMatchesTheRequestedWindow() {
        for (group in MusicGroup.entries) {
            val duration = AmbientMusic.renderedDuration(group, seconds = 2.0)
            assertEquals("${group.name} length", 2.0, duration, 0.001)
        }
    }

    @Test
    fun loopsAreLongEnoughToFeelAmbient() {
        // A loop shorter than a minute becomes noticeably repetitive; the design calls for 60-90s.
        assertTrue(
            "loop is ${AmbientMusic.LOOP_SECONDS}s",
            AmbientMusic.LOOP_SECONDS in 60.0..90.0
        )
    }

    @Test
    fun renderingIsDeterministic() {
        for (group in MusicGroup.entries) {
            val first = AmbientMusic.renderedChecksum(group, seconds = 1.0)
            val second = AmbientMusic.renderedChecksum(group, seconds = 1.0)
            assertEquals("${group.name} is not deterministic", first, second)
        }
    }
}
