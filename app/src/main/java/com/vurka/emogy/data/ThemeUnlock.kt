package com.vurka.emogy.data

/**
 * Rule for cosmetic theme ownership.
 *
 * Kept out of the composable so it can be unit-tested and so nobody re-introduces the bug where
 * "all 1000 levels are selectable" was mistaken for "all themes are unlocked".
 */
object ThemeUnlock {

    /**
     * @param purchased true when the theme was bought with coins.
     * @param completedLevels number of levels the player has actually completed.
     * @param milestoneLevels levels required by the theme (0 = always available).
     */
    fun isUnlocked(purchased: Boolean, completedLevels: Int, milestoneLevels: Int): Boolean =
        purchased || milestoneLevels <= 0 || completedLevels >= milestoneLevels
}
