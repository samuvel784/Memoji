package com.vurka.memoji.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "memory_match_prefs")

class ProgressRepository(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private val HIGHEST_UNLOCKED_KEY = intPreferencesKey("highest_unlocked_level")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
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

        fun starsKey(level: Int) = intPreferencesKey("stars_$level")
        fun bestMovesKey(level: Int) = intPreferencesKey("best_moves_$level")
        fun bestTimeKey(level: Int) = intPreferencesKey("best_time_$level")
        fun rewardClaimedKey(rewardId: String) = booleanPreferencesKey("reward_claimed_$rewardId")
    }

    val highestUnlockedLevel: Flow<Int> = dataStore.data.map { prefs ->
        prefs[HIGHEST_UNLOCKED_KEY] ?: LevelCatalog.TOTAL_LEVELS
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SOUND_ENABLED_KEY] ?: true
    }

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

    suspend fun unlockNextLevel(completedLevel: Int, nextLevel: Int, stars: Int, moves: Int, timeSec: Int) {
        dataStore.edit { prefs ->
            val currentHighest = prefs[HIGHEST_UNLOCKED_KEY] ?: 1
            if (nextLevel > currentHighest) {
                prefs[HIGHEST_UNLOCKED_KEY] = nextLevel.coerceAtMost(LevelCatalog.TOTAL_LEVELS)
            }

            val sKey = starsKey(completedLevel)
            val existingStars = prefs[sKey] ?: 0
            if (stars > existingStars) {
                prefs[sKey] = stars
            }

            val mKey = bestMovesKey(completedLevel)
            val existingMoves = prefs[mKey] ?: 0
            if (existingMoves == 0 || moves < existingMoves) {
                prefs[mKey] = moves
            }

            val tKey = bestTimeKey(completedLevel)
            val existingTime = prefs[tKey] ?: 0
            if (existingTime == 0 || timeSec < existingTime) {
                prefs[tKey] = timeSec
            }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED_KEY] = enabled }
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
        val currentTime = System.currentTimeMillis()
        dataStore.edit { prefs ->
            val lastDate = prefs[LAST_PLAY_DATE_KEY] ?: 0L
            val currentStreak = prefs[DAILY_STREAK_KEY] ?: 0

            val oneDayMs = 24 * 60 * 60 * 1000L
            val daysDifference = (currentTime - lastDate) / oneDayMs

            val newStreak = when {
                lastDate == 0L -> 1
                daysDifference == 0L -> currentStreak // same day
                daysDifference == 1L -> currentStreak + 1 // consecutive day
                else -> 1 // gap, reset
            }
            prefs[DAILY_STREAK_KEY] = newStreak
            prefs[LAST_PLAY_DATE_KEY] = currentTime
        }
    }

    suspend fun resetProgress() {
        dataStore.edit { prefs ->
            val sound = prefs[SOUND_ENABLED_KEY] ?: true
            val haptics = prefs[HAPTICS_ENABLED_KEY] ?: true
            val theme = prefs[THEME_MODE_KEY] ?: "Default"
            val zen = prefs[IS_ZEN_MODE_KEY] ?: false
            prefs.clear()
            prefs[SOUND_ENABLED_KEY] = sound
            prefs[HAPTICS_ENABLED_KEY] = haptics
            prefs[THEME_MODE_KEY] = theme
            prefs[IS_ZEN_MODE_KEY] = zen
            prefs[HIGHEST_UNLOCKED_KEY] = LevelCatalog.TOTAL_LEVELS
        }
    }
}
