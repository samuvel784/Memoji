package com.vurka.emogy.ui.theme

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True when the player asked the system to remove animations ("Remove animations" in
 * Accessibility settings sets the animator duration scale to 0).
 *
 * Decorative motion (drifting backdrop emoji, confetti, screen shake, popping cards) is skipped
 * when this is set; gameplay feedback that carries meaning still happens instantly.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            !ValueAnimator.areAnimatorsEnabled()
        } else {
            false
        }
    }
}
