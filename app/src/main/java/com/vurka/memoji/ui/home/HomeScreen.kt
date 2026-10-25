package com.vurka.memoji.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.memoji.R
import com.vurka.memoji.data.LevelCatalog
import com.vurka.memoji.ui.components.AnimatedBackdrop
import com.vurka.memoji.ui.components.HowToPlayBottomSheet
import com.vurka.memoji.ui.components.bob
import com.vurka.memoji.ui.components.popIn
import com.vurka.memoji.ui.components.pressBounce
import com.vurka.memoji.ui.components.pulse
import com.vurka.memoji.viewmodel.LevelSelectionViewModel

private val BackdropParticles = listOf("✦", "✧", "❖", "◈", "◆", "◇", "•", "·", "✦", "✧")

@Composable
fun HomeScreen(
    viewModel: LevelSelectionViewModel,
    onPlayClicked: (Int) -> Unit,
    onWorldMapClicked: () -> Unit,
    onStatsClicked: () -> Unit,
    onStoreClicked: () -> Unit,
    onAchievementsClicked: () -> Unit,
    onThemesClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val streak by viewModel.dailyStreak.collectAsState()
    val totalStars by viewModel.totalStars.collectAsState()
    var showHowToPlay by remember { mutableStateOf(false) }

    if (showHowToPlay) HowToPlayBottomSheet(onDismiss = { showHowToPlay = false })

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
    val world = remember { LevelCatalog.getWorldInfo(1) }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackdrop(brush = bg, emojis = BackdropParticles, count = 22, alpha = if (isDark) 0.15f else 0.35f)

        // ---------- Modern Bright Center Hero Core ----------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
                    0.4f, 0.9f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p"
                )
                // Outer Glow Ring
                Box(
                    Modifier
                        .size(220.dp)
                        .graphicsLayer { alpha = pulseAnim; scaleX = 0.95f + pulseAnim * 0.15f; scaleY = 0.95f + pulseAnim * 0.15f }
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
                // Glassmorphism Card Frame
                Box(
                    Modifier
                        .size(170.dp)
                        .popIn(100)
                        .shadow(24.dp, RoundedCornerShape(38.dp), spotColor = Color(0xFF8B5CF6))
                        .clip(RoundedCornerShape(38.dp))
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
                            RoundedCornerShape(38.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_1792172025849),
                        contentDescription = "Memoji Logo",
                        modifier = Modifier
                            .size(118.dp)
                            .pulse(1f, 1.03f, 1800)
                            .clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "MEMOJI MEMORY",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFE11D48), Color(0xFF8B5CF6), Color(0xFF2563EB))
                    )
                ),
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.popIn(300)
            )
            Spacer(Modifier.height(6.dp))
            
            // Advanced Glass Info Pill
            Row(
                modifier = Modifier
                    .popIn(450)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.95f))
                    .border(1.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color(0xFFFF4D6D).copy(alpha = 0.5f))), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Public, contentDescription = "Worlds", tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                    Text("${LevelCatalog.WORLD_COUNT} Worlds", color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(3.dp).background(Color(0xFF64748B), CircleShape))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Extension, contentDescription = "Levels", tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                    Text("${LevelCatalog.TOTAL_LEVELS} Levels", color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            // Current World Badge
            Row(
                modifier = Modifier
                    .popIn(600)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(world.colorA), Color(world.colorB))))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Explore, contentDescription = "World", tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("World ${world.index} • ${world.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ---------- Top Bar: Stats & Settings Glass Nodes ----------
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassOrbButton(Icons.AutoMirrored.Filled.ShowChart, "Analytics", Color(0xFF3B82F6), "stats_button", 150, isDark, onStatsClicked)
                GlassOrbButton(Icons.Default.ShoppingCart, "Store", Color(0xFF10B981), "store_button", 200, isDark, onStoreClicked)
            }
            Row(Modifier.popIn(300), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassInfoChip(Icons.Default.LocalFireDepartment, "$streak", Color(0xFFFF4D6D), isDark)
                GlassInfoChip(Icons.Default.Star, "$totalStars", Color(0xFFF59E0B), isDark)
            }
            GlassOrbButton(Icons.Default.Settings, "Preferences", Color(0xFF64748B), "settings_button", 250, isDark, onSettingsClicked)
        }

        // ---------- Middle Flank Nodes: Tasks & Themes ----------
        GlassOrbButton(
            Icons.Default.EmojiEvents, "Tasks", Color(0xFFF59E0B), "achievements_button", 400, isDark, onAchievementsClicked,
            modifier = Modifier.align(Alignment.CenterStart).offset(y = 60.dp).padding(start = 16.dp)
        )
        GlassOrbButton(
            Icons.Default.Palette, "Themes", Color(0xFFEC4899), "themes_button", 500, isDark, onThemesClicked,
            modifier = Modifier.align(Alignment.CenterEnd).offset(y = 60.dp).padding(end = 16.dp)
        )

        // ---------- Bottom Flank Nodes: Guide & World Map ----------
        GlassOrbButton(
            Icons.AutoMirrored.Filled.HelpOutline, "Guide", Color(0xFF06B6D4), "how_to_play_button", 600, isDark, { showHowToPlay = true },
            modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 16.dp, bottom = 24.dp)
        )
        GlassOrbButton(
            Icons.Default.Map, "World Map", Color(0xFF10B981), "world_map_button", 700, isDark, onWorldMapClicked,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 24.dp)
        )

        // ---------- Bottom Center: Bright PLAY Action Pod (Level 1) ----------
        BrightPlayButton(
            onClick = { onPlayClicked(1) },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp)
        )
    }
}

@Composable
private fun BrightPlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ring by rememberInfiniteTransition(label = "ring").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "r"
    )
    Box(modifier.popIn(500), contentAlignment = Alignment.Center) {
        // Pulsing Outer Ring
        Box(
            Modifier
                .size(width = 200.dp, height = 66.dp)
                .graphicsLayer { scaleX = 1f + ring * 0.25f; scaleY = 1f + ring * 0.45f; alpha = (1f - ring) * 0.6f }
                .border(2.5.dp, Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFFF7A45))), RoundedCornerShape(36.dp))
        )
        Row(
            modifier = Modifier
                .pulse(1f, 1.03f, 1000)
                .pressBounce()
                .size(width = 200.dp, height = 66.dp)
                .shadow(16.dp, RoundedCornerShape(36.dp), spotColor = Color(0xFFE11D48))
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFE11D48),
                            Color(0xFFFF7A45),
                            Color(0xFFF59E0B)
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(36.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .testTag("play_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text("PLAY", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("LEVEL 1", color = Color.White.copy(alpha = 0.95f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun GlassOrbButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    delayMs: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.popIn(delayMs).bob(6f, 2800 + delayMs, delayMs / 1000f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .pressBounce()
                .size(56.dp)
                .shadow(12.dp, CircleShape, spotColor = accentColor)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f))
                .border(BorderStroke(1.5.dp, Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))), CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
            letterSpacing = 0.5.sp
        )
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
