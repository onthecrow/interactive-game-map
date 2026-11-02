package com.onthecrow.interactivegamemap.map.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@Composable
internal expect fun MapView(
    map: GameMap?,
    locations: List<MapLocation>,
    selectedLocation: MapLocation?,
    entrances: List<MapLocation>,
    floors: List<MapFloor>,
    foundLocationIds: Set<Int>,
    focusedLocation: MapLocation?,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
)
