package com.onthecrow.interactivegamemap.network

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RemoteConfigFormatTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun read(name: String): RemoteConfig {
        return json.decodeFromString(File("../../config/remote_config/$name").readText())
    }

    @Test
    fun `genshin config has all maps and dungeons`() {
        val config = read("genshin_maps.json")

        assertEquals(2, config.defaultMapId)
        assertEquals(setOf(2, 7, 9, 34, 36, 37, 40), config.maps.map { it.id }.toSet())
        assertTrue(config.maps.all { it.iconUrl == "https://game-cdn.appsample.com/gim/icons/map-${it.id}.png" })
        assertTrue(config.maps.all { it.tiles.scheme == TileScheme.CENTERED && it.bounds != null })
        assertEquals(334, config.maps.sumOf { map -> map.dungeons.sumOf { it.floors.size } })
        assertEquals(222, config.maps.sumOf { it.dungeons.size })
    }

    @Test
    fun `rdr2 config has the world map with interiors`() {
        val map = read("rdr2_maps.json").maps.single()

        assertEquals(1, map.id)
        assertEquals(TileScheme.XYZ, map.tiles.scheme)
        // Drawn in the middle of the world on whole tiles, so the camera kept in the bounds doesn't jump across
        // the antimeridian
        assertEquals(2, map.tiles.zoomOffset)
        assertTrue(map.minZoom > map.tiles.zoomOffset)
        val bounds = assertNotNull(map.bounds)
        assertTrue(bounds.west > -90.0 && bounds.east < 90.0)
        assertTrue(map.camera.lng in bounds.west..bounds.east && map.camera.lat in bounds.south..bounds.north)
        assertEquals(1f, map.dungeonBaseOpacity)
        assertEquals(21, map.dungeons.size)
        assertTrue(map.dungeons.all { it.floors.size == 1 })
    }
}
