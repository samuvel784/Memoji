package com.vurka.emogy

import com.vurka.emogy.data.HomeProgressRules
import com.vurka.emogy.data.LevelCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeProgressTest {

    @Test
    fun freshInstallHasNotPlayedYet() {
        assertFalse(
            "level 1 is only the default Continue target, not evidence of play",
            HomeProgressRules.hasPlayedBefore(lastPlayedLevel = 1, totalCardFlips = 0),
        )
    }

    @Test
    fun loadingStateIsNotTreatedAsPlayed() {
        assertFalse(HomeProgressRules.hasPlayedBefore(lastPlayedLevel = 0, totalCardFlips = 0))
    }

    @Test
    fun flippingACardCountsAsHavingStarted() {
        assertTrue(HomeProgressRules.hasPlayedBefore(lastPlayedLevel = 1, totalCardFlips = 1))
    }

    @Test
    fun choosingAnotherLevelCountsAsHavingStarted() {
        assertTrue(HomeProgressRules.hasPlayedBefore(lastPlayedLevel = 25, totalCardFlips = 0))
    }

    @Test
    fun catalogueBoundsAreRespected() {
        assertFalse(
            HomeProgressRules.hasPlayedBefore(
                lastPlayedLevel = LevelCatalog.TOTAL_LEVELS + 1,
                totalCardFlips = 10,
            )
        )
    }
}
