package com.vurka.memoji

import com.vurka.memoji.util.AudioMix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMixTest {

    @Test
    fun gainMultipliesMasterAndChannel() {
        assertEquals(1f, AudioMix.gain(1f, 1f), 0.0001f)
        assertEquals(0.25f, AudioMix.gain(0.5f, 0.5f), 0.0001f)
        assertEquals(0f, AudioMix.gain(0f, 0.9f), 0.0001f)
        assertEquals(0f, AudioMix.gain(1f, 0f), 0.0001f)
    }

    @Test
    fun gainClampsOutOfRangeValues() {
        assertEquals(1f, AudioMix.gain(4f, 3f), 0.0001f)
        assertEquals(0f, AudioMix.gain(-2f, 1f), 0.0001f)
    }

    @Test
    fun masterBelowThresholdCountsAsMuted() {
        assertFalse(AudioMix.isAudible(0f))
        assertFalse(AudioMix.isAudible(AudioMix.SILENCE_THRESHOLD))
        assertTrue(AudioMix.isAudible(0.01f))
        assertTrue(AudioMix.isAudible(1f))
    }

    @Test
    fun comboPitchRisesOnEveryStepUntilTheOctaveCap() {
        val rates = (0..AudioMix.MAX_COMBO_STEPS).map { AudioMix.comboRate(it) }
        assertEquals("first match is the base pitch", 1f, rates[0], 0.0001f)
        for (i in 1 until rates.size) {
            assertTrue(
                "combo $i (${rates[i - 1]}) must be higher than combo ${i - 1} (${rates[i]})",
                rates[i] > rates[i - 1]
            )
        }
        // Five steps up the pentatonic ladder lands on the octave.
        assertEquals(2f, AudioMix.comboRate(5), 0.0001f)
    }

    @Test
    fun comboPitchIsCappedInsteadOfClimbingForever() {
        assertEquals(AudioMix.comboRate(5), AudioMix.comboRate(6), 0.0001f)
        assertEquals(AudioMix.comboRate(5), AudioMix.comboRate(500), 0.0001f)
        assertEquals(1f, AudioMix.comboRate(-3), 0.0001f)
    }
}
