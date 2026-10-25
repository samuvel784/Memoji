package com.vurka.memoji.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.memoji.ui.components.HowToPlayBottomSheet
import com.vurka.memoji.ui.theme.EmeraldGreen
import com.vurka.memoji.ui.theme.RoyalBlue
import com.vurka.memoji.ui.theme.VividRed
import com.vurka.memoji.viewmodel.LevelSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LevelSelectionViewModel,
    onBackPressed: () -> Unit
) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isZenMode by viewModel.isZenMode.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var showResetDialog by remember { mutableStateOf(false) }
    var showHowToPlay by remember { mutableStateOf(false) }

    if (showHowToPlay) {
        HowToPlayBottomSheet(onDismiss = { showHowToPlay = false })
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Progress?", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset all unlocked levels and stats back to Level 1. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllProgress()
                        showResetDialog = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C2541))
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
                            "Sound Effects",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text("Card flips and victory audio", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) },
                        modifier = Modifier.testTag("sound_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VividRed)
                    )
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
