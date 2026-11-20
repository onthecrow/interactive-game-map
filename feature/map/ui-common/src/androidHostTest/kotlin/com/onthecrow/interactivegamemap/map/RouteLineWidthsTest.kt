package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.RoutePath
import com.onthecrow.interactivegamemap.map.model.RoutePathKind
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteLineWidthsTest {
    private val style = MapStyle(backgroundColor = Color.Black)

    // A road in the west, a leg across country in the east
    private val paths = listOf(
        RoutePath(RoutePathKind.RIDE, listOf(GeoPoint(0.0, -10.0), GeoPoint(1.0, -9.0))),
        RoutePath(RoutePathKind.OFF_ROAD, listOf(GeoPoint(0.0, 9.0), GeoPoint(1.0, 10.0))),
    )
    private val west = MapBounds(west = -11.0, south = -1.0, east = -8.0, north = 2.0)
    private val east = MapBounds(west = 8.0, south = -1.0, east = 11.0, north = 2.0)
    private val both = MapBounds(west = -11.0, south = -1.0, east = 11.0, north = 2.0)

    private fun RouteLineWidths.changes(zoom: Float, view: MapBounds): List<Pair<Int, Float>> =
        mutableListOf<Pair<Int, Float>>().also { out -> update(zoom, view) { index, width -> out += index to width } }

    private fun widths(zoom: Float) = RouteLineWidths(style, paths).apply {
        initial(0, zoom)
        initial(1, zoom)
    }

    @Test
    fun `a width is set only when it changes`() {
        val widths = widths(8f)
        assertEquals(8f, widths.initial(0, 8f))
        assertEquals(4f, widths.initial(1, 8f))
        // The same zoom, or another one where the widths are at their narrowest or widest: nothing to set
        assertEquals(emptyList(), widths.changes(8f, both))
        val far = widths(3f)
        assertEquals(emptyList(), far.changes(6f, both))
        val near = widths(10f)
        assertEquals(emptyList(), near.changes(13f, both))
    }

    @Test
    fun `the widths follow the zoom in steps`() {
        val widths = widths(8f)
        assertEquals(emptyList(), widths.changes(8.02f, both))
        val step = widths.changes(8.05f, both)
        assertEquals(listOf(0, 1), step.map { it.first })
        assertEquals(style.routeLineWidth(8.05f).value, step[0].second, 1e-4f)
        assertEquals(style.offRoadLineWidth(8.05f).value, step[1].second, 1e-4f)
        assertEquals(emptyList(), widths.changes(8.06f, both))
    }

    @Test
    fun `only the lines in view take their widths, the others once they come into it`() {
        val widths = widths(8f)
        assertEquals(listOf(0), widths.changes(9f, west).map { it.first })
        assertEquals(listOf(1), widths.changes(9f, east).map { it.first })
        assertEquals(emptyList(), widths.changes(9f, both))
        // Near the view is as good as in it
        val nearEast = MapBounds(west = 4.0, south = -1.0, east = 8.0, north = 2.0)
        assertEquals(listOf(1), widths.changes(10f, nearEast).map { it.first })
    }
}
