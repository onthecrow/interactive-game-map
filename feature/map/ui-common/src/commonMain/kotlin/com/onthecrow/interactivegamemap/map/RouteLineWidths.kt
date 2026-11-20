package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.RoutePath
import com.onthecrow.interactivegamemap.map.model.RoutePathKind
import kotlin.math.roundToInt

// The widths of the route follow the zoom in steps this long: 3.5% wider or narrower a step, too little to see
private const val WIDTH_ZOOM_STEP = 0.05f

// The lines this part of the view past each of its edges take their widths as well, before they come into it
private const val VIEW_MARGIN = 0.25

/**
 * The widths of the lines of a route as the camera moves: they grow and shrink with the map while it zooms.
 *
 * Google Maps lays a line out again whenever its width is set, on the main thread, a few hundred lines a frame were
 * too much for a pinch to be smooth. So a width is set only when it changes, in steps of the zoom, and only on the
 * lines in view or about to come into it: the others take theirs when the camera brings them there.
 *
 * Widths are in dp, the lines are the [paths] of the route by their indices.
 */
internal class RouteLineWidths(private val style: MapStyle, paths: List<RoutePath>) {
    private val kinds = paths.map { it.kind }
    private val bounds = paths.map { path ->
        MapBounds(
            west = path.points.minOfOrNull { it.lng } ?: 0.0,
            south = path.points.minOfOrNull { it.lat } ?: 0.0,
            east = path.points.maxOfOrNull { it.lng } ?: 0.0,
            north = path.points.maxOfOrNull { it.lat } ?: 0.0,
        )
    }
    // The width each line has now
    private val widths = FloatArray(paths.size) { Float.NaN }

    /** The width line [index] is drawn with at [zoom], when it's added to the map. */
    fun initial(index: Int, zoom: Float): Float = width(kinds[index], stepped(zoom)).also { widths[index] = it }

    /**
     * With the camera at [zoom] showing [view], [set] a new width on each line in or near it whose width at that zoom
     * isn't the one it has.
     */
    fun update(zoom: Float, view: MapBounds, set: (index: Int, width: Float) -> Unit) {
        val step = stepped(zoom)
        val ride = width(RoutePathKind.RIDE, step)
        val dashes = width(RoutePathKind.OFF_ROAD, step)
        val near = view.grownBy(VIEW_MARGIN)
        kinds.forEachIndexed { index, kind ->
            val width = if (kind == RoutePathKind.RIDE) ride else dashes
            if (widths[index] != width && bounds[index].overlaps(near)) {
                widths[index] = width
                set(index, width)
            }
        }
    }

    private fun width(kind: RoutePathKind, zoom: Float): Float = when (kind) {
        RoutePathKind.RIDE -> style.routeLineWidth(zoom).value
        RoutePathKind.OFF_ROAD, RoutePathKind.FAST_TRAVEL -> style.offRoadLineWidth(zoom).value
    }

    private fun stepped(zoom: Float): Float = (zoom / WIDTH_ZOOM_STEP).roundToInt() * WIDTH_ZOOM_STEP
}

private fun MapBounds.grownBy(part: Double): MapBounds {
    val width = east - west
    val height = north - south
    return MapBounds(west = west - width * part, south = south - height * part, east = east + width * part, north = north + height * part)
}

private fun MapBounds.overlaps(other: MapBounds): Boolean =
    west <= other.east && east >= other.west && south <= other.north && north >= other.south
