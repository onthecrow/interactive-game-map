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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.view.DescriptionLink
import com.onthecrow.interactivegamemap.map.view.LocationView
import com.onthecrow.interactivegamemap.map.view.MapActionButton
import com.onthecrow.interactivegamemap.map.view.MapControlSize
import com.onthecrow.interactivegamemap.map.view.MapSelectorView
import com.onthecrow.interactivegamemap.map.view.MapView
import com.onthecrow.interactivegamemap.map.view.MarkerListView
import com.onthecrow.interactivegamemap.map.view.SystemBackHandler
import com.onthecrow.interactivegamemap.map.view.ZoomButtonsView
import com.onthecrow.interactivegamemap.map.view.parseDescriptionLink
import com.onthecrow.interactivegamemap.ui.AppTheme
import com.onthecrow.interactivegamemap.ui.view.SearchFieldView
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.ic_filter_list
import interactivegamemap.feature.map.ui_common.generated.resources.ic_layers
import interactivegamemap.feature.map.ui_common.generated.resources.ic_settings
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_layers
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_markers
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_hint
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_in_map_hint
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_settings
import kotlin.math.min
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

// The space between the controls and the edges of the screen, and between the controls
private val ControlsMargin = 16.dp
private val ControlsGap = 8.dp

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
        val cameraController = remember { MapCameraController() }

        MapView(
            modifier = Modifier.fillMaxSize(),
            map = state.currentMap,
            locations = displayedMarkers,
            clusters = state.clusters,
            selectedLocation = state.selectedLocation,
            entrances = state.entrances,
            floors = state.displayedFloors,
            foundLocationIds = state.foundLocationIds,
            focusedLocation = state.focusedLocation,
            camera = state.camera,
            cameraController = cameraController,
            hideMarkersWhileMoving = state.hideMarkersWhileMoving,
            // The Google logo stays above the search field
            bottomPadding = MapControlSize + ControlsMargin,
            onEvent = onEvent,
        )

        // The popup of a marker takes the place of the controls
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = state.selectedLocation == null,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 220)),
        ) {
            MapControlsLayout(
                modifier = Modifier.fillMaxSize(),
                zoomButtons = {
                    ZoomButtonsView(
                        canZoomIn = state.canZoomIn,
                        canZoomOut = state.canZoomOut,
                        onZoomInClick = cameraController::zoomIn,
                        onZoomOutClick = cameraController::zoomOut,
                    )
                },
                sideButtons = {
                    Column(verticalArrangement = Arrangement.spacedBy(ControlsGap)) {
                        if (state.hasDungeonFloors) {
                            MapActionButton(
                                imageVector = vectorResource(Res.drawable.ic_layers),
                                contentDescription = stringResource(Res.string.map_screen_layers),
                                isActive = state.isDungeonsActive,
                                onClick = { onEvent(MapEvent.OnDungeonsClick) },
                            )
                        }
                        MapActionButton(
                            imageVector = vectorResource(Res.drawable.ic_filter_list),
                            contentDescription = stringResource(Res.string.map_screen_markers),
                            onClick = { onEvent(MapEvent.OnMarkerListClick) },
                        )
                    }
                },
                bottomBar = {
                    SearchBar(state = state, onEvent = onEvent)
                },
            )
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
        val uriHandler = LocalUriHandler.current

        SystemBackHandler(enabled = state.selectedLocation != null) {
            onEvent(MapEvent.OnLocationDismiss)
        }

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
                isFound = displayedLocation.id in state.foundLocationIds,
                onFloorClick = { onEvent(MapEvent.OnFloorClick(it)) },
                onExitClick = { onEvent(MapEvent.OnLocationDismiss) },
                onFoundClick = { onEvent(MapEvent.OnFoundClick(displayedLocation.id)) },
                onLinkClick = { url ->
                    when (val link = parseDescriptionLink(url)) {
                        is DescriptionLink.Location -> onEvent(MapEvent.OnLocationLinkClick(link.id))
                        is DescriptionLink.Search -> {
                            onEvent(MapEvent.OnLocationDismiss)
                            onEvent(MapEvent.OnSearchValueChange(link.query))
                        }
                        is DescriptionLink.External -> uriHandler.openUri(link.url)
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchBar(
    state: MapScreenState,
    onEvent: (MapEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ControlsGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hasMapChoice = state.maps.size > 1
        if (hasMapChoice) {
            MapSelectorView(
                maps = state.maps,
                currentMapId = state.currentMap?.id,
                onMapSelect = { onEvent(MapEvent.OnMapSelect(it)) },
            )
        }
        SearchFieldView(
            modifier = Modifier.weight(1f)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(100.dp)),
            value = state.searchValue,
            hint = state.currentMap?.takeIf { hasMapChoice }
                ?.let { stringResource(Res.string.map_screen_search_in_map_hint, it.name) }
                ?: stringResource(Res.string.map_screen_search_hint),
            onClearClick = { onEvent(MapEvent.OnClearSearchClick) },
            onValueChange = { onEvent(MapEvent.OnSearchValueChange(it)) },
        )
        MapActionButton(
            imageVector = vectorResource(Res.drawable.ic_settings),
            contentDescription = stringResource(Res.string.map_screen_settings),
            onClick = { onEvent(MapEvent.OnSettingsClick) },
        )
    }
}

/**
 * The search bar at the bottom, the side buttons above its right end, the zoom buttons above them in the
 * middle of the screen when they fit there. Everything stays clear of the system bars and of the keyboard.
 */
@Composable
private fun MapControlsLayout(
    zoomButtons: @Composable () -> Unit,
    sideButtons: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insets = WindowInsets.safeDrawing
    Layout(
        modifier = modifier,
        contents = listOf(zoomButtons, sideButtons, bottomBar),
    ) { (zoomMeasurables, sideMeasurables, barMeasurables), constraints ->
        val margin = ControlsMargin.roundToPx()
        val gap = ControlsGap.roundToPx()
        val left = insets.getLeft(this, layoutDirection) + margin
        val right = constraints.maxWidth - insets.getRight(this, layoutDirection) - margin
        val top = insets.getTop(this) + margin
        val bottom = constraints.maxHeight - insets.getBottom(this) - margin
        val childConstraints = Constraints(
            maxWidth = (right - left).coerceAtLeast(0),
            maxHeight = (bottom - top).coerceAtLeast(0),
        )
        val bar = barMeasurables.map { it.measure(childConstraints) }
        val side = sideMeasurables.map { it.measure(childConstraints) }
        val zoom = zoomMeasurables.map { it.measure(childConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var lowest = bottom
            bar.forEach {
                lowest -= it.height
                it.place(left, lowest)
            }
            side.forEach {
                lowest -= gap + it.height
                it.place(right - it.width, lowest)
            }
            zoom.forEach {
                val centered = (constraints.maxHeight - it.height) / 2
                it.place(right - it.width, min(centered, lowest - gap - it.height).coerceAtLeast(top))
            }
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
