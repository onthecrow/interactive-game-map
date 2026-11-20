package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapCamera
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapTiles
import com.onthecrow.interactivegamemap.map.model.TileSchemes
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraTest {
    private val map = GameMap(
        id = 1,
        name = "World",
        minZoom = 4,
        maxZoom = 10,
        tiles = MapTiles("", TileSchemes.XYZ),
        camera = MapCamera(0.0, 0.0, 6),
        bounds = MapBounds(west = -40.0, south = -4.0, east = 16.0, north = 36.0),
        dungeonBaseOpacity = 1f,
    )

    private fun worldY(lat: Double, zoom: Float): Double {
        val s = sin(lat * PI / 180)
        return (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * 256 * 2.0.pow(zoom.toDouble())
    }

    @Test
    fun `a location looked at is brought between too far and too close`() {
        assertEquals(7f, map.focusZoom(5f))
        assertEquals(7.5f, map.focusZoom(7.5f))
        assertEquals(8.5f, map.focusZoom(10f))
    }

    @Test
    fun `the target goes below the location by the asked distance`() {
        val point = listOf(GeoPoint(20.0, 5.0))
        val (lat, lng) = cameraTargetAbove(point, 8f, 150.0)
        assertEquals(5.0, lng, 1e-9)
        assertEquals(150.0, worldY(lat, 8f) - worldY(20.0, 8f), 1e-6)
        assertEquals(20.0, cameraTargetAbove(point, 8f, 0.0).first, 1e-9)
    }

    @Test
    fun `a location and its fast travel point are both in view, farther out only when they must`() {
        val area = 400.dp to 500.dp
        val location = GeoPoint(20.0, 5.0)
        // Close together: the usual zoom of a location
        val near = listOf(location, GeoPoint(20.0, 5.01))
        assertEquals(map.focusZoom(9f), map.focusZoom(9f, near, area.first, area.second))
        // Far apart: out until both fit, and the middle of the two goes where one location would
        val far = listOf(location, GeoPoint(25.0, -5.0))
        val zoom = map.focusZoom(9f, far, area.first, area.second)
        assertTrue(zoom < map.focusZoom(9f))
        val scale = 256 * 2.0.pow(zoom.toDouble())
        val width = (5.0 - -5.0) / 360 * scale
        val height = worldY(20.0, zoom) - worldY(25.0, zoom)
        // The zoom is a Float, a hair of rounding is fine
        assertTrue(width <= 400 - 2 * 72 + 0.01 && height <= 500 - 2 * 40 + 0.01, "$width x $height")
        assertTrue(width > 400 - 2 * 72 - 1 || height > 500 - 2 * 40 - 1, "fits snugly: $width x $height")
        val (_, lng) = cameraTargetAbove(far, zoom, 0.0)
        assertEquals(0.0, lng, 1e-9)
        // Never past the map's minimum zoom
        assertEquals(4f, map.focusZoom(9f, listOf(GeoPoint(-80.0, -170.0), GeoPoint(80.0, 170.0)), area.first, area.second))
    }

    @Test
    fun `the bounds of markers close together are widened, far apart ones are kept`() {
        val near = listOf(MapLocation.EMPTY.copy(lat = 10.0, lng = 0.0), MapLocation.EMPTY.copy(lat = 10.1, lng = 0.2))
        val widened = locationsBounds(near, map.bounds)!!
        assertEquals(56.0 / 16, widened.east - widened.west, 1e-9)
        assertEquals(40.0 / 16, widened.north - widened.south, 1e-9)
        assertTrue(widened.west < 0.0 && widened.east > 0.2)

        val far = listOf(MapLocation.EMPTY.copy(lat = -2.0, lng = -30.0), MapLocation.EMPTY.copy(lat = 30.0, lng = 10.0))
        assertEquals(MapBounds(west = -30.0, south = -2.0, east = 10.0, north = 30.0), locationsBounds(far, map.bounds))

        assertNull(locationsBounds(emptyList(), map.bounds))
    }
}
