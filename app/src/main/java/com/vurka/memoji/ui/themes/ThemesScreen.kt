package com.vurka.memoji.ui.themes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import com.vurka.memoji.model.CardBackTheme
import com.vurka.memoji.data.ThemeUnlock
import com.vurka.memoji.ui.components.AppTopBar
import com.vurka.memoji.ui.theme.CardBackVisuals
import com.vurka.memoji.ui.theme.EmeraldGreen
import com.vurka.memoji.ui.theme.RoyalBlue
import com.vurka.memoji.ui.theme.VividRed
import com.vurka.memoji.viewmodel.LevelSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    viewModel: LevelSelectionViewModel,
    onBackPressed: () -> Unit,
) {
    val levelsCompleted by viewModel.totalLevelsCompleted.collectAsState()
    val selectedThemeId by viewModel.selectedThemeId.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Card-Back Themes",
                onBack = onBackPressed
            )
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
            items(viewModel.themes) { theme ->
                val isPurchased by viewModel.isThemePurchased(theme.id).collectAsState(initial = false)
                // Unlocking is driven by real progress or a coin purchase - never by the fact that
                // every level is selectable.
                val isUnlocked = ThemeUnlock.isUnlocked(
                    purchased = isPurchased,
                    completedLevels = levelsCompleted,
                    milestoneLevels = theme.milestoneLevel,
                )
                val isSelected = selectedThemeId == theme.id

                ThemeCard(
                    theme = theme,
                    isUnlocked = isUnlocked,
                    isSelected = isSelected,
                    isDark = isDark,
                    lockedProgress = if (!isUnlocked) "$levelsCompleted/${theme.milestoneLevel} levels" else null
                ) {
                    if (isUnlocked) viewModel.setSelectedThemeId(theme.id)
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: CardBackTheme,
    isUnlocked: Boolean,
    isSelected: Boolean,
    isDark: Boolean,
    lockedProgress: String? = null,
    onClick: () -> Unit,
) {
    val cardBg = if (isUnlocked) {
        if (isSelected) (if (isDark) Color(0xFF2A2450) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF1C2541) else Color.White)
    } else {
        if (isDark) Color(0xFF131B30) else Color(0xFFF1F5F9)
    }

    val cardBorder = if (isSelected) {
        BorderStroke(2.dp, VividRed)
    } else if (isUnlocked) {
        BorderStroke(1.5.dp, RoyalBlue)
    } else {
        BorderStroke(1.dp, if (isDark) Color(0xFF2A3A6A) else Color(0xFFCBD5E1))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Live preview of the actual card back
                val visual = CardBackVisuals.forTheme(theme.id, isDark)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(visual.gradient)
                        .border(BorderStroke(2.dp, visual.border), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = visual.symbol, fontSize = 22.sp)
                }
                Column {
                    Text(
                        text = theme.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onBackground else Color(0xFF64748B)
                    )
                    Text(
                        text = theme.description,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    if (lockedProgress != null) {
                        Text(
                            text = "🔒 $lockedProgress",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(28.dp)
                )
            } else if (!isUnlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
