package com.vurka.emogy.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.emogy.R
import com.vurka.emogy.data.DailyChallenge
import com.vurka.emogy.data.HomeProgressRules
import com.vurka.emogy.data.LevelCatalog
import com.vurka.emogy.ui.components.AnimatedBackdrop
import com.vurka.emogy.ui.components.HowToPlayBottomSheet
import com.vurka.emogy.ui.components.WelcomeSheet
import com.vurka.emogy.ui.components.popIn
import com.vurka.emogy.ui.components.pressBounce
import com.vurka.emogy.ui.components.pulse
import com.vurka.emogy.ui.theme.Chrome
import com.vurka.emogy.util.SoundManager
import com.vurka.emogy.viewmodel.LevelSelectionViewModel

private val BackdropParticles = listOf("✦", "✧", "❖", "◈", "◆", "◇", "•", "·", "✦", "✧")

@Composable
fun HomeScreen(
    viewModel: LevelSelectionViewModel,
    onPlayClicked: (Int) -> Unit,
    onWorldMapClicked: () -> Unit,
    onStatsClicked: () -> Unit,
    onStoreClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDailyClicked: () -> Unit,
) {
    val streak by viewModel.dailyStreak.collectAsState()
    val dailyChallenge by viewModel.dailyChallenge.collectAsState()
    val totalStars by viewModel.totalStars.collectAsState()
    val lastPlayed by viewModel.lastPlayedLevel.collectAsState()
    val completedLevels by viewModel.totalLevelsCompleted.collectAsState()
    val totalFlips by viewModel.totalCardFlips.collectAsState()
    val soundOn by viewModel.soundEnabled.collectAsState()
    val welcomeSeen by viewModel.welcomeSeen.collectAsState()
    var showHowToPlay by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(false) }

    if (showWelcome) {
        WelcomeSheet(
            onDismiss = {
                showWelcome = false
                viewModel.markWelcomeSeen()
            },
            onHowToPlay = {
                showWelcome = false
                viewModel.markWelcomeSeen()
                showHowToPlay = true
            }
        )
    }
    if (showHowToPlay) HowToPlayBottomSheet(onDismiss = { showHowToPlay = false })

    // A fresh install stores level 1 as the Continue target, so wait for real evidence of play
    // before showing the first-run sheet or calling the button CONTINUE.
    val progressLoaded = lastPlayed in 1..LevelCatalog.TOTAL_LEVELS
    val hasPlayed = HomeProgressRules.hasPlayedBefore(lastPlayed, totalFlips)
    LaunchedEffect(progressLoaded, welcomeSeen, hasPlayed) {
        if (progressLoaded && welcomeSeen == false && !hasPlayed) {
            showWelcome = true
        }
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bg = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF090D16),
                    Color(0xFF131B2E),
                    Color(0xFF1E1B4B),
                    Color(0xFF0F172A)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFF1F2),
                    Color(0xFFEFF6FF),
                    Color(0xFFF3E8FF),
                    Color(0xFFF0FDFA)
                )
            )
        }
    }

    // 0 means DataStore has not answered yet; fall back to level 1 without pretending it is progress.
    val continueLevel = if (lastPlayed in 1..LevelCatalog.TOTAL_LEVELS) lastPlayed else 1
    val world = remember(continueLevel) { LevelCatalog.getWorldInfo(continueLevel) }
    val worldStart = (world.index - 1) * LevelCatalog.LEVELS_PER_WORLD + 1
    val worldEnd = world.index * LevelCatalog.LEVELS_PER_WORLD
    val progress = (completedLevels.toFloat() / LevelCatalog.TOTAL_LEVELS).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        AnimatedBackdrop(brush = bg, emojis = BackdropParticles, count = 22, alpha = if (isDark) 0.15f else 0.35f)

        Column(Modifier.fillMaxSize()) {
            // ---------- Compact fixed top bar ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallOrb(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.cd_statistics), Color(0xFF3B82F6), "stats_button", isDark) {
                    SoundManager.playTap(soundOn)
                    onStatsClicked()
                }
                Spacer(Modifier.width(8.dp))
                SmallOrb(Icons.Default.ShoppingCart, stringResource(R.string.cd_coin_store), Color(0xFF10B981), "store_button", isDark) {
                    SoundManager.playTap(soundOn)
                    onStoreClicked()
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassInfoChip(Icons.Default.LocalFireDepartment, "$streak", Color(0xFFFF4D6D), isDark)
                    GlassInfoChip(Icons.Default.Star, "$totalStars", Color(0xFFF59E0B), isDark)
                }
                Spacer(Modifier.weight(1f))
                SmallOrb(Icons.Default.Settings, stringResource(R.string.cd_settings), Color(0xFF64748B), "settings_button", isDark) {
                    SoundManager.playTap(soundOn)
                    onSettingsClicked()
                }
            }

            // ---------- Scrollable content: nothing can fall off a small screen ----------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Hero
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                        val glow by rememberInfiniteTransition(label = "glow").animateFloat(
                            0.35f, 0.8f,
                            infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                            label = "g"
                        )
                        Box(
                            Modifier
                                .size(150.dp)
                                .graphicsLayer { alpha = glow; scaleX = 0.9f + glow * 0.16f; scaleY = 0.9f + glow * 0.16f }
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFFF4D6D).copy(alpha = 0.35f),
                                            Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        Box(
                            Modifier
                                .size(126.dp)
                                .popIn(100)
                                .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF8B5CF6))
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.9f))
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFF4D6D).copy(alpha = 0.8f),
                                                Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                                Color(0xFF06B6D4).copy(alpha = 0.4f)
                                            )
                                        )
                                    ),
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo_1792172025849),
                                contentDescription = stringResource(R.string.cd_logo),
                                modifier = Modifier
                                    .size(92.dp)
                                    .pulse(1f, 1.03f, 1800)
                                    .clip(RoundedCornerShape(22.dp))
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFE11D48), Color(0xFF8B5CF6), Color(0xFF2563EB))
                            )
                        ),
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        letterSpacing = 4.sp,
                        modifier = Modifier.popIn(300)
                    )
                    Text(
                        stringResource(R.string.app_subtitle),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .popIn(450)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.95f))
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color(0xFFFF4D6D).copy(alpha = 0.5f))
                                ),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                            Text(
                                stringResource(R.string.home_worlds, LevelCatalog.WORLD_COUNT),
                                color = if (isDark) Color.White else Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(Modifier.size(3.dp).background(Color(0xFF64748B), CircleShape))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                            Text(
                                stringResource(R.string.home_levels, LevelCatalog.TOTAL_LEVELS),
                                color = if (isDark) Color.White else Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ---------- Real progress ----------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.92f))
                        .border(
                            BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = Color(world.colorA), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.home_world_header, world.index, world.name),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.home_level_range, worldStart, worldEnd),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(world.colorA),
                        trackColor = if (isDark) Color(0xFF2A3A6A) else Color(0xFFE2E8F0)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.home_progress, completedLevels, LevelCatalog.TOTAL_LEVELS),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                        Text(
                            stringResource(R.string.home_stars, totalStars),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFDE047) else Color(0xFFB45309)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ---------- Primary action ----------
                PrimaryPlayButton(
                    levelNumber = continueLevel,
                    label = stringResource(if (hasPlayed) R.string.home_continue else R.string.home_play),
                    onClick = {
                        SoundManager.playTap(soundOn)
                        onPlayClicked(continueLevel)
                    }
                )

                Spacer(Modifier.height(10.dp))

                DailyChallengeCard(
                    completed = dailyChallenge.isCompleted(
                        DailyChallenge.epochDay(System.currentTimeMillis())
                    ),
                    streak = streak,
                    isDark = isDark,
                    onClick = {
                        SoundManager.playTap(soundOn)
                        onDailyClicked()
                    }
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressBounce()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.95f))
                        .border(
                            BorderStroke(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.55f)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            SoundManager.playTap(soundOn)
                            onWorldMapClicked()
                        }
                        .testTag("world_map_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.home_choose_level),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.home_all_open, LevelCatalog.TOTAL_LEVELS),
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.offline_badge),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DailyChallengeCard(
    completed: Boolean,
    streak: Int,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val accent = Color(0xFF06B6D4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .pressBounce()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.95f))
            .border(
                BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
                RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .testTag("daily_button")
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📅", fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (completed) stringResource(R.string.home_daily_done) else stringResource(R.string.home_daily),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) Color.White else Color(0xFF1E293B),
                maxLines = 1
            )
            Text(
                if (completed) {
                    stringResource(R.string.home_daily_replay)
                } else {
                    stringResource(
                        R.string.home_daily_meta,
                        DailyChallenge.PAIR_COUNT,
                        DailyChallenge.TIME_LIMIT_SEC,
                        DailyChallenge.REWARD_COINS
                    )
                },
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = 1
            )
        }
        if (streak > 0) {
            Row(
                modifier = Modifier
                    .background(Color(0xFFFF4D6D).copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("🔥", fontSize = 12.sp)
                Text(
                    "$streak",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            if (completed) stringResource(R.string.home_done) else stringResource(R.string.home_play),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = if (completed) Color(0xFF10B981) else accent
        )
    }
}

@Composable
private fun PrimaryPlayButton(
    levelNumber: Int,
    label: String,
    onClick: () -> Unit
) {
    val ring by rememberInfiniteTransition(label = "ring").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "r"
    )
    Box(Modifier.fillMaxWidth().popIn(500), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer { scaleX = 1f + ring * 0.03f; scaleY = 1f + ring * 0.08f; alpha = (1f - ring) * 0.5f }
                .border(
                    2.5.dp,
                    Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFFF7A45))),
                    RoundedCornerShape(20.dp)
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pressBounce()
                .shadow(14.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFE11D48))
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFFF7A45), Color(0xFFF59E0B))))
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .testTag("play_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text(
                    stringResource(R.string.home_level, levelNumber),
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun SmallOrb(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .pressBounce()
            .size(Chrome.TouchTarget)
            .shadow(8.dp, CircleShape, spotColor = accentColor)
            .clip(CircleShape)
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f))
            .border(BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun GlassInfoChip(icon: ImageVector, value: String, iconColor: Color, isDark: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.9f))
            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color(0xFF0F172A))
    }
}
