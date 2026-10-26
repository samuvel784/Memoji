package com.vurka.memoji.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Small, shared design tokens.
 *
 * Screens used to repeat literal values (`16.dp`, `0xFF1C2541`, `RoundedCornerShape(20.dp)`)
 * which drift apart over time. These are the values actually used across the app today - they are
 * not a redesign, just one place to change them.
 */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object Radii {
    val chip = 50.dp
    val small = 14.dp
    val card = 18.dp
    val large = 24.dp
    val hero = 32.dp
}

/** Chrome colours that stay constant in both light and dark themes. */
object Chrome {
    /** Deep navy used by top bars and the in-game HUD. */
    val TopBar = Color(0xFF1C2541)
    val TopBarText = Color(0xFFF8FAFC)
    /** Minimum comfortable touch target. */
    val TouchTarget = 48.dp
}
