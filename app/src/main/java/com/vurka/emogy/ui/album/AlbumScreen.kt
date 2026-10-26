package com.vurka.emogy.ui.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurka.emogy.R
import com.vurka.emogy.data.AlbumCategoryProgress
import com.vurka.emogy.data.AlbumEntryProgress
import com.vurka.emogy.data.AlbumProgress
import com.vurka.emogy.data.AlbumProgressCalculator
import com.vurka.emogy.data.EmojiCatalog
import com.vurka.emogy.ui.components.AppTopBar
import com.vurka.emogy.ui.theme.EmeraldGreen
import com.vurka.emogy.ui.theme.GoldStar
import com.vurka.emogy.util.SoundManager
import com.vurka.emogy.viewmodel.LevelSelectionViewModel

/**
 * The offline sticker book. Matching a pair in any level or the Daily Challenge unlocks that
 * emoji; there is nothing to buy and nothing leaves the device.
 */
@Composable
fun AlbumScreen(
    viewModel: LevelSelectionViewModel,
) {
    val collected by viewModel.collectedEmojis.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val album = remember(collected) {
        AlbumProgressCalculator.calculate(EmojiCatalog.items, collected)
    }
    var selected by remember { mutableStateOf<AlbumEntryProgress?>(null) }

    selected?.let { entry ->
        AlbumEntryDialog(
            entry = entry,
            isDark = isDark,
            onDismiss = { selected = null },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.album_title), onBack = null) {
                Row(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📖", fontSize = 16.sp)
                    Text(
                        "${album.collected}/${album.total}",
                        color = GoldStar,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // The app-level scaffold already reserves room for the bottom navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 58.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("album_screen"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "album_summary") {
                AlbumSummary(album = album, isDark = isDark)
            }

            album.categories.forEach { category ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "album_header_${category.name}") {
                    CategoryHeader(category = category, isDark = isDark)
                }
                items(
                    items = category.entries,
                    key = { "album_entry_${it.entry.id}" },
                ) { entry ->
                    AlbumTile(
                        entry = entry,
                        isDark = isDark,
                        onClick = {
                            SoundManager.playTap(soundEnabled)
                            selected = entry
                        }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "album_footer") {
                Text(
                    text = stringResource(R.string.album_footer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun AlbumSummary(album: AlbumProgress, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1C2541) else Color.White
        ),
        border = BorderStroke(1.5.dp, GoldStar.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📖", fontSize = 30.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (album.isComplete) {
                            stringResource(R.string.album_complete)
                        } else {
                            stringResource(R.string.album_collection)
                        },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.album_collected, album.collected, album.total),
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
                Text(
                    text = stringResource(R.string.album_percent, (album.fraction * 100).toInt()),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldStar
                )
            }
            LinearProgressIndicator(
                progress = { album.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = GoldStar,
                trackColor = if (isDark) Color(0xFF2A3A6A) else Color(0xFFE2E8F0)
            )
            Text(
                text = if (album.isComplete) {
                    stringResource(R.string.album_complete_body)
                } else {
                    stringResource(R.string.album_progress_body)
                },
                fontSize = 12.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun CategoryHeader(category: AlbumCategoryProgress, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = category.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (category.isComplete) {
            Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Black, color = EmeraldGreen)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "${category.collected}/${category.total}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                category.isComplete -> EmeraldGreen
                isDark -> Color(0xFF94A3B8)
                else -> Color(0xFF64748B)
            }
        )
    }
}

@Composable
private fun AlbumTile(
    entry: AlbumEntryProgress,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val collected = entry.isCollected
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                when {
                    collected && isDark -> Color(0xFF24325B)
                    collected -> Color.White
                    isDark -> Color(0xFF151D33)
                    else -> Color(0xFFE8EDF5)
                }
            )
            .clickable(onClick = onClick)
            .testTag("album_tile_${entry.entry.id}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = entry.entry.emoji,
            fontSize = 27.sp,
            modifier = Modifier.alpha(if (collected) 1f else 0.2f)
        )
        if (!collected) {
            Text(
                text = "🔒",
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 3.dp)
                    .alpha(0.75f)
            )
        }
    }
}

@Composable
private fun AlbumEntryDialog(
    entry: AlbumEntryProgress,
    isDark: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(entry.entry.emoji, fontSize = 46.sp) },
        title = {
            Text(
                text = if (entry.isCollected) entry.entry.name else stringResource(R.string.album_locked),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (entry.isCollected) {
                    Text(
                        text = entry.entry.category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldStar
                    )
                    Text(
                        text = stringResource(R.string.album_claimed),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.album_locked_body),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (entry.isCollected) stringResource(R.string.action_nice) else stringResource(R.string.action_got_it),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
