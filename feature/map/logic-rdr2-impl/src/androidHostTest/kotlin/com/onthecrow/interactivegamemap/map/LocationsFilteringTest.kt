package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.mapper.toMapLocation
import com.onthecrow.interactivegamemap.map.mapper.withIconUrls
import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.LocationBounds
import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationsFilteringTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val catalog: MarkerCatalog = json.decodeFromString(read("marker_catalog.json"))
    private val locations = json.decodeFromString<List<Location>>(read("markers.json")).map { it.toMapLocation(catalog) }

    private val repository = object : LocationsRepository {
        override suspend fun getLocations(language: String): List<MapLocation> = locations
        override suspend fun getMarkerCatalog(language: String): MarkerCatalog = catalog
    }
    private val getLocations = GetLocationsUseCaseImpl(repository)
    private val getMarkerGroups = GetMarkerGroupsUseCaseImpl(repository)

    private fun read(name: String) = File("src/commonMain/composeResources/files/$name").readText()

    private fun onlyCategory(id: String) = catalog.categories.keys.associateWith { it == id }

    @Test
    fun `decodes every marker with a known category`() {
        assertEquals(6155, locations.size)
        assertTrue(locations.all { it.mapId == 1 && it.categoryId in catalog.categories })
    }

    @Test
    fun `every marker has a pin of the app with the pictogram of its category`() {
        assertTrue(
            locations.all {
                it.pin?.icon?.url?.startsWith("files/pins/") == true &&
                    it.pin?.pictogram?.url?.startsWith("files/pictograms/") == true &&
                    it.pin?.dungeonIcon == null
            }
        )
        assertEquals(30, locations.first().pin?.width)
        assertEquals(30, locations.first().pin?.height)
    }

    @Test
    fun `the icons of every category are files of the app, the list shows the pictogram`() {
        catalog.categories.values.forEach { category ->
            assertEquals(category.pin.pictogram, category.icon, category.name)
            val icons = listOfNotNull(category.icon, category.pin.icon, category.pin.pictogram)
            (icons + icons.mapNotNull { it.wear?.texture }).forEach {
                assertTrue(File("src/commonMain/composeResources/${it.url}").isFile, it.url)
            }
            // As on the map of the game: the worn pictogram on a whole disc, the location at its middle
            assertTrue(category.icon.wear != null && category.pin.icon.wear == null, category.name)
            assertEquals("files/pins/disc.xml", category.pin.icon.url)
            assertEquals(0.5f, category.pin.anchorY)
        }
        // Nothing is downloaded from the source of the markers
        assertTrue(read("marker_catalog.json").contains("http").not())
    }

    @Test
    fun `the files of the icons become data URLs, read once for every file`() = runBlocking {
        val reads = mutableListOf<String>()
        val result = catalog.withIconUrls { url -> reads += url; "data:$url" }

        assertEquals(reads.distinct(), reads)
        assertTrue(result.categories.values.all { it.icon.url.startsWith("data:files/") && it.pin.pictogram?.url == it.icon.url })
        assertEquals("data:files/pins/disc.xml", result.categories.getValue("33").pin.icon.url)
        assertEquals("data:files/textures/grunge.png", result.categories.getValue("33").pin.pictogram?.wear?.texture?.url)
    }

    @Test
    fun `markers on interior images open them`() {
        val inInteriors = locations.filter { it.dungeon != null }
        val byId = locations.associateBy { it.id }

        assertEquals(49, inInteriors.size)
        assertTrue(inInteriors.all { it.dungeon?.dungeonId in 1..21 && it.dungeon?.floorId == it.dungeon?.dungeonId })
        // The saloon of Valentine
        assertEquals(LocationDungeon(21, 21, emptyList()), byId.getValue(168).dungeon)
        // "On the surface."
        assertNull(byId.getValue(450917).dungeon)
    }

    @Test
    fun `hidden by default categories are not returned`() = runBlocking {
        val result = getLocations(LocationsFilter(mapId = 1, bounds = LocationBounds(-180.0, -90.0, 180.0, 90.0)))

        assertEquals(1000, result.size)
        assertTrue(result.none { it.categoryId == PLANT })
    }

    @Test
    fun `only the selected cycle is returned`() = runBlocking {
        val visibility = onlyCategory(AMERICAN_FLOWER)

        val byDefault = getLocations(LocationsFilter(mapId = 1, categoryVisibility = visibility))
        val selected = getLocations(
            LocationsFilter(mapId = 1, categoryVisibility = visibility, cycles = mapOf(AMERICAN_FLOWER to 3))
        )

        assertEquals(54, byDefault.size)
        assertTrue(byDefault.all { it.cycle == LocationsFilter.DEFAULT_CYCLE })
        assertEquals(54, selected.size)
        assertTrue(selected.all { it.cycle == 3 })
    }

    @Test
    fun `search ignores visibility and cycles`() = runBlocking {
        val result = getLocations(
            LocationsFilter(mapId = 1, query = "agarita", categoryVisibility = onlyCategory(PLANT))
        )

        assertEquals(setOf(1, 2, 3, 4, 5, 6), result.mapNotNull { it.cycle }.toSet())
    }

    @Test
    fun `only markers inside the bounds are returned`() = runBlocking {
        val bounds = LocationBounds(west = 5.4, south = 24.8, east = 9.0, north = 31.3)

        val result = getLocations(LocationsFilter(mapId = 1, bounds = bounds))

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { bounds.contains(it.lat, it.lng) })
    }

    @Test
    fun `groups have counts for every cycle and skip empty categories`() = runBlocking {
        val groups = getMarkerGroups(1, LocationsFilter.DEFAULT_LANGUAGE)
        val nazarItems = groups.single { it.name == "Madam Nazar's Items" }
        val flower = nazarItems.categories.single { it.category.id == AMERICAN_FLOWER }

        assertEquals(8, groups.size)
        assertEquals(324, flower.count)
        assertEquals((1..6).associateWith { 54 }, flower.countByCycle)
        assertEquals(5, nazarItems.categories.size)
        assertTrue(groups.flatMap { it.categories }.all { it.count > 0 })
    }

    private companion object {
        private const val PLANT = "43"
        private const val AMERICAN_FLOWER = "554"
    }
}
