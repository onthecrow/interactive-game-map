package com.onthecrow.interactivegamemap.map.view

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileOverlayOptions
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import org.koin.compose.koinInject

@Composable
internal actual fun MapView(
    locations: List<MapLocation>,
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

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    val configProvider: RemoteConfigProvider = koinInject<RemoteConfigProvider>()
    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()

    val markers = remember { mutableMapOf<String, Marker>() }

    LaunchedEffect(locations) {
        mapView.getMapAsync { map ->
            val actualIds = locations.map { it.id.toString() }.toSet()

            markers.entries.removeAll { entry ->
                val shouldRemove = entry.key !in actualIds
                if (shouldRemove) {
                    entry.value.remove()
                }
                shouldRemove
            }

            locations.forEach { location ->
                val existing = markers[location.id.toString()]
                val pos = LatLng(location.lat, location.lng)
                if (existing == null) {
                    markers[location.id.toString()] =
                        map.addMarker(MarkerOptions().position(pos))!!.apply {
                            tag = location
                        }
                } else {
                    existing.position = pos
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        configProvider.getRemoteConfigFlow().collect { config ->
            mapView.getMapAsync {
                it.setMaxZoomPreference(config.maxZoom.toFloat())
                it.setMinZoomPreference(config.minZoom.toFloat())

                // todo map setting for each app
//                val mobiCenter = LatLng(0.25, 0.4) // lat, lng
//                it.moveCamera(CameraUpdateFactory.newLatLngZoom(mobiCenter, 12f))

                val north = config.boundsNorth ?: return@getMapAsync
                val south = config.boundsSouth ?: return@getMapAsync
                val east = config.boundsEast ?: return@getMapAsync
                val west = config.boundsWest ?: return@getMapAsync

                Log.d("$$$$", "$north $south $east $west")

                val bounds = LatLngBounds(
                    LatLng(south.toDouble(), west.toDouble()),
                    LatLng(north.toDouble(), east.toDouble()),
                )
                it.setLatLngBoundsForCameraTarget(bounds)
//                val startLatLng = LatLng(config.startLat.toDouble(), config.startLng.toDouble())
//                it.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, config.initialZoom.toFloat()))
            }
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map: GoogleMap ->
//            val target = LatLng(55.751244, 37.618423) // Москва
//            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 12f))
//            val bounds = LatLngBounds(
//                LatLng(-0.75, -1.0),  // south, west
//                LatLng(1.0, 1.0)   // north, east
//            )
//            map.setLatLngBoundsForCameraTarget(bounds)

            val tileOverlayOptions = TileOverlayOptions().tileProvider { i, i1, i2 ->
                val tile = mapTileProvider.getTile(i, i1, i2)
                if (tile == null) {
                    null
                } else {
                    Tile(tile.width, tile.height, tile.data)
                }
            }
            map.mapType = GoogleMap.MAP_TYPE_NONE
            map.addTileOverlay(tileOverlayOptions)
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
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
