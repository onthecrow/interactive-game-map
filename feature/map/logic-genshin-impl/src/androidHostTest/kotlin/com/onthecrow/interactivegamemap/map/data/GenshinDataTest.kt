package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.hoyolab.ChangelogCountDto
import com.onthecrow.interactivegamemap.map.hoyolab.ChangelogEntryDto
import com.onthecrow.interactivegamemap.map.hoyolab.FloorDto
import com.onthecrow.interactivegamemap.map.hoyolab.HOYOLAB_TILE_SCHEME
import com.onthecrow.interactivegamemap.map.hoyolab.LabelDto
import com.onthecrow.interactivegamemap.map.hoyolab.MapDetailDto
import com.onthecrow.interactivegamemap.map.hoyolab.MapInfoDto
import com.onthecrow.interactivegamemap.map.hoyolab.OverlayDto
import com.onthecrow.interactivegamemap.map.hoyolab.PointDto
import com.onthecrow.interactivegamemap.map.hoyolab.PointGroupRefDto
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.MapCamera
import com.onthecrow.interactivegamemap.map.model.MapTiles
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenshinDataTest {
    private val detail = MapDetailDto(
        totalSize = listOf(4096, 4096),
        padding = listOf(0, 0),
        origin = listOf(2048, 2048),
        mapVersion = "abc",
        minZoom = -4,
        maxZoom = 0,
        originalSize = listOf(4096, 4096),
    )
    private val labels = SnapshotLabels(
        language = "en",
        groups = listOf(
            LabelDto(
                id = 1,
                name = "Waypoints",
                children = listOf(
                    LabelDto(id = 3, name = "Teleport Waypoint", icon = "https://icons/3.png"),
                    LabelDto(id = 154, name = "Domain", icon = "https://icons/154.png"),
                ),
            ),
            LabelDto(id = 13, name = "Chests", children = listOf(LabelDto(id = 20, name = "Common Chest"))),
        ),
        mapNames = mapOf(2 to "Teyvat"),
    )
    private val floor = FloorDto(
        id = 372,
        name = "Abandoned Station_B1",
        overlay = OverlayDto(url = "https://overlay.png", left = -100.0, top = -50.0, right = 100.0, bottom = 50.0),
        entranceIds = listOf(11),
    )
    private val snapshot = GenshinSnapshot(
        version = "v",
        maps = listOf(SnapshotMap(2, detail)),
        points = snapshotPoints(
            mapOf(
                2 to listOf(
                    PointDto(id = 10, labelId = 20, x = 1.234567, y = -2.0),
                    PointDto(id = 11, labelId = 3, x = 0.0, y = 0.0),
                    PointDto(id = 12, labelId = 20, x = 5.0, y = 5.0, group = PointGroupRefDto(248, 372)),
                    PointDto(id = 13, labelId = 999, x = 5.0, y = 5.0),
                ),
            ),
        ),
        groups = listOf(SnapshotGroup(id = 248, mapId = 2, floors = listOf(floor))),
    )

    @Test
    fun `the catalog has every leaf of the tree with our pin and the picture in it`() {
        val catalog = labels.toCatalog(pin = MarkerIcon("data:pin"), mapIds = listOf(2, 7))

        assertEquals(setOf("3", "154", "20"), catalog.categories.keys)
        val waypoint = catalog.categories.getValue("3")
        assertTrue(waypoint.visibleByDefault)
        assertTrue(waypoint.icon.url.startsWith("https://icons/3.png?x-oss-process="))
        assertEquals("data:pin", waypoint.pin.icon.url)
        assertEquals(waypoint.icon, waypoint.pin.pictogram)
        assertEquals(1f, waypoint.pin.anchorY)
        assertEquals(false, catalog.categories.getValue("20").visibleByDefault)
        assertEquals(listOf("1", "13"), catalog.groupsByMap.getValue(7).map { it.id })
        assertEquals(mapOf(2 to "Teyvat"), catalog.mapNames)
    }

    @Test
    fun `the points become locations, the ones shown at first ahead`() {
        val catalog = labels.toCatalog(pin = MarkerIcon("data:pin"), mapIds = listOf(2))
        val locations = snapshot.toLocations(catalog)

        assertEquals(listOf(11, 10, 12, 13), locations.map { it.id })
        assertEquals("Teleport Waypoint", locations.first().name)
        assertEquals(1.23, snapshot.points.xs.first())
        val underground = locations.single { it.id == 12 }
        assertEquals(LocationDungeon(dungeonId = 248, floorId = 372, entranceIds = listOf(11)), underground.dungeon)
        // A category the names don't know yet: the point stays, without a pin until they come
        assertNull(locations.single { it.id == 13 }.pin)
    }

    @Test
    fun `a map gets its tiles' version, grid and underground floors`() {
        val map = GameMap(
            id = 2,
            name = "Teyvat",
            minZoom = 11,
            maxZoom = 15,
            tiles = MapTiles("https://tiles/2/{{version}}/{{x}}_{{y}}_{{level}}.webp", HOYOLAB_TILE_SCHEME),
            camera = MapCamera(0.0, 0.0, 12),
            dungeonBaseOpacity = 0.5f,
        )
        val result = map.withSource(snapshot.maps.single(), snapshot.groups)

        assertEquals("https://tiles/2/abc/{{x}}_{{y}}_{{level}}.webp", result.tiles.urlTemplate)
        assertEquals(mapOf("columns" to "16", "rows" to "16", "minLevel" to "-4"), result.tiles.params)
        val floor = result.dungeons.single().floors.single()
        assertEquals("B1", floor.name)
        assertTrue(floor.bounds.north > floor.bounds.south && floor.bounds.east > floor.bounds.west)
    }

    @Test
    fun `the version changes with the changelog and the tiles`() {
        val maps = listOf(MapInfoDto(2, "Teyvat", detail = detail))
        val day = ChangelogEntryDto("1794240000", listOf(ChangelogCountDto("Chest", 3)))
        val base = sourceVersion(maps, listOf(day))

        assertEquals(base, sourceVersion(maps, listOf(day)))
        // More points changed later the same day
        val more = day.copy(points = listOf(ChangelogCountDto("Chest", 4)))
        assertNotEquals(base, sourceVersion(maps, listOf(more)))
        val newTiles = listOf(MapInfoDto(2, "Teyvat", detail = detail.copy(mapVersion = "def")))
        assertNotEquals(base, sourceVersion(newTiles, listOf(day)))
    }
}
