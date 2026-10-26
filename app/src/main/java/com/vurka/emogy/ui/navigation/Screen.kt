package com.vurka.emogy.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LevelSelect : Screen("levels")
    object WorldMap : Screen("world_map")
    object Stats : Screen("stats")
    object Achievements : Screen("achievements")
    object Themes : Screen("themes")
    object Store : Screen("store")
    object Daily : Screen("daily")
    object Album : Screen("album")
    object More : Screen("more")
    object DailyGame : Screen("daily_game/{day}") {
        fun createRoute(day: Long) = "daily_game/$day"
    }
    object Game : Screen("game/{levelNumber}") {
        fun createRoute(levelNumber: Int) = "game/$levelNumber"
    }
    object Settings : Screen("settings")
}
