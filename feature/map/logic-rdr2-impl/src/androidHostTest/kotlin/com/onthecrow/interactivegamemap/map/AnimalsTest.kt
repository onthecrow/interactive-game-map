package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.mapper.ANIMAL_ID_OFFSET
import com.onthecrow.interactivegamemap.map.mapper.MAX_ANIMAL_POINTS
import com.onthecrow.interactivegamemap.map.mapper.categoryId
import com.onthecrow.interactivegamemap.map.mapper.toMapLocation
import com.onthecrow.interactivegamemap.map.mapper.toMapLocations
import com.onthecrow.interactivegamemap.map.mapper.withAnimals
import com.onthecrow.interactivegamemap.map.mapper.withIconUrls
import com.onthecrow.interactivegamemap.map.model.AnimalCompendium
import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.LocationArea
import com.onthecrow.interactivegamemap.map.model.LocationBounds
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

class AnimalsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val animals = json.decodeFromString<AnimalCompendium>(read("animals.json")).animals
    private val catalog = json.decodeFromString<MarkerCatalog>(read("marker_catalog.json")).withAnimals(animals)
    private val markers = json.decodeFromString<List<Location>>(read("markers.json")).map { it.toMapLocation(catalog) }
    private val animalLocations = animals.toMapLocations(catalog)

    private val repository = object : LocationsRepository {
        override suspend fun getLocations(language: String): List<MapLocation> = markers + animalLocations
        override suspend fun getMarkerCatalog(language: String): MarkerCatalog = catalog
    }

    private fun read(name: String) = File("src/commonMain/composeResources/files/$name").readText()

    @Test
    fun `every animal of the world map but the legendary ones has a marker`() {
        val expected = animals.filter { it.map == "world" && !it.legendary }

        assertEquals(178, animals.size)
        assertEquals(154, animalLocations.size)
        assertEquals(expected.map { it.name }, animalLocations.map { it.name })
        assertTrue(
            animalLocations.all {
                it.mapId == 1 && it.categoryId.startsWith("animal-") && it.pin?.pictogram?.url == PAW &&
                    it.description.startsWith("**")
            }
        )
    }

    @Test
    fun `an animal is the light paw on the disc of the legendary animals`() {
        val legendary = catalog.categories.getValue("37")
        val pin = animalLocations.first().pin!!

        assertTrue(animalLocations.all { it.pin == pin })
        // The worn paw on the disc, as the lists show it
        assertEquals(legendary.pin.copy(pictogram = legendary.icon.copy(url = PAW)), pin)
        assertEquals(pin.pictogram, catalog.categories.getValue(animals.first { !it.legendary }.categoryId).icon)
        assertEquals(0.5f, pin.anchorY)
    }

    @Test
    fun `the paw of an animal becomes a data URL as well`() = runBlocking {
        val animal = catalog.withIconUrls { url -> "data:$url" }.categories.getValue(animals.first { !it.legendary }.categoryId)

        assertEquals("data:$PAW", animal.pin.pictogram?.url)
        assertEquals("data:files/textures/grunge.png", animal.pin.pictogram?.wear?.texture?.url)
    }

    @Test
    fun `ids follow the compendium and don't clash with the source's markers`() {
        val ids = animalLocations.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.none { id -> markers.any { it.id == id } })
        assertEquals(ANIMAL_ID_OFFSET + 63 * MAX_ANIMAL_POINTS, animalLocations.single { it.name == "American Gray Fox" }.id)
    }

    @Test
    fun `the zones of an animal are its areas around the marker`() {
        val grayFox = animalLocations.single { it.name == "American Gray Fox" }
        val outline = grayFox.areas.single().outline

        assertEquals(104, animalLocations.count { it.areas.isNotEmpty() })
        assertEquals(36, outline.size)
        // In Cumberland Forest, around the marker
        assertTrue(grayFox.lat in outline.minOf { it.lat }..outline.maxOf { it.lat })
        assertTrue(grayFox.lng in outline.minOf { it.lng }..outline.maxOf { it.lng })
    }

    @Test
    fun `every animal is a category of the animals' group, in the order of the guide`() = runBlocking {
        val groups = GetMarkerGroupsUseCaseImpl(repository)(1, LocationsFilter.DEFAULT_LANGUAGE)
        val animalGroup = groups.last()
        val names = animalGroup.categories.map { it.category.name }

        assertEquals(9, groups.size)
        assertEquals("Animals", animalGroup.name)
        assertNull(animalGroup.description)
        assertEquals(154, animalGroup.categories.size)
        assertTrue(
            animalGroup.categories.all { it.count == 1 && !it.category.visibleByDefault && !it.category.showsCount }
        )
        assertEquals(listOf("American Alligator", "American Alligator (small)", "Nine-Banded Armadillo"), names.take(3))
        assertEquals("Pileated Woodpecker", names.last())
        assertTrue(names.indexOf("American Red Fox") < names.indexOf("American Gray Fox"))
    }

    @Test
    fun `the animals are hidden by default and found by a search`() = runBlocking {
        val getLocations = GetLocationsUseCaseImpl(repository)
        val world = LocationBounds(-180.0, -90.0, 180.0, 90.0)

        val byDefault = getLocations(LocationsFilter(mapId = 1, bounds = world, limit = Int.MAX_VALUE))
        val search = getLocations(LocationsFilter(mapId = 1, query = "gray fox"))

        assertTrue(byDefault.none { it.categoryId.startsWith("animal-") })
        assertEquals(listOf("American Gray Fox"), search.map { it.name })
    }

    @Test
    fun `an animal is shown on its own`() = runBlocking {
        val grayFox = animals.single { it.name == "American Gray Fox" }

        val result = GetLocationsUseCaseImpl(repository)(
            LocationsFilter(mapId = 1, categoryVisibility = catalog.categories.keys.associateWith { it == grayFox.categoryId })
        )

        assertEquals(listOf("American Gray Fox"), result.map { it.name })
    }

    @Test
    fun `no zone of an animal lies inside another one`() {
        animalLocations.forEach { location ->
            location.areas.forEachIndexed { i, area ->
                location.areas.forEachIndexed { j, other ->
                    val inside = area.outline.count { other.contains(it) }
                    assertTrue(i == j || inside < area.outline.size * 0.8, "${location.name}: zone $i is inside zone $j")
                }
            }
        }
        // Two small circles side by side and an ellipse, not one ellipse over both circles
        assertEquals(3, animalLocations.single { it.name == "Desert Bighorn Sheep" }.areas.size)
    }

    private fun LocationArea.contains(point: GeoPoint): Boolean {
        var inside = false
        outline.indices.forEach { i ->
            val a = outline[i]
            val b = outline[(i + 1) % outline.size]
            if ((a.lat > point.lat) != (b.lat > point.lat) &&
                point.lng < (b.lng - a.lng) * (point.lat - a.lat) / (b.lat - a.lat) + a.lng
            ) {
                inside = !inside
            }
        }
        return inside
    }

    private companion object {
        private const val PAW = "files/pictograms/animal.xml"
    }
}
