package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import googlemaps.GMSCameraPosition
import googlemaps.GMSMapView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2DMake

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun MapScreen(modifier: Modifier) {
    val camera: GMSCameraPosition = remember {
        val target = CLLocationCoordinate2DMake(55.751244, 37.618423) // Москва
        GMSCameraPosition.cameraWithTarget(target = target, zoom = 12f)
    }

    UIKitView(
        modifier = modifier,
        factory = {
            GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera)
        },
        update = { mapView ->
            mapView.myLocationEnabled = true
            mapView.settings.compassButton = true
            mapView.settings.myLocationButton = true
        }
    )
}
