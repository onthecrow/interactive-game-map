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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.view.DescriptionLink
import com.onthecrow.interactivegamemap.map.view.LocationView
import com.onthecrow.interactivegamemap.map.view.MapSelectorView
import com.onthecrow.interactivegamemap.map.view.MapView
import com.onthecrow.interactivegamemap.map.view.MarkerListView
import com.onthecrow.interactivegamemap.map.view.RouteBuildDialog
import com.onthecrow.interactivegamemap.map.view.RouteExitDialog
import com.onthecrow.interactivegamemap.map.view.ControlHintView
import com.onthecrow.interactivegamemap.map.view.RoutePagerView
import com.onthecrow.interactivegamemap.map.view.SystemBackHandler
import com.onthecrow.interactivegamemap.map.view.parseDescriptionLink
import com.onthecrow.interactivegamemap.ui.components.GameMapButton
import com.onthecrow.interactivegamemap.ui.components.GameModalSheet
import com.onthecrow.interactivegamemap.ui.components.GameSearchField
import com.onthecrow.interactivegamemap.ui.components.GameZoomControl
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_layers
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_markers
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_route
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_recenter
import interactivegamemap.feature.map.ui_common.generated.resources.recenter_hint_text
import interactivegamemap.feature.map.ui_common.generated.resources.recenter_hint_title
import interactivegamemap.feature.map.ui_common.generated.resources.route_hint_text
import interactivegamemap.feature.map.ui_common.generated.resources.route_hint_title
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_hint
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_in_map_hint
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_settings
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_zoom_in
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_zoom_out
import kotlin.math.min
import org.jetbrains.compose.resources.stringResource

/** The size of the buttons over the map, the search field is as tall. */
internal val MapControlSize = 48.dp

// The space between the controls and the edges of the screen, and between the controls
private val ControlsMargin = 16.dp
private val ControlsGap = 8.dp

@Composable
internal fun MapScreen(
    state: MapScreenState,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
) {
    // The height of the screen and of the popup, to put a location looked at in the middle of what's left above it
    var screenHeight by remember { mutableStateOf(0) }
    var screenWidth by remember { mutableStateOf(0) }
    var popupHeight by remember { mutableStateOf(0) }

    Box(
        modifier = modifier.onSizeChanged {
            screenHeight = it.height
            screenWidth = it.width
        },
    ) {
        val displayedMarkers = remember(state.locations, state.entrances, state.selectedLocation) {
            (state.locations + state.entrances + listOfNotNull(state.selectedLocation))
                .distinctBy { it.id }
        }
        val cameraController = remember { MapCameraController() }
        // The search field lets go of the focus, and its outline, at a tap on the map or on a button at the side
        val focusManager = LocalFocusManager.current
        val onMapEvent: (MapEvent) -> Unit = remember(onEvent, focusManager) {
            { event ->
                if (event.isTapOnMap) focusManager.clearFocus()
                onEvent(event)
            }
        }
        val onSideEvent: (MapEvent) -> Unit = remember(onEvent, focusManager) {
            { event ->
                focusManager.clearFocus()
                onEvent(event)
            }
        }
        val density = LocalDensity.current
        val topInset = WindowInsets.safeDrawing.getTop(density)
        val bottomInset = WindowInsets.systemBars.getBottom(density)
        val layoutDirection = LocalLayoutDirection.current
        val leftInset = WindowInsets.safeDrawing.getLeft(density, layoutDirection)
        val rightInset = WindowInsets.safeDrawing.getRight(density, layoutDirection)

        // The middle of what the popup leaves of the map, or its upper third before the popup is measured
        fun focusY(): Dp {
            val y = when {
                state.selectedLocation == null -> (topInset + screenHeight - bottomInset) / 2
                popupHeight > 0 && screenHeight > 0 -> (topInset + screenHeight - bottomInset - popupHeight) / 2
                else -> screenHeight / 3
            }
            return with(density) { y.toDp() }
        }

        // What the popup leaves of the map: all of it but the bars, the popup and what's above the safe area
        fun areaWidth(): Dp = with(density) { (screenWidth - leftInset - rightInset).coerceAtLeast(0).toDp() }
        fun areaHeight(): Dp = with(density) {
            val popup = if (state.selectedLocation != null) popupHeight else 0
            (screenHeight - topInset - bottomInset - popup).coerceAtLeast(0).toDp()
        }

        LaunchedEffect(state.focusedLocation, state.focusCount) {
            val location = state.focusedLocation ?: return@LaunchedEffect
            // The popup of a new selection is measured on its first frames
            repeat(2) { withFrameNanos {} }
            // On a route, a jump to a fast travel point before the location shows where to go from: both are in view
            val jump = state.routeFastTravelBefore(location.id)
            val points = listOfNotNull(jump?.let { GeoPoint(it.lat, it.lng) }, GeoPoint(location.lat, location.lng))
            cameraController.focus(points, focusY(), areaWidth(), areaHeight())
        }

        // The bubble of a fast travel point goes over it, the camera brings it in view at the zoom there is
        LaunchedEffect(state.selectedRouteStop) {
            val stop = state.displayedRoute?.fastTravels?.firstOrNull { it.number == state.selectedRouteStop } ?: return@LaunchedEffect
            val point = listOf(GeoPoint(stop.point.lat, stop.point.lng))
            cameraController.focus(point, focusY(), areaWidth(), areaHeight(), keepZoom = true)
        }

        LaunchedEffect(state.cameraFit) {
            state.cameraFit?.let { cameraController.fit(it.bounds) }
        }

        MapView(
            modifier = Modifier.fillMaxSize(),
            map = state.currentMap,
            locations = displayedMarkers,
            clusters = state.clusters,
            selectedLocation = state.selectedLocation,
            entrances = state.entrances,
            floors = state.displayedFloors,
            foundLocationIds = state.foundLocationIds,
            route = state.displayedRoute,
            selectedRouteStop = state.selectedRouteStop,
            camera = state.camera,
            cameraController = cameraController,
            hideMarkersWhileMoving = state.hideMarkersWhileMoving,
            // The Google logo stays above the search field
            bottomPadding = MapControlSize + ControlsMargin,
            onEvent = onMapEvent,
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
                    GameZoomControl(
                        canZoomIn = state.canZoomIn,
                        canZoomOut = state.canZoomOut,
                        onZoomInClick = {
                            focusManager.clearFocus()
                            cameraController.zoomIn()
                        },
                        onZoomOutClick = {
                            focusManager.clearFocus()
                            cameraController.zoomOut()
                        },
                        zoomInDescription = stringResource(Res.string.map_screen_zoom_in),
                        zoomOutDescription = stringResource(Res.string.map_screen_zoom_out),
                    )
                },
                recenterButton = {
                    WithHintAtLeft(
                        hint = {
                            ControlHintView(
                                visible = state.isHintVisible(MapHint.RECENTER),
                                title = stringResource(Res.string.recenter_hint_title),
                                text = stringResource(Res.string.recenter_hint_text),
                            )
                        },
                    ) {
                        RecenterButton(visible = state.isRecenterVisible, onClick = { onSideEvent(MapEvent.OnRecenterClick) })
                    }
                },
                sideButtons = {
                    Column(verticalArrangement = Arrangement.spacedBy(ControlsGap)) {
                        // Only while there is a route to build or to end; pressed with a route on, it offers to end it
                        if (state.isRouteButtonVisible) {
                            WithHintAtLeft(
                                hint = {
                                    ControlHintView(
                                        visible = state.isHintVisible(MapHint.ROUTE),
                                        title = stringResource(Res.string.route_hint_title),
                                        text = stringResource(Res.string.route_hint_text),
                                    )
                                },
                            ) {
                                GameMapButton(
                                    icon = GameMapTheme.icons.route,
                                    contentDescription = stringResource(Res.string.map_screen_route),
                                    active = state.route != null,
                                    onClick = { onSideEvent(MapEvent.OnRouteClick) },
                                )
                            }
                        }
                        if (state.hasDungeonFloors) {
                            GameMapButton(
                                icon = GameMapTheme.icons.layers,
                                contentDescription = stringResource(Res.string.map_screen_layers),
                                active = state.isDungeonsActive,
                                onClick = { onSideEvent(MapEvent.OnDungeonsClick) },
                            )
                        }
                        GameMapButton(
                            icon = GameMapTheme.icons.markers,
                            contentDescription = stringResource(Res.string.map_screen_markers),
                            onClick = { onSideEvent(MapEvent.OnMarkerListClick) },
                        )
                    }
                },
                bottomBar = {
                    SearchBar(state = state, onEvent = onEvent)
                },
            )
        }

        if (state.isMarkerListVisible) {
            GameModalSheet(
                onDismissRequest = { onEvent(MapEvent.OnMarkerListDismiss) },
            ) {
                MarkerListView(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }

        var displayedLocation by remember { mutableStateOf(MapLocation.EMPTY) }
        // The popup keeps its look while it fades out
        var isPagerDisplayed by remember { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current
        val onLinkClick: (String) -> Unit = { url ->
            when (val link = parseDescriptionLink(url)) {
                is DescriptionLink.Location -> onEvent(MapEvent.OnLocationLinkClick(link.id))
                is DescriptionLink.Search -> onEvent(MapEvent.OnSearchLinkClick(link.query))
                is DescriptionLink.External -> uriHandler.openUri(link.url)
            }
        }

        SystemBackHandler(enabled = state.selectedLocation != null) {
            onEvent(MapEvent.OnLocationDismiss)
        }

        LaunchedEffect(state.selectedLocation, state.isRoutePagerVisible) {
            if (state.selectedLocation != null) {
                displayedLocation = state.selectedLocation
                isPagerDisplayed = state.isRoutePagerVisible
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .imePadding(),
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
            // While the popup fades out the last one stays
            val showsPager = if (state.selectedLocation != null) state.isRoutePagerVisible else isPagerDisplayed
            val measured = Modifier.onSizeChanged { popupHeight = it.height }
            if (showsPager && state.routeLocations.isNotEmpty()) {
                RoutePagerView(modifier = measured, state = state, onEvent = onEvent, onLinkClick = onLinkClick)
            } else {
                LocationView(
                    // A tap on it, not on a button of it, takes the camera back to the marker
                    modifier = measured.padding(16.dp).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onEvent(MapEvent.OnPopupClick) },
                    ),
                    title = displayedLocation.name,
                    description = displayedLocation.description,
                    floors = state.currentMap.findDungeon(displayedLocation)?.floors.orEmpty(),
                    selectedFloorId = state.selectedFloorId,
                    locationFloorId = displayedLocation.dungeon?.floorId,
                    isFound = displayedLocation.id in state.foundLocationIds,
                    onFloorClick = { onEvent(MapEvent.OnFloorClick(it)) },
                    onExitClick = { onEvent(MapEvent.OnLocationDismiss) },
                    onFoundClick = { onEvent(MapEvent.OnFoundClick(displayedLocation.id)) },
                    onLinkClick = onLinkClick,
                )
            }
        }

        // The popups of a route take the place of the controls too, but the zoom stays where it is without them, over
        // the popups when they're tall
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(end = ControlsMargin),
            visible = state.isRoutePagerVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 220)),
        ) {
            ZoomAtRightLayout(
                modifier = Modifier.fillMaxSize(),
                zoomButtons = {
                    GameZoomControl(
                        canZoomIn = state.canZoomIn,
                        canZoomOut = state.canZoomOut,
                        onZoomInClick = cameraController::zoomIn,
                        onZoomOutClick = cameraController::zoomOut,
                        zoomInDescription = stringResource(Res.string.map_screen_zoom_in),
                        zoomOutDescription = stringResource(Res.string.map_screen_zoom_out),
                    )
                },
                recenterButton = {
                    RecenterButton(visible = state.isRecenterVisible, onClick = { onEvent(MapEvent.OnRecenterClick) })
                },
            )
        }

        state.routeBuild?.let { progress ->
            RouteBuildDialog(progress = progress, onCancel = { onEvent(MapEvent.OnRouteBuildCancel) })
        }
        if (state.isRouteExitConfirmVisible) {
            RouteExitDialog(
                onConfirm = { onEvent(MapEvent.OnRouteExitConfirm) },
                onContinue = { onEvent(MapEvent.OnRouteContinue) },
                onDismiss = { onEvent(MapEvent.OnRouteExitDismiss) },
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
        GameSearchField(
            modifier = Modifier.weight(1f),
            value = state.searchValue,
            hint = state.currentMap?.takeIf { hasMapChoice }
                ?.let { stringResource(Res.string.map_screen_search_in_map_hint, it.name) }
                ?: stringResource(Res.string.map_screen_search_hint),
            onClearClick = { onEvent(MapEvent.OnClearSearchClick) },
            onValueChange = { onEvent(MapEvent.OnSearchValueChange(it)) },
        )
        GameMapButton(
            icon = GameMapTheme.icons.settings,
            contentDescription = stringResource(Res.string.map_screen_settings),
            onClick = { onEvent(MapEvent.OnSettingsClick) },
        )
    }
}

/**
 * The search bar at the bottom, the side buttons above its right end, the zoom buttons above them in the
 * middle of the screen when they fit there; when the screen is too short, the zoom buttons at the right edge and the
 * side buttons beside them. The recenter button is under the zoom buttons, its place is kept while it's away, so the
 * zoom buttons stay where they are when it comes. Everything stays clear of the system bars and of the keyboard.
 */
@Composable
private fun MapControlsLayout(
    zoomButtons: @Composable () -> Unit,
    recenterButton: @Composable () -> Unit,
    sideButtons: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insets = WindowInsets.safeDrawing
    Layout(
        modifier = modifier,
        contents = listOf(zoomButtons, recenterButton, sideButtons, bottomBar),
    ) { (zoomMeasurables, recenterMeasurables, sideMeasurables, barMeasurables), constraints ->
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
        val recenter = recenterMeasurables.map { it.measure(childConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var barTop = bottom
            bar.forEach {
                barTop -= it.height
                it.place(left, barTop)
            }
            val placement = zoomPlacement(
                screenHeight = constraints.maxHeight,
                right = right,
                top = top,
                barTop = barTop,
                sideTop = barTop - side.sumOf { gap + it.height },
                gap = gap,
                width = zoom.maxOfOrNull { it.width } ?: 0,
                height = zoom.maxOfOrNull { it.height } ?: 0,
                below = gap + MapControlSize.roundToPx(),
            )
            var sideY = barTop
            side.forEach {
                sideY -= gap + it.height
                it.place(placement.sideRight - it.width, sideY)
            }
            zoom.forEach {
                it.place(placement.x, placement.y)
                recenter.forEach { button -> button.place(placement.x + it.width - button.width, placement.y + it.height + gap) }
            }
        }
    }
}

/** Where the zoom buttons go, [x] and [y] of their top left corner, and the right edge of the side buttons. */
internal data class ZoomPlacement(val x: Int, val y: Int, val sideRight: Int)

/**
 * Where the zoom buttons go: over the side buttons in the middle of the screen, or lower when the middle is taken; on a
 * screen too short for that, like a phone on its side, at the right edge above the bar, the side buttons moved to
 * their left.
 *
 * @param sideTop the top of the side buttons over [barTop], the top of the bar, when they're at the right edge
 * @param below the room kept under the zoom buttons, for the recenter button
 */
internal fun zoomPlacement(
    screenHeight: Int,
    right: Int,
    top: Int,
    barTop: Int,
    sideTop: Int,
    gap: Int,
    width: Int,
    height: Int,
    below: Int = 0,
): ZoomPlacement {
    val centered = (screenHeight - height) / 2
    return if (sideTop - gap - height - below >= top) {
        ZoomPlacement(x = right - width, y = min(centered, sideTop - gap - height - below), sideRight = right)
    } else {
        ZoomPlacement(
            x = right - width,
            y = min(centered, barTop - gap - height - below).coerceAtLeast(top),
            sideRight = right - width - gap,
        )
    }
}

/**
 * The zoom buttons in the middle of the right side, over the popups of a route, the recenter button under them. The
 * rest of it takes no touches, they go to the map and the popups.
 */
@Composable
private fun ZoomAtRightLayout(
    zoomButtons: @Composable () -> Unit,
    recenterButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(modifier = modifier, contents = listOf(zoomButtons, recenterButton)) { (zoomMeasurables, recenterMeasurables), constraints ->
        val gap = ControlsGap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val zoom = zoomMeasurables.map { it.measure(loose) }
        val recenter = recenterMeasurables.map { it.measure(loose) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            zoom.forEach {
                val y = (constraints.maxHeight - it.height) / 2
                it.place(constraints.maxWidth - it.width, y)
                recenter.forEach { button -> button.place(constraints.maxWidth - button.width, y + it.height + gap) }
            }
        }
    }
}

/** The button taking the camera back to the selected marker or to all of them, coming and going with them. */
@Composable
private fun RecenterButton(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
            scaleIn(initialScale = 0.8f, animationSpec = tween(durationMillis = 220)),
        exit = fadeOut(animationSpec = tween(durationMillis = 220)) +
            scaleOut(targetScale = 0.8f, animationSpec = tween(durationMillis = 220)),
    ) {
        GameMapButton(
            icon = GameMapTheme.icons.recenter,
            contentDescription = stringResource(Res.string.map_screen_recenter),
            onClick = onClick,
        )
    }
}

// Between the tip of a hint's pointer and the button it points at
private val HintGap = 2.dp

/**
 * [content] with [hint] at its left, in the middle of its height and outside its bounds, so the layout around it
 * doesn't move. The hint takes no touches there: a tap on it goes to the map.
 */
@Composable
private fun WithHintAtLeft(hint: @Composable () -> Unit, content: @Composable () -> Unit) {
    Layout(contents = listOf(content, hint)) { (contentMeasurables, hintMeasurables), constraints ->
        val placeable = contentMeasurables.map { it.measure(constraints) }
        val width = placeable.maxOfOrNull { it.width } ?: 0
        val height = placeable.maxOfOrNull { it.height } ?: 0
        val gap = HintGap.roundToPx()
        val hints = hintMeasurables.map {
            it.measure(Constraints(maxWidth = (constraints.maxWidth - width - gap).coerceAtLeast(0)))
        }
        layout(width, height) {
            placeable.forEach { it.place(0, 0) }
            hints.forEach { it.place(-it.width - gap, (height - it.height) / 2) }
        }
    }
}

/** A tap on the map, a marker or a point of the route on it. */
private val MapEvent.isTapOnMap: Boolean
    get() = this == MapEvent.OnLocationDismiss || this == MapEvent.OnClusterClick ||
        this is MapEvent.OnLocationClick || this is MapEvent.OnRouteStopClick
