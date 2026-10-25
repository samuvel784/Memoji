package com.vurka.memoji.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LevelSelect : Screen("levels")
    object WorldMap : Screen("world_map")
    object Stats : Screen("stats")
    object Achievements : Screen("achievements")
    object Themes : Screen("themes")
    object Store : Screen("store")
    object Game : Screen("game/{levelNumber}") {
        fun createRoute(levelNumber: Int) = "game/$levelNumber"
    }
    object Settings : Screen("settings")
}
