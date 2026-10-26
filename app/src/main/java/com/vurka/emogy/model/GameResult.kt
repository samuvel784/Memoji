package com.vurka.emogy.model

data class GameResult(
    val levelNumber: Int,
    val won: Boolean,
    val stars: Int,
    val moves: Int,
    val timeElapsedSec: Int,
    val bestMoves: Int,
    val bestTimeSec: Int
)
