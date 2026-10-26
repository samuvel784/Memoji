package com.vurka.emogy

import com.vurka.emogy.data.LevelCatalog
import com.vurka.emogy.util.MusicGroup
import com.vurka.emogy.util.WorldMusic
import org.junit.Assert.assertEquals
import org.junit.Test

class WorldMusicTest {

    @Test
    fun everyWorldMapsToItsFamily() {
        for (world in 1..LevelCatalog.WORLD_COUNT) {
            val expected = when (world) {
                in 1..5 -> MusicGroup.ORGANIC
                in 6..10 -> MusicGroup.FESTIVE
                in 11..15 -> MusicGroup.AIRY
                else -> MusicGroup.COSMIC
            }
            assertEquals("world $world", expected, WorldMusic.groupForWorld(world))
        }
    }

    @Test
    fun familyBoundariesAreWhereTheDesignSays() {
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForWorld(5))
        assertEquals(MusicGroup.FESTIVE, WorldMusic.groupForWorld(6))
        assertEquals(MusicGroup.FESTIVE, WorldMusic.groupForWorld(10))
        assertEquals(MusicGroup.AIRY, WorldMusic.groupForWorld(11))
        assertEquals(MusicGroup.AIRY, WorldMusic.groupForWorld(15))
        assertEquals(MusicGroup.COSMIC, WorldMusic.groupForWorld(16))
    }

    @Test
    fun levelsMapThroughTheirWorld() {
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForLevel(1))
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForLevel(50))
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForLevel(51)) // world 2, still organic
        // Level 201 is the first level of world 5, still organic; 251 starts world 6.
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForLevel(201))
        assertEquals(MusicGroup.FESTIVE, WorldMusic.groupForLevel(251))
        assertEquals(MusicGroup.AIRY, WorldMusic.groupForLevel(501))
        assertEquals(MusicGroup.COSMIC, WorldMusic.groupForLevel(751))
        assertEquals(MusicGroup.COSMIC, WorldMusic.groupForLevel(LevelCatalog.TOTAL_LEVELS))
    }

    @Test
    fun outOfRangeValuesAreClampedInsteadOfCrashing() {
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForWorld(-10))
        assertEquals(MusicGroup.COSMIC, WorldMusic.groupForWorld(999))
        assertEquals(MusicGroup.ORGANIC, WorldMusic.groupForLevel(-4))
        assertEquals(MusicGroup.COSMIC, WorldMusic.groupForLevel(99_999))
    }
}
