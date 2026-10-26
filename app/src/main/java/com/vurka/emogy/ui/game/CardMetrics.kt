package com.vurka.emogy.ui.game

/**
 * Pure layout maths for the memory board. Cards are square and the grid is fixed-column, so the
 * glyph has to be capped against the card width; otherwise a large accessibility font scale makes
 * emoji spill out of six-column cards on a 360 dp phone.
 */
internal object CardMetrics {

    fun columnsFor(cardCount: Int): Int = when {
        cardCount <= 12 -> 3
        cardCount <= 20 -> 4
        cardCount <= 30 -> 5
        else -> 6
    }

    /** Preferred glyph size in sp before the card-width cap. */
    fun emojiSizeSp(columns: Int): Float = when (columns) {
        3 -> 40f
        4 -> 34f
        5 -> 28f
        else -> 24f
    }

    /**
     * Largest glyph size that stays inside [cardWidthDp]. The result is in sp, so multiplying it
     * by the font scale gives at most ~62% of the card. Returning sp (not Dp) keeps the call site
     * a plain `Text(fontSize = ...)`.
     */
    fun glyphSizeSp(preferredSp: Float, cardWidthDp: Float, fontScale: Float): Float {
        if (cardWidthDp <= 0f) return preferredSp
        val safeScale = fontScale.coerceAtLeast(0.1f)
        val capSp = cardWidthDp * 0.62f / safeScale
        return minOf(preferredSp, capSp)
    }
}
