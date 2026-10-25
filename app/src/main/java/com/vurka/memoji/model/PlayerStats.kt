package com.vurka.memoji.model

data class PlayerStats(
    val totalLevelsCompleted: Int = 0,
    val totalStarsEarned: Int = 0,
    val totalMatchesFound: Int = 0,
    val totalMovesMade: Int = 0,
    val selectedTheme: String = "Cosmic Meadow"
)
