package com.vurka.emogy.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.emogy.ui.navigation.TopLevelDestinations
import com.vurka.emogy.ui.theme.Chrome
import com.vurka.emogy.ui.theme.GoldStar
import com.vurka.emogy.ui.theme.TextMuted

/**
 * One bottom bar for the five top-level destinations. Secondary screens (a game, a store, settings)
 * deliberately hide it and use their own back arrow instead.
 */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    NavigationBar(
        containerColor = Chrome.TopBar,
        tonalElevation = 0.dp
    ) {
        TopLevelDestinations.tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { if (currentRoute != tab.route) onSelect(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag(tab.testTag),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoldStar,
                    selectedTextColor = GoldStar,
                    indicatorColor = Color.White.copy(alpha = 0.12f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}
