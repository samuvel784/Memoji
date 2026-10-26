package com.vurka.emogy.data

/**
 * Home-screen display rules that are small enough to verify without Compose.
 *
 * A fresh install stores level 1 as the Continue target, so "last played level is set" is not the
 * same as "this player has ever played". Card flips (or a level other than 1) are the evidence that
 * someone has actually started.
 */
object HomeProgressRules {

    fun hasPlayedBefore(lastPlayedLevel: Int, totalCardFlips: Int): Boolean {
        if (lastPlayedLevel !in 1..LevelCatalog.TOTAL_LEVELS) return false
        return totalCardFlips > 0 || lastPlayedLevel > 1
    }
}
