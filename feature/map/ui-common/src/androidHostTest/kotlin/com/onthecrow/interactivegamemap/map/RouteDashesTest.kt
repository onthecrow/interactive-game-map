package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.GeoPoint
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteDashesTest {
    // dp on the screen between two points at [zoom]
    private fun dp(a: GeoPoint, b: GeoPoint, zoom: Float): Double {
        val scale = 256 * 2.0.pow(zoom.toDouble())
        fun y(lat: Double) = sin(lat * PI / 180).let { (0.5 - ln((1 + it) / (1 - it)) / (4 * PI)) * scale }
        return hypot((b.lng - a.lng) / 360 * scale, y(b.lat) - y(a.lat))
    }

    @Test
    fun `a straight leg is cut into dashes of the length on the screen`() {
        val leg = listOf(GeoPoint(10.0, 0.0), GeoPoint(10.0, 0.1))
        val zoom = 8f
        val length = dp(leg[0], leg[1], zoom)
        val dashes = dashes(leg, zoom, dash = 10f, gap = 5f)
        // Every 15 dp a dash starts
        assertEquals(kotlin.math.ceil(length / 15).toInt(), dashes.size)
        dashes.dropLast(1).forEach { assertEquals(10.0, dp(it.first(), it.last(), zoom), 1e-6) }
        assertEquals(leg.first(), dashes.first().first())
        assertTrue(dp(dashes.last().last(), leg.last(), zoom) < 5.0 + 1e-6)
    }

    @Test
    fun `a leg shorter than a dash is one dash, zooming in makes more dashes`() {
        // About 0.1 dp long at zoom 4, 116 dp at zoom 14
        val leg = listOf(GeoPoint(10.0, 0.0), GeoPoint(10.0, 0.01))
        assertEquals(listOf(leg), dashes(leg, 4f, dash = 10f, gap = 5f))
        assertTrue(dashes(leg, 14f, dash = 10f, gap = 5f).size > 1)
    }
}
