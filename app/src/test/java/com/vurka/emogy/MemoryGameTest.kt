package com.vurka.emogy

import com.vurka.emogy.data.EmojiCatalog
import com.vurka.emogy.data.LevelCatalog
import com.vurka.emogy.game.MemoryGameEngine
import com.vurka.emogy.model.LevelConfig
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryGameTest {

    @Test
    fun testDeckGeneration() {
        val levelConfig = LevelConfig(
            levelNumber = 1,
            pairCount = 4,
            categories = listOf("Animals"),
            previewTimeSec = 3,
            timeLimitSec = 0,
            moveLimit = 0,
            difficultyLabel = "Easy"
        )
        val engine = MemoryGameEngine(levelConfig)
        val deck = engine.generateDeck()

        assertEquals(8, deck.size) // 4 pairs * 2 = 8 cards
        val uniqueIds = deck.map { it.id }.toSet()
        assertEquals(8, uniqueIds.size) // All card IDs unique

        val emojiCounts = deck.groupingBy { it.emojiId }.eachCount()
        for ((_, count) in emojiCounts) {
            assertEquals(2, count) // Exactly 2 cards per emoji
        }
    }

    @Test
    fun testLevelCatalogGeneration() {
        val allLevels = LevelCatalog.getAllLevels()
        assertEquals(LevelCatalog.TOTAL_LEVELS, allLevels.size)
        assertEquals(1000, allLevels.size)

        val level1 = LevelCatalog.getLevel(1)
        assertEquals(1, level1.levelNumber)
        assertTrue(level1.pairCount >= 3)

        val last = LevelCatalog.getLevel(LevelCatalog.TOTAL_LEVELS)
        assertEquals(LevelCatalog.TOTAL_LEVELS, last.levelNumber)
    }

    @Test
    fun testStarRatingBasedOnTimeAndMistakes() {
        val levelConfig = LevelConfig(1, 4, emptyList(), 3, 0, 0, "Easy")
        val engine = MemoryGameEngine(levelConfig)

        // 3 Stars: 0 or 1 mistake and fast time (10 seconds for 4 pairs)
        val stars3 = engine.calculateStars(totalPairs = 4, timeElapsedSec = 10, mistakes = 1, timeLimitSec = 0)
        assertEquals(3, stars3)

        // 2 Stars: Moderate mistakes (3 mistakes <= 4 pairs)
        val stars2 = engine.calculateStars(totalPairs = 4, timeElapsedSec = 15, mistakes = 3, timeLimitSec = 0)
        assertEquals(2, stars2)

        // 1 Star: High mistakes (6 mistakes) or very slow time
        val stars1 = engine.calculateStars(totalPairs = 4, timeElapsedSec = 60, mistakes = 7, timeLimitSec = 0)
        assertEquals(1, stars1)
    }

    @Test
    fun testThemedPoolPrefersCategory() {
        // Nature has enough emojis to satisfy a 4-pair deck.
        val pool = EmojiCatalog.getPoolForCategories(
            categories = listOf("Nature"),
            requiredCount = 4,
            random = Random(42)
        )
        assertTrue(pool.size >= 4)
        assertTrue(pool.take(4).all { it.category == "Nature" })
    }

    @Test
    fun testThemedPoolFillsFromCatalogWhenCategoryTooSmall() {
        // A tiny category cannot satisfy a large deck, so extras must be added.
        val requiredCount = 18
        val pool = EmojiCatalog.getPoolForCategories(
            categories = listOf("Faces"),
            requiredCount = requiredCount,
            random = Random(7)
        )
        assertTrue(pool.size >= requiredCount)
        assertEquals(pool.size, pool.map { it.id }.distinct().size)
    }

    @Test
    fun testThemedDeckGenerationAlwaysProducesPairs() {
        for (level in listOf(1, 55, 120, 260, 480, 700, 999, 1000)) {
            val config = LevelCatalog.getLevel(level)
            val engine = MemoryGameEngine(config, Random(level))
            val deck = engine.generateDeck()
            assertEquals(config.pairCount * 2, deck.size)
            val counts = deck.groupingBy { it.emojiId }.eachCount()
            assertTrue(counts.values.all { it == 2 })
        }
    }

    @Test
    fun testEveryWorldHasCategories() {
        assertTrue(LevelCatalog.getWorldCategories(1).contains("Animals"))
        assertTrue(LevelCatalog.getWorldCategories(500).isNotEmpty())
        assertTrue(LevelCatalog.getWorldCategories(1000).isNotEmpty())
        assertTrue(LevelCatalog.getWorldName(1).isNotBlank())
        assertTrue(LevelCatalog.getWorldName(500).isNotBlank())
    }

    @Test
    fun testEmojiCatalogIsLargeAndUnique() {
        val all = EmojiCatalog.items
        assertTrue(all.size >= 250)
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertTrue(EmojiCatalog.getAllCategories().size >= 11)
    }

    @Test
    fun testEveryLevelIsPlayable() {
        for (config in LevelCatalog.getAllLevels()) {
            assertTrue(config.pairCount in 3..20)
            if (config.moveLimit > 0) assertTrue(config.moveLimit > config.pairCount)
        }
    }

    @Test
    fun testTwentyWorldsOfFiftyLevels() {
        assertEquals(20, LevelCatalog.getWorlds().size)
        assertEquals(LevelCatalog.getWorldInfo(1).name, LevelCatalog.getWorldInfo(50).name)
        assertTrue(LevelCatalog.getWorldInfo(50).name != LevelCatalog.getWorldInfo(51).name)
    }

    @Test
    fun testDifficultyNeverEasesWithinWorldProgression() {
        val regular = LevelCatalog.getAllLevels().filter { !it.isBoss }
        var lastPairs = 0
        var lastMismatchDelay = Int.MAX_VALUE
        for (cfg in regular) {
            assertTrue("pairs must not shrink at ${cfg.levelNumber}", cfg.pairCount >= lastPairs)
            assertTrue("mismatch window must not grow at ${cfg.levelNumber}", cfg.mismatchDelayMs <= lastMismatchDelay)
            lastPairs = cfg.pairCount
            lastMismatchDelay = cfg.mismatchDelayMs
        }
    }

    @Test
    fun testLateLevelsAreHarderThanEarlyLevels() {
        val early = LevelCatalog.getLevel(30)
        val late = LevelCatalog.getLevel(900)
        assertTrue(late.pairCount > early.pairCount)
        assertTrue(late.previewTimeSec <= early.previewTimeSec)
        assertTrue(late.shuffleIntervalSec > 0)
        assertEquals(0, early.shuffleIntervalSec)
        assertTrue(late.autoHideMs > 0)
        assertTrue(late.maxHints in 0..1)
        // seconds available per pair shrink
        val earlyPerPair = early.timeLimitSec.toFloat() / early.pairCount
        val latePerPair = late.timeLimitSec.toFloat() / late.pairCount
        assertTrue(latePerPair < earlyPerPair)
    }

    @Test
    fun testBossLevelsEveryTwentyFive() {
        assertTrue(LevelCatalog.getLevel(25).isBoss)
        assertTrue(LevelCatalog.getLevel(1000).isBoss)
        assertTrue(!LevelCatalog.getLevel(24).isBoss)
        assertEquals(40, LevelCatalog.getAllLevels().count { it.isBoss })
        assertEquals(0, LevelCatalog.getLevel(500).maxHints) // bosses give no hints
    }

    @Test
    fun testShuffleKeepsMatchedCardsAndHidesFaces() {
        val config = LevelCatalog.getLevel(300)
        val engine = MemoryGameEngine(config, Random(3))
        val deck = engine.generateDeck().mapIndexed { i, c ->
            if (i % 4 == 0) c.copy(isMatched = true, isFlipped = true) else c
        }
        val shuffled = engine.shuffleUnmatched(deck)
        assertEquals(deck.map { it.id }.sorted(), shuffled.map { it.id }.sorted())
        for (i in deck.indices) {
            if (deck[i].isMatched) assertEquals(deck[i], shuffled[i])
        }
        assertTrue(shuffled.filter { !it.isMatched }.none { it.isFlipped })
    }
}
