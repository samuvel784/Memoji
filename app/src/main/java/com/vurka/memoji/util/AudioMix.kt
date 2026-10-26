package com.vurka.memoji.util

/**
 * Pure audio arithmetic shared by the effect engine, the music engine and the settings UI.
 *
 * Keeping this free of Android types means the interesting parts - gain staging, the combo pitch
 * ladder and the "is this effectively muted?" rule - are unit-testable without a device.
 */
object AudioMix {

    const val DEFAULT_MASTER = 1f
    const val DEFAULT_MUSIC = 0.45f
    const val DEFAULT_EFFECTS = 0.9f

    /** Anything quieter than this is treated as silence, so a muted app never wakes the mixer. */
    const val SILENCE_THRESHOLD = 0.001f

    /** Combo steps are capped at one octave so [android.media.SoundPool] rates stay legal. */
    const val MAX_COMBO_STEPS = 5

    /** Major pentatonic ladder: combo 0 is the base cue, combo 5 is one octave up. */
    private val COMBO_SEMITONES = intArrayOf(0, 2, 4, 7, 9, 12)

    /**
     * Final gain for one channel: master volume multiplied by that channel's own slider.
     * Both inputs are clamped, so a bad preference value can never produce a boost.
     */
    fun gain(master: Float, channel: Float): Float =
        (master.coerceIn(0f, 1f) * channel.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    /** True when the master slider is above the silence threshold. */
    fun isAudible(master: Float): Boolean =
        master.coerceIn(0f, 1f) > SILENCE_THRESHOLD

    /**
     * Playback rate for a match chime at [combo] consecutive matches.
     *
     * The ladder is a major pentatonic scale (0, 2, 4, 7, 9, 12 semitones), so a combo of 5 is a
     * full octave above the first match. Values above 5 stay at the octave cap instead of climbing
     * into an unpleasant, chipmunk-like range.
     */
    fun comboRate(combo: Int): Float {
        val step = combo.coerceIn(0, MAX_COMBO_STEPS)
        val semitones = COMBO_SEMITONES[step]
        return Math.pow(2.0, semitones / 12.0).toFloat()
    }
}
