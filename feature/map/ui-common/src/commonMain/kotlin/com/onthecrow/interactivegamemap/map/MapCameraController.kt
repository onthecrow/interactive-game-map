package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.Route
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

// A location looked at is shown between these zooms below the maximum one: close enough to find it, far enough to see
// around it
private const val FOCUS_ZOOM_FAR = 3f
private const val FOCUS_ZOOM_NEAR = 1.5f

// The bounds of a few markers close together are widened to this part of the map, so a fit doesn't zoom in all the way
private const val MIN_FIT_SPAN = 1.0 / 16
// Without bounds of the map, at least this many degrees
private const val MIN_FIT_DEGREES = 0.01

// Google Maps draws the world 256 dp wide at zoom 0
private const val WORLD_SIZE = 256.0

/** Where the camera looks, it is put back when the map view is created again and at the next launch. */
@Serializable
data class CameraPosition(
    val lat: Double,
    val lng: Double,
    val zoom: Float,
)

internal sealed interface CameraCommand {
    data object ZoomIn : CameraCommand
    data object ZoomOut : CameraCommand

    /**
     * The [points] around [focusY] from the top of the map view, the middle of what the popup leaves of it, an area of
     * [areaWidth] by [areaHeight]: a location, or a location and the fast travel point a route jumps to before it. At
     * a zoom to look at them from, out far enough for all of them; or at the zoom there is when [keepZoom].
     */
    data class Focus(
        val points: List<GeoPoint>,
        val focusY: Dp,
        val areaWidth: Dp,
        val areaHeight: Dp,
        val keepZoom: Boolean = false,
    ) : CameraCommand

    /** All of [bounds] on the screen. */
    data class Fit(val bounds: MapBounds) : CameraCommand
}

/** Moves the camera of the map view at the requests of the controls over the map and of the screen. */
internal class MapCameraController {
    private val _commands = MutableSharedFlow<CameraCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<CameraCommand> = _commands.asSharedFlow()

    fun zoomIn() {
        _commands.tryEmit(CameraCommand.ZoomIn)
    }

    fun zoomOut() {
        _commands.tryEmit(CameraCommand.ZoomOut)
    }

    fun focus(points: List<GeoPoint>, focusY: Dp, areaWidth: Dp, areaHeight: Dp, keepZoom: Boolean = false) {
        if (points.isEmpty()) return
        _commands.tryEmit(CameraCommand.Focus(points, focusY, areaWidth, areaHeight, keepZoom))
    }

    fun fit(bounds: MapBounds) {
        _commands.tryEmit(CameraCommand.Fit(bounds))
    }
}

/** The zoom to look at a location from [zoom]: the same one when it's close enough and not too close. */
internal fun GameMap.focusZoom(zoom: Float): Float {
    val far = (maxZoom - FOCUS_ZOOM_FAR).coerceAtLeast(minZoom.toFloat())
    val near = (maxZoom - FOCUS_ZOOM_NEAR).coerceAtLeast(far)
    return zoom.coerceIn(far, near)
}

/**
 * The zoom to look at [points] from [zoom]: as for one location, or farther out when that's what it takes for all of
 * them to fit in [width] by [height] dp, the [FocusPadding] around them left out; never past the map's minimum.
 */
internal fun GameMap.focusZoom(zoom: Float, points: List<GeoPoint>, width: Dp, height: Dp): Float {
    val focus = focusZoom(zoom)
    if (points.size < 2) return focus
    val world = points.map { worldDp(it, 1.0) }
    val spanX = world.maxOf { it.first } - world.minOf { it.first }
    val spanY = world.maxOf { it.second } - world.minOf { it.second }
    val fitWidth = (width - FocusPadding.horizontal * 2).value.coerceAtLeast(1f)
    val fitHeight = (height - FocusPadding.vertical * 2).value.coerceAtLeast(1f)
    val scale = minOf(if (spanX > 0) fitWidth / spanX else Double.MAX_VALUE, if (spanY > 0) fitHeight / spanY else Double.MAX_VALUE)
    val fit = (log2(scale) - log2(WORLD_SIZE)).toFloat()
    return minOf(focus, fit).coerceAtLeast(minZoom.toFloat())
}

/** The space left around the points a focus fits, clear of the pins on them and of the zoom buttons at the side. */
internal object FocusPadding {
    val horizontal = 72.dp
    val vertical = 40.dp
}

/**
 * The camera target at [zoom] that puts the middle of [points] [above] dp higher than the middle of the camera, as
 * (lat, lng).
 */
internal fun cameraTargetAbove(points: List<GeoPoint>, zoom: Float, above: Double): Pair<Double, Double> {
    val scale = WORLD_SIZE * 2.0.pow(zoom.toDouble())
    val world = points.map { worldDp(it, scale) }
    val x = (world.minOf { it.first } + world.maxOf { it.first }) / 2
    val y = (world.minOf { it.second } + world.maxOf { it.second }) / 2 + above
    val lat = atan(sinh(PI - 2 * PI * y / scale)) * 180 / PI
    return lat to x / scale * 360 - 180
}

/** Where [point] is on the Web Mercator world [scale] dp wide. */
private fun worldDp(point: GeoPoint, scale: Double): Pair<Double, Double> {
    val s = sin(point.lat * PI / 180)
    return (point.lng + 180) / 360 * scale to (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * scale
}

/** The bounds around [locations], at least a part of [mapBounds] wide and high; null without locations. */
internal fun locationsBounds(locations: List<MapLocation>, mapBounds: MapBounds?): MapBounds? =
    pointsBounds(locations.asSequence().map { GeoPoint(it.lat, it.lng) }, mapBounds)

/** The bounds around all of [route] on the map, its lines and its [locations], as [locationsBounds]. */
internal fun routeBounds(route: Route, locations: List<MapLocation>, mapBounds: MapBounds?): MapBounds? =
    pointsBounds(
        route.paths.asSequence().flatMap { it.points } + locations.asSequence().map { GeoPoint(it.lat, it.lng) },
        mapBounds,
    )

private fun pointsBounds(points: Sequence<GeoPoint>, mapBounds: MapBounds?): MapBounds? {
    var west = Double.POSITIVE_INFINITY
    var east = Double.NEGATIVE_INFINITY
    var south = Double.POSITIVE_INFINITY
    var north = Double.NEGATIVE_INFINITY
    for (point in points) {
        west = minOf(west, point.lng)
        east = maxOf(east, point.lng)
        south = minOf(south, point.lat)
        north = maxOf(north, point.lat)
    }
    if (west > east) return null
    val minWidth = mapBounds?.let { (it.east - it.west) * MIN_FIT_SPAN } ?: MIN_FIT_DEGREES
    val minHeight = mapBounds?.let { (it.north - it.south) * MIN_FIT_SPAN } ?: MIN_FIT_DEGREES
    if (east - west < minWidth) {
        val middle = (east + west) / 2
        west = middle - minWidth / 2
        east = middle + minWidth / 2
    }
    if (north - south < minHeight) {
        val middle = (north + south) / 2
        south = middle - minHeight / 2
        north = middle + minHeight / 2
    }
    return MapBounds(west = west, south = south, east = east, north = north)
}
