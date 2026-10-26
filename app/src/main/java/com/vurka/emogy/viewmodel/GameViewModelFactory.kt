package com.vurka.emogy.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vurka.emogy.data.ProgressRepository
import com.vurka.emogy.data.ThemeRepository

class GameViewModelFactory(
    application: Application,
    private val levelNumber: Int,
    private val isDaily: Boolean = false,
    private val dailyEpochDay: Long = 0L,
) : ViewModelProvider.Factory {
    private val progress = ProgressRepository(application)
    private val themeRepository = ThemeRepository(application)
    private val feedback = AndroidGameFeedback(application)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(
                levelNumber = levelNumber,
                progress = RepositoryGameProgress(progress),
                settings = RepositoryGameSettings(progress),
                feedback = feedback,
                clock = SystemGameClock,
                themeIdFlow = themeRepository.selectedThemeId,
                snapshots = RepositoryGameSnapshotStore(progress),
                isDaily = isDaily,
                dailyEpochDay = dailyEpochDay,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
