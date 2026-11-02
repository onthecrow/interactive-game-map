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
import com.onthecrow.interactivegamemap.network.GameMap
import com.onthecrow.interactivegamemap.network.MapFloor

internal val EntrancePathColor = Color(0xFFFFC107)

internal const val FOUND_MARKER_ALPHA = 0.5f

// The space between the Google logo and the left and bottom edges, not counting the system bars
internal val GoogleLogoPadding = 8.dp

// Drawing order of the map layers, the tiles are at the bottom
internal const val Z_INDEX_DUNGEON_DIM = 1
internal const val Z_INDEX_DUNGEON_FLOOR = 2
internal const val Z_INDEX_ENTRANCE_PATH = 3
internal const val Z_INDEX_CLUSTER = 4

// The space around the locations of a cluster when the camera is moved to them after a tap
internal val ClusterBoundsPadding = 64.dp

/** Zero hides a marker, see [markerAlpha]. */
internal const val HIDDEN_MARKER_ALPHA = 0f

/**
 * @param camera where the camera was left, it is placed there again when the view is created, otherwise at
 * the camera of [map]
 * @param hideMarkersWhileMoving whether the markers are hidden while the camera moves, except the selected one
 * @param bottomPadding the space the controls take at the bottom over the system bars, the Google logo stays
 * above it
 */
@Composable
internal expect fun MapView(
    map: GameMap?,
    locations: List<MapLocation>,
    clusters: List<MarkerCluster>,
    selectedLocation: MapLocation?,
    entrances: List<MapLocation>,
    floors: List<MapFloor>,
    foundLocationIds: Set<Int>,
    focusedLocation: MapLocation?,
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
