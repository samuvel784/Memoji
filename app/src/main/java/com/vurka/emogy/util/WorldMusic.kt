package com.vurka.emogy.util

import com.vurka.emogy.data.LevelCatalog

/**
 * Ambient music families. Each group covers five of the twenty worlds, so moving from one region
 * of the map to the next changes the mood without needing twenty separate tracks.
 */
enum class MusicGroup(val id: String, val displayName: String) {
    ORGANIC("organic", "Organic"),
    FESTIVE("festive", "Festive"),
    AIRY("airy", "Airy"),
    COSMIC("cosmic", "Cosmic"),
}

/** Maps the 20 authored worlds onto the four ambient tracks. Pure, so the mapping is testable. */
object WorldMusic {

    fun groupForWorld(worldIndex: Int): MusicGroup {
        val safe = worldIndex.coerceIn(1, LevelCatalog.WORLD_COUNT)
        return when (safe) {
            in 1..5 -> MusicGroup.ORGANIC
            in 6..10 -> MusicGroup.FESTIVE
            in 11..15 -> MusicGroup.AIRY
            else -> MusicGroup.COSMIC
        }
    }

    fun groupForLevel(level: Int): MusicGroup {
        val safe = level.coerceIn(1, LevelCatalog.TOTAL_LEVELS)
        val world = (safe - 1) / LevelCatalog.LEVELS_PER_WORLD + 1
        return groupForWorld(world)
    }
}
