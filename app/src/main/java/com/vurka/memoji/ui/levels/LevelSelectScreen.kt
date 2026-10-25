package com.vurka.memoji.ui.levels

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vurka.memoji.data.EmojiCatalog
import com.vurka.memoji.data.LevelCatalog
import com.vurka.memoji.data.WorldInfo
import com.vurka.memoji.model.LevelConfig
import com.vurka.memoji.ui.components.AnimatedBackdrop
import com.vurka.memoji.ui.components.bob
import com.vurka.memoji.ui.components.pressBounce
import com.vurka.memoji.ui.components.pulse
import com.vurka.memoji.viewmodel.LevelSelectionViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Map model: [banner(world 1), level 1..50, banner(world 2), level 51..100, ... , summit]
// The list is displayed with reverseLayout, so index 0 is at the bottom and the path climbs up.
// ---------------------------------------------------------------------------------------------

private sealed interface MapRow {
    val worldIndex: Int

    data class Banner(val world: WorldInfo) : MapRow {
        override val worldIndex: Int get() = world.index
    }

    data class Node(val config: LevelConfig, val world: WorldInfo) : MapRow {
        override val worldIndex: Int get() = world.index
    }

    data class Summit(override val worldIndex: Int) : MapRow
}

private val NodeRowHeight = 96.dp
private val BannerRowHeight = 150.dp
private val SummitRowHeight = 220.dp

private val mapRows: List<MapRow> by lazy {
    buildList {
        val levels = LevelCatalog.getAllLevels()
        LevelCatalog.getWorlds().forEach { w ->
            add(MapRow.Banner(w))
            val start = (w.index - 1) * LevelCatalog.LEVELS_PER_WORLD
            for (i in start until start + LevelCatalog.LEVELS_PER_WORLD) add(MapRow.Node(levels[i], w))
        }
        add(MapRow.Summit(LevelCatalog.WORLD_COUNT))
    }
}

/** Horizontal position (-1..1) of each row's path point. Banners and summit sit in the centre. */
private val rowBiases: FloatArray by lazy {
    FloatArray(mapRows.size) { i ->
        when (val row = mapRows[i]) {
            is MapRow.Node -> 0.66f * sin(row.config.levelNumber * 0.62f)
            else -> 0f
        }
    }
}

private val Gold = Color(0xFFFDE047)

@Composable
fun LevelSelectScreen(
    viewModel: LevelSelectionViewModel,
    onLevelSelected: (Int) -> Unit,
    onBackPressed: () -> Unit
) {
    val highest by viewModel.highestUnlockedLevel.collectAsStateWithLifecycle()
    val starsMap by viewModel.starsMap.collectAsStateWithLifecycle()
    val totalStars by viewModel.totalStars.collectAsStateWithLifecycle()
    val zen by viewModel.isZenMode.collectAsStateWithLifecycle()

    val rows = mapRows
    val worlds = remember { LevelCatalog.getWorlds() }
    val currentIndex = remember(highest) {
        rows.indexOfFirst { it is MapRow.Node && it.config.levelNumber == highest }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableIntStateOf(0) }

    // Land on the player's current level, a few rows up from the bottom edge.
    LaunchedEffect(currentIndex) {
        listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
    }

    // The world that is roughly in the middle of the screen drives the background and the title chip.
    val visibleWorldIdx by remember {
        derivedStateOf { rows[(listState.firstVisibleItemIndex + 4).coerceIn(0, rows.lastIndex)].worldIndex }
    }
    val world = worlds[(visibleWorldIdx - 1).coerceIn(0, worlds.lastIndex)]
    val showJump by remember(currentIndex) {
        derivedStateOf { abs(listState.firstVisibleItemIndex + 4 - currentIndex) > 7 }
    }

    val deepNavy = Color(0xFF0B132B)
    val topColor by animateColorAsState(lerp(deepNavy, Color(world.colorB), 0.55f), tween(700), label = "mapTop")
    val bottomColor by animateColorAsState(lerp(deepNavy, Color(world.colorA), 0.30f), tween(700), label = "mapBottom")
    val backdropEmojis = remember(world.index) {
        world.categories.ifEmpty { EmojiCatalog.getAllCategories() }
            .flatMap { EmojiCatalog.getByCategory(it) }.take(10).map { it.emoji }
            .ifEmpty { listOf("⭐", "✨") }
    }

    val insets = WindowInsets.systemBars.asPaddingValues()

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Brush.verticalGradient(listOf(topColor, bottomColor))) }
    ) {
        AnimatedBackdrop(brush = SolidColor(Color.Transparent), emojis = backdropEmojis, count = 10, alpha = 0.12f)

        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = insets.calculateTopPadding() + 112.dp,
                bottom = insets.calculateBottomPadding() + 24.dp
            )
        ) {
            itemsIndexed(
                items = rows,
                key = { _, row ->
                    when (row) {
                        is MapRow.Banner -> "b${row.world.index}"
                        is MapRow.Node -> "n${row.config.levelNumber}"
                        is MapRow.Summit -> "summit"
                    }
                },
                contentType = { _, row ->
                    when (row) {
                        is MapRow.Banner -> 0
                        is MapRow.Node -> 1
                        is MapRow.Summit -> 2
                    }
                }
            ) { index, row ->
                when (row) {
                    is MapRow.Banner -> WorldBannerRow(
                        world = row.world,
                        reachable = firstLevelOf(row.world) <= highest,
                        isFirst = index == 0,
                        starsMap = starsMap
                    )

                    is MapRow.Node -> {
                        val level = row.config.levelNumber
                        val stars = starsMap[level] ?: 0
                        MapNodeRow(
                            level = row.config,
                            world = row.world,
                            stars = stars,
                            unlocked = level <= highest,
                            isCurrent = level == highest && stars == 0,
                            bias = rowBiases[index],
                            biasPrev = rowBiases[(index - 1).coerceAtLeast(0)],
                            biasNext = rowBiases[(index + 1).coerceAtMost(rows.lastIndex)],
                            prevDone = level <= highest,
                            nextDone = level + 1 <= highest || (level == LevelCatalog.TOTAL_LEVELS && stars > 0),
                            onClick = { selectedLevel = level }
                        )
                    }

                    is MapRow.Summit -> SummitRow(reached = (starsMap[LevelCatalog.TOTAL_LEVELS] ?: 0) > 0)
                }
            }
        }

        // ---------- Top bar ----------
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(deepNavy.copy(alpha = 0.95f), deepNavy.copy(alpha = 0.7f), Color.Transparent))
                )
                .statusBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackPressed, modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("World Map", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                    Text(
                        "Level $highest / ${LevelCatalog.TOTAL_LEVELS}",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
                Text(
                    "⭐ $totalStars",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            AnimatedContent(
                targetState = world,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                label = "worldChip"
            ) { w ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(w.colorA), Color(w.colorB))))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(w.emoji, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("World ${w.index} • ${w.name}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---------- "Jump to my level" ----------
        AnimatedVisibility(
            visible = showJump,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFFF7A45))))
                    .clickable {
                        scope.launch { listState.animateScrollToItem((currentIndex - 3).coerceAtLeast(0)) }
                    }
                    .testTag("jump_to_current")
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📍 Level $highest", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }

    if (selectedLevel > 0) {
        val config = LevelCatalog.getLevel(selectedLevel)
        LevelPreviewDialog(
            config = config,
            world = LevelCatalog.getWorldInfo(selectedLevel),
            stars = starsMap[selectedLevel] ?: 0,
            locked = selectedLevel > highest,
            zen = zen,
            onPlay = {
                val l = selectedLevel
                selectedLevel = 0
                onLevelSelected(l)
            },
            onDismiss = { selectedLevel = 0 }
        )
    }
}

private fun firstLevelOf(world: WorldInfo): Int = (world.index - 1) * LevelCatalog.LEVELS_PER_WORLD + 1

// ---------------------------------------------------------------------------------------------
// Path drawing
// ---------------------------------------------------------------------------------------------

private fun DrawScope.pathX(bias: Float, nodePx: Float): Float =
    (size.width - nodePx) / 2f * (1f + bias) + nodePx / 2f

/** S-curve from the row's path point to the middle of the row's top or bottom edge. */
private fun DrawScope.trailPath(fromX: Float, fromY: Float, toX: Float, toY: Float): Path {
    val length = toY - fromY
    return Path().apply {
        moveTo(fromX, fromY)
        cubicTo(fromX, fromY + length * 0.45f, toX, toY - length * 0.45f, toX, toY)
    }
}

private fun DrawScope.drawTrail(path: Path, done: Boolean, world: WorldInfo) {
    val track = if (done) Color(world.colorA).copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.22f)
    drawPath(path, track, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
    if (done) {
        drawPath(path, Gold, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    } else {
        drawPath(
            path,
            Color.White.copy(alpha = 0.45f),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f))
            )
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------------------------

@Composable
private fun MapNodeRow(
    level: LevelConfig,
    world: WorldInfo,
    stars: Int,
    unlocked: Boolean,
    isCurrent: Boolean,
    bias: Float,
    biasPrev: Float,
    biasNext: Float,
    prevDone: Boolean,
    nextDone: Boolean,
    onClick: () -> Unit
) {
    val boss = level.isBoss
    val nodeSize: Dp = if (boss) 76.dp else 62.dp
    val shape: Shape = if (boss) RoundedCornerShape(26.dp) else CircleShape
    val colorA = Color(world.colorA)
    val colorB = Color(world.colorB)
    val number = level.levelNumber

    val decorations = remember(world.index) {
        world.categories.ifEmpty { EmojiCatalog.getAllCategories() }
            .flatMap { EmojiCatalog.getByCategory(it) }.map { it.emoji }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(NodeRowHeight)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val nodePx = nodeSize.toPx()
            val cx = pathX(bias, nodePx)
            val cy = size.height / 2f
            val xmPrev = (cx + pathX(biasPrev, nodePx)) / 2f
            drawTrail(trailPath(cx, cy, xmPrev, size.height), prevDone, world)
            val xmNext = (cx + pathX(biasNext, nodePx)) / 2f
            drawTrail(trailPath(cx, cy, xmNext, 0f), nextDone, world)
        }

        // Scenery on the opposite side of the path.
        if (number % 3 != 0 && decorations.isNotEmpty()) {
            Text(
                text = decorations[(number * 7) % decorations.size],
                fontSize = (20 + (number * 5) % 14).sp,
                modifier = Modifier
                    .align(BiasAbsoluteAlignment(if (bias >= 0f) -0.82f else 0.82f, 0f))
                    .graphicsLayer { alpha = 0.55f }
            )
        }

        // The node itself
        Box(
            Modifier
                .align(BiasAbsoluteAlignment(bias, 0f))
                .size(nodeSize)
        ) {
            if (isCurrent) PulseRing(shape = shape, color = Gold)

            val fill = when {
                !unlocked -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF334155)))
                stars > 0 -> Brush.linearGradient(listOf(colorA, colorB))
                else -> Brush.linearGradient(listOf(colorA.copy(alpha = 0.75f), colorB.copy(alpha = 0.75f)))
            }
            val borderColor = when {
                isCurrent -> Color.White
                boss && unlocked -> Gold
                unlocked -> Color.White.copy(alpha = 0.7f)
                else -> Color.White.copy(alpha = 0.25f)
            }
            val description = when {
                !unlocked -> "Level $number, locked"
                stars > 0 -> "Level $number, $stars stars"
                else -> "Level $number, not completed"
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (isCurrent) Modifier.pulse(1f, 1.08f, 800) else Modifier)
                    .pressBounce()
                    .shadow(if (unlocked) 8.dp else 2.dp, shape)
                    .clip(shape)
                    .background(fill)
                    .border(BorderStroke(if (isCurrent) 3.dp else 2.dp, borderColor), shape)
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = description }
                    .testTag("level_item_$number"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!unlocked) {
                        Text("🔒", fontSize = 18.sp)
                        Text("$number", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                    } else {
                        Text("$number", fontSize = if (boss) 20.sp else 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        if (stars > 0) {
                            Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gold)
                        } else if (boss) {
                            Text("BOSS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                        }
                    }
                }
            }

            if (boss) {
                Text("👑", fontSize = 22.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp))
            }
            if (isCurrent) {
                // Player marker hopping above the current level.
                Text(
                    "🧙",
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = if (boss) (-42).dp else (-30).dp)
                        .bob(amplitudePx = 10f, periodMs = 1200)
                )
            }
        }
    }
}

@Composable
private fun PulseRing(shape: Shape, color: Color) {
    val v = rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringV"
    )
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = 1f + v.value * 0.55f
                scaleX = s
                scaleY = s
                alpha = (1f - v.value) * 0.8f
            }
            .border(3.dp, color, shape)
    )
}

@Composable
private fun WorldBannerRow(world: WorldInfo, reachable: Boolean, isFirst: Boolean, starsMap: Map<Int, Int>) {
    val first = firstLevelOf(world)
    val last = first + LevelCatalog.LEVELS_PER_WORLD - 1
    val earned = remember(world.index, starsMap) { (first..last).sumOf { starsMap[it] ?: 0 } }
    val max = LevelCatalog.LEVELS_PER_WORLD * 3

    Box(
        Modifier
            .fillMaxWidth()
            .height(BannerRowHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val path = Path().apply {
                moveTo(cx, size.height / 2f)
                lineTo(cx, 0f)
            }
            drawTrail(path, reachable, world)
            if (!isFirst) {
                val down = Path().apply {
                    moveTo(cx, size.height / 2f)
                    lineTo(cx, size.height)
                }
                drawTrail(down, reachable, world)
            }
        }
        val alpha = if (reachable) 1f else 0.55f
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .graphicsLayer { this.alpha = alpha }
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Color(world.colorA), Color(world.colorB))))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(world.emoji, fontSize = 40.sp, modifier = Modifier.bob(amplitudePx = 6f, periodMs = 2200))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "World ${world.index}${if (reachable) "" else "  🔒"}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(world.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                Text("Levels $first–$last", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { earned.toFloat() / max },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Gold,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("⭐ $earned/$max", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SummitRow(reached: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SummitRowHeight),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏔️", fontSize = 64.sp, modifier = Modifier.bob(amplitudePx = 8f, periodMs = 2600))
            Text("🚩", fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (reached) "You conquered every world!" else "The Summit awaits",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "${LevelCatalog.TOTAL_LEVELS} levels • ${LevelCatalog.WORLD_COUNT} worlds",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Level preview
// ---------------------------------------------------------------------------------------------

@Composable
private fun LevelPreviewDialog(
    config: LevelConfig,
    world: WorldInfo,
    stars: Int,
    locked: Boolean,
    zen: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    if (config.isBoss) "👑 Boss Level ${config.levelNumber}" else "Level ${config.levelNumber}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Text(
                    "${world.emoji} ${world.name} • ${config.difficultyLabel}",
                    fontSize = 13.sp,
                    color = Color(world.colorA)
                )
            }
        },
        text = {
            if (locked) {
                Text("🔒 Finish level ${config.levelNumber - 1} to unlock this one.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        if (stars > 0) "★".repeat(stars) + "☆".repeat(3 - stars) else "☆☆☆",
                        color = Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    InfoLine("🃏", "${config.pairCount} pairs (${config.pairCount * 2} cards)")
                    InfoLine("👀", "${config.previewTimeSec}s to memorise")
                    if (zen) {
                        InfoLine("🧘", "Zen mode: no timer, move limit or shuffles")
                    } else {
                        InfoLine("⏳", if (config.timeLimitSec > 0) "${config.timeLimitSec}s time limit" else "No time limit")
                        InfoLine("👟", if (config.moveLimit > 0) "${config.moveLimit} moves max" else "No move limit")
                        if (config.shuffleIntervalSec > 0) InfoLine("🔀", "Cards reshuffle every ${config.shuffleIntervalSec}s")
                        if (config.autoHideMs > 0) InfoLine("⚡", "A lone card hides after ${"%.1f".format(config.autoHideMs / 1000f)}s")
                    }
                    InfoLine(
                        "💡",
                        when {
                            zen || config.maxHints < 0 -> "Unlimited hints"
                            config.maxHints == 0 -> "No hints"
                            else -> "${config.maxHints} hints"
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (!locked) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.testTag("play_level_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (!locked) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InfoLine(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
}
