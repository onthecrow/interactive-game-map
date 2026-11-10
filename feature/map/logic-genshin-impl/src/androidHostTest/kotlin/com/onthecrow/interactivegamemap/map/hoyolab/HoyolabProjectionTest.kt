package com.onthecrow.interactivegamemap.map.hoyolab

import com.onthecrow.interactivegamemap.map.model.MapTiles
import com.onthecrow.interactivegamemap.map.tiles.TileUrl
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoyolabProjectionTest {
    // Teyvat as HoYoLAB had it in November 2026
    private val detail = MapDetailDto(
        totalSize = listOf(36864, 32768),
        padding = listOf(7625, 6881),
        origin = listOf(24207, 16411),
        mapVersion = "v1",
        minZoom = -4,
        maxZoom = 0,
        originalSize = listOf(21613, 19006),
    )
    private val projection = HoyolabProjection(detail)
    private val resolver = HoyolabTileUrlResolver()
    private val tiles = MapTiles(
        urlTemplate = "https://tiles/{{x}}_{{y}}_{{level}}.webp",
        scheme = HOYOLAB_TILE_SCHEME,
        params = mapOf(TILE_PARAM_COLUMNS to "144", TILE_PARAM_ROWS to "128", TILE_PARAM_MIN_LEVEL to "-4"),
    )

    /** The tile of the map view's grid at [zoom] that holds a position, by the map view's own projection. */
    private fun viewTile(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val scale = (1 shl zoom).toDouble()
        val x = (lng + 180) / 360 * scale
        val latRad = lat * PI / 180
        val y = (1 - ln(tan(latRad) + 1 / kotlin.math.cos(latRad)) / PI) / 2 * scale
        return floor(x).toInt() to floor(y).toInt()
    }

    @Test
    fun `a point lies on the tile the source draws it on, at every level`() {
        // The tile 58_51_P0 of the source is under the point -9302.16, -3331.59 of its map
        val x = -9302.16
        val y = -3331.59
        for (level in 0 downTo -4) {
            val zoom = HOYOLAB_BASE_ZOOM + level
            val (tileX, tileY) = viewTile(projection.lat(y), projection.lng(x), zoom)
            val url = resolver.resolve(tiles, tileX, tileY, zoom)
            val scale = 1 shl -level
            val expected = "${(x + 24207).toInt() / 256 / scale}_${(y + 16411).toInt() / 256 / scale}"
            val name = if (level == 0) "P0" else "N${-level}"
            assertEquals(TileUrl.Url("https://tiles/${expected}_$name.webp"), url)
        }
    }

    @Test
    fun `there are no tiles past the grid or the levels`() {
        val half = 1 shl (HOYOLAB_BASE_ZOOM - 1)
        assertEquals(TileUrl.Outside, resolver.resolve(tiles, half - 1, half, HOYOLAB_BASE_ZOOM))
        assertEquals(TileUrl.Outside, resolver.resolve(tiles, half + 144, half, HOYOLAB_BASE_ZOOM))
        assertTrue(resolver.resolve(tiles, half + 143, half + 127, HOYOLAB_BASE_ZOOM) is TileUrl.Url)
        assertEquals(TileUrl.Outside, resolver.resolve(tiles, 2 * half, 2 * half, HOYOLAB_BASE_ZOOM + 1))
        assertEquals(TileUrl.Outside, resolver.resolve(tiles, half / 32, half / 32, HOYOLAB_BASE_ZOOM - 5))
        // The last column of N4: 144 / 16 = 9 tiles
        val n4 = HOYOLAB_BASE_ZOOM - 4
        val halfN4 = 1 shl (n4 - 1)
        assertTrue(resolver.resolve(tiles, halfN4 + 8, halfN4 + 7, n4) is TileUrl.Url)
        assertEquals(TileUrl.Outside, resolver.resolve(tiles, halfN4 + 9, halfN4, n4))
    }

    @Test
    fun `the map's bounds are its picture inside the padding`() {
        val bounds = projection.mapBounds()
        assertEquals(projection.lng(7625.0 - 24207), bounds.west, 1e-9)
        assertEquals(projection.lng(7625.0 + 21613 - 24207), bounds.east, 1e-9)
        assertEquals(projection.lat(6881.0 - 16411), bounds.north, 1e-9)
        assertEquals(projection.lat(6881.0 + 19006 - 16411), bounds.south, 1e-9)
        assertTrue(bounds.north > bounds.south && bounds.east > bounds.west)
    }

    @Test
    fun `the languages of the app map to HoYoLAB's`() {
        assertEquals("en-us", hoyolabLanguage("en"))
        assertEquals("ru-ru", hoyolabLanguage("ru"))
        assertEquals("zh-cn", hoyolabLanguage("zh"))
        assertEquals("pt-pt", hoyolabLanguage("pt"))
        assertEquals("ja-jp", hoyolabLanguage("ja"))
    }
}
