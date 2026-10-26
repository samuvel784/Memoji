package com.vurka.emogy

import com.vurka.emogy.data.AudioPreferences
import com.vurka.emogy.util.AudioMix
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPreferencesTest {

    @Test
    fun oldMutedBooleanMigratesToZeroMasterVolume() {
        assertEquals(0f, AudioPreferences.masterVolume(null, legacySoundEnabled = false))
    }

    @Test
    fun oldEnabledBooleanMigratesToTheDefaultMasterVolume() {
        assertEquals(
            AudioMix.DEFAULT_MASTER,
            AudioPreferences.masterVolume(null, legacySoundEnabled = true),
        )
    }

    @Test
    fun brandNewPlayerGetsTheDefaultVolume() {
        assertEquals(AudioMix.DEFAULT_MASTER, AudioPreferences.masterVolume(null, null))
    }

    @Test
    fun aStoredSliderWinsOverTheLegacyBoolean() {
        assertEquals(0.42f, AudioPreferences.masterVolume(0.42f, legacySoundEnabled = false))
    }

    @Test
    fun storedValuesAreClampedToTheValidRange() {
        assertEquals(1f, AudioPreferences.masterVolume(4f, legacySoundEnabled = true))
        assertEquals(0f, AudioPreferences.volume(-3f, AudioMix.DEFAULT_MUSIC))
    }

    @Test
    fun resolveKeepsIndependentMusicAndEffectVolumes() {
        val settings = AudioPreferences.resolve(
            storedMaster = 0.8f,
            storedMusic = 0.3f,
            storedEffects = 0.6f,
            legacySoundEnabled = false,
        )

        assertEquals(0.8f, settings.master)
        assertEquals(0.3f, settings.music)
        assertEquals(0.6f, settings.effects)
    }

    @Test
    fun resolveFallsBackToThePerChannelDefaults() {
        val settings = AudioPreferences.resolve(
            storedMaster = null,
            storedMusic = null,
            storedEffects = null,
            legacySoundEnabled = false,
        )

        assertEquals(0f, settings.master)
        assertEquals(AudioMix.DEFAULT_MUSIC, settings.music)
        assertEquals(AudioMix.DEFAULT_EFFECTS, settings.effects)
    }
}
