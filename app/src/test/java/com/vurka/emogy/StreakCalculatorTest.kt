package com.vurka.emogy

import com.vurka.emogy.data.StreakCalculator
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val berlin: TimeZone = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Berlin"))
            .toInstant()
            .toEpochMilli()

    @Test
    fun firstEverPlayStartsTheStreak() {
        assertEquals(1, StreakCalculator.nextStreak(0, 0L, at(2026, 10, 26, 12), berlin))
    }

    @Test
    fun sameDayKeepsTheStreak() {
        val last = at(2026, 10, 26, 8)
        val now = at(2026, 10, 26, 23, 59)
        assertEquals(4, StreakCalculator.nextStreak(4, last, now, berlin))
    }

    @Test
    fun nextCalendarDayIncrementsEvenWhenLessThan24HoursPassed() {
        val last = at(2026, 10, 25, 23, 30)
        val now = at(2026, 10, 26, 0, 15)
        assertEquals(4, StreakCalculator.nextStreak(3, last, now, berlin))
    }

    @Test
    fun aMissedDayResetsTheStreak() {
        val last = at(2026, 10, 23, 20)
        val now = at(2026, 10, 26, 9)
        assertEquals(1, StreakCalculator.nextStreak(9, last, now, berlin))
    }

    @Test
    fun springForwardDayStillCountsAsOneDay() {
        // 2026-03-29 is the DST switch in Berlin, so that calendar day is only 23 hours long.
        val last = at(2026, 3, 28, 23, 30)
        val now = at(2026, 3, 29, 22, 30)
        assertEquals(2, StreakCalculator.nextStreak(1, last, now, berlin))
    }

    @Test
    fun autumnDayStillCountsAsOneDay() {
        // 2026-10-25 is the fall-back day in Berlin, so that calendar day is 25 hours long.
        val last = at(2026, 10, 24, 23, 30)
        val now = at(2026, 10, 25, 22, 30)
        assertEquals(2, StreakCalculator.nextStreak(1, last, now, berlin))
    }

    @Test
    fun clockMovedBackwardsDoesNotResetTheStreak() {
        val last = at(2026, 10, 26, 12)
        val now = at(2026, 10, 26, 10)
        assertEquals(5, StreakCalculator.nextStreak(5, last, now, berlin))
    }
}
