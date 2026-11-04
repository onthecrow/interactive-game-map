package com.onthecrow.interactivegamemap.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.view.LocationView
import com.onthecrow.interactivegamemap.map.view.MapActionButton
import com.onthecrow.interactivegamemap.map.view.MapSelectorView
import com.onthecrow.interactivegamemap.map.view.MapView
import com.onthecrow.interactivegamemap.map.view.MarkerListView
import com.onthecrow.interactivegamemap.ui.AppTheme
import com.onthecrow.interactivegamemap.ui.view.AppBarSearchView
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.ic_dungeon
import interactivegamemap.feature.map.ui_common.generated.resources.ic_marker_list
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_hint
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_in_map_hint
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapScreen(
    state: MapScreenState,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
) {
    Box(modifier = modifier) {
        val displayedMarkers = remember(state.locations, state.entrances, state.selectedLocation) {
            (state.locations + state.entrances + listOfNotNull(state.selectedLocation))
                .distinctBy { it.id }
        }

        MapView(
            modifier = Modifier.fillMaxSize(),
            map = state.currentMap,
            locations = displayedMarkers,
            selectedLocation = state.selectedLocation,
            entrances = state.entrances,
            floors = state.displayedFloors,
            onEvent = onEvent,
        )

        val hasMapChoice = state.maps.size > 1

        AppBarSearchView(
            modifier = Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            value = state.searchValue,
            showBackButton = false,
            hint = state.currentMap?.takeIf { hasMapChoice }
                ?.let { stringResource(Res.string.map_screen_search_in_map_hint, it.name) }
                ?: stringResource(Res.string.map_screen_search_hint),
            leadingContent = if (hasMapChoice) {
                {
                    MapSelectorView(
                        maps = state.maps,
                        currentMapId = state.currentMap?.id,
                        onMapSelect = { onEvent(MapEvent.OnMapSelect(it)) },
                    )
                }
            } else {
                null
            },
            onClearClick = { onEvent(MapEvent.OnClearSearchClick) },
            onValueChange = { onEvent(MapEvent.OnSearchValueChange(it)) },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapActionButton(
                imageVector = vectorResource(Res.drawable.ic_marker_list),
                onClick = { onEvent(MapEvent.OnMarkerListClick) },
            )
            if (state.hasDungeonFloors) {
                MapActionButton(
                    imageVector = vectorResource(Res.drawable.ic_dungeon),
                    isActive = state.isDungeonsActive,
                    onClick = { onEvent(MapEvent.OnDungeonsClick) },
                )
            }
        }

        if (state.isMarkerListVisible) {
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismissRequest = { onEvent(MapEvent.OnMarkerListDismiss) },
            ) {
                MarkerListView(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }

        var displayedLocation by remember { mutableStateOf(MapLocation.EMPTY) }

        LaunchedEffect(state.selectedLocation) {
            if (state.selectedLocation != null) {
                displayedLocation = state.selectedLocation
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp),
            visible = state.selectedLocation != null,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 300)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(durationMillis = 300)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 220)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(durationMillis = 220)
            )
        ) {
            LocationView(
                title = displayedLocation.name,
                description = displayedLocation.description,
                floors = state.currentMap.findDungeon(displayedLocation)?.floors.orEmpty(),
                selectedFloorId = state.selectedFloorId,
                locationFloorId = displayedLocation.dungeon?.floorId,
                onFloorClick = { onEvent(MapEvent.OnFloorClick(it)) },
                onExitClick = { onEvent(MapEvent.OnLocationDismiss) },
            )
        }
    }
}

@Preview
@Composable
private fun MapScreenPreview() {
    AppTheme {
        MapScreen(
            modifier = Modifier.fillMaxSize(),
            state = MapScreenState(),
        )
    }
}
