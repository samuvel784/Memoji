package com.vurka.emogy

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vurka.emogy.ui.components.AppBottomBar
import com.vurka.emogy.ui.navigation.Screen
import com.vurka.emogy.ui.navigation.TopLevelDestinations
import com.vurka.emogy.ui.theme.MemoryMatchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-port-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class AppBottomBarTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun showBar(currentRoute: String?, onSelect: (String) -> Unit = {}) {
        compose.setContent {
            MemoryMatchTheme(themeMode = "Light") {
                AppBottomBar(currentRoute = currentRoute, onSelect = onSelect)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun allFiveTabsAreVisible() {
        showBar(Screen.Home.route)

        TopLevelDestinations.tabs.forEach { tab ->
            compose.onNodeWithTag(tab.testTag).assertIsDisplayed()
        }
    }

    @Test
    fun theCurrentRouteIsMarkedSelected() {
        showBar(Screen.Daily.route)

        compose.onNodeWithTag("nav_daily").assertIsSelected()
        compose.onNodeWithTag("nav_home").assertIsNotSelected()
    }

    @Test
    fun tappingAnotherTabReportsItsRoute() {
        val requested = mutableListOf<String>()
        showBar(Screen.Home.route) { requested += it }

        compose.onNodeWithTag("nav_map").performClick()
        compose.onNodeWithTag("nav_more").performClick()

        assertEquals(listOf(Screen.WorldMap.route, Screen.More.route), requested)
    }

    @Test
    fun tappingTheCurrentTabDoesNotNavigate() {
        val requested = mutableListOf<String>()
        showBar(Screen.Album.route) { requested += it }

        compose.onNodeWithTag("nav_album").performClick()

        assertTrue("the selected tab should be a no-op", requested.isEmpty())
    }
}
