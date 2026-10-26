package com.vurka.emogy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vurka.emogy.util.AudioMix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "memory_match_prefs")

/** The three audio sliders, bundled so the UI and the engines read one consistent snapshot. */
data class AudioSettings(
    val master: Float = AudioMix.DEFAULT_MASTER,
    val music: Float = AudioMix.DEFAULT_MUSIC,
    val effects: Float = AudioMix.DEFAULT_EFFECTS,
) {
    val isAudible: Boolean get() = AudioMix.isAudible(master)
}

/** Outcome of recording one finished level, used to drive the victory screen. */
data class LevelRecordResult(
    val stars: Int,
    val isFirstClear: Boolean,
    val coinsAwarded: Int,
    val isPersonalBest: Boolean,
)

class ProgressRepository(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private val LAST_PLAYED_LEVEL_KEY = intPreferencesKey("last_played_level")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val MASTER_VOLUME_KEY = floatPreferencesKey("master_volume")
        private val MUSIC_VOLUME_KEY = floatPreferencesKey("music_volume")
        private val EFFECTS_VOLUME_KEY = floatPreferencesKey("effects_volume")
        private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode") // "Default", "Light", "Dark"
        private val IS_ZEN_MODE_KEY = booleanPreferencesKey("is_zen_mode")
        private val ASSIST_MODE_KEY = booleanPreferencesKey("assist_mode")
        private val WELCOME_SEEN_KEY = booleanPreferencesKey("welcome_seen")
        private val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme")
        private val DAILY_STREAK_KEY = intPreferencesKey("daily_streak")
        private val LAST_PLAY_DATE_KEY = longPreferencesKey("last_play_date")
        private val TOTAL_GAMES_PLAYED_KEY = intPreferencesKey("total_games_played")
        private val PERFECT_GAMES_KEY = intPreferencesKey("perfect_games")
        private val COINS_KEY = intPreferencesKey("user_coins")
        private val TOTAL_FLIPS_KEY = intPreferencesKey("total_card_flips")
        private val ANIMAL_FLIPS_KEY = intPreferencesKey("animal_card_flips")
        private val COLLECTED_EMOJIS_KEY = stringSetPreferencesKey("collected_emojis")
        private val IN_PROGRESS_RUN_KEY = stringPreferencesKey("in_progress_run")
        private val DAILY_COMPLETED_DAY_KEY = longPreferencesKey("daily_completed_day")
        private val DAILY_BEST_TIME_KEY = intPreferencesKey("daily_best_time")
        private val DAILY_BEST_MOVES_KEY = intPreferencesKey("daily_best_moves")
        private val DAILY_COMPLETED_COUNT_KEY = intPreferencesKey("daily_completed_count")

        fun starsKey(level: Int) = intPreferencesKey("stars_$level")
        fun bestMovesKey(level: Int) = intPreferencesKey("best_moves_$level")
        fun bestTimeKey(level: Int) = intPreferencesKey("best_time_$level")
        fun rewardClaimedKey(rewardId: String) = booleanPreferencesKey("reward_claimed_$rewardId")
    }

    /**
     * The level the Continue button should resume from. This is *not* a progress metric: every
     * level 1..1000 is always playable, so availability never derives from this value.
     */
    val lastPlayedLevel: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[LAST_PLAYED_LEVEL_KEY] ?: 1).coerceIn(1, LevelCatalog.TOTAL_LEVELS)
    }

    /**
     * Master volume. Older builds only stored the boolean [SOUND_ENABLED_KEY]; it is migrated here
     * (on -> 1, off -> 0) so nobody loses their mute preference.
     */
    val masterVolume: Flow<Float> = dataStore.data.map { prefs ->
        AudioPreferences.masterVolume(
            stored = prefs[MASTER_VOLUME_KEY],
            legacySoundEnabled = prefs[SOUND_ENABLED_KEY],
        )
    }

    val musicVolume: Flow<Float> = dataStore.data.map { prefs ->
        AudioPreferences.volume(prefs[MUSIC_VOLUME_KEY], AudioMix.DEFAULT_MUSIC)
    }

    val effectsVolume: Flow<Float> = dataStore.data.map { prefs ->
        AudioPreferences.volume(prefs[EFFECTS_VOLUME_KEY], AudioMix.DEFAULT_EFFECTS)
    }

    val audioSettings: Flow<AudioSettings> =
        combine(masterVolume, musicVolume, effectsVolume) { master, music, effects ->
            AudioSettings(master = master, music = music, effects = effects)
        }.distinctUntilChanged()

    /** Derived from the master slider so every existing call site keeps working unchanged. */
    val soundEnabled: Flow<Boolean> = masterVolume
        .map { AudioMix.isAudible(it) }
        .distinctUntilChanged()

    val hapticsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAPTICS_ENABLED_KEY] ?: true
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: "Default"
    }

    val selectedTheme: Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_THEME_KEY] ?: "default"
    }

    fun getLevelStars(level: Int): Flow<Int> = dataStore.data.map { prefs ->
        prefs[starsKey(level)] ?: 0
    }

    fun getLevelBestMoves(level: Int): Flow<Int> = dataStore.data.map { prefs ->
        prefs[bestMovesKey(level)] ?: 0
    }

    fun getLevelBestTime(level: Int): Flow<Int> = dataStore.data.map { prefs ->
        prefs[bestTimeKey(level)] ?: 0
    }

    /** One flow for every level's stars (levelNumber -> stars); avoids one DataStore flow per tile. */
    val levelStars: Flow<Map<Int, Int>> = dataStore.data.map<Preferences, Map<Int, Int>> { prefs ->
        val out = HashMap<Int, Int>()
        for ((key, value) in prefs.asMap()) {
            if ((key.name.startsWith("stars_")) && (value is Int)) {
                key.name.removePrefix("stars_").toIntOrNull()?.let { out[it] = value }
            }
        }
        out
    }.distinctUntilChanged()

    val totalStarsEarned: Flow<Int> = levelStars.map { it.values.sum() }

    /**
     * Levels that have actually been completed (at least one star). This is real progress and is
     * completely independent from level availability: all 1..1000 levels are always selectable.
     */
    val completedLevels: Flow<Set<Int>> = levelStars.map { stars ->
        stars.filterValues { it > 0 }.keys
    }

    val totalLevelsCompleted: Flow<Int> = completedLevels.map { it.size }

    /** Highest level the player has genuinely completed, or 0 for a brand-new player. */
    val highestCompletedLevel: Flow<Int> = completedLevels.map { it.maxOrNull() ?: 0 }

    suspend fun setLastPlayedLevel(level: Int) {
        dataStore.edit { prefs ->
            prefs[LAST_PLAYED_LEVEL_KEY] = level.coerceIn(1, LevelCatalog.TOTAL_LEVELS)
        }
    }

    /** Saves one resumable run. The codec lives in [GameSnapshotCodec] so it can be unit-tested. */
    suspend fun saveGameSnapshot(snapshot: GameSnapshot) {
        dataStore.edit { prefs ->
            prefs[IN_PROGRESS_RUN_KEY] = GameSnapshotCodec.encode(snapshot)
        }
    }

    suspend fun loadGameSnapshot(): GameSnapshot? {
        val raw = dataStore.data.first()[IN_PROGRESS_RUN_KEY]
        return GameSnapshotCodec.decode(raw)
    }

    suspend fun clearGameSnapshot() {
        dataStore.edit { prefs -> prefs.remove(IN_PROGRESS_RUN_KEY) }
    }

    /**
     * Records the outcome of one finished level exactly once per victory.
     *
     * Stars and personal bests only ever improve, so replaying a level with a worse result cannot
     * erase an existing record. Coins are only paid the first time a level is cleared, which keeps
     * the reward loop from being farmed by replaying level 1.
     */
    suspend fun recordLevelResult(
        level: Int,
        stars: Int,
        moves: Int,
        timeSec: Int,
    ): LevelRecordResult {
        val safeLevel = level.coerceIn(1, LevelCatalog.TOTAL_LEVELS)
        val safeStars = stars.coerceIn(0, 3)
        var result = LevelRecordResult(
            stars = safeStars,
            isFirstClear = false,
            coinsAwarded = 0,
            isPersonalBest = false,
        )

        dataStore.edit { prefs ->
            prefs[LAST_PLAYED_LEVEL_KEY] = safeLevel

            val previous = LevelBest(
                stars = prefs[starsKey(safeLevel)] ?: 0,
                moves = prefs[bestMovesKey(safeLevel)] ?: 0,
                timeSec = prefs[bestTimeKey(safeLevel)] ?: 0,
            )
            val outcome = LevelRewardCalculator.calculate(
                previous = previous,
                stars = stars,
                moves = moves,
                timeSec = timeSec,
            )

            prefs[starsKey(safeLevel)] = outcome.stars
            if (outcome.bestMoves > 0) prefs[bestMovesKey(safeLevel)] = outcome.bestMoves
            prefs[bestTimeKey(safeLevel)] = outcome.bestTimeSec
            if (outcome.coinsAwarded > 0) {
                prefs[COINS_KEY] = (prefs[COINS_KEY] ?: 0) + outcome.coinsAwarded
            }

            result = LevelRecordResult(
                stars = outcome.stars,
                isFirstClear = outcome.isFirstClear,
                coinsAwarded = outcome.coinsAwarded,
                isPersonalBest = outcome.isPersonalBest,
            )
        }
        return result
    }

    /**
     * Records one completed daily board. The first completion for a local day pays coins;
     * replays only improve the daily bests. Doing this in one DataStore transaction keeps a
     * double-tap from paying twice.
     */
    suspend fun recordDailyChallenge(day: Long, moves: Int, timeSec: Int): DailyOutcome {
        var outcome = DailyOutcome(
            isFirstCompletion = false,
            coinsAwarded = 0,
            bestTimeSec = 0,
            bestMoves = 0,
            isPersonalBestTime = false,
        )
        dataStore.edit { prefs ->
            val previous = DailyChallengeProgress(
                lastCompletedDay = prefs[DAILY_COMPLETED_DAY_KEY] ?: 0L,
                bestTimeSec = prefs[DAILY_BEST_TIME_KEY] ?: 0,
                bestMoves = prefs[DAILY_BEST_MOVES_KEY] ?: 0,
                completedCount = prefs[DAILY_COMPLETED_COUNT_KEY] ?: 0,
            )
            outcome = DailyChallengeRules.outcome(previous, day, moves, timeSec)

            prefs[DAILY_COMPLETED_DAY_KEY] = maxOf(previous.lastCompletedDay, day)
            prefs[DAILY_BEST_TIME_KEY] = outcome.bestTimeSec
            prefs[DAILY_BEST_MOVES_KEY] = outcome.bestMoves
            if (outcome.isFirstCompletion) {
                prefs[DAILY_COMPLETED_COUNT_KEY] = previous.completedCount + 1
            }
            if (outcome.coinsAwarded > 0) {
                prefs[COINS_KEY] = (prefs[COINS_KEY] ?: 0) + outcome.coinsAwarded
            }
        }
        return outcome
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        setMasterVolume(if (enabled) 1f else 0f)
    }    suspend fun setMasterVolume(volume: Float) {
        dataStore.edit { it[MASTER_VOLUME_KEY] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setMusicVolume(volume: Float) {
        dataStore.edit { it[MUSIC_VOLUME_KEY] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setEffectsVolume(volume: Float) {
        dataStore.edit { it[EFFECTS_VOLUME_KEY] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_ENABLED_KEY] = enabled }
    }

    val isZenMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_ZEN_MODE_KEY] ?: false
    }

    suspend fun setIsZenMode(enabled: Boolean) {
        dataStore.edit { it[IS_ZEN_MODE_KEY] = enabled }
    }

    /**
     * Opt-in help mode. Off by default, so the game keeps its original difficulty for anyone who
     * does not ask for assistance.
     */
    val isAssistMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ASSIST_MODE_KEY] ?: false
    }

    suspend fun setIsAssistMode(enabled: Boolean) {
        dataStore.edit { it[ASSIST_MODE_KEY] = enabled }
    }

    /** Whether the one-time first-run welcome has already been shown and dismissed. */
    val welcomeSeen: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[WELCOME_SEEN_KEY] ?: false
    }

    suspend fun markWelcomeSeen() {
        dataStore.edit { it[WELCOME_SEEN_KEY] = true }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    suspend fun setSelectedTheme(theme: String) {
        dataStore.edit { it[SELECTED_THEME_KEY] = theme }
    }

    val dailyStreak: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DAILY_STREAK_KEY] ?: 0
    }

    /**
     * Today's challenge, plus all-time daily bests. The UI compares [DailyChallengeProgress.lastCompletedDay]
     * with the current local day; the storage layer never needs to know the clock.
     */
    val dailyChallenge: Flow<DailyChallengeProgress> = dataStore.data.map { prefs ->
        DailyChallengeProgress(
            lastCompletedDay = prefs[DAILY_COMPLETED_DAY_KEY] ?: 0L,
            bestTimeSec = prefs[DAILY_BEST_TIME_KEY] ?: 0,
            bestMoves = prefs[DAILY_BEST_MOVES_KEY] ?: 0,
            completedCount = prefs[DAILY_COMPLETED_COUNT_KEY] ?: 0,
        )
    }.distinctUntilChanged()

    val lastPlayDate: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_PLAY_DATE_KEY] ?: 0L
    }

    val totalGamesPlayed: Flow<Int> = dataStore.data.map { prefs ->
        prefs[TOTAL_GAMES_PLAYED_KEY] ?: 0
    }

    val perfectGames: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PERFECT_GAMES_KEY] ?: 0
    }

    val coins: Flow<Int> = dataStore.data.map { prefs ->
        prefs[COINS_KEY] ?: 0
    }

    val totalCardFlips: Flow<Int> = dataStore.data.map { prefs ->
        prefs[TOTAL_FLIPS_KEY] ?: 0
    }

    val animalCardFlips: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ANIMAL_FLIPS_KEY] ?: 0
    }

    /**
     * Emoji the player has matched at least once. Stored as ids (the emoji glyph itself), so the
     * album survives catalogue reordering and never stores a game board.
     */
    val collectedEmojis: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[COLLECTED_EMOJIS_KEY] ?: emptySet()
    }.distinctUntilChanged()

    suspend fun collectEmoji(emojiId: String) {
        if (emojiId.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[COLLECTED_EMOJIS_KEY] ?: emptySet()
            if (emojiId !in current) {
                prefs[COLLECTED_EMOJIS_KEY] = current + emojiId
            }
        }
    }

    fun isRewardClaimed(rewardId: String): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[rewardClaimedKey(rewardId)] ?: false
    }

    suspend fun incrementCardFlips(count: Int = 1, isAnimal: Boolean = false) {
        dataStore.edit { prefs ->
            val currentFlips = prefs[TOTAL_FLIPS_KEY] ?: 0
            prefs[TOTAL_FLIPS_KEY] = currentFlips + count
            if (isAnimal) {
                val currentAnimals = prefs[ANIMAL_FLIPS_KEY] ?: 0
                prefs[ANIMAL_FLIPS_KEY] = currentAnimals + count
            }
        }
    }

    suspend fun claimReward(rewardId: String, coinReward: Int) {
        dataStore.edit { prefs ->
            val key = rewardClaimedKey(rewardId)
            val claimed = prefs[key] ?: false
            if (!claimed) {
                prefs[key] = true
                val currentCoins = prefs[COINS_KEY] ?: 0
                prefs[COINS_KEY] = currentCoins + coinReward

                if (rewardId.startsWith("daily_")) {
                    val count = prefs[intPreferencesKey("daily_tasks_claimed_count")] ?: 0
                    prefs[intPreferencesKey("daily_tasks_claimed_count")] = count + 1
                } else if (rewardId.startsWith("weekly_")) {
                    val count = prefs[intPreferencesKey("weekly_tasks_claimed_count")] ?: 0
                    prefs[intPreferencesKey("weekly_tasks_claimed_count")] = count + 1
                }
            }
        }
    }

    fun isThemePurchased(themeId: String): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("theme_purchased_$themeId")] ?: false
    }

    val dailyClaimedCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey("daily_tasks_claimed_count")] ?: 0
    }

    val weeklyClaimedCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey("weekly_tasks_claimed_count")] ?: 0
    }

    suspend fun buyItem(itemId: String, cost: Int): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val themeKey = if (itemId.startsWith("theme_")) {
                booleanPreferencesKey("theme_purchased_${itemId.removePrefix("theme_")}")
            } else {
                null
            }
            val alreadyOwned = themeKey?.let { prefs[it] == true } ?: false
            val outcome = PurchaseCalculator.buy(
                coins = prefs[COINS_KEY] ?: 0,
                cost = cost,
                alreadyOwned = alreadyOwned,
            )
            if (outcome.success) {
                if (!alreadyOwned) prefs[COINS_KEY] = outcome.remainingCoins
                if (themeKey != null) prefs[themeKey] = true
            }
            success = outcome.success
        }
        return success
    }

    suspend fun updatePlayStats(isPerfect: Boolean) {
        dataStore.edit { prefs ->
            val total = (prefs[TOTAL_GAMES_PLAYED_KEY] ?: 0) + 1
            prefs[TOTAL_GAMES_PLAYED_KEY] = total
            if (isPerfect) {
                prefs[PERFECT_GAMES_KEY] = (prefs[PERFECT_GAMES_KEY] ?: 0) + 1
            }
        }
    }

    suspend fun updateDailyStreak() {
        val now = System.currentTimeMillis()
        dataStore.edit { prefs ->
            val lastDate = prefs[LAST_PLAY_DATE_KEY] ?: 0L
            val currentStreak = prefs[DAILY_STREAK_KEY] ?: 0
            prefs[DAILY_STREAK_KEY] = StreakCalculator.nextStreak(currentStreak, lastDate, now)
            prefs[LAST_PLAY_DATE_KEY] = now
        }
    }

    suspend fun resetProgress() {
        dataStore.edit { prefs ->
            // Preserve the player's audio, haptics and theme preferences. The legacy boolean is
            // migrated here too, so a muted player stays muted after a reset.
            val audio = AudioPreferences.resolve(
                storedMaster = prefs[MASTER_VOLUME_KEY],
                storedMusic = prefs[MUSIC_VOLUME_KEY],
                storedEffects = prefs[EFFECTS_VOLUME_KEY],
                legacySoundEnabled = prefs[SOUND_ENABLED_KEY],
            )
            val haptics = prefs[HAPTICS_ENABLED_KEY] ?: true
            val theme = prefs[THEME_MODE_KEY] ?: "Default"
            val zen = prefs[IS_ZEN_MODE_KEY] ?: false
            val assist = prefs[ASSIST_MODE_KEY] ?: false
            val welcomeSeen = prefs[WELCOME_SEEN_KEY] ?: false
            prefs.clear()
            prefs[MASTER_VOLUME_KEY] = audio.master
            prefs[MUSIC_VOLUME_KEY] = audio.music
            prefs[EFFECTS_VOLUME_KEY] = audio.effects
            prefs[HAPTICS_ENABLED_KEY] = haptics
            prefs[THEME_MODE_KEY] = theme
            prefs[IS_ZEN_MODE_KEY] = zen
            prefs[ASSIST_MODE_KEY] = assist
            if (welcomeSeen) prefs[WELCOME_SEEN_KEY] = true
            // Purchases are wiped with the rest of the progress, so the selection must fall back to
            // the free default instead of leaving a paid theme selected but unowned.
            prefs[LAST_PLAYED_LEVEL_KEY] = 1
        }
    }
}
