package com.onthecrow.interactivegamemap.map.view

import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.android.gms.maps.model.TextureStyle
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.onthecrow.interactivegamemap.map.CameraCommand
import com.onthecrow.interactivegamemap.map.CameraPosition
import com.onthecrow.interactivegamemap.map.cameraTargetAbove
import com.onthecrow.interactivegamemap.map.focusZoom
import com.onthecrow.interactivegamemap.map.MapAnalytics
import com.onthecrow.interactivegamemap.map.MapCameraController
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MarkerCluster
import com.onthecrow.interactivegamemap.map.label
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.MarkerPinImage
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.Route
import com.onthecrow.interactivegamemap.map.model.RouteFastTravel
import com.onthecrow.interactivegamemap.map.model.RoutePathKind
import com.onthecrow.interactivegamemap.map.pinImage
import com.onthecrow.interactivegamemap.map.RouteStopBubbleLift
import com.onthecrow.interactivegamemap.map.rememberRouteStopBubble
import com.onthecrow.interactivegamemap.map.rememberRouteStopImages
import com.onthecrow.interactivegamemap.map.RouteLineWidths
import com.onthecrow.interactivegamemap.map.rememberCrayonDashTexture
import com.onthecrow.interactivegamemap.map.rememberCrayonTexture
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import com.onthecrow.interactivegamemap.map.rememberClusterImages
import com.onthecrow.interactivegamemap.map.rememberMarkerPinImages
import com.onthecrow.interactivegamemap.map.trackFirstTile
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapFloor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
internal actual fun MapView(
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
    modifier: Modifier,
    onEvent: (MapEvent) -> Unit,
) {
    // A map view takes its background once, so the other style of the map comes in a new one
    key(isDarkMap) {
        GoogleMapView(
            map = map,
            isDarkMap = isDarkMap,
            locations = locations,
            clusters = clusters,
            selectedLocation = selectedLocation,
            entrances = entrances,
            floors = floors,
            foundLocationIds = foundLocationIds,
            route = route,
            selectedRouteStop = selectedRouteStop,
            camera = camera,
            cameraController = cameraController,
            hideMarkersWhileMoving = hideMarkersWhileMoving,
            bottomPadding = bottomPadding,
            modifier = modifier,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun GoogleMapView(
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
    modifier: Modifier,
    onEvent: (MapEvent) -> Unit,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Color.Green),
            contentAlignment = Alignment.Center
        ) {
            Text("Map preview stub")
        }
        return
    }

    val context = LocalContext.current
    val mapStyle: MapStyle = koinInject<MapStyle>()
    // The tiles, the floors and the markers alike
    val tileColors = mapStyle.tileColors(isDarkMap)

    val mapView = remember {
        MapView(context, GoogleMapOptions().backgroundColor(mapStyle.backgroundColor(isDarkMap).toArgb())).apply {
            onCreate(Bundle())
        }
    }

    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()
    val mapAnalytics: MapAnalytics = koinInject<MapAnalytics>()

    val googleMap by produceState<GoogleMap?>(null, mapView) {
        mapView.getMapAsync { value = it }
    }

    // The pin with the popup over it
    val selectedId = selectedLocation?.id
    val highlight = GameMapTheme.colors.accent
    val pinImages = rememberMarkerPinImages(locations, selectedId, highlight, tileColors)
    val pinIcons = remember { mutableMapOf<MarkerPinImage, BitmapDescriptor>() }
    val markers = remember { mutableMapOf<String, Marker>() }
    val markerImages = remember { mutableMapOf<String, ImageBitmap?>() }
    val clusterImages = rememberClusterImages(clusters, tileColors)
    val clusterIcons = remember { mutableMapOf<ImageBitmap, BitmapDescriptor>() }
    val clusterMarkers = remember { mutableMapOf<Int, Marker>() }
    var isCameraMoving by remember { mutableStateOf(false) }
    // The view starts somewhere else, that camera isn't the user's one
    var isCameraPlaced by remember { mutableStateOf(false) }
    val areMarkersHidden = hideMarkersWhileMoving && isCameraMoving
    val entrancePaths = remember { mutableListOf<Polyline>() }
    val areaPolygons = remember { mutableListOf<Polygon>() }
    var tileOverlay by remember { mutableStateOf<TileOverlay?>(null) }
    var dungeonDim by remember { mutableStateOf<Polygon?>(null) }
    val floorOverlays = remember { mutableMapOf<Int, GroundOverlay>() }

    LaunchedEffect(googleMap) {
        googleMap?.run {
            mapType = GoogleMap.MAP_TYPE_NONE
            uiSettings.isRotateGesturesEnabled = false
            uiSettings.isTiltGesturesEnabled = false
            uiSettings.isCompassEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
        }
    }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val systemInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val logoPadding = with(density) { GoogleLogoPadding.roundToPx() }
    val logoPaddingLeft = systemInsets.getLeft(density, layoutDirection) + logoPadding
    val logoPaddingBottom = systemInsets.getBottom(density) + with(density) { bottomPadding.roundToPx() } + logoPadding

    LaunchedEffect(googleMap, logoPaddingLeft, logoPaddingBottom) {
        googleMap?.setPadding(logoPaddingLeft, 0, 0, logoPaddingBottom)
    }

    LaunchedEffect(googleMap, map) {
        val googleMap = googleMap ?: return@LaunchedEffect
        val map = map ?: return@LaunchedEffect

        tileOverlay?.remove()
        googleMap.resetMinMaxZoomPreference()
        googleMap.setLatLngBoundsForCameraTarget(map.bounds?.toLatLngBounds())
        googleMap.setMinZoomPreference(map.minZoom.toFloat())
        googleMap.setMaxZoomPreference(map.maxZoom.toFloat())
        val position = camera ?: CameraPosition(map.camera.lat, map.camera.lng, map.camera.zoom.toFloat())
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(position.lat, position.lng), position.zoom))
        isCameraPlaced = true
        val load = mapAnalytics.startLoad(map.id)
        tileOverlay = googleMap.addTileOverlay(
            TileOverlayOptions().tileProvider(MergedTileProvider(mapTileProvider, map.tiles, map.maxZoom, load, tileColors))
        )
        mapAnalytics.trackFirstTile(load)
    }

    LaunchedEffect(googleMap, locations, pinImages, selectedId, highlight, tileColors) {
        val googleMap = googleMap ?: return@LaunchedEffect
        val actualIds = locations.map { it.id.toString() }.toSet()

        markers.entries.removeAll { entry ->
            val shouldRemove = entry.key !in actualIds
            if (shouldRemove) {
                entry.value.remove()
                markerImages.remove(entry.key)
            }
            shouldRemove
        }

        locations.forEach { location ->
            val id = location.id.toString()
            val marker = markers.getOrPut(id) {
                // A marker stays hidden until the load of its image is finished
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(location.lat, location.lng))
                        .visible(false)
                )!!.apply {
                    tag = location
                }
            }
            val pinImage = location.pinImage(selectedId, highlight, tileColors)
            if (pinImage != null && pinImage !in pinImages) return@forEach

            val image = pinImage?.let { pinImages[it] }
            if (id !in markerImages || markerImages[id] !== image) {
                // The default pin is displayed when there is no image
                marker.setIcon(
                    image?.let {
                        pinIcons.getOrPut(pinImage) { BitmapDescriptorFactory.fromBitmap(it.asAndroidBitmap()) }
                    }
                )
                // At its point by default, a disc lies on the location by its middle
                marker.setAnchor(0.5f, pinImage?.anchorY?.takeIf { image != null } ?: 1f)
                marker.isVisible = true
                markerImages[id] = image
            }
        }
    }

    LaunchedEffect(googleMap, locations, foundLocationIds, areMarkersHidden, selectedId) {
        locations.forEach { location ->
            val marker = markers[location.id.toString()] ?: return@forEach
            marker.alpha = markerAlpha(
                location = location,
                isFound = location.id in foundLocationIds,
                areMarkersHidden = areMarkersHidden,
                selectedId = selectedId,
            )
            marker.zIndex = if (location.id == selectedId) Z_INDEX_SELECTED_MARKER.toFloat() else 0f
        }
    }

    LaunchedEffect(googleMap, clusters, clusterImages) {
        val googleMap = googleMap ?: return@LaunchedEffect
        val actualIds = clusters.map { it.id }.toSet()

        clusterMarkers.entries.removeAll { entry ->
            val shouldRemove = entry.key !in actualIds
            if (shouldRemove) {
                entry.value.remove()
            }
            shouldRemove
        }

        clusters.forEach { cluster ->
            val image = clusterImages[cluster.label] ?: return@forEach
            val icon = clusterIcons.getOrPut(image) { BitmapDescriptorFactory.fromBitmap(image.asAndroidBitmap()) }
            val marker = clusterMarkers.getOrPut(cluster.id) {
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(cluster.lat, cluster.lng))
                        .anchor(0.5f, 0.5f)
                        .zIndex(Z_INDEX_CLUSTER.toFloat())
                        .icon(icon)
                )!!
            }
            marker.setIcon(icon)
            marker.tag = cluster
        }
    }

    LaunchedEffect(googleMap, clusters, areMarkersHidden) {
        clusterMarkers.values.forEach { it.alpha = if (areMarkersHidden) HIDDEN_MARKER_ALPHA else 1f }
    }

    val currentMap by rememberUpdatedState(map)
    val currentSelected by rememberUpdatedState(selectedLocation)

    LaunchedEffect(googleMap, cameraController) {
        val googleMap = googleMap ?: return@LaunchedEffect
        cameraController.commands.collect { command ->
            googleMap.animateCamera(
                when (command) {
                    CameraCommand.ZoomIn -> zoomBy(googleMap, currentSelected, 1f)
                    CameraCommand.ZoomOut -> zoomBy(googleMap, currentSelected, -1f)
                    is CameraCommand.Focus -> {
                        val position = googleMap.cameraPosition
                        val zoom = currentMap?.takeIf { !command.keepZoom }
                            ?.focusZoom(position.zoom, command.points, command.areaWidth, command.areaHeight)
                            ?: position.zoom
                        // The target is in the middle of the map's padding, the points go above it
                        val center = googleMap.projection.toScreenLocation(position.target)
                        val above = center.y / density.density - command.focusY.value
                        val (lat, lng) = cameraTargetAbove(command.points, zoom, above.toDouble())
                        CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom)
                    }
                    is CameraCommand.Fit -> CameraUpdateFactory.newLatLngBounds(
                        command.bounds.toLatLngBounds(),
                        with(density) { FitBoundsPadding.roundToPx() },
                    )
                }
            )
        }
    }

    val hasFloors = floors.isNotEmpty()

    LaunchedEffect(googleMap, map, hasFloors) {
        dungeonDim?.remove()
        dungeonDim = null

        val googleMap = googleMap ?: return@LaunchedEffect
        val map = map ?: return@LaunchedEffect
        val bounds = map.bounds ?: return@LaunchedEffect

        if (hasFloors && map.dungeonBaseOpacity < 1f) {
            dungeonDim = googleMap.addPolygon(
                PolygonOptions()
                    .add(
                        LatLng(bounds.south, bounds.west),
                        LatLng(bounds.north, bounds.west),
                        LatLng(bounds.north, bounds.east),
                        LatLng(bounds.south, bounds.east),
                    )
                    .fillColor(Color.Black.copy(alpha = 1f - map.dungeonBaseOpacity).toArgb())
                    .strokeWidth(0f)
                    .zIndex(Z_INDEX_DUNGEON_DIM.toFloat())
            )
        }
    }

    LaunchedEffect(googleMap, floors) {
        val actualIds = floors.map { it.id }.toSet()

        floorOverlays.entries.removeAll { entry ->
            val shouldRemove = entry.key !in actualIds
            if (shouldRemove) {
                entry.value.remove()
            }
            shouldRemove
        }

        val googleMap = googleMap ?: return@LaunchedEffect

        floors.filter { it.id !in floorOverlays }.forEach { floor ->
            launch {
                val bitmap = withContext(Dispatchers.IO) {
                    mapTileProvider.getImage(floor.imageUrl)
                        ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        ?.let { image -> tileColors?.let { image.recoloured(it) } ?: image }
                } ?: return@launch

                googleMap.addGroundOverlay(
                    GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .positionFromBounds(floor.bounds.toLatLngBounds())
                        .zIndex(Z_INDEX_DUNGEON_FLOOR.toFloat())
                )?.let { floorOverlays[floor.id] = it }
            }
        }
    }

    LaunchedEffect(googleMap, selectedLocation, entrances) {
        entrancePaths.forEach { it.remove() }
        entrancePaths.clear()

        val googleMap = googleMap ?: return@LaunchedEffect
        val selectedLocation = selectedLocation ?: return@LaunchedEffect

        entrances.forEach { entrance ->
            entrancePaths += googleMap.addPolyline(
                PolylineOptions()
                    .add(
                        LatLng(selectedLocation.lat, selectedLocation.lng),
                        LatLng(entrance.lat, entrance.lng),
                    )
                    .color(EntrancePathColor.toArgb())
                    .zIndex(Z_INDEX_ENTRANCE_PATH.toFloat())
            )
        }
    }

    // The lines along the roads and across country follow the zoom, see RouteLines.follow
    val routeLines = remember { RouteLines() }
    val crayon = rememberCrayonTexture(mapStyle.routeColor)
    val crayonDash = rememberCrayonDashTexture(mapStyle.routeColor)
    val crayonStrokes = remember(crayon, crayonDash) {
        // A crayon stroke: the texture is stamped over a transparent line, its ragged edges show the map
        fun stroke(image: ImageBitmap) = StrokeStyle.transparentColorBuilder()
            .stamp(TextureStyle.newBuilder(BitmapDescriptorFactory.fromBitmap(image.asAndroidBitmap())).build())
            .build()
        stroke(crayon) to stroke(crayonDash)
    }

    LaunchedEffect(googleMap, route, crayonStrokes) {
        routeLines.clear()

        val googleMap = googleMap ?: return@LaunchedEffect
        val route = route ?: return@LaunchedEffect
        val (stroke, dashes) = crayonStrokes

        val widths = RouteLineWidths(mapStyle, route.paths)
        val zoom = googleMap.cameraPosition.zoom
        routeLines.widths = widths
        route.paths.forEachIndexed { index, path ->
            val segments = (path.points.size - 1).coerceAtLeast(1).toDouble()
            val options = PolylineOptions()
                .addAll(path.points.map { LatLng(it.lat, it.lng) })
                .jointType(JointType.ROUND)
                .zIndex(Z_INDEX_ROUTE.toFloat())
            when (path.kind) {
                RoutePathKind.RIDE -> options
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .addSpan(StyleSpan(stroke, segments))
                // Crayon dashes across country and to the fast travel points, from the texture: one span for the line
                RoutePathKind.OFF_ROAD, RoutePathKind.FAST_TRAVEL -> options.addSpan(StyleSpan(dashes, segments))
            }
            routeLines.polylines += googleMap.addPolyline(options.width(widths.initial(index, zoom) * density.density))
        }
    }

    val routeStopImages = rememberRouteStopImages(route?.fastTravels?.map { it.number }.orEmpty())
    val routeStops = remember { mutableListOf<Marker>() }

    LaunchedEffect(googleMap, route, routeStopImages) {
        routeStops.forEach { it.remove() }
        routeStops.clear()

        val googleMap = googleMap ?: return@LaunchedEffect

        route?.fastTravels.orEmpty().forEach { stop ->
            val image = routeStopImages[stop.number] ?: return@forEach
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(stop.point.lat, stop.point.lng))
                    .anchor(0.5f, 0.5f)
                    .zIndex(Z_INDEX_ROUTE_STOP.toFloat())
                    .icon(BitmapDescriptorFactory.fromBitmap(image.asAndroidBitmap()))
            )?.let {
                // A tap on it shows its bubble, see the click listener
                it.tag = stop
                routeStops += it
            }
        }
    }

    val bubbleStop = route?.fastTravels?.firstOrNull { it.number == selectedRouteStop }
    val bubbleImage = rememberRouteStopBubble(bubbleStop)
    var bubble by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(googleMap, bubbleStop, bubbleImage) {
        bubble?.remove()
        bubble = null

        val googleMap = googleMap ?: return@LaunchedEffect
        val stop = bubbleStop ?: return@LaunchedEffect
        val image = bubbleImage ?: return@LaunchedEffect
        // Its bottom is the tip of the pointer, a bit above the middle of the point
        val lift = with(density) { RouteStopBubbleLift.toPx() } / image.height
        bubble = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(stop.point.lat, stop.point.lng))
                .anchor(0.5f, 1f + lift)
                .zIndex(Z_INDEX_ROUTE_BUBBLE.toFloat())
                .icon(BitmapDescriptorFactory.fromBitmap(image.asAndroidBitmap()))
        )
    }

    LaunchedEffect(googleMap, selectedLocation) {
        areaPolygons.forEach { it.remove() }
        areaPolygons.clear()

        val googleMap = googleMap ?: return@LaunchedEffect

        selectedLocation?.areas.orEmpty().forEach { area ->
            areaPolygons += googleMap.addPolygon(
                PolygonOptions()
                    .addAll(area.outline.map { LatLng(it.lat, it.lng) })
                    .fillColor(mapStyle.areaColor.copy(alpha = AREA_FILL_ALPHA).toArgb())
                    .strokeColor(mapStyle.areaColor.toArgb())
                    .strokeWidth(with(density) { AreaStrokeWidth.toPx() })
                    .zIndex(Z_INDEX_AREA.toFloat())
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { mapView ->
                    mapView.setOnMarkerClickListener {
                        when (val tag = it.tag) {
                            is MapLocation -> onEvent(MapEvent.OnLocationClick(tag))
                            is RouteFastTravel -> onEvent(MapEvent.OnRouteStopClick(tag.number))
                            // Close enough for the locations of the cluster to be apart
                            is MarkerCluster -> {
                                mapView.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        tag.bounds.toLatLngBounds(),
                                        with(density) { ClusterBoundsPadding.roundToPx() },
                                    )
                                )
                                onEvent(MapEvent.OnClusterClick)
                            }
                        }
                        true
                    }
                    mapView.setOnCameraMoveListener {
                        routeLines.follow(mapView, this, density.density)
                    }
                    // Turned, the view shows more of the map or less without the camera moving
                    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                            routeLines.follow(mapView, this, density.density)
                        }
                    }
                    mapView.setOnCameraMoveStartedListener {
                        isCameraMoving = true
                    }
                    mapView.setOnMapClickListener {
                        onEvent(MapEvent.OnLocationDismiss)
                    }
                    mapView.setOnCameraIdleListener {
                        isCameraMoving = false
                        routeLines.follow(mapView, this, density.density)
                        if (!isCameraPlaced) return@setOnCameraIdleListener
                        val position = mapView.cameraPosition
                        // The visible region of the projection doesn't include the padding
                        val southWest = mapView.projection.fromScreenLocation(Point(0, height))
                        val northEast = mapView.projection.fromScreenLocation(Point(width, 0))
                        onEvent(
                            MapEvent.OnCameraIdle(
                                viewport = MapBounds(
                                    west = southWest.longitude,
                                    south = southWest.latitude,
                                    east = northEast.longitude,
                                    north = northEast.latitude,
                                ),
                                camera = CameraPosition(position.target.latitude, position.target.longitude, position.zoom),
                            )
                        )
                    }
                }
            }
        },
    )

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapViewLifecycle = remember(mapView) { MapViewLifecycle() }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> return@LifecycleEventObserver
            }
            mapViewLifecycle.lastEvent = event
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // The view is left for good, as when the other style of the map takes a new one: it stops and lets go of the map
    DisposableEffect(mapView) {
        onDispose {
            when (mapViewLifecycle.lastEvent) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onPause()
                    mapView.onStop()
                    mapView.onDestroy()
                }
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_PAUSE -> {
                    mapView.onStop()
                    mapView.onDestroy()
                }
                Lifecycle.Event.ON_DESTROY -> Unit
                else -> mapView.onDestroy()
            }
        }
    }
}

/** The last event of the lifecycle a map view got, so it can be taken down when it's left. */
private class MapViewLifecycle(var lastEvent: Lifecycle.Event? = null)

/** The lines of the route on the map, by the indices of its paths, and their widths. */
private class RouteLines {
    val polylines = mutableListOf<Polyline>()
    var widths: RouteLineWidths? = null

    fun clear() {
        polylines.forEach { it.remove() }
        polylines.clear()
        widths = null
    }

    /**
     * The widths of the lines in [view], the map, or near it, for the camera now: the route grows and shrinks with the
     * map while it zooms, not only once it stops.
     */
    fun follow(map: GoogleMap, view: View, density: Float) {
        val widths = widths ?: return
        if (view.width == 0 || view.height == 0) return
        // All of the view: the visible region of the projection doesn't include the padding
        val southWest = map.projection.fromScreenLocation(Point(0, view.height))
        val northEast = map.projection.fromScreenLocation(Point(view.width, 0))
        val bounds = MapBounds(
            west = southWest.longitude,
            south = southWest.latitude,
            east = northEast.longitude,
            north = northEast.latitude,
        )
        widths.update(map.cameraPosition.zoom, bounds) { index, width -> polylines[index].width = width * density }
    }
}

/**
 * A zoom step around [location] when it's on the screen, so it stays where it is; around the middle otherwise, as when
 * the map was moved away from it.
 */
private fun zoomBy(googleMap: GoogleMap, location: MapLocation?, amount: Float): CameraUpdate {
    val point = location?.let { LatLng(it.lat, it.lng) }
        ?.takeIf { googleMap.projection.visibleRegion.latLngBounds.contains(it) }
        ?: return CameraUpdateFactory.zoomBy(amount)
    return CameraUpdateFactory.zoomBy(amount, googleMap.projection.toScreenLocation(point))
}

private fun MapBounds.toLatLngBounds(): LatLngBounds {
    return LatLngBounds(LatLng(south, west), LatLng(north, east))
}
