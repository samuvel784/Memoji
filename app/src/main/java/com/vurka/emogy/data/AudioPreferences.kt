package com.vurka.emogy.data

import com.vurka.emogy.util.AudioMix

/**
 * Resolves the persisted audio sliders, including the one-way migration from the old
 * `sound_enabled` boolean. Kept pure so the migration can be tested without DataStore.
 */
object AudioPreferences {

    fun masterVolume(stored: Float?, legacySoundEnabled: Boolean?): Float =
        stored?.coerceIn(0f, 1f)
            ?: if (legacySoundEnabled == false) 0f else AudioMix.DEFAULT_MASTER

    fun volume(stored: Float?, default: Float): Float =
        stored?.coerceIn(0f, 1f) ?: default

    fun resolve(
        storedMaster: Float?,
        storedMusic: Float?,
        storedEffects: Float?,
        legacySoundEnabled: Boolean?,
    ): AudioSettings = AudioSettings(
        master = masterVolume(storedMaster, legacySoundEnabled),
        music = volume(storedMusic, AudioMix.DEFAULT_MUSIC),
        effects = volume(storedEffects, AudioMix.DEFAULT_EFFECTS),
    )
}
