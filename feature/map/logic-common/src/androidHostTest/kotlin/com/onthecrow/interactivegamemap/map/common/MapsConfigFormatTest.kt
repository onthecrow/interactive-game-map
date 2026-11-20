package com.onthecrow.interactivegamemap.map.common

import com.onthecrow.interactivegamemap.map.model.MapsConfig
import com.onthecrow.interactivegamemap.map.model.TileSchemes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapsConfigFormatTest {

    private fun read(name: String): MapsConfig {
        return parseMapsConfig(File("../../../config/remote_config/$name").readText())
    }

    @Test
    fun `genshin config has every map of HoYoLAB on its tiles`() {
        val config = read("genshin_maps.json")

        assertEquals(2, config.defaultMapId)
        assertEquals(listOf(2, 7, 9, 34, 36, 37, 40), config.maps.map { it.id })
        assertTrue(config.maps.all { it.tiles.scheme == "hoyolab" && "{{version}}" in it.tiles.urlTemplate })
        assertTrue(config.maps.all { it.tiles.urlTemplate.startsWith("https://act-webstatic.hoyoverse.com/") })
        assertTrue(config.maps.all { it.maxZoom == 15 && it.minZoom in 11..13 })
        assertTrue(config.maps.all { it.iconUrl.orEmpty().startsWith("https://act-webstatic.hoyoverse.com/") })
        config.maps.forEach { map ->
            val bounds = assertNotNull(map.bounds)
            assertTrue(map.camera.lng in bounds.west..bounds.east && map.camera.lat in bounds.south..bounds.north)
        }
        // The underground places come with the data of the game
        assertTrue(config.maps.all { it.dungeons.isEmpty() })
    }

    @Test
    fun `rdr2 config has the world map with interiors`() {
        val map = read("rdr2_maps.json").maps.single()

        assertEquals(1, map.id)
        assertEquals(TileSchemes.XYZ, map.tiles.scheme)
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
