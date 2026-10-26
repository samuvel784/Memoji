package com.vurka.emogy.data

/** The stored best result for one level. Zero means "not recorded yet". */
data class LevelBest(
    val stars: Int = 0,
    val moves: Int = 0,
    val timeSec: Int = 0,
)

/** What recording a finished run should change, independent of DataStore. */
data class LevelRewardOutcome(
    val stars: Int,
    val bestMoves: Int,
    val bestTimeSec: Int,
    val isFirstClear: Boolean,
    val coinsAwarded: Int,
    val isPersonalBest: Boolean,
)

/**
 * Reward rules for one finished level:
 *  - stars and personal bests only improve, so a worse replay never erases a record;
 *  - coins are paid once, on the first clear, as a flat bonus plus a star bonus.
 */
object LevelRewardCalculator {
    const val COINS_PER_FIRST_CLEAR = 15
    const val COINS_PER_STAR = 5

    fun calculate(
        previous: LevelBest,
        stars: Int,
        moves: Int,
        timeSec: Int,
    ): LevelRewardOutcome {
        val safeStars = stars.coerceIn(0, 3)
        val safeMoves = moves.coerceAtLeast(0)
        val safeTime = timeSec.coerceAtLeast(0)
        // A 0-star result is not a clear, so it can neither claim the first-clear bonus nor make a
        // later real clear look like a replay.
        val firstClear = safeStars > 0 && previous.stars <= 0
        val personalBest = previous.timeSec <= 0 || safeTime < previous.timeSec

        return LevelRewardOutcome(
            stars = maxOf(previous.stars, safeStars),
            bestMoves = when {
                previous.moves <= 0 -> safeMoves
                safeMoves <= 0 -> previous.moves
                else -> minOf(previous.moves, safeMoves)
            },
            bestTimeSec = if (personalBest) safeTime else previous.timeSec,
            isFirstClear = firstClear,
            coinsAwarded = if (firstClear) {
                COINS_PER_FIRST_CLEAR + COINS_PER_STAR * safeStars
            } else {
                0
            },
            isPersonalBest = personalBest,
        )
    }
}

/** Result of spending coins on one shop item. */
data class PurchaseOutcome(
    val success: Boolean,
    val remainingCoins: Int,
)

/**
 * Wallet rules for the shop. Buying an item that is already owned succeeds without charging
 * again, which makes a double tap or a slow UI update harmless.
 */
object PurchaseCalculator {
    fun buy(coins: Int, cost: Int, alreadyOwned: Boolean): PurchaseOutcome {
        val safeCoins = coins.coerceAtLeast(0)
        val safeCost = cost.coerceAtLeast(0)
        return when {
            alreadyOwned -> PurchaseOutcome(success = true, remainingCoins = safeCoins)
            safeCoins < safeCost -> PurchaseOutcome(success = false, remainingCoins = safeCoins)
            else -> PurchaseOutcome(success = true, remainingCoins = safeCoins - safeCost)
        }
    }
}
