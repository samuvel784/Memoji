package com.vurka.emogy

import com.vurka.emogy.data.DailyChallenge
import com.vurka.emogy.data.DailyChallengeProgress
import com.vurka.emogy.data.DailyChallengeRules
import com.vurka.emogy.game.MemoryGameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class DailyChallengeTest {

    @Test
    fun theSameDayProducesTheSameBoard() {
        val day = 20_645L
        val config = DailyChallenge.config
        val first = MemoryGameEngine(config, Random(DailyChallenge.seedFor(day))).generateDeck()
        val second = MemoryGameEngine(config, Random(DailyChallenge.seedFor(day))).generateDeck()

        assertEquals(first.map { it.emojiId }, second.map { it.emojiId })
        assertEquals(DailyChallenge.PAIR_COUNT * 2, first.size)
    }

    @Test
    fun aDifferentDayProducesADifferentBoard() {
        val config = DailyChallenge.config
        val first = MemoryGameEngine(config, Random(DailyChallenge.seedFor(20_645L))).generateDeck()
        val second = MemoryGameEngine(config, Random(DailyChallenge.seedFor(20_646L))).generateDeck()

        assertNotEquals(first.map { it.emojiId }, second.map { it.emojiId })
    }

    @Test
    fun theDailyRulesMatchTheLobbyCopy() {
        assertEquals(8, DailyChallenge.config.pairCount)
        assertEquals(120, DailyChallenge.config.timeLimitSec)
        assertEquals(3, DailyChallenge.config.maxHints)
        assertEquals(0, DailyChallenge.config.moveLimit)
        assertEquals(0, DailyChallenge.config.shuffleIntervalSec)
    }

    @Test
    fun firstCompletionPaysCoinsAndSetsTheBests() {
        val outcome = DailyChallengeRules.outcome(
            previous = DailyChallengeProgress(),
            day = 20_645L,
            moves = 30,
            timeSec = 75,
        )

        assertTrue(outcome.isFirstCompletion)
        assertEquals(DailyChallenge.REWARD_COINS, outcome.coinsAwarded)
        assertEquals(75, outcome.bestTimeSec)
        assertEquals(30, outcome.bestMoves)
        assertTrue(outcome.isPersonalBestTime)
    }

    @Test
    fun replayingTheSameDayImprovesBestsWithoutPayingAgain() {
        val previous = DailyChallengeProgress(
            lastCompletedDay = 20_645L,
            bestTimeSec = 75,
            bestMoves = 30,
            completedCount = 1,
        )

        val faster = DailyChallengeRules.outcome(previous, 20_645L, moves = 24, timeSec = 60)
        assertFalse(faster.isFirstCompletion)
        assertEquals(0, faster.coinsAwarded)
        assertEquals(60, faster.bestTimeSec)
        assertEquals(24, faster.bestMoves)
        assertTrue(faster.isPersonalBestTime)

        val slower = DailyChallengeRules.outcome(
            DailyChallengeProgress(20_645L, faster.bestTimeSec, faster.bestMoves, 1),
            20_645L,
            moves = 40,
            timeSec = 110,
        )
        assertEquals(0, slower.coinsAwarded)
        assertEquals(60, slower.bestTimeSec)
        assertEquals(24, slower.bestMoves)
        assertFalse(slower.isPersonalBestTime)
    }

    @Test
    fun tomorrowIsANewCompletion() {
        val previous = DailyChallengeProgress(lastCompletedDay = 20_645L, completedCount = 1)

        val outcome = DailyChallengeRules.outcome(previous, 20_646L, moves = 20, timeSec = 50)

        assertTrue(outcome.isFirstCompletion)
        assertEquals(DailyChallenge.REWARD_COINS, outcome.coinsAwarded)
    }

    @Test
    fun movingTheClockBackCannotPayTheSameDayTwice() {
        val previous = DailyChallengeProgress(lastCompletedDay = 20_645L, completedCount = 1)

        val outcome = DailyChallengeRules.outcome(previous, 20_644L, moves = 20, timeSec = 50)

        assertFalse(outcome.isFirstCompletion)
        assertEquals(0, outcome.coinsAwarded)
        assertTrue(previous.isCompleted(20_644L))
    }

    @Test
    fun theDayChangesAtLocalMidnight() {
        val utc = TimeZone.getTimeZone("UTC")
        // 2026-10-26T23:59:59Z and 2026-10-27T00:00:00Z.
        val before = DailyChallenge.epochDay(1_793_059_199_000L, utc)
        val after = DailyChallenge.epochDay(1_793_059_200_000L, utc)

        assertEquals(1L, after - before)
    }

    @Test
    fun dateLabelFormatsTheStoredDayWithoutFollowingTheClock() {
        val utc = TimeZone.getTimeZone("UTC")
        val day = DailyChallenge.epochDay(1_793_059_200_000L, utc)

        assertEquals(
            "Tuesday, October 27, 2026",
            DailyChallenge.dateLabel(day, utc, Locale.US),
        )
        assertEquals(
            "Wednesday, October 28, 2026",
            DailyChallenge.dateLabel(day + 1, utc, Locale.US),
        )
    }

    @Test
    fun dateLabelKeepsTheSameCalendarDayAcrossExtremeZones() {
        val utc = TimeZone.getTimeZone("UTC")
        val day = DailyChallenge.epochDay(1_793_059_200_000L, utc)

        assertEquals(
            "Tuesday, October 27, 2026",
            DailyChallenge.dateLabel(day, TimeZone.getTimeZone("GMT-11:00"), Locale.US),
        )
        assertEquals(
            "Tuesday, October 27, 2026",
            DailyChallenge.dateLabel(day, TimeZone.getTimeZone("GMT+14:00"), Locale.US),
        )
    }
}
