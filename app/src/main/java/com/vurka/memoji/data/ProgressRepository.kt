package com.vurka.memoji.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vurka.memoji.util.AudioMix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
        private val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme")
        private val DAILY_STREAK_KEY = intPreferencesKey("daily_streak")
        private val LAST_PLAY_DATE_KEY = longPreferencesKey("last_play_date")
        private val TOTAL_GAMES_PLAYED_KEY = intPreferencesKey("total_games_played")
        private val PERFECT_GAMES_KEY = intPreferencesKey("perfect_games")
        private val COINS_KEY = intPreferencesKey("user_coins")
        private val TOTAL_FLIPS_KEY = intPreferencesKey("total_card_flips")
        private val ANIMAL_FLIPS_KEY = intPreferencesKey("animal_card_flips")

        /** Coins for first clearing a level: a flat bonus plus a small star bonus. */
        const val COINS_PER_FIRST_CLEAR = 15
        const val COINS_PER_STAR = 5

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
        prefs[MASTER_VOLUME_KEY]
            ?: if (prefs[SOUND_ENABLED_KEY] == false) 0f else AudioMix.DEFAULT_MASTER
    }

    val musicVolume: Flow<Float> = dataStore.data.map { prefs ->
        prefs[MUSIC_VOLUME_KEY] ?: AudioMix.DEFAULT_MUSIC
    }

    val effectsVolume: Flow<Float> = dataStore.data.map { prefs ->
        prefs[EFFECTS_VOLUME_KEY] ?: AudioMix.DEFAULT_EFFECTS
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

            val sKey = starsKey(safeLevel)
            val existingStars = prefs[sKey] ?: 0
            val firstClear = existingStars <= 0

            val mKey = bestMovesKey(safeLevel)
            val existingMoves = prefs[mKey] ?: 0

            val tKey = bestTimeKey(safeLevel)
            val existingTime = prefs[tKey] ?: 0
            val personalBest = existingTime == 0 || timeSec < existingTime

            if (safeStars > existingStars) prefs[sKey] = safeStars
            if (existingMoves == 0 || moves < existingMoves) prefs[mKey] = moves
            if (personalBest) prefs[tKey] = timeSec

            val award = if (firstClear) COINS_PER_FIRST_CLEAR + COINS_PER_STAR * safeStars else 0
            if (award > 0) prefs[COINS_KEY] = (prefs[COINS_KEY] ?: 0) + award

            result = LevelRecordResult(
                stars = maxOf(existingStars, safeStars),
                isFirstClear = firstClear,
                coinsAwarded = award,
                isPersonalBest = personalBest,
            )
        }
        return result
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        setMasterVolume(if (enabled) 1f else 0f)
    }

    suspend fun setMasterVolume(volume: Float) {
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

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    suspend fun setSelectedTheme(theme: String) {
        dataStore.edit { it[SELECTED_THEME_KEY] = theme }
    }

    val dailyStreak: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DAILY_STREAK_KEY] ?: 0
    }

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
            val coins = prefs[COINS_KEY] ?: 0
            if (coins >= cost) {
                prefs[COINS_KEY] = coins - cost
                if (itemId.startsWith("theme_")) {
                    val themeId = itemId.removePrefix("theme_")
                    prefs[booleanPreferencesKey("theme_purchased_$themeId")] = true
                }
                success = true
            }
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
            // Migrate the old boolean too, so a muted player stays muted after a reset.
            val master = prefs[MASTER_VOLUME_KEY]
                ?: if (prefs[SOUND_ENABLED_KEY] == false) 0f else AudioMix.DEFAULT_MASTER
            val music = prefs[MUSIC_VOLUME_KEY] ?: AudioMix.DEFAULT_MUSIC
            val effects = prefs[EFFECTS_VOLUME_KEY] ?: AudioMix.DEFAULT_EFFECTS
            val haptics = prefs[HAPTICS_ENABLED_KEY] ?: true
            val theme = prefs[THEME_MODE_KEY] ?: "Default"
            val zen = prefs[IS_ZEN_MODE_KEY] ?: false
            prefs.clear()
            prefs[MASTER_VOLUME_KEY] = master
            prefs[MUSIC_VOLUME_KEY] = music
            prefs[EFFECTS_VOLUME_KEY] = effects
            prefs[HAPTICS_ENABLED_KEY] = haptics
            prefs[THEME_MODE_KEY] = theme
            prefs[IS_ZEN_MODE_KEY] = zen
            prefs[LAST_PLAYED_LEVEL_KEY] = 1
        }
    }
}
