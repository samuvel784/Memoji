package com.vurka.memoji.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.memoji.ui.components.HowToPlayBottomSheet
import com.vurka.memoji.ui.components.AppTopBar
import com.vurka.memoji.ui.theme.EmeraldGreen
import com.vurka.memoji.ui.theme.RoyalBlue
import com.vurka.memoji.ui.theme.VividRed
import com.vurka.memoji.util.SoundManager
import com.vurka.memoji.viewmodel.LevelSelectionViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LevelSelectionViewModel,
    onBackPressed: () -> Unit
) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val masterVolume by viewModel.masterVolume.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val effectsVolume by viewModel.effectsVolume.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isZenMode by viewModel.isZenMode.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var showResetDialog by remember { mutableStateOf(false) }
    var showResetDone by remember { mutableStateOf(false) }
    var showHowToPlay by remember { mutableStateOf(false) }

    if (showHowToPlay) {
        HowToPlayBottomSheet(onDismiss = { showHowToPlay = false })
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset game progress?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("This permanently clears:")
                    Text("• completed levels, stars and personal bests", fontSize = 13.sp)
                    Text("• coins, purchased card backs and claimed rewards", fontSize = 13.sp)
                    Text("• games played, perfect games and your daily streak", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "All ${com.vurka.memoji.data.LevelCatalog.TOTAL_LEVELS} levels stay playable. " +
                            "Your sound, haptics, theme mode and Zen mode settings are kept.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllProgress()
                        showResetDialog = false
                        showResetDone = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VividRed),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = RoyalBlue)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showResetDone) {
        AlertDialog(
            onDismissRequest = { showResetDone = false },
            title = { Text("Progress reset", fontWeight = FontWeight.Bold) },
            text = { Text("Your stats start from zero again. Every level is still open — jump back in wherever you like.") },
            confirmButton = {
                Button(
                    onClick = { showResetDone = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.testTag("reset_done_button")
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Settings",
                onBack = onBackPressed
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val cardBg = if (isDark) Color(0xFF1C2541) else Color.White
            val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF2A3A6A)) else BorderStroke(1.dp, Color(0xFFCBD5E1))

            // Sound Effects Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Sound",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text("Master switch for music and effects", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) },
                        modifier = Modifier.testTag("sound_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VividRed)
                    )
                }
            }

            // Separate master / music / effects sliders, stored in DataStore.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Volume",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (soundEnabled) {
                            "Music stays quiet under the sound effects."
                        } else {
                            "Muted. Raise Master or switch Sound back on."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(6.dp))
                    VolumeSlider(
                        label = "Master",
                        value = masterVolume,
                        testTag = "master_volume_slider",
                        onChange = viewModel::setMasterVolume
                    )
                    VolumeSlider(
                        label = "Music",
                        value = musicVolume,
                        testTag = "music_volume_slider",
                        onChange = viewModel::setMusicVolume
                    )
                    VolumeSlider(
                        label = "Effects",
                        value = effectsVolume,
                        testTag = "effects_volume_slider",
                        onChange = { value ->
                            viewModel.setEffectsVolume(value)
                            // Instant feedback at the new level, without waiting for the flow.
                            SoundManager.setVolumes(masterVolume, value)
                            SoundManager.playTick(urgent = false, enabled = soundEnabled)
                        }
                    )
                }
            }

            // Sound preview row: lets players hear the palette before playing.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Preview Sounds",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (soundEnabled) "Tap to hear each cue" else "Turn sound on to preview",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { SoundManager.playFlip(true) },
                            enabled = soundEnabled,
                            label = { Text("Flip", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playMatch(4, true) },
                            enabled = soundEnabled,
                            label = { Text("Match x5", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playCombo(true) },
                            enabled = soundEnabled,
                            label = { Text("Combo", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playStar(2, true) },
                            enabled = soundEnabled,
                            label = { Text("Star", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playTick(urgent = false, enabled = true) },
                            enabled = soundEnabled,
                            label = { Text("Tick", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playWhoosh(true) },
                            enabled = soundEnabled,
                            label = { Text("Whoosh", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playCoin(true) },
                            enabled = soundEnabled,
                            label = { Text("Coin", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playChest(true) },
                            enabled = soundEnabled,
                            label = { Text("Chest", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playBoss(true) },
                            enabled = soundEnabled,
                            label = { Text("Boss", fontSize = 12.sp) }
                        )
                        AssistChip(
                            onClick = { SoundManager.playVictory(true) },
                            enabled = soundEnabled,
                            label = { Text("Victory", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Haptics Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Haptic Feedback",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text("Touch and match vibration", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) },
                        modifier = Modifier.testTag("haptics_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalBlue)
                    )
                }
            }

            // Zen Mode Toggle (relaxed: no timers, no move limits)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Zen Mode",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Relaxed play — no time or move limits",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = isZenMode,
                        onCheckedChange = { viewModel.setIsZenMode(it) },
                        modifier = Modifier.testTag("zen_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        )
                    )
                }
            }

            // Theme Mode Selector (System / Light / Dark)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Theme Mode",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text("Red & Blue Game Palette", fontSize = 13.sp, color = Color(0xFF94A3B8))

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Default", "Light", "Dark").forEach { mode ->
                            val isSelected = themeMode == mode
                            val bg = if (isSelected) VividRed else if (isDark) Color(0xFF24325B) else Color(0xFFE2E8F0)
                            val textColor = if (isSelected || isDark) Color.White else Color(0xFF0F172A)
                            val icon = when (mode) {
                                "Light" -> Icons.Default.LightMode
                                "Dark" -> Icons.Default.DarkMode
                                else -> Icons.Default.SettingsBrightness
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .testTag("theme_mode_$mode"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = mode,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // How to Play Guide Button
            OutlinedButton(
                onClick = { showHowToPlay = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_how_to_play_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.8.dp, RoyalBlue)
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = RoyalBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("How to Play Guide", color = if (isDark) Color.White else RoyalBlue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            // Reset Progress Button
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("reset_progress_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F1239))
            ) {
                Text("Reset All Game Progress", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * A labelled volume slider.
 *
 * The local value follows the drag, and DataStore is only written when the finger lifts. That keeps
 * a long drag from producing dozens of preference writes, while the percentage still updates live.
 */
@Composable
private fun VolumeSlider(
    label: String,
    value: Float,
    testTag: String,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "${(local * 100).roundToInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .semantics { contentDescription = "$label volume" },
            colors = SliderDefaults.colors(
                thumbColor = VividRed,
                activeTrackColor = VividRed,
                inactiveTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}
