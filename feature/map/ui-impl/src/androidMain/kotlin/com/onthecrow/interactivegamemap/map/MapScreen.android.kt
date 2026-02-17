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

@Composable
internal actual fun MapScreen(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map: GoogleMap ->
            val target = LatLng(55.751244, 37.618423) // Москва
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 12f))

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
