package com.onthecrow.interactivegamemap.map.view

import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.onthecrow.interactivegamemap.map.CameraCommand
import com.onthecrow.interactivegamemap.map.CameraPosition
import com.onthecrow.interactivegamemap.map.MapAnalytics
import com.onthecrow.interactivegamemap.map.MapCameraController
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MarkerCluster
import com.onthecrow.interactivegamemap.map.label
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.MarkerPinImage
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.pinImage
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import com.onthecrow.interactivegamemap.map.rememberClusterImages
import com.onthecrow.interactivegamemap.map.rememberMarkerPinImages
import com.onthecrow.interactivegamemap.map.trackFirstTile
import com.onthecrow.interactivegamemap.network.GameMap
import com.onthecrow.interactivegamemap.network.MapBounds
import com.onthecrow.interactivegamemap.network.MapFloor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
internal actual fun MapView(
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

    val mapView = remember {
        MapView(context, GoogleMapOptions().backgroundColor(mapStyle.backgroundColor.toArgb())).apply {
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
    val pinImages = rememberMarkerPinImages(locations, selectedId, highlight)
    val pinIcons = remember { mutableMapOf<MarkerPinImage, BitmapDescriptor>() }
    val markers = remember { mutableMapOf<String, Marker>() }
    val markerImages = remember { mutableMapOf<String, ImageBitmap?>() }
    val clusterImages = rememberClusterImages(clusters)
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
            TileOverlayOptions().tileProvider(MergedTileProvider(mapTileProvider, map.tiles, map.maxZoom, load))
        )
        mapAnalytics.trackFirstTile(load)
    }

    LaunchedEffect(googleMap, locations, pinImages, selectedId, highlight) {
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
            val pinImage = location.pinImage(selectedId, highlight)
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

    LaunchedEffect(googleMap, cameraController) {
        val googleMap = googleMap ?: return@LaunchedEffect
        cameraController.commands.collect { command ->
            googleMap.animateCamera(
                when (command) {
                    CameraCommand.ZOOM_IN -> CameraUpdateFactory.zoomIn()
                    CameraCommand.ZOOM_OUT -> CameraUpdateFactory.zoomOut()
                }
            )
        }
    }

    LaunchedEffect(googleMap, focusedLocation) {
        val location = focusedLocation ?: return@LaunchedEffect
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.lat, location.lng)))
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
                    mapTileProvider.getImage(floor.imageUrl)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
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
                            // Close enough for the locations of the cluster to be apart
                            is MarkerCluster -> mapView.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    tag.bounds.toLatLngBounds(),
                                    with(density) { ClusterBoundsPadding.roundToPx() },
                                )
                            )
                        }
                        true
                    }
                    mapView.setOnCameraMoveStartedListener {
                        isCameraMoving = true
                    }
                    mapView.setOnMapClickListener {
                        onEvent(MapEvent.OnLocationDismiss)
                    }
                    mapView.setOnCameraIdleListener {
                        isCameraMoving = false
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
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

private fun MapBounds.toLatLngBounds(): LatLngBounds {
    return LatLngBounds(LatLng(south, west), LatLng(north, east))
}
