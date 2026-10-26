package com.vurka.emogy.game

import com.vurka.emogy.model.MemoryCard

/**
 * Rules for the opt-in Assist mode.
 *
 * Assist is deliberately narrower than a hint: it shows exactly one matching pair, never the whole
 * board. It is available on every level (including bosses, which give no hints) so it works as an
 * accessibility aid rather than a difficulty shortcut. Its own cooldown keeps it a nudge instead of
 * a solve button, and it never costs hints, moves, stars or coins.
 */
object AssistRules {
    /** Seconds between two reveals. */
    const val COOLDOWN_SEC = 20

    /** How long the pair stays face up. */
    const val REVEAL_MS = 2_600L

    /**
     * Picks two face-down, unmatched cards that form a pair.
     *
     * Face-up cards are excluded, so Assist can never fight a hint reveal or a half-finished player
     * selection. Returns an empty set when no pair is available (board complete or only one card of
     * every remaining pair is face up).
     */
    fun pickPair(cards: List<MemoryCard>): Set<Int> =
        cards
            .asSequence()
            .filter { !it.isMatched && !it.isFlipped }
            .groupBy { it.emojiId }
            .values
            .firstOrNull { it.size >= 2 }
            ?.take(2)
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()
}
