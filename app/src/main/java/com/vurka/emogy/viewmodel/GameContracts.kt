package com.vurka.emogy.viewmodel

import com.vurka.emogy.data.DailyOutcome
import com.vurka.emogy.data.GameSnapshot
import com.vurka.emogy.data.LevelRecordResult
import kotlinx.coroutines.flow.Flow

/** Wall clock used only for timestamps and snapshot expiry, so tests never depend on the device. */
fun interface GameClock {
    fun nowMillis(): Long
}

object SystemGameClock : GameClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** Every audible/haptic moment the game can produce, independent of the Android engines. */
enum class GameCue {
    FLIP,
    MATCH,
    MISMATCH,
    HINT,
    SHUFFLE,
    VICTORY,
    DEFEAT,
    PAUSE,
    TICK,
    TICK_URGENT,
}

interface GameFeedback {
    fun play(
        cue: GameCue,
        combo: Int = 0,
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
    )
}

/**
 * Settings the game needs while a run is active.
 *
 * [zenMode] is deliberately a cold [Flow]: its first emission is the persisted value. Reading a
 * StateFlow with an artificial `false` initial value is what let a Zen run start with finite hints
 * before DataStore had answered.
 */
interface GameSettings {
    val soundEnabled: Flow<Boolean>
    val hapticsEnabled: Flow<Boolean>
    val zenMode: Flow<Boolean>

    /** Opt-in Assist mode: a heart button reveals one matching pair on request. */
    val assistMode: Flow<Boolean>
}

/** The progress writes a finished run needs. [ProgressRepository] is the production implementation. */
interface GameProgress {
    suspend fun setLastPlayedLevel(level: Int)

    suspend fun recordLevelResult(
        level: Int,
        stars: Int,
        moves: Int,
        timeSec: Int,
    ): LevelRecordResult

    suspend fun recordDailyChallenge(
        day: Long,
        moves: Int,
        timeSec: Int,
    ): DailyOutcome

    suspend fun updateDailyStreak()

    suspend fun updatePlayStats(isPerfect: Boolean)

    suspend fun incrementCardFlips(count: Int = 1, isAnimal: Boolean = false)

    /** Adds one matched emoji to the offline album. Idempotent. */
    suspend fun collectEmoji(emojiId: String)
}

interface GameSnapshotStore {
    suspend fun load(): GameSnapshot?
    suspend fun save(snapshot: GameSnapshot)
    suspend fun clear()
}
