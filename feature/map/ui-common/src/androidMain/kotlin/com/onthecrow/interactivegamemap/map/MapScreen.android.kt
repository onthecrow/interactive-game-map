package com.onthecrow.interactivegamemap.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileOverlayOptions
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import org.koin.compose.koinInject

@Composable
actual fun MapScreen(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    val configProvider: RemoteConfigProvider = koinInject<RemoteConfigProvider>()
    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()

    LaunchedEffect(Unit) {
        configProvider.getRemoteConfigFlow().collect { config ->
            mapView.getMapAsync {
                it.setMaxZoomPreference(config.maxZoom.toFloat())
                it.setMinZoomPreference(config.minZoom.toFloat())

                // todo map setting for each app
                val mobiCenter = LatLng(0.25, 0.4) // lat, lng
                it.moveCamera(CameraUpdateFactory.newLatLngZoom(mobiCenter, 12f))
                val bounds = LatLngBounds(
                    LatLng(-0.75, -1.0),  // south, west
                    LatLng(1.0, 1.0)   // north, east
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
            val bounds = LatLngBounds(
                LatLng(-0.75, -1.0),  // south, west
                LatLng(1.0, 1.0)   // north, east
            )
            map.setLatLngBoundsForCameraTarget(bounds)

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
        factory = { mapView },
    )

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
