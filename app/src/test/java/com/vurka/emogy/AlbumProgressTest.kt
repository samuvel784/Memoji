package com.vurka.emogy

import com.vurka.emogy.data.AlbumProgressCalculator
import com.vurka.emogy.model.EmojiEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumProgressTest {

    private val catalog = listOf(
        EmojiEntry("🐶", "🐶", "Animals", "Dog"),
        EmojiEntry("🐱", "🐱", "Animals", "Cat"),
        EmojiEntry("🍕", "🍕", "Food", "Pizza"),
    )

    @Test
    fun emptyCollectionStartsWithEverythingLocked() {
        val album = AlbumProgressCalculator.calculate(catalog, emptySet())

        assertEquals(0, album.collected)
        assertEquals(3, album.total)
        assertEquals(0f, album.fraction, 0.0001f)
        assertFalse(album.isComplete)
        assertEquals(listOf("Animals", "Food"), album.categories.map { it.name })
        assertTrue(album.categories.flatMap { it.entries }.none { it.isCollected })
    }

    @Test
    fun collectedEmojiAreCountedPerCategoryAndOverall() {
        val album = AlbumProgressCalculator.calculate(catalog, setOf("🐶", "🍕"))

        assertEquals(2, album.collected)
        assertEquals(3, album.total)
        assertEquals(2f / 3f, album.fraction, 0.0001f)
        assertFalse(album.isComplete)

        val animals = album.categories.first { it.name == "Animals" }
        assertEquals(1, animals.collected)
        assertEquals(2, animals.total)
        assertEquals(0.5f, animals.fraction, 0.0001f)
        assertFalse(animals.isComplete)

        val food = album.categories.first { it.name == "Food" }
        assertEquals(1, food.collected)
        assertEquals(1, food.total)
        assertTrue(food.isComplete)
    }

    @Test
    fun unknownOrRetiredIdsDoNotInflateTheAlbum() {
        val album = AlbumProgressCalculator.calculate(
            catalog = catalog,
            collectedIds = setOf("🦄", "", "🐶"),
        )

        assertEquals(1, album.collected)
        assertEquals(3, album.total)
        assertEquals("only catalogue entries count", 1, album.categories.sumOf { it.collected })
    }

    @Test
    fun catalogueOrderIsPreservedForStableSections() {
        val shuffled = listOf(catalog[2], catalog[0], catalog[1])
        val album = AlbumProgressCalculator.calculate(shuffled, emptySet())

        assertEquals(listOf("Food", "Animals"), album.categories.map { it.name })
        assertEquals(listOf("🍕"), album.categories[0].entries.map { it.entry.id })
        assertEquals(listOf("🐶", "🐱"), album.categories[1].entries.map { it.entry.id })
    }

    @Test
    fun completeAlbumIsReportedAsComplete() {
        val album = AlbumProgressCalculator.calculate(catalog, catalog.mapTo(HashSet()) { it.id })

        assertEquals(3, album.collected)
        assertEquals(1f, album.fraction, 0.0001f)
        assertTrue(album.isComplete)
        assertTrue(album.categories.all { it.isComplete })
    }

    @Test
    fun emptyCatalogueCannotDivideByZero() {
        val album = AlbumProgressCalculator.calculate(emptyList(), setOf("🐶"))

        assertEquals(0, album.collected)
        assertEquals(0, album.total)
        assertEquals(0f, album.fraction, 0.0001f)
        assertFalse(album.isComplete)
    }
}
