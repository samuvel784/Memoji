package com.vurka.memoji.model

data class LevelConfig(
    val levelNumber: Int,
    val pairCount: Int,
    val categories: List<String>,
    val previewTimeSec: Int,
    val timeLimitSec: Int, // 0 = unlimited
    val moveLimit: Int,   // 0 = unlimited
    val difficultyLabel: String,
    /** Every N seconds the unmatched cards swap places (0 = never). */
    val shuffleIntervalSec: Int = 0,
    /** Hints allowed in this level (-1 = unlimited, 0 = none). */
    val maxHints: Int = -1,
    /** How long a wrong pair stays visible before flipping back. */
    val mismatchDelayMs: Int = 1000,
    /** A lone face-up card flips back by itself after this long (0 = never). */
    val autoHideMs: Int = 0,
    /** Boss levels are noticeably tougher and get special map visuals. */
    val isBoss: Boolean = false
)
