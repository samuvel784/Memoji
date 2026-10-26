package com.vurka.emogy.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.vurka.emogy.R

/** One destination in the persistent bottom bar. */
data class TopLevelTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
)

/**
 * The five destinations that keep the bottom bar visible.
 *
 * Everything else (a board, the daily board, settings, rewards, the store…) is a secondary screen:
 * it hides the bar and offers its own back arrow. Keeping the list in one place means the scaffold
 * and the bar cannot drift apart.
 */
object TopLevelDestinations {

    val tabs: List<TopLevelTab> = listOf(
        TopLevelTab(Screen.Home.route, R.string.nav_home, Icons.Default.Home, "nav_home"),
        TopLevelTab(Screen.WorldMap.route, R.string.nav_map, Icons.Default.Map, "nav_map"),
        TopLevelTab(Screen.Daily.route, R.string.nav_daily, Icons.Default.Today, "nav_daily"),
        TopLevelTab(Screen.Album.route, R.string.nav_album, Icons.Default.PhotoLibrary, "nav_album"),
        TopLevelTab(Screen.More.route, R.string.nav_more, Icons.Default.MoreHoriz, "nav_more"),
    )

    val routes: Set<String> = tabs.map { it.route }.toSet()

    /** True only for the five roots; secondary routes and board routes hide the bar. */
    fun showsBottomBar(route: String?): Boolean = route != null && route in routes

    fun tabFor(route: String?): TopLevelTab? = tabs.firstOrNull { it.route == route }
}
