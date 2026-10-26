package com.vurka.memoji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vurka.memoji.data.LevelCatalog
import com.vurka.memoji.ui.game.GameScreen
import com.vurka.memoji.ui.home.HomeScreen
import com.vurka.memoji.ui.levels.LevelSelectScreen
import com.vurka.memoji.ui.navigation.Screen
import com.vurka.memoji.ui.settings.SettingsScreen
import com.vurka.memoji.ui.stats.AchievementsScreen
import com.vurka.memoji.ui.stats.StatsScreen
import com.vurka.memoji.ui.store.StoreScreen
import com.vurka.memoji.ui.themes.ThemesScreen
import com.vurka.memoji.ui.theme.MemoryMatchTheme
import com.vurka.memoji.util.MusicManager
import com.vurka.memoji.util.SoundManager
import com.vurka.memoji.util.WorldMusic
import com.vurka.memoji.viewmodel.GameViewModel
import com.vurka.memoji.viewmodel.GameViewModelFactory
import com.vurka.memoji.viewmodel.LevelSelectionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Renders the deterministic sound set once, off the main thread.
        SoundManager.init(this)
        MusicManager.init(this)
        setContent {
            val levelSelectionViewModel: LevelSelectionViewModel = viewModel()
            val themeMode by levelSelectionViewModel.themeMode.collectAsState()
            val audioSettings by levelSelectionViewModel.audioSettings.collectAsState()
            val lastPlayedLevel by levelSelectionViewModel.lastPlayedLevel.collectAsState()

            // Push the persisted sliders into both engines only after DataStore has answered.
            // Until then the engines keep their defaults but nothing starts, so a muted player
            // never hears a startup note.
            LaunchedEffect(audioSettings) {
                audioSettings?.let { settings ->
                    SoundManager.setVolumes(settings.master, settings.effects)
                    MusicManager.setVolumes(settings.master, settings.music)
                }
            }

            // Home and most menus inherit the ambience of the level you last played.
            LaunchedEffect(lastPlayedLevel, audioSettings) {
                if (lastPlayedLevel > 0 && audioSettings != null) {
                    MusicManager.setGroup(WorldMusic.groupForLevel(lastPlayedLevel))
                }
            }

            MemoryMatchTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(320),
                            initialOffsetX = { it / 6 }
                        ) + fadeIn(animationSpec = tween(320))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(320),
                            targetOffsetX = { -it / 6 }
                        ) + fadeOut(animationSpec = tween(240))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(320),
                            initialOffsetX = { -it / 6 }
                        ) + fadeIn(animationSpec = tween(320))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(320),
                            targetOffsetX = { it / 6 }
                        ) + fadeOut(animationSpec = tween(240))
                    }
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = levelSelectionViewModel,
                            onPlayClicked = { levelNum ->
                                navController.navigate(Screen.Game.createRoute(levelNum))
                            },
                            onWorldMapClicked = {
                                navController.navigate(Screen.WorldMap.route)
                            },
                            onStatsClicked = {
                                navController.navigate(Screen.Stats.route)
                            },
                            onStoreClicked = {
                                navController.navigate(Screen.Store.route)
                            },
                            onAchievementsClicked = {
                                navController.navigate(Screen.Achievements.route)
                            },
                            onThemesClicked = {
                                navController.navigate(Screen.Themes.route)
                            },
                            onSettingsClicked = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }

                    composable(Screen.Themes.route) {
                        ThemesScreen(
                            viewModel = levelSelectionViewModel,
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Screen.WorldMap.route) {
                        LevelSelectScreen(
                            viewModel = levelSelectionViewModel,
                            onLevelSelected = { levelNum ->
                                navController.navigate(Screen.Game.createRoute(levelNum))
                            },
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Screen.Stats.route) {
                        StatsScreen(
                            viewModel = levelSelectionViewModel,
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Screen.Achievements.route) {
                        AchievementsScreen(
                            viewModel = levelSelectionViewModel,
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Screen.Store.route) {
                        StoreScreen(
                            viewModel = levelSelectionViewModel,
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = Screen.Game.route,
                        arguments = listOf(navArgument("levelNumber") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val levelNum = backStackEntry.arguments?.getInt("levelNumber") ?: 1
                        val gameViewModel: GameViewModel = viewModel(
                            factory = GameViewModelFactory(application, levelNum)
                        )

                        GameScreen(
                            viewModel = gameViewModel,
                            onBackPressed = { navController.popBackStack() },
                            onNextLevel = { next ->
                                // Replace the finished run so "back" never reopens a solved board.
                                navController.navigate(Screen.Game.createRoute(next)) {
                                    popUpTo(Screen.Game.route) { inclusive = true }
                                }
                            },
                            onWorldMap = {
                                // Reuse the existing map entry when there is one, so its scroll
                                // position and selection survive the trip through the level.
                                val popped = navController.popBackStack(Screen.WorldMap.route, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Screen.WorldMap.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                    }
                                }
                            },
                            onHome = {
                                if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                                    navController.navigate(Screen.Home.route)
                                }
                            }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = levelSelectionViewModel,
                            onBackPressed = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SoundManager.setAppForeground(true)
        MusicManager.onEnterForeground()
    }

    override fun onStop() {
        // No cue or loop may leak into the background.
        SoundManager.setAppForeground(false)
        MusicManager.onLeaveForeground()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            MusicManager.release()
        } catch (_: Exception) {
        }
        try {
            SoundManager.release()
        } catch (_: Exception) {
        }
    }
}
