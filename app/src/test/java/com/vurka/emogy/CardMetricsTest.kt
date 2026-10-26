package com.vurka.emogy

import com.vurka.emogy.ui.game.CardMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardMetricsTest {

    @Test
    fun smallBoardsUseFewerColumns() {
        assertEquals(3, CardMetrics.columnsFor(8))
        assertEquals(4, CardMetrics.columnsFor(20))
        assertEquals(5, CardMetrics.columnsFor(30))
        assertEquals(6, CardMetrics.columnsFor(40))
    }

    @Test
    fun aFullBoardUsesSixColumnsAtAnyWidth() {
        // A 40-card board must not become 3 columns in landscape: that would make cards gigantic
        // and force scrolling. The column count is a function of the deck, not the viewport.
        assertEquals(6, CardMetrics.columnsFor(40))
    }

    @Test
    fun normalFontKeepsThePreferredGlyphSize() {
        val glyph = CardMetrics.glyphSizeSp(
            preferredSp = 24f,
            cardWidthDp = 52f,
            fontScale = 1f,
        )

        assertEquals(24f, glyph, 0.001f)
    }

    @Test
    fun twoHundredPercentFontIsCappedInsideTheCard() {
        // 360 dp phone, 6 columns, 14 dp side padding, 5 gaps of 8 dp -> ~48.7 dp cards.
        val cardWidth = 48.7f
        val glyphSp = CardMetrics.glyphSizeSp(
            preferredSp = 24f,
            cardWidthDp = cardWidth,
            fontScale = 2f,
        )

        val renderedDp = glyphSp * 2f
        assertTrue("glyph $renderedDp dp must fit a $cardWidth dp card", renderedDp < cardWidth)
        assertTrue(glyphSp < 24f)
    }

    @Test
    fun hugeFontScaleNeverProducesAZeroOrNegativeGlyph() {
        val glyph = CardMetrics.glyphSizeSp(
            preferredSp = 24f,
            cardWidthDp = 48f,
            fontScale = 10f,
        )

        assertTrue(glyph > 0f)
        assertTrue(glyph * 10f < 48f)
    }

    @Test
    fun unmeasuredCardKeepsThePreferredSize() {
        assertEquals(
            24f,
            CardMetrics.glyphSizeSp(preferredSp = 24f, cardWidthDp = 0f, fontScale = 2f),
            0.001f,
        )
    }
}
