package com.vurka.emogy

import com.vurka.emogy.data.EmojiCatalog
import com.vurka.emogy.data.LevelCatalog
import com.vurka.emogy.game.MemoryGameEngine
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The emoji filter is a device-font feature, but the catalogue and pool rules are pure Kotlin, so
 * they can be verified here without Robolectric or an Android font stack.
 */
class EmojiCatalogTest {

    private val missing = setOf("🍕", "🦄")

    @After
    fun tearDown() {
        EmojiCatalog.resetGlyphFilter()
    }

    @Test
    fun unsupportedEmojiAreRemovedFromTheCatalogue() {
        assertTrue(EmojiCatalog.items.any { it.emoji == "🍕" })

        EmojiCatalog.installGlyphFilter { it !in missing }

        assertFalse(EmojiCatalog.items.any { it.emoji == "🍕" })
        assertFalse(EmojiCatalog.items.any { it.emoji == "🦄" })
        assertFalse(EmojiCatalog.getByCategory("Food").any { it.emoji == "🍕" })
        assertTrue(EmojiCatalog.items.isNotEmpty())
    }

    @Test
    fun resettingTheFilterRestoresTheFullCatalogue() {
        val fullSize = EmojiCatalog.items.size
        EmojiCatalog.installGlyphFilter { it !in missing }
        EmojiCatalog.resetGlyphFilter()

        assertEquals(fullSize, EmojiCatalog.items.size)
        assertTrue(EmojiCatalog.items.any { it.emoji == "🍕" })
    }

    @Test
    fun everyLevelDealsAPlayableDeckWithoutUnsupportedEmoji() {
        EmojiCatalog.installGlyphFilter { it !in missing }

        for (level in 1..LevelCatalog.TOTAL_LEVELS) {
            val config = LevelCatalog.getLevel(level)
            val deck = MemoryGameEngine(config, Random(level)).generateDeck()

            assertEquals("level $level deck size", config.pairCount * 2, deck.size)
            assertEquals(
                "level $level distinct pairs",
                config.pairCount,
                deck.map { it.emojiId }.distinct().size,
            )
            deck.groupBy { it.emojiId }.forEach { (emojiId, cards) ->
                assertEquals("level $level pair $emojiId", 2, cards.size)
            }
            assertEquals(
                "level $level card ids",
                (0 until config.pairCount * 2).toList(),
                deck.map { it.id }.sorted(),
            )
            assertTrue(
                "level $level used an unsupported emoji",
                deck.none { it.emoji in missing },
            )
        }
    }

    @Test
    fun largestDeckIsStillSupportedAndDistinct() {
        val config = LevelCatalog.getLevel(LevelCatalog.TOTAL_LEVELS)
        assertEquals(20, config.pairCount)

        EmojiCatalog.installGlyphFilter { it !in missing }
        val deck = MemoryGameEngine(config, Random(7)).generateDeck()

        assertEquals(40, deck.size)
        assertEquals(20, deck.map { it.emojiId }.distinct().size)
        assertTrue(deck.none { it.emoji in missing })
    }

    @Test
    fun tooSmallSupportedPoolFallsBackToTheRawCatalogue() {
        // Simulates a device font that can only draw two emoji. A level must still be dealt rather
        // than crash or render an empty board.
        EmojiCatalog.installGlyphFilter { it == "🐶" || it == "🐱" }
        assertEquals(2, EmojiCatalog.items.size)

        val pool = EmojiCatalog.getPoolForCategories(
            categories = listOf("Animals"),
            requiredCount = 4,
            random = Random(1),
        )

        assertEquals(4, pool.size)
        assertEquals(4, pool.map { it.id }.distinct().size)
    }

    @Test
    fun categoriesAreReadFromTheFilteredPool() {
        EmojiCatalog.installGlyphFilter { it !in missing }

        assertFalse(EmojiCatalog.getAllCategories().isEmpty())
        EmojiCatalog.getAllCategories().forEach { category ->
            assertTrue(EmojiCatalog.getByCategory(category).isNotEmpty())
        }
    }
}
