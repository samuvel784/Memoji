package com.vurka.memoji.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vurka.memoji.data.EmojiCatalog
import com.vurka.memoji.data.LevelCatalog
import com.vurka.memoji.model.LevelConfig
import com.vurka.memoji.model.MemoryCard
import com.vurka.memoji.ui.components.AnimatedBackdrop
import com.vurka.memoji.ui.theme.CardBackVisual
import com.vurka.memoji.ui.theme.CardBackVisuals
import com.vurka.memoji.ui.theme.Chrome
import com.vurka.memoji.ui.theme.EmeraldGreen
import com.vurka.memoji.ui.theme.GoldStar
import com.vurka.memoji.ui.theme.LocalReduceMotion
import com.vurka.memoji.ui.theme.RoyalBlue
import com.vurka.memoji.ui.theme.VividRed
import com.vurka.memoji.util.SoundManager
import com.vurka.memoji.viewmodel.GameViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val HudSurface = Chrome.TopBar

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackPressed: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onWorldMap: () -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()
    val zen by viewModel.isZenMode.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val levelNumber = viewModel.levelNumber
    val config = viewModel.levelConfig
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val reduceMotion = LocalReduceMotion.current

    // The clock must never run while the app is in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pauseIfRunning() }
    // Back opens the pause menu instead of silently losing the run.
    BackHandler(enabled = !state.isPaused && !state.isGameOver) { viewModel.togglePause() }

    // Screen shake whenever a new mistake happens (applied in the draw phase only).
    val shake = remember { Animatable(0f) }
    var lastMistakes by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.mistakes) {
        if (state.mistakes > lastMistakes && !reduceMotion) {
            val frames = listOf(16f, -16f, 11f, -11f, 6f, -6f, 0f)
            frames.forEach { target -> shake.animateTo(target, animationSpec = tween(durationMillis = 45)) }
        }
        if (reduceMotion) shake.snapTo(0f)
        lastMistakes = state.mistakes
    }

    val world = remember(levelNumber) { LevelCatalog.getWorldInfo(levelNumber) }
    val worldEmojis = remember(levelNumber) {
        EmojiCatalog.items
            .asSequence()
            .filter { it.category in world.categories.ifEmpty { EmojiCatalog.getAllCategories() } }
            .map { it.emoji }
            .toList()
            .shuffled(kotlin.random.Random(levelNumber))
            .take(8)
    }
    val baseBg = MaterialTheme.colorScheme.background
    val backdropBrush = remember(world, baseBg) {
        Brush.verticalGradient(
            listOf(
                Color(world.colorB).copy(alpha = 0.55f).compositeOver(baseBg),
                Color(world.colorA).copy(alpha = 0.20f).compositeOver(baseBg),
                baseBg
            )
        )
    }
    val backStyle = remember(themeId, isDark) { CardBackVisuals.forTheme(themeId, isDark) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackdrop(brush = backdropBrush, emojis = worldEmojis, count = 12, alpha = 0.14f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            GameTopBar(
                levelNumber = levelNumber,
                worldEmoji = world.emoji,
                worldName = viewModel.worldName,
                isBoss = config.isBoss,
                hintsLeft = if (zen) -1 else state.hintsLeft,
                hintCooldown = state.hintCooldownRemainingSec,
                hintEnabled = (!state.isHintOnCooldown) && (!state.isPreviewing) && (zen || (state.hintsLeft != 0)),
                onBack = { viewModel.togglePause() },
                onHint = viewModel::useHint,
                onPause = viewModel::togglePause,
                onRestart = viewModel::startNewGame
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HudCard(
                    moves = state.moves,
                    moveLimit = if (zen) 0 else config.moveLimit,
                    mistakes = state.mistakes,
                    matched = state.matchedPairsCount,
                    total = state.totalPairs,
                    timeElapsed = state.timeElapsedSec,
                    timeRemaining = state.timeRemainingSec,
                    timeLimit = if (zen) 0 else config.timeLimitSec,
                    isDark = isDark
                )

                Spacer(Modifier.height(8.dp))
                ProgressAndTime(
                    matched = state.matchedPairsCount,
                    total = state.totalPairs,
                    timeRemaining = state.timeRemainingSec,
                    timeLimit = if (zen) 0 else config.timeLimitSec,
                    isDark = isDark
                )

                Spacer(Modifier.height(8.dp))
                StatusRow(
                    config = config,
                    zen = zen,
                    isPreviewing = state.isPreviewing,
                    combo = state.combo,
                    shuffleCountdown = state.shuffleCountdownSec
                )

                Spacer(Modifier.height(8.dp))
                CardGrid(
                    cards = state.cards,
                    backStyle = backStyle,
                    isDark = isDark,
                    shakeProvider = { shake.value },
                    onCardClick = viewModel::onCardClicked,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating feedback
        Box(Modifier.fillMaxSize()) {
            ComboPopup(combo = state.combo)
            ShufflePopup(shuffleCount = state.shuffleCount)
        }

        if (state.isGameOver && state.isWon) {
            ConfettiCelebration(modifier = Modifier.fillMaxSize())

            VictoryDialog(
                levelNumber = levelNumber,
                worldName = viewModel.worldName,
                isBoss = config.isBoss,
                stars = state.stars,
                moves = state.moves,
                mistakes = state.mistakes,
                timeElapsedSec = state.timeElapsedSec,
                maxCombo = state.maxCombo,
                coinsAwarded = state.coinsAwarded,
                isPersonalBest = state.isPersonalBest,
                soundEnabled = soundEnabled,
                onNext = { onNextLevel((levelNumber + 1).coerceAtMost(LevelCatalog.TOTAL_LEVELS)) },
                onReplay = viewModel::startNewGame,
                onWorldMap = onWorldMap,
                onHome = onHome,
            )
        }

        if (state.isGameOver && !state.isWon) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        if (state.lossReason == "moves") "👟 Out of Moves!" else "⏳ Time's Up!",
                        fontWeight = FontWeight.Bold, fontSize = 22.sp, color = VividRed
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (state.lossReason == "moves") {
                                "You used all ${config.moveLimit} moves with ${state.matchedPairsCount} of ${state.totalPairs} pairs found."
                            } else {
                                "Matched ${state.matchedPairsCount} of ${state.totalPairs} pairs before the clock ran out."
                            }
                        )
                        Text(
                            if (config.isBoss) {
                                "Boss levels give no hints and a tighter clock. A calm retry usually wins."
                            } else {
                                "Nothing is locked — retry now or pick an easier level from the map."
                            },
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playTap(soundEnabled)
                            viewModel.startNewGame()
                        },
                        modifier = Modifier.testTag("retry_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VividRed)
                    ) {
                        Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                SoundManager.playTap(soundEnabled)
                                onWorldMap()
                            }
                        ) {
                            Text("Choose Level", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = {
                                SoundManager.playTap(soundEnabled)
                                onBackPressed()
                            }
                        ) {
                            Text("Exit", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (state.isPaused) {
            AlertDialog(
                onDismissRequest = { viewModel.togglePause() },
                title = { Text("Game Paused", fontWeight = FontWeight.Bold, color = RoyalBlue) },
                text = { Text("Take a quick break. The clock is stopped.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Text("Resume", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onBackPressed, modifier = Modifier.testTag("quit_button")) {
                        Text("Quit", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Victory
// ---------------------------------------------------------------------------------------------

@Composable
private fun VictoryDialog(
    levelNumber: Int,
    worldName: String,
    isBoss: Boolean,
    stars: Int,
    moves: Int,
    mistakes: Int,
    timeElapsedSec: Int,
    maxCombo: Int,
    coinsAwarded: Int,
    isPersonalBest: Boolean,
    soundEnabled: Boolean,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onWorldMap: () -> Unit,
    onHome: () -> Unit,
) {
    val isFinalLevel = levelNumber >= LevelCatalog.TOTAL_LEVELS

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (isFinalLevel) "🏔️" else "🎉", fontSize = 44.sp)
                Text(
                    if (isFinalLevel) "Summit Reached!" else "Victory!",
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = VividRed
                )
                Text(
                    if (isBoss) "👑 Boss level $levelNumber defeated" else "Level $levelNumber • $worldName",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (index in 1..3) {
                        VictoryStar(earned = index <= stars, delayMs = 250 + (index * 300))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VictoryStat("⏱️", "${timeElapsedSec}s", "Time")
                    VictoryStat("👟", moves.toString(), "Moves")
                    VictoryStat("❌", mistakes.toString(), "Mistakes")
                    VictoryStat("🔥", "x$maxCombo", "Combo")
                }

                if (mistakes == 0) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "💎 Perfect run — no mistakes!",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        fontSize = 14.sp
                    )
                }
                if (isPersonalBest) {
                    Spacer(Modifier.height(6.dp))
                    Text("🏅 New personal best time", fontWeight = FontWeight.Bold, color = RoyalBlue, fontSize = 13.sp)
                }
                if (coinsAwarded > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("🪙 +$coinsAwarded coins", fontWeight = FontWeight.Bold, color = GoldStar, fontSize = 14.sp)
                }

                Spacer(Modifier.height(20.dp))
                if (isFinalLevel) {
                    Text(
                        "You finished all ${LevelCatalog.TOTAL_LEVELS} levels. Every world is yours — replay any of them whenever you like.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                } else {
                    Button(
                        onClick = {
                            SoundManager.playTap(soundEnabled)
                            onNext()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("next_level_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VividRed),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Next Level ${levelNumber + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = {
                            SoundManager.playTap(soundEnabled)
                            onReplay()
                        },
                        modifier = Modifier.testTag("replay_button")
                    ) {
                        Text("Replay", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            SoundManager.playTap(soundEnabled)
                            onWorldMap()
                        },
                        modifier = Modifier.testTag("victory_map_button")
                    ) {
                        Text("World Map", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            SoundManager.playTap(soundEnabled)
                            onHome()
                        },
                        modifier = Modifier.testTag("victory_home_button")
                    ) {
                        Text("Home", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top bar & HUD
// ---------------------------------------------------------------------------------------------

@Composable
private fun GameTopBar(
    levelNumber: Int,
    worldEmoji: String,
    worldName: String,
    isBoss: Boolean,
    hintsLeft: Int,
    hintCooldown: Int,
    hintEnabled: Boolean,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onPause: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HudSurface.copy(alpha = 0.92f), RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isBoss) "👑 Boss • Level $levelNumber" else "Level $levelNumber",
                fontWeight = FontWeight.Bold,
                color = if (isBoss) GoldStar else Color.White,
                fontSize = 18.sp,
                maxLines = 1
            )
            Text(
                text = "$worldEmoji $worldName",
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = onHint, enabled = hintEnabled, modifier = Modifier.testTag("hint_button")) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = "Hint",
                    tint = if (hintEnabled) GoldStar else Color(0xFF64748B)
                )
                val badge = when {
                    hintCooldown > 0 -> hintCooldown.toString()
                    hintsLeft >= 0 -> hintsLeft.toString()
                    else -> null
                }
                if (badge != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(16.dp)
                            .background(if (hintCooldown > 0) Color(0xFF475569) else VividRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        IconButton(onClick = onPause, modifier = Modifier.testTag("pause_button")) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
        }
        IconButton(onClick = onRestart, modifier = Modifier.testTag("restart_button")) {
            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White)
        }
    }
}

@Composable
private fun HudCard(
    moves: Int,
    moveLimit: Int,
    mistakes: Int,
    matched: Int,
    total: Int,
    timeElapsed: Int,
    timeRemaining: Int,
    timeLimit: Int,
    isDark: Boolean
) {
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val movesLeft = if (moveLimit > 0) moveLimit - moves else Int.MAX_VALUE
    val timeLow = (timeLimit > 0) && (timeRemaining <= 10)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .background(if (isDark) HudSurface else Color.White, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (moveLimit > 0) "👟 $moves/$moveLimit" else "👟 $moves",
            color = if (movesLeft <= 3) Color(0xFFF43F5E) else VividRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text("❌ $mistakes", color = Color(0xFFF43F5E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("🧩 $matched/$total", color = RoyalBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (timeLimit > 0) "⏳ ${timeRemaining}s" else "⏱️ ${timeElapsed}s",
            color = if (timeLow) Color(0xFFF43F5E) else textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProgressAndTime(matched: Int, total: Int, timeRemaining: Int, timeLimit: Int, isDark: Boolean) {
    val progress by animateFloatAsState(
        targetValue = if (total > 0) matched.toFloat() / total.toFloat() else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "matchProgress"
    )
    val track = if (isDark) Color(0xFF2A3A6A) else Color(0xFFCBD5E1)
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = EmeraldGreen,
        trackColor = track
    )
    if (timeLimit > 0) {
        Spacer(Modifier.height(5.dp))
        val frac by animateFloatAsState(
            targetValue = (timeRemaining.toFloat() / timeLimit.toFloat()).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 900),
            label = "timeBar"
        )
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                frac > 0.5f -> RoyalBlue
                frac > 0.2f -> GoldStar
                else -> VividRed
            },
            trackColor = track
        )
    }
}

/** One fixed-height row: "Memorize!" during the preview, otherwise level-rule chips. */
@Composable
private fun StatusRow(
    config: LevelConfig,
    zen: Boolean,
    isPreviewing: Boolean,
    combo: Int,
    shuffleCountdown: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPreviewing) {
            RuleChip("👀 Memorize the cards!", GoldStar)
        } else {
            if (zen) RuleChip("🧘 Zen", EmeraldGreen)
            if (combo >= 2) RuleChip("🔥 Combo x$combo", Color(0xFFF97316))
            if ((!zen) && (config.shuffleIntervalSec > 0)) {
                RuleChip("🔀 ${shuffleCountdown}s", if (shuffleCountdown <= 5) VividRed else Color(0xFF8B5CF6))
            }
            if ((!zen) && (config.autoHideMs > 0)) {
                RuleChip("⚡ Hides ${"%.1f".format(config.autoHideMs / 1000f)}s", Color(0xFF06B6D4))
            }
        }
    }
}

@Composable
private fun RuleChip(text: String, color: Color) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.85f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun BoxScope.ComboPopup(combo: Int) {
    var visible by remember { mutableStateOf(value = false) }
    LaunchedEffect(combo) {
        if (combo >= 3) {
            visible = true
            delay(900.milliseconds)
        }
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 300f)) + fadeIn(),
        exit = fadeOut(tween(250)) + slideOutVertically { -it }
    ) {
        Text(
            text = "🔥 COMBO x$combo!",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
                .background(Color(0xFFF97316).copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun BoxScope.ShufflePopup(shuffleCount: Int) {
    var visible by remember { mutableStateOf(value = false) }
    LaunchedEffect(shuffleCount) {
        if (shuffleCount > 0) {
            visible = true
            delay(1100.milliseconds)
        }
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 260f)) + fadeIn(),
        exit = fadeOut(tween(300))
    ) {
        Text(
            text = "🔀 Shuffled!",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
                .background(Color(0xFF7C3AED).copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 10.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Card grid
// ---------------------------------------------------------------------------------------------

private fun columnsFor(cardCount: Int): Int = when {
    cardCount <= 12 -> 3
    cardCount <= 20 -> 4
    cardCount <= 30 -> 5
    else -> 6
}

private fun emojiSizeFor(columns: Int): TextUnit = when (columns) {
    3 -> 40.sp
    4 -> 34.sp
    5 -> 28.sp
    else -> 24.sp
}

@Composable
private fun CardGrid(
    cards: List<MemoryCard>,
    backStyle: CardBackVisual,
    isDark: Boolean,
    shakeProvider: () -> Float,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = columnsFor(cards.size)
    val emojiSize = emojiSizeFor(columns)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shakeProvider() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards, key = { it.id }) { card ->
            MemoryCardItem(
                card = card,
                backStyle = backStyle,
                isDark = isDark,
                emojiSize = emojiSize,
                entranceDelayMs = (card.id % 24) * 28,
                onClick = { onCardClick(card.id) },
                // Cards glide to their new slots when the board reshuffles.
                modifier = Modifier.animateItem()
            )
        }
    }
}

/**
 * One card. The flip angle is only read inside [graphicsLayer] lambdas, so the 3D flip runs in the
 * draw phase and never triggers recomposition while animating.
 */
@Composable
fun MemoryCardItem(
    card: MemoryCard,
    backStyle: CardBackVisual,
    isDark: Boolean,
    emojiSize: TextUnit,
    modifier: Modifier = Modifier,
    entranceDelayMs: Int = 0,
    onClick: () -> Unit
) {
    val faceUp = card.isFlipped || card.isMatched
    val reduceMotion = LocalReduceMotion.current
    val rotation = animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 300, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    // Staggered entrance when the deck appears.
    val entrance = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            entrance.snapTo(1f)
        } else {
            delay(entranceDelayMs.milliseconds)
            entrance.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f))
        }
    }

    // Bouncy "pop" the moment a pair is matched.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(card.isMatched) {
        if (card.isMatched && !reduceMotion) {
            pop.animateTo(1.18f, tween(110))
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    val shape = remember { RoundedCornerShape(18.dp) }
    val frontBrush: Brush = remember(card.isMatched, isDark) {
        when {
            card.isMatched -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
            isDark -> SolidColor(Color(0xFF24325B))
            else -> SolidColor(Color.White)
        }
    }
    val frontBorder = if (card.isMatched) BorderStroke(2.5.dp, Color(0xFFA7F3D0)) else BorderStroke(1.5.dp, RoyalBlue)
    val description = when {
        card.isMatched -> "Matched pair, ${card.emoji}"
        faceUp -> "Card ${card.emoji}, face up"
        else -> "Hidden card"
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 14f * density
                val sc = entrance.value * pop.value * (if (card.isMatched) 0.97f else 1f)
                scaleX = sc
                scaleY = sc
                alpha = entrance.value.coerceIn(0f, 1f)
            }
            .shadow(4.dp, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = description; role = Role.Button }
            .testTag("card_${card.id}")
    ) {
        // Back face (shown while the flip angle is below 90 degrees)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (rotation.value <= 90f) 1f else 0f }
                .clip(shape)
                .background(backStyle.gradient, shape)
                .drawBehind {
                    // Subtle diagonal weave so the card back has some texture.
                    val step = size.width / 4f
                    for (k in -4..8) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(k * step, 0f),
                            end = Offset(k * step + size.height, size.height),
                            strokeWidth = 2f
                        )
                    }
                }
                .border(BorderStroke(2.dp, backStyle.border), shape)
                // The glyph is decorative; the card's own semantics describe it, so a face-down
                // card never reveals its emoji to TalkBack.
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(text = backStyle.symbol, fontSize = (emojiSize.value * 0.75f).sp)
        }

        // Front face (mirrored back to normal because the container is rotated 180 degrees)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (rotation.value > 90f) 1f else 0f
                    rotationY = 180f
                }
                .clip(shape)
                .background(frontBrush, shape)
                .border(frontBorder, shape)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(text = card.emoji, fontSize = emojiSize)
        }
    }
}

@Composable
fun VictoryStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VictoryStar(earned: Boolean, delayMs: Int) {
    val scale = remember { Animatable(0f) }
    val spin = remember { Animatable(-90f) }
    LaunchedEffect(Unit) {
        delay(delayMs.milliseconds)
        coroutineScope {
            launch { scale.animateTo(if (earned) 1.15f else 0.8f, spring(dampingRatio = 0.4f, stiffness = 180f)) }
            launch { spin.animateTo(0f, tween(450)) }
        }
        scale.animateTo(if (earned) 1f else 0.8f, tween(150))
    }
    Text(
        text = if (earned) "⭐" else "☆",
        fontSize = 38.sp,
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = spin.value }
    )
}
