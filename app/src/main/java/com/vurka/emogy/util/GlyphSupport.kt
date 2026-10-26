package com.vurka.emogy.util

import android.graphics.Paint

/** Small seam so the catalogue filter can be unit-tested without an Android font stack. */
fun interface GlyphSupport {
    fun supports(emoji: String): Boolean

    companion object {
        val ALL = GlyphSupport { true }
    }
}

/**
 * Checks whether the device's font stack can draw an emoji. For sequences (variation selectors,
 * ZWJ families) the full string is tried first, then the base codepoint. A missing base glyph is
 * the case that used to render as an empty box.
 */
object AndroidGlyphSupport : GlyphSupport {
    private val paint = Paint()

    override fun supports(emoji: String): Boolean {
        if (emoji.isBlank()) return false
        return runCatching {
            if (paint.hasGlyph(emoji)) return true
            val first = emoji.codePoints().findFirst().orElse(0)
            first != 0 && paint.hasGlyph(String(Character.toChars(first)))
        }.getOrDefault(false)
    }
}
