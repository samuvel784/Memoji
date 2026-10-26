package com.vurka.emogy

import com.vurka.emogy.data.GameSnapshot
import com.vurka.emogy.data.GameSnapshotCodec
import com.vurka.emogy.data.RESUME_WINDOW_MILLIS
import com.vurka.emogy.data.SnapshotCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSnapshotTest {

    private fun snapshot() = GameSnapshot(
        level = 42,
        savedAtMillis = 1_700_000_000_000L,
        cards = listOf(
            SnapshotCard(0, "🕷️", "🕷️", isMatched = false),
            SnapshotCard(1, "🕷️", "🕷️", isMatched = false),
            SnapshotCard(2, "👨‍👩‍👧", "👨‍👩‍👧", isMatched = true),
            SnapshotCard(3, "👨‍👩‍👧", "👨‍👩‍👧", isMatched = true),
        ),
        moves = 3,
        mistakes = 1,
        matchedPairs = 1,
        timeElapsedSec = 12,
        timeRemainingSec = 48,
        hintsLeft = 2,
        combo = 1,
        maxCombo = 2,
        shuffleCountdownSec = 17,
        shuffleCount = 4,
        wasPaused = true,
    )

    @Test
    fun roundTripPreservesEveryFieldAndEmojiSequence() {
        val decoded = GameSnapshotCodec.decode(GameSnapshotCodec.encode(snapshot()))
        assertEquals(snapshot(), decoded)
    }

    @Test
    fun nullAndCorruptPayloadsAreRejected() {
        assertNull(GameSnapshotCodec.decode(null))
        assertNull(GameSnapshotCodec.decode(""))
        assertNull(GameSnapshotCodec.decode("not a snapshot"))
        assertNull(GameSnapshotCodec.decode("v99\n42|1\n0|0|0|0|0|0|0|0|0|0|0"))
        assertNull(GameSnapshotCodec.decode("v1\n42|1\n0|0|0|0|0|0|0|0|0|0|0\nbroken"))
    }

    @Test
    fun snapshotExpiresAfterTheResumeWindow() {
        val saved = snapshot()
        assertFalse(saved.isExpired(saved.savedAtMillis + RESUME_WINDOW_MILLIS))
        assertTrue(saved.isExpired(saved.savedAtMillis + RESUME_WINDOW_MILLIS + 1))
        // A clock moved backwards must not destroy an otherwise valid run.
        assertFalse(saved.isExpired(saved.savedAtMillis - 60_000))
    }
}
