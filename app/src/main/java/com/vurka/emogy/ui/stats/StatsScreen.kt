package com.vurka.emogy.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.emogy.R
import com.vurka.emogy.data.LevelCatalog
import com.vurka.emogy.ui.components.AppTopBar
import com.vurka.emogy.ui.theme.RoyalBlue
import com.vurka.emogy.ui.theme.VividRed
import com.vurka.emogy.viewmodel.LevelSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: LevelSelectionViewModel,
    onBackPressed: () -> Unit,
) {
    val starsMap by viewModel.starsMap.collectAsState()
    val totalStars by viewModel.totalStars.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()
    val totalGamesPlayed by viewModel.totalGamesPlayed.collectAsState()
    val perfectGames by viewModel.perfectGames.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Completion is derived from recorded stars, so it stays honest even after a level reset and
    // never treats "all levels selectable" as progress.
    val levelsCompleted by viewModel.totalLevelsCompleted.collectAsState()
    val highestCompleted by viewModel.highestCompletedLevel.collectAsState()
    val lastPlayed by viewModel.lastPlayedLevel.collectAsState()

    // The current world follows real progress; a brand-new player sits in world 1.
    val progressLevel = highestCompleted.coerceIn(1, LevelCatalog.TOTAL_LEVELS)
    val currentWorldIndex = ((progressLevel - 1) / LevelCatalog.LEVELS_PER_WORLD) + 1
    val worldInfo = LevelCatalog.getWorldInfo(progressLevel)
    val worldStart = (currentWorldIndex - 1) * LevelCatalog.LEVELS_PER_WORLD + 1
    val worldEnd = currentWorldIndex * LevelCatalog.LEVELS_PER_WORLD
    val completedInWorld = (worldStart..worldEnd).count { (starsMap[it] ?: 0) > 0 }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.stats_title),
                onBack = onBackPressed
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WorldProgressCard(
                worldIndex = currentWorldIndex,
                worldName = worldInfo.name,
                worldEmoji = worldInfo.emoji,
                completedCount = completedInWorld,
                totalCount = LevelCatalog.LEVELS_PER_WORLD,
                colorA = worldInfo.colorA,
                colorB = worldInfo.colorB,
                isDark = isDark
            )
            StatCard(
                title = stringResource(R.string.stats_levels_available),
                value = stringResource(R.string.stats_levels_available_value, LevelCatalog.TOTAL_LEVELS),
                icon = "🚀", isDark = isDark, accentColor = VividRed
            )
            StatCard(
                title = stringResource(R.string.stats_levels_completed),
                value = "$levelsCompleted",
                icon = "🏆", isDark = isDark, accentColor = RoyalBlue
            )
            StatCard(
                title = stringResource(R.string.stats_total_stars),
                value = stringResource(R.string.stats_total_stars_value, totalStars, LevelCatalog.TOTAL_LEVELS * 3),
                icon = "⭐", isDark = isDark, accentColor = VividRed
            )
            StatCard(
                title = stringResource(R.string.stats_completion_rate),
                value = stringResource(
                    R.string.stats_percent,
                    (levelsCompleted * 100 / LevelCatalog.TOTAL_LEVELS).coerceIn(0, 100)
                ),
                icon = "📈", isDark = isDark, accentColor = RoyalBlue
            )
            StatCard(
                title = stringResource(R.string.stats_last_played),
                value = if (lastPlayed in 1..LevelCatalog.TOTAL_LEVELS) "$lastPlayed" else "—",
                icon = "📍", isDark = isDark, accentColor = VividRed
            )
            StatCard(
                title = stringResource(R.string.stats_daily_streak),
                value = stringResource(R.string.stats_daily_streak_value, dailyStreak),
                icon = "⚡", isDark = isDark, accentColor = VividRed
            )
            StatCard(
                title = stringResource(R.string.stats_games),
                value = "$totalGamesPlayed",
                icon = "🎮", isDark = isDark, accentColor = RoyalBlue
            )
            StatCard(
                title = stringResource(R.string.stats_perfect),
                value = "$perfectGames",
                icon = "✨", isDark = isDark, accentColor = VividRed
            )
            StatCard(
                title = stringResource(R.string.stats_perfect_rate),
                value = stringResource(
                    R.string.stats_percent,
                    if (totalGamesPlayed > 0) (perfectGames * 100 / totalGamesPlayed) else 0
                ),
                icon = "🌟", isDark = isDark, accentColor = RoyalBlue
            )
        }
    }
}

@Composable
fun WorldProgressCard(
    worldIndex: Int,
    worldName: String,
    worldEmoji: String,
    completedCount: Int,
    totalCount: Int,
    colorA: Long,
    colorB: Long,
    isDark: Boolean,
) {
    val cardBg = if (isDark) Color(0xFF1C2541) else Color.White
    val cardBorder = BorderStroke(1.5.dp, Color(colorA).copy(alpha = if (isDark) 0.6f else 0.4f))
    val progress = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = worldEmoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = stringResource(R.string.stats_current_world, worldIndex, LevelCatalog.WORLD_COUNT),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(colorA).copy(alpha = 0.9f)
                        )
                        Text(
                            text = worldName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.stats_world_progress, completedCount, totalCount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(colorB)
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(colorB),
                trackColor = if (isDark) Color(0xFF2A3A6A) else Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: String, isDark: Boolean, accentColor: Color) {
    val cardBg = if (isDark) Color(0xFF1C2541) else Color.White
    val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF2A3A6A)) else BorderStroke(1.dp, Color(0xFFCBD5E1))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = icon, fontSize = 30.sp)
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
