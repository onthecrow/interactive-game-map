package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteStyleTest {
    private val style = MapStyle(backgroundColor = Color.Black)

    @Test
    fun `the route along the roads doubles a zoom level in, between its narrowest and widest`() {
        assertEquals(8f, style.routeLineWidth(8f).value)
        assertEquals(16f, style.routeLineWidth(9f).value)
        assertEquals(4f, style.routeLineWidth(2f).value)
        assertEquals(24f, style.routeLineWidth(14f).value)
    }

    @Test
    fun `the dashes in their texture are as many widths of the road long at any zoom`() {
        val (height, stroke) = dashTextureRows()
        // The texture is 64 wide and stamped as wide as the dashed line
        assertEquals(64 * (2.2 + 1.4) / 0.5, height.toDouble(), 0.5)
        assertEquals(64 * 2.2 / 0.5, stroke.toDouble(), 0.5)
        // The dashed line is half the road at every zoom, so a dash is 2.2 roads long and its gap 1.4
        (0..40).map { it / 2f }.forEach { zoom ->
            val road = style.routeLineWidth(zoom).value
            val width = style.offRoadLineWidth(zoom).value
            assertEquals(road * 0.5f, width, 1e-4f, "zoom $zoom")
            assertEquals(road * 2.2f, width * stroke / 64f, road * 0.01f, "dash at zoom $zoom")
            assertEquals(road * 1.4f, width * (height - stroke) / 64f, road * 0.01f, "gap at zoom $zoom")
        }
    }
}
