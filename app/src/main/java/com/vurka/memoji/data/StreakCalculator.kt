package com.vurka.memoji.data

import java.util.TimeZone

/**
 * Day-level streak maths.
 *
 * The old implementation divided the raw millisecond difference by 86_400_000, which misses a
 * calendar day whenever a DST shift makes a day 23 or 25 hours long, and treats
 * "yesterday 23:59 -> today 00:01" as a full day in between. We compare *local calendar days*
 * instead, using the zone offset that is in effect at each timestamp.
 */
object StreakCalculator {

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Number of whole days since 1970-01-01 in the given zone. */
    fun localEpochDay(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val offset = timeZone.getOffset(epochMillis).toLong()
        return Math.floorDiv(epochMillis + offset, MILLIS_PER_DAY)
    }

    /**
     * Streak value to store after a play.
     *
     * @param currentStreak the stored streak.
     * @param lastPlayMillis timestamp of the previous counted play, or 0 when there is none.
     * @param nowMillis timestamp of the play being counted.
     */
    fun nextStreak(
        currentStreak: Int,
        lastPlayMillis: Long,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Int {
        if (lastPlayMillis <= 0L) return 1
        val daysApart = localEpochDay(nowMillis, timeZone) - localEpochDay(lastPlayMillis, timeZone)
        return when {
            // Same calendar day: keep the streak (and heal corrupt zero values).
            daysApart <= 0L -> currentStreak.coerceAtLeast(1)
            daysApart == 1L -> currentStreak + 1
            else -> 1
        }
    }
}
