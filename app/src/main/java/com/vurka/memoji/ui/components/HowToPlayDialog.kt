package com.vurka.memoji.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.memoji.ui.theme.VividRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayBottomSheet(
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF94A3B8)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📖 How to Play",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_how_to_play")) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InstructionCard(
                icon = "🃏",
                title = "Flip & Match Pairs",
                description = "Tap two cards to reveal them. Identical emojis remain face-up. Remember where mismatched emojis are!",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "💡",
                title = "Use Power-up Hints",
                description = "Need help? Tap the lightbulb to briefly peek at all hidden cards. Has a 10s cooldown timer.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "⚡",
                title = "Daily Streaks",
                description = "Play every day to build your streak! Track your progress in Statistics.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "🎨",
                title = "Card-Back Themes",
                description = "Complete levels or spend coins to unlock card backs. Preview them live in the Themes menu.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "🧘",
                title = "Zen Mode",
                description = "Prefer a relaxed game? Turn on Zen Mode in Settings to remove all time and move limits.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "🌍",
                title = "Themed Worlds",
                description = "Each of the 20 worlds uses its own emoji theme - animals, food, nature, and more!",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "⭐",
                title = "Earn 3 Stars",
                description = "Solve puzzles with fewer moves and faster times to score up to 3 stars and track personal bests.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionCard(
                icon = "🗺️",
                title = "1000-Level World Map",
                description = "Advance sequentially through Easy, Medium, Hard, and Expert tiers across 20 themed worlds.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("got_it_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VividRed)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Got It! Let's Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun InstructionCard(
    icon: String,
    title: String,
    description: String,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF24325B) else Color(0xFFF1F5F9)
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1C2541) else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
