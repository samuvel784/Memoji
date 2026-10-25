package com.vurka.memoji.model

data class MemoryCard(
    val id: Int,
    val emojiId: String,
    val emoji: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)
