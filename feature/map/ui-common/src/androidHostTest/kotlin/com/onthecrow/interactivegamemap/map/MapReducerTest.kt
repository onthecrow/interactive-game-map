package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapMarkerCategory
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup
import com.onthecrow.interactivegamemap.map.model.MarkerCategory
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.map.model.MarkerPin
import com.onthecrow.interactivegamemap.network.GameMap
import com.onthecrow.interactivegamemap.network.MapBounds
import com.onthecrow.interactivegamemap.network.MapCamera
import com.onthecrow.interactivegamemap.network.MapDungeon
import com.onthecrow.interactivegamemap.network.MapFloor
import com.onthecrow.interactivegamemap.network.MapTiles
import com.onthecrow.interactivegamemap.network.TileScheme
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapReducerTest {
    private val reducer = MapReducer()

    private fun floor(id: Int, lng: Double) = MapFloor(
        id = id,
        name = "B$id",
        imageUrl = "",
        bounds = MapBounds(west = lng, south = 0.0, east = lng + 1.0, north = 1.0),
    )

    // Ten dungeons in a row with two floors each, the second floor is never displayed by the button
    private val map = GameMap(
        id = 1,
        name = "Map",
        minZoom = 1,
        maxZoom = 2,
        tiles = MapTiles("", TileScheme.XYZ),
        camera = MapCamera(0.0, 0.0, 1),
        dungeonBaseOpacity = 0.5f,
        dungeons = (0 until 10).map { i ->
            MapDungeon(id = i, floors = listOf(floor(i * 2, i * 10.0), floor(i * 2 + 1, i * 10.0)))
        },
    )
    private val world = MapBounds(west = -10.0, south = -10.0, east = 200.0, north = 10.0)
    private val location = MapLocation.EMPTY.copy(id = 7, dungeon = LocationDungeon(3, 7, emptyList()))
    private val state = MapScreenState(currentMap = map, viewport = world)

    private fun MapScreenState.after(event: MapEvent) = runBlocking { reducer.reduce(this@after, event) }

    @Test
    fun `button shows the first floors around the center of the screen`() {
        val floors = state.after(MapEvent.OnDungeonsClick).displayedFloors

        assertEquals(8, floors.size)
        assertTrue(floors.all { it.id % 2 == 0 })
    }

    @Test
    fun `floors outside the screen are not displayed`() {
        val floors = state
            .after(MapEvent.OnDungeonsClick)
            .after(MapEvent.OnCameraIdle(MapBounds(west = 15.0, south = 0.0, east = 35.0, north = 1.0)))
            .displayedFloors

        assertEquals(listOf(4, 6), floors.map { it.id }.sorted())
    }

    @Test
    fun `selected marker shows only its floor`() {
        val selected = state
            .after(MapEvent.OnDungeonsClick)
            .after(MapEvent.OnLocationClick(location))

        assertEquals(listOf(7), selected.displayedFloors.map { it.id })
        assertEquals(8, selected.after(MapEvent.OnLocationDismiss).displayedFloors.size)
    }

    @Test
    fun `button turns off everything that is displayed`() {
        val off = state.after(MapEvent.OnDungeonsClick).after(MapEvent.OnDungeonsClick)
        assertFalse(off.isDungeonsActive)
        assertTrue(off.displayedFloors.isEmpty())

        val floorClosed = state.after(MapEvent.OnLocationClick(location)).after(MapEvent.OnDungeonsClick)
        assertFalse(floorClosed.isDungeonsActive)
        assertTrue(floorClosed.displayedFloors.isEmpty())
    }

    private fun category(id: String, name: String): MapMarkerCategory {
        val pin = MarkerPin(icon = MarkerIcon(id), width = 28, height = 28)
        return MapMarkerCategory(
            category = MarkerCategory(id, name, MarkerIcon(id), pin, visibleByDefault = true),
            count = 1,
            countByCycle = emptyMap(),
        )
    }

    private val groups = listOf(
        MapMarkerGroup("chest", "Chests", null, listOf(category("1", "Common Chest"), category("2", "Oculus"))),
        MapMarkerGroup("featured", "Featured", null, listOf(category("1", "Common Chest"), category("3", "Waypoint"))),
    )

    @Test
    fun `search finds a category once and ignores the case`() {
        val searching = state.copy(markerGroups = groups).after(MapEvent.OnMarkerListQueryChange(" chest "))

        assertTrue(searching.isMarkerListSearching)
        assertEquals(listOf("1"), searching.foundCategories.map { it.category.id })
    }

    @Test
    fun `search is cleared with the marker list`() {
        val closed = state
            .after(MapEvent.OnMarkerListQueryChange("chest"))
            .after(MapEvent.OnMarkerListDismiss)

        assertFalse(closed.isMarkerListSearching)
    }

    @Test
    fun `groups are collapsed and expanded`() {
        val loaded = state.after(MapEvent.OnMarkerListSettingsLoaded(MarkerListSettings(setOf("chest"))))
        assertTrue(loaded.isGroupCollapsed("chest"))
        assertFalse(loaded.isGroupCollapsed("featured"))

        val toggled = loaded.after(MapEvent.OnGroupClick("chest")).after(MapEvent.OnGroupClick("featured"))
        assertEquals(setOf("featured"), toggled.collapsedGroupIds)
    }

    @Test
    fun `selected markers are restored and saved`() {
        val saved = MarkerListSettings(
            collapsedGroupIds = setOf("chest"),
            categoryVisibility = mapOf("1" to false),
            cycles = mapOf("2" to 4),
        )
        val loaded = state.copy(markerGroups = groups).after(MapEvent.OnMarkerListSettingsLoaded(saved))

        assertFalse(loaded.isCategoryVisible(groups.first().categories.first().category))
        assertEquals(4, loaded.selectedCycle("2"))
        assertEquals(mapOf("1" to false), loaded.locationsFilter?.categoryVisibility)
        assertEquals(saved, loaded.markerListSettings)

        val changed = loaded.after(MapEvent.OnCategoryClick("3")).after(MapEvent.OnCycleClick("2", 5))
        assertEquals(mapOf("1" to false, "3" to false), changed.markerListSettings.categoryVisibility)
        assertEquals(mapOf("2" to 5), changed.markerListSettings.cycles)
    }

    @Test
    fun `preferences keep the settings and survive broken data`() {
        val values = mutableMapOf<String, String>()
        val preferences = MapPreferences(object : PreferencesStorage {
            override fun getString(key: String) = values[key]
            override fun putString(key: String, value: String) {
                values[key] = value
            }
        })
        val settings = MarkerListSettings(setOf("chest"), mapOf("o3" to false, "o5" to true), mapOf("554" to 3))

        assertEquals(MarkerListSettings(), preferences.getMarkerListSettings())
        preferences.setMarkerListSettings(settings)
        assertEquals(settings, preferences.getMarkerListSettings())

        values.replaceAll { _, _ -> "not json" }
        assertEquals(MarkerListSettings(), preferences.getMarkerListSettings())
    }

    @Test
    fun `pin image depends on the dungeon`() {
        val plainIcon = MarkerIcon("plain")
        val dungeonIcon = MarkerIcon("dungeon")
        val onSurface = MapLocation.EMPTY.copy(pin = MarkerPin(plainIcon, dungeonIcon, width = 28, height = 28))
        val inDungeon = onSurface.copy(dungeon = LocationDungeon(1, 1, emptyList()))
        val withoutImage = inDungeon.copy(pin = MarkerPin(plainIcon, width = 33, height = 44))

        assertEquals(MarkerPinImage(plainIcon, false, 28, 28), onSurface.pinImage)
        assertEquals(MarkerPinImage(dungeonIcon, false, 28, 28), inDungeon.pinImage)
        assertEquals(MarkerPinImage(plainIcon, true, 33, 44), withoutImage.pinImage)
    }
}
