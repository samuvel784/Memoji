package com.vurka.emogy

import com.vurka.emogy.game.AssistRules
import com.vurka.emogy.model.MemoryCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistRulesTest {

    private fun card(
        id: Int,
        emojiId: String,
        isFlipped: Boolean = false,
        isMatched: Boolean = false,
    ) = MemoryCard(
        id = id,
        emojiId = emojiId,
        emoji = emojiId,
        isFlipped = isFlipped,
        isMatched = isMatched,
    )

    @Test
    fun picksExactlyTwoFaceDownCardsOfTheSameEmoji() {
        val cards = listOf(
            card(0, "cat"),
            card(1, "dog"),
            card(2, "cat"),
            card(3, "dog"),
        )

        val pair = AssistRules.pickPair(cards)

        assertEquals(2, pair.size)
        val picked = cards.filter { it.id in pair }
        assertEquals(1, picked.map { it.emojiId }.distinct().size)
        assertTrue(picked.none { it.isMatched || it.isFlipped })
    }

    @Test
    fun neverRevealsAMatchedPair() {
        val cards = listOf(
            card(0, "cat", isMatched = true),
            card(1, "cat", isMatched = true),
            card(2, "dog"),
            card(3, "dog"),
        )

        val pair = AssistRules.pickPair(cards)

        assertEquals(setOf(2, 3), pair)
    }

    @Test
    fun skipsAPairThatIsAlreadyPartiallyFaceUp() {
        val cards = listOf(
            card(0, "cat", isFlipped = true),
            card(1, "cat"),
            card(2, "dog"),
            card(3, "dog"),
        )

        val pair = AssistRules.pickPair(cards)

        assertEquals(setOf(2, 3), pair)
    }

    @Test
    fun returnsNothingWhenTheBoardIsComplete() {
        val cards = listOf(
            card(0, "cat", isMatched = true),
            card(1, "cat", isMatched = true),
        )

        assertTrue(AssistRules.pickPair(cards).isEmpty())
    }

    @Test
    fun returnsNothingWhenOnlyOneCardOfEachPairIsAvailable() {
        val cards = listOf(
            card(0, "cat", isFlipped = true),
            card(1, "cat"),
            card(2, "dog", isFlipped = true),
            card(3, "dog"),
        )

        assertTrue(AssistRules.pickPair(cards).isEmpty())
    }
}
