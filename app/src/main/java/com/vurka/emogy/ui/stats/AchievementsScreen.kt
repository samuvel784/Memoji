package com.vurka.emogy.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.vurka.emogy.ui.components.AppTopBar
import com.vurka.emogy.ui.theme.EmeraldGreen
import com.vurka.emogy.ui.theme.GoldStar
import com.vurka.emogy.ui.theme.RoyalBlue
import com.vurka.emogy.util.SoundManager
import com.vurka.emogy.viewmodel.LevelSelectionViewModel

data class RewardTask(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val current: Int,
    val target: Int,
    val coinReward: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: LevelSelectionViewModel,
    onBackPressed: () -> Unit,
) {
    val coins by viewModel.coins.collectAsState()
    val totalLevelsCompleted by viewModel.totalLevelsCompleted.collectAsState()
    val totalCardFlips by viewModel.totalCardFlips.collectAsState()
    val animalCardFlips by viewModel.animalCardFlips.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()
    val totalGamesPlayed by viewModel.totalGamesPlayed.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val levelsCompleted = totalLevelsCompleted

    val tasks = listOf(
        RewardTask("complete_2", "Novice Explorer", "Complete 2 levels", "🌟", levelsCompleted, 2, 50),
        RewardTask("complete_5", "Pathfinder", "Complete 5 levels", "🧭", levelsCompleted, 5, 120),
        RewardTask("complete_10", "Adventurer", "Complete 10 levels", "🗺️", levelsCompleted, 10, 250),
        RewardTask("flip_10", "Card Novice", "Flip 10 cards", "🃏", totalCardFlips, 10, 40),
        RewardTask("flip_50", "Card Master", "Flip 50 cards", "🎴", totalCardFlips, 50, 150),
        RewardTask("animal_10", "Wildlife Tracker", "Flip 10 Animal / Tiger cards", "🐯", animalCardFlips, 10, 200),
        RewardTask("play_3", "Dedicated Player", "Play 3 games", "🎮", totalGamesPlayed, 3, 75),
        RewardTask("streak_3", "On Fire", "Reach a 3-day streak", "🔥", dailyStreak, 3, 250)
    )

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.rewards_title), onBack = onBackPressed) {
                Row(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🪙", fontSize = 16.sp)
                    Text("$coins", color = GoldStar, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1C2541) else Color(0xFFEFF6FF)
                    ),
                    border = BorderStroke(1.5.dp, GoldStar)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.rewards_vault),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.rewards_vault_body),
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            items(tasks) { task ->
                val claimedState = viewModel.isRewardClaimed(task.id).collectAsState(initial = false)
                val isClaimed = claimedState.value
                val isCompleted = task.current >= task.target

                RewardTaskCard(
                    task = task,
                    isCompleted = isCompleted,
                    isClaimed = isClaimed,
                    isDark = isDark,
                    onClaim = {
                        if (isCompleted && !isClaimed) {
                            viewModel.claimReward(task.id, task.coinReward)
                            SoundManager.playAchievement(soundEnabled)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RewardTaskCard(
    task: RewardTask,
    isCompleted: Boolean,
    isClaimed: Boolean,
    isDark: Boolean,
    onClaim: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1C2541) else Color.White
    val cardBorder = if (isClaimed) {
        BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
    } else if (isCompleted) {
        BorderStroke(1.5.dp, GoldStar)
    } else {
        BorderStroke(1.dp, if (isDark) Color(0xFF2A3A6A) else Color(0xFFCBD5E1))
    }

    val progress = (task.current.toFloat() / task.target.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = task.icon, fontSize = 30.sp)
                    Column {
                        Text(
                            text = task.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                when {
                    isClaimed -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.cd_claimed), tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.rewards_claimed), color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    isCompleted -> {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldStar),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                stringResource(R.string.rewards_claim, task.coinReward),
                                color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 12.sp
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.rewards_coin, task.coinReward),
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(R.string.rewards_progress),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(
                            R.string.rewards_progress_value,
                            task.current.coerceAtMost(task.target),
                            task.target
                        ),
                        fontSize = 11.sp,
                        color = if (isCompleted) EmeraldGreen else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isCompleted) EmeraldGreen else RoyalBlue,
                    trackColor = if (isDark) Color(0xFF2A3A6A) else Color(0xFFE2E8F0)
                )
            }
        }
    }
}
