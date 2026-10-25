package com.vurka.memoji.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vurka.memoji.data.LevelCatalog
import com.vurka.memoji.data.ProgressRepository
import com.vurka.memoji.data.ThemeRepository
import com.vurka.memoji.model.CardBackTheme
import com.vurka.memoji.model.LevelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LevelSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProgressRepository(application)
    private val themeRepository = ThemeRepository(application)

    val highestUnlockedLevel: StateFlow<Int> = repository.highestUnlockedLevel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )

    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val themeMode: StateFlow<String> = repository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Default"
    )

    val isZenMode: StateFlow<Boolean> = repository.isZenMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setIsZenMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsZenMode(enabled)
        }
    }

    val allLevels: List<LevelConfig> = LevelCatalog.getAllLevels()

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticsEnabled(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    val starsMap: StateFlow<Map<Int, Int>> = repository.levelStars.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val totalStars: StateFlow<Int> = repository.totalStarsEarned.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun getStars(level: Int): Flow<Int> = repository.getLevelStars(level)
    fun getBestMoves(level: Int): Flow<Int> = repository.getLevelBestMoves(level)
    fun getBestTime(level: Int): Flow<Int> = repository.getLevelBestTime(level)

    val themes = listOf(
        CardBackTheme("default", "Classic Blue", "The original design", 1, "🃏"),
        CardBackTheme("red_crimson", "Crimson Flame", "Unlocked at Level 50", 50, "🔥"),
        CardBackTheme("aurora_mint", "Aurora Mint", "Unlocked at Level 100", 100, "🍀"),
        CardBackTheme("royal_gold", "Royal Gold", "Unlocked at Level 200", 200, "👑"),
        CardBackTheme("neon_candy", "Neon Candy", "Unlocked at Level 350", 350, "🍭"),
        CardBackTheme("space_void", "Space Void", "Unlocked at Level 500", 500, "🌌"),
        CardBackTheme("dragon_fire", "Dragon Fire", "Unlocked at Level 800", 800, "🐉")
    )

    val selectedThemeId: StateFlow<String> = themeRepository.selectedThemeId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "default"
    )

    fun setSelectedThemeId(id: String) {
        viewModelScope.launch {
            themeRepository.setSelectedThemeId(id)
        }
    }

    val dailyStreak: StateFlow<Int> = repository.dailyStreak.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalGamesPlayed: StateFlow<Int> = repository.totalGamesPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val perfectGames: StateFlow<Int> = repository.perfectGames.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val coins: StateFlow<Int> = repository.coins.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalCardFlips: StateFlow<Int> = repository.totalCardFlips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val animalCardFlips: StateFlow<Int> = repository.animalCardFlips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun claimReward(rewardId: String, coinAmount: Int) {
        viewModelScope.launch {
            repository.claimReward(rewardId, coinAmount)
        }
    }

    fun isRewardClaimed(rewardId: String): Flow<Boolean> = repository.isRewardClaimed(rewardId)

    val dailyClaimedCount: StateFlow<Int> = repository.dailyClaimedCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val weeklyClaimedCount: StateFlow<Int> = repository.weeklyClaimedCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun isThemePurchased(themeId: String): Flow<Boolean> = repository.isThemePurchased(themeId)

    suspend fun buyItem(itemId: String, cost: Int): Boolean = repository.buyItem(itemId, cost)

    fun updatePlayStats(isPerfect: Boolean) {
        viewModelScope.launch {
            repository.updatePlayStats(isPerfect)
        }
    }

    fun updateDailyStreak() {
        viewModelScope.launch {
            repository.updateDailyStreak()
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetProgress()
        }
    }
}
