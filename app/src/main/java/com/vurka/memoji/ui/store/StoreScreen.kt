package com.vurka.memoji.ui.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.memoji.ui.components.AppTopBar
import com.vurka.memoji.ui.theme.CardBackVisuals
import com.vurka.memoji.ui.theme.EmeraldGreen
import com.vurka.memoji.ui.theme.GoldStar
import com.vurka.memoji.ui.theme.RoyalBlue
import com.vurka.memoji.ui.theme.VividRed
import com.vurka.memoji.util.SoundManager
import com.vurka.memoji.viewmodel.LevelSelectionViewModel
import kotlinx.coroutines.launch

data class StoreItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val symbol: String,
    val themeId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    viewModel: LevelSelectionViewModel,
    onBackPressed: () -> Unit,
) {
    val coins by viewModel.coins.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var showNotEnoughDialog by remember { mutableStateOf(false) }

    val storeItems = listOf(
        StoreItem("theme_red_crimson", "Crimson Flame Theme", "Unlock fiery card-back visual", 300, "🔥", "red_crimson"),
        StoreItem("theme_aurora_mint", "Aurora Mint Theme", "Unlock fresh mint visual", 500, "🍀", "aurora_mint"),
        StoreItem("theme_royal_gold", "Royal Gold Theme", "Unlock royal gold visual", 800, "👑", "royal_gold"),
        StoreItem("theme_neon_candy", "Neon Candy Theme", "Unlock vibrant neon visual", 1200, "🍭", "neon_candy"),
        StoreItem("theme_space_void", "Space Void Theme", "Unlock cosmic space visual", 1800, "🌌", "space_void"),
        StoreItem("theme_dragon_fire", "Dragon Fire Theme", "Unlock legendary dragon visual", 2500, "🐉", "dragon_fire")
    )

    if (showNotEnoughDialog) {
        AlertDialog(
            onDismissRequest = { showNotEnoughDialog = false },
            title = { Text("Not Enough Coins!", fontWeight = FontWeight.Bold, color = VividRed) },
            text = { Text("Complete Daily, Weekly, and Monthly tasks to earn more coins!") },
            confirmButton = {
                Button(
                    onClick = { showNotEnoughDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VividRed)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Item Store", onBack = onBackPressed) {
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
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C2541) else Color(0xFFEFF6FF)),
                    border = BorderStroke(1.5.dp, RoyalBlue)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒 Coin Store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Spend your hard-earned coins to unlock exclusive themes!", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            items(storeItems) { item ->
                val isPurchased by viewModel.isThemePurchased(item.themeId ?: "").collectAsState(initial = false)
                StoreItemCard(
                    item = item,
                    isPurchased = isPurchased,
                    userCoins = coins,
                    isDark = isDark,
                    onBuy = {
                        scope.launch {
                            val success = viewModel.buyItem(item.id, item.cost)
                            if (success) {
                                SoundManager.playCoin(soundEnabled)
                            } else {
                                SoundManager.playMismatch(soundEnabled)
                                showNotEnoughDialog = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StoreItemCard(
    item: StoreItem,
    isPurchased: Boolean,
    userCoins: Int,
    isDark: Boolean,
    onBuy: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1C2541) else Color.White
    val cardBorder = if (isPurchased) BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)) else BorderStroke(1.5.dp, RoyalBlue)
    val canAfford = userCoins >= item.cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                val visual = item.themeId?.let { CardBackVisuals.forTheme(it, isDark) }
                if (visual != null) {
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
                } else {
                    Text(text = item.symbol, fontSize = 32.sp)
                }

                Column {
                    Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(text = item.description, fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            }

            when {
                isPurchased -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Owned", tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                        Text("Owned", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onBuy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) VividRed else Color(0xFF475569),
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("🪙 ${item.cost}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
