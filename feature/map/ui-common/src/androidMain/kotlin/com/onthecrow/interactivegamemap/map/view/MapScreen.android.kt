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
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.MarkerPinImage
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.pinImage
import com.onthecrow.interactivegamemap.map.rememberMarkerPinImages
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
    selectedLocation: MapLocation?,
    entrances: List<MapLocation>,
    floors: List<MapFloor>,
    foundLocationIds: Set<Int>,
    focusedLocation: MapLocation?,
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

    val googleMap by produceState<GoogleMap?>(null, mapView) {
        mapView.getMapAsync { value = it }
    }

    val pinImages = rememberMarkerPinImages(locations)
    val pinIcons = remember { mutableMapOf<MarkerPinImage, BitmapDescriptor>() }
    val markers = remember { mutableMapOf<String, Marker>() }
    val markerImages = remember { mutableMapOf<String, ImageBitmap?>() }
    val entrancePaths = remember { mutableListOf<Polyline>() }
    var tileOverlay by remember { mutableStateOf<TileOverlay?>(null) }
    var dungeonDim by remember { mutableStateOf<Polygon?>(null) }
    val floorOverlays = remember { mutableMapOf<Int, GroundOverlay>() }

    LaunchedEffect(googleMap) {
        googleMap?.run {
            mapType = GoogleMap.MAP_TYPE_NONE
            uiSettings.isRotateGesturesEnabled = false
            uiSettings.isTiltGesturesEnabled = false
            uiSettings.isCompassEnabled = false
            uiSettings.isMyLocationButtonEnabled = true
        }
    }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val systemInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val logoPadding = with(density) { GoogleLogoPadding.roundToPx() }
    val logoPaddingLeft = systemInsets.getLeft(density, layoutDirection) + logoPadding
    val logoPaddingBottom = systemInsets.getBottom(density) + logoPadding

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
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(map.camera.lat, map.camera.lng),
                map.camera.zoom.toFloat(),
            )
        )
        tileOverlay = googleMap.addTileOverlay(
            TileOverlayOptions().tileProvider(MergedTileProvider(mapTileProvider, map.tiles, map.maxZoom))
        )
    }

    LaunchedEffect(googleMap, locations, pinImages) {
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
            val pinImage = location.pinImage
            if (pinImage != null && pinImage !in pinImages) return@forEach

            val image = pinImage?.let { pinImages[it] }
            if (id !in markerImages || markerImages[id] !== image) {
                // The default pin is displayed when there is no image
                marker.setIcon(
                    image?.let {
                        pinIcons.getOrPut(pinImage) { BitmapDescriptorFactory.fromBitmap(it.asAndroidBitmap()) }
                    }
                )
                marker.isVisible = true
                markerImages[id] = image
            }
        }
    }

    LaunchedEffect(googleMap, locations, foundLocationIds) {
        locations.forEach { location ->
            markers[location.id.toString()]?.alpha = if (location.id in foundLocationIds) FOUND_MARKER_ALPHA else 1f
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

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { mapView ->
                    mapView.setOnMarkerClickListener {
                        onEvent(MapEvent.OnLocationClick(it.tag as MapLocation))
                        true
                    }
                    mapView.setOnMapClickListener {
                        onEvent(MapEvent.OnLocationDismiss)
                    }
                    mapView.setOnCameraIdleListener {
                        // The visible region of the projection doesn't include the padding
                        val southWest = mapView.projection.fromScreenLocation(Point(0, height))
                        val northEast = mapView.projection.fromScreenLocation(Point(width, 0))
                        onEvent(
                            MapEvent.OnCameraIdle(
                                MapBounds(
                                    west = southWest.longitude,
                                    south = southWest.latitude,
                                    east = northEast.longitude,
                                    north = northEast.latitude,
                                )
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
