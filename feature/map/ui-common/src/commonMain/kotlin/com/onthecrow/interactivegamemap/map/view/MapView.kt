package com.onthecrow.interactivegamemap.map.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.CameraPosition
import com.onthecrow.interactivegamemap.map.MapCameraController
import com.onthecrow.interactivegamemap.map.MarkerCluster
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapFloor
import com.onthecrow.interactivegamemap.map.model.Route

internal val EntrancePathColor = Color(0xFFFFC107)

internal const val FOUND_MARKER_ALPHA = 0.5f

// The space between the Google logo and the left and bottom edges, not counting the system bars
internal val GoogleLogoPadding = 8.dp

// Drawing order of the map layers, the tiles are at the bottom
internal const val Z_INDEX_DUNGEON_DIM = 1
internal const val Z_INDEX_DUNGEON_FLOOR = 2
internal const val Z_INDEX_AREA = 3
internal const val Z_INDEX_ENTRANCE_PATH = 4
internal const val Z_INDEX_ROUTE = 4
internal const val Z_INDEX_CLUSTER = 5
internal const val Z_INDEX_ROUTE_STOP = 6
internal const val Z_INDEX_SELECTED_MARKER = 7
internal const val Z_INDEX_ROUTE_BUBBLE = 8

// The route along the roads, across country and its jumps to fast travel points follow the zoom, see routeLineWidth

// The areas of the selected location: a see-through fill, so the map under it stays readable
internal const val AREA_FILL_ALPHA = 0.18f
internal val AreaStrokeWidth = 2.dp

// The space around the locations of a cluster when the camera is moved to them after a tap
internal val ClusterBoundsPadding = 64.dp

// The space around the markers the camera is fitted to, clear of the buttons at the side
internal val FitBoundsPadding = 64.dp

/** Zero hides a marker, see [markerAlpha]. */
internal const val HIDDEN_MARKER_ALPHA = 0f

/**
 * @param isDarkMap whether the tiles and the floors are recoloured by [com.onthecrow.interactivegamemap.map.MapStyle.darkTiles]
 * @param camera where the camera was left, it is placed there again when the view is created, otherwise at
 * the camera of [map]
 * @param hideMarkersWhileMoving whether the markers are hidden while the camera moves, except the selected one
 * @param bottomPadding the space the controls take at the bottom over the system bars, the Google logo stays
 * above it
 * @param route drawn as a line with the numbers of its fast travel points, see [rememberRouteStopImages]
 * @param selectedRouteStop the number of the fast travel point of [route] with its bubble over it
 *
 * The zoom buttons zoom around [selectedLocation] when there is one, so it stays where it is on the screen.
 */
@Composable
internal expect fun MapView(
    map: GameMap?,
    isDarkMap: Boolean,
    locations: List<MapLocation>,
    clusters: List<MarkerCluster>,
    selectedLocation: MapLocation?,
    entrances: List<MapLocation>,
    floors: List<MapFloor>,
    foundLocationIds: Set<Int>,
    route: Route?,
    selectedRouteStop: Int?,
    camera: CameraPosition?,
    cameraController: MapCameraController,
    hideMarkersWhileMoving: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
)

/** The alpha of a location's marker: hidden while the camera moves, see-through when the location is found. */
internal fun markerAlpha(location: MapLocation, isFound: Boolean, areMarkersHidden: Boolean, selectedId: Int?): Float {
    return when {
        areMarkersHidden && location.id != selectedId -> HIDDEN_MARKER_ALPHA
        isFound -> FOUND_MARKER_ALPHA
        else -> 1f
    }
}
