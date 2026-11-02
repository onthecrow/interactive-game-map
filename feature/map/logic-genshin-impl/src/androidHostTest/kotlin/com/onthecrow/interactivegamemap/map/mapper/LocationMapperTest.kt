package com.onthecrow.interactivegamemap.map.mapper

import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.LocationResponse
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerLocale
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationMapperTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val catalog: MarkerCatalog = json.decodeFromString(read("marker_catalog.json"))
    private val locations = json.decodeFromString<LocationResponse>(read("markers.json")).toMapLocations(catalog)

    private fun read(name: String) = File("src/commonMain/composeResources/files/$name").readText()

    @Test
    fun `decodes every marker with unique ids`() {
        assertEquals(87682, locations.size)
        assertEquals(locations.size, locations.map { it.id }.toSet().size)
    }

    @Test
    fun `parses dungeon meta`() {
        val byId = locations.associateBy { it.id }

        assertEquals(LocationDungeon(82, 5, listOf(50594, 48559)), byId.getValue(18596).dungeon)
        // "l=<entrance>|<dungeon>|" has no floor
        assertEquals(LocationDungeon(45, null, listOf(35795)), byId.getValue(38318).dungeon)
        // "i" only marks a marker with an image
        assertNull(byId.getValue(15130).dungeon)
    }

    @Test
    fun `every marker has a catalog category and a name`() {
        assertTrue(locations.all { it.categoryId in catalog.categories && it.name.isNotEmpty() })
    }

    @Test
    fun `every marker has a pin with an image for dungeons`() {
        val pins = catalog.categories.mapValues { it.value.pin }

        assertTrue(locations.all { it.pin === pins[it.categoryId] })
        assertTrue(pins.all { (id, pin) -> pin.dungeonIcon?.url == "$ICONS/$id-layer.png?v=3" })
        // Teleport Waypoint is a plain type and a large one, Common Chest isn't plain
        assertEquals("$ICONS/o3.png?v=3", pins.getValue("o3").icon.url)
        assertEquals(34, pins.getValue("o3").width)
        assertEquals("$ICONS/o64-pin.png?v=3", pins.getValue("o64").icon.url)
    }

    @Test
    fun `markers visible by default go first`() {
        val visibleByDefault = locations.map { catalog.categories.getValue(it.categoryId).visibleByDefault }
        assertEquals(visibleByDefault.sortedDescending(), visibleByDefault)
    }

    @Test
    fun `groups of every map reference known categories`() {
        assertEquals(setOf(2, 7, 9, 34, 36, 37, 40), catalog.groupsByMap.keys)
        val categoryIds = catalog.groupsByMap.values.flatten().flatMap { it.categoryIds }
        assertTrue(categoryIds.all { it in catalog.categories })
    }

    private companion object {
        private const val ICONS = "https://game-cdn.appsample.com/gim/markers"
    }

    @Test
    fun `locales name every category, group and map`() {
        val groupIds = catalog.groupsByMap.values.flatten().map { it.id }.toSet()
        val locales = File("src/commonMain/composeResources/files/locales").listFiles().orEmpty()

        assertEquals(setOf("de", "es", "fr", "ja", "ko", "pt", "ru", "zh"), locales.map { it.nameWithoutExtension }.toSet())
        locales.forEach { file ->
            val locale = json.decodeFromString<MarkerLocale>(file.readText())
            val localized = catalog.localized(locale)

            assertEquals(catalog.categories.keys, locale.categories.keys, file.name)
            assertEquals(groupIds, locale.groups.keys, file.name)
            assertEquals(catalog.groupsByMap.keys, localized.mapNames.keys, file.name)
            assertEquals(locale.categories.getValue("o3"), localized.categories.getValue("o3").name, file.name)
        }
    }
}
