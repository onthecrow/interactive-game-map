package com.onthecrow.interactivegamemap.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class FoundLocationsTransferTest {
    private class FakeRepository(initial: Set<Int>) : FoundLocationsRepository {
        val found = MutableStateFlow(initial)
        override fun getFoundLocationIds(): StateFlow<Set<Int>> = found
        override suspend fun setLocationFound(locationId: Int, isFound: Boolean) {
            found.value = if (isFound) found.value + locationId else found.value - locationId
        }
        override suspend fun addFoundLocations(locationIds: Collection<Int>): Int {
            val added = locationIds.toSet() - found.value
            found.value = found.value + added
            return added.size
        }
    }

    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-20T10:15:30Z")
    }

    @Test
    fun `an export is read back by an import into another app of the same game`() = runBlocking {
        val export = ExportFoundLocationsUseCaseImpl(FakeRepository(setOf(5, 1, 3)), GameId("rdr2"), clock)()
        assertEquals("rdr2-found-markers-2026-11-20.json", export.fileName)
        assertEquals(3, export.count)

        val other = FakeRepository(setOf(3, 9))
        val result = ImportFoundLocationsUseCaseImpl(other, GameId("rdr2"))(export.content)
        assertEquals(FoundLocationsImport.Done(added = 2, total = 3), result)
        assertEquals(setOf(1, 3, 5, 9), other.found.value)
    }

    @Test
    fun `a file of another game or not a file of found markers adds nothing`() = runBlocking {
        val export = ExportFoundLocationsUseCaseImpl(FakeRepository(setOf(1)), GameId("genshin"), clock)()
        val repository = FakeRepository(emptySet())
        val import = ImportFoundLocationsUseCaseImpl(repository, GameId("rdr2"))

        assertEquals(FoundLocationsImport.OtherGame("genshin"), import(export.content))
        assertIs<FoundLocationsImport.NotAFile>(import(""))
        assertIs<FoundLocationsImport.NotAFile>(import("""{"found":[1,2]}"""))
        assertIs<FoundLocationsImport.NotAFile>(import("""{"format":"other","game":"rdr2","exported_at":"","found":[1]}"""))
        val newer = """{"format":"interactive-game-map/found-markers","version":2,"game":"rdr2","exported_at":"","found":[1]}"""
        assertIs<FoundLocationsImport.NotAFile>(import(newer))
        assertEquals(emptySet(), repository.found.value)
    }

    /**
     * Files of version 1 as the apps write them since 2026-11-20, and as a user may get them back: reformatted by
     * an editor, with fields a later version adds. They must import as long as the apps exist: never edit them.
     */
    private val goldenFiles = listOf(
        """{"format":"interactive-game-map/found-markers","version":1,"game":"rdr2",""" +
            """"exported_at":"2026-11-20T00:37:55.511893Z","found":[54,56,62,63,71,451435]}""",
        """
        {
          "format": "interactive-game-map/found-markers",
          "version": 1,
          "game": "rdr2",
          "exported_at": "2026-11-20T00:37:55Z",
          "found": [451435, 54, 56, 62, 63, 71, 54],
          "note": "a field of a later version"
        }
        """.trimIndent(),
    )

    @Test
    fun `files of every version ever exported still import`() = runBlocking {
        goldenFiles.forEach { file ->
            val repository = FakeRepository(setOf(54))
            val result = ImportFoundLocationsUseCaseImpl(repository, GameId("rdr2"))(file)
            assertEquals(FoundLocationsImport.Done(added = 5, total = 6), result, file)
            assertEquals(setOf(54, 56, 62, 63, 71, 451435), repository.found.value)
        }
    }

    @Test
    fun `the file is stable JSON other apps and versions can read`() = runBlocking {
        val export = ExportFoundLocationsUseCaseImpl(FakeRepository(setOf(2, 1)), GameId("rdr2"), clock)()
        assertEquals(
            """{"format":"interactive-game-map/found-markers","version":1,"game":"rdr2",""" +
                """"exported_at":"2026-11-20T10:15:30Z","found":[1,2]}""",
            export.content,
        )
    }
}
