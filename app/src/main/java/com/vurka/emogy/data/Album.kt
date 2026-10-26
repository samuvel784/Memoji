package com.vurka.emogy.data

import com.vurka.emogy.model.EmojiEntry

/** One album tile: the catalogue entry plus whether the player has matched it. */
data class AlbumEntryProgress(
    val entry: EmojiEntry,
    val isCollected: Boolean,
)

/** The album section for one emoji category, in catalogue order. */
data class AlbumCategoryProgress(
    val name: String,
    val entries: List<AlbumEntryProgress>,
) {
    val collected: Int = entries.count { it.isCollected }
    val total: Int = entries.size
    val fraction: Float = if (total == 0) 0f else collected.toFloat() / total
    val isComplete: Boolean = total > 0 && collected == total
}

/** Whole-album progress, already filtered to entries the device can draw. */
data class AlbumProgress(
    val categories: List<AlbumCategoryProgress>,
    val collected: Int,
    val total: Int,
) {
    val fraction: Float = if (total == 0) 0f else collected.toFloat() / total
    val isComplete: Boolean = total > 0 && collected == total
}

/**
 * Pure album maths, kept away from Compose and DataStore.
 *
 * Unknown ids in [collectedIds] are ignored, so a retired or unsupported emoji can never inflate
 * the counter. Category order follows the catalogue, which keeps the album stable between builds.
 */
object AlbumProgressCalculator {

    fun calculate(
        catalog: List<EmojiEntry>,
        collectedIds: Set<String>,
    ): AlbumProgress {
        val buckets = LinkedHashMap<String, MutableList<AlbumEntryProgress>>()
        catalog.distinctBy { it.id }.forEach { entry ->
            buckets.getOrPut(entry.category) { mutableListOf() } +=
                AlbumEntryProgress(entry = entry, isCollected = entry.id in collectedIds)
        }

        val categories = buckets.map { (name, entries) ->
            AlbumCategoryProgress(name = name, entries = entries)
        }

        return AlbumProgress(
            categories = categories,
            collected = categories.sumOf { it.collected },
            total = categories.sumOf { it.total },
        )
    }
}
