package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.unit.Dp
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
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
     * The location at [focusY] from the top of the map view, the middle of what the popup leaves of it; at a zoom to
     * look at it from, or at the zoom there is when [keepZoom].
     */
    data class Focus(val lat: Double, val lng: Double, val focusY: Dp, val keepZoom: Boolean = false) : CameraCommand

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

    fun focus(lat: Double, lng: Double, focusY: Dp, keepZoom: Boolean = false) {
        _commands.tryEmit(CameraCommand.Focus(lat, lng, focusY, keepZoom))
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
 * The camera target at [zoom] that puts the location [lat], [lng] [above] dp higher than the middle of the camera,
 * as (lat, lng).
 */
internal fun cameraTargetAbove(lat: Double, lng: Double, zoom: Float, above: Double): Pair<Double, Double> {
    val world = WORLD_SIZE * 2.0.pow(zoom.toDouble())
    val s = sin(lat * PI / 180)
    val y = (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * world + above
    val targetLat = atan(sinh(PI - 2 * PI * y / world)) * 180 / PI
    return targetLat to lng
}

/** The bounds around [locations], at least a part of [mapBounds] wide and high; null without locations. */
internal fun locationsBounds(locations: List<MapLocation>, mapBounds: MapBounds?): MapBounds? {
    if (locations.isEmpty()) return null
    var west = locations.minOf { it.lng }
    var east = locations.maxOf { it.lng }
    var south = locations.minOf { it.lat }
    var north = locations.maxOf { it.lat }
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
