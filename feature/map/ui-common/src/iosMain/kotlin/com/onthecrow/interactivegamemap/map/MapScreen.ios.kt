package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.onthecrow.interactivegamemap.network.RemoteConfig
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import googlemaps.GMSCameraPosition
import googlemaps.GMSMapView
import googlemaps.GMSTileLayer
import googlemaps.GMSTileReceiverProtocol
import googlemaps.animateToCameraPosition
import googlemaps.kGMSTypeNone
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray?.toUIImageOrNull(): UIImage? {
    return try {
        val bytes = this ?: run {
            return null
        }

        val size = bytes.size
        if (size == 0) {
            return null
        }

        val data: NSData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
        }

        UIImage(data = data)
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapScreen(modifier: Modifier) {
    val camera: GMSCameraPosition = remember {
        val target = CLLocationCoordinate2DMake(55.751244, 37.618423) // Москва
        GMSCameraPosition.cameraWithTarget(target = target, zoom = 12f)
    }
    val scope = rememberCoroutineScope()

    val configProvider: RemoteConfigProvider = koinInject<RemoteConfigProvider>()
    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()

    var config by remember { mutableStateOf<RemoteConfig?>(null) }

    LaunchedEffect(configProvider) {
        configProvider.getRemoteConfigFlow()
            .collect {
                config = it
            }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera)
        },
        update = { mapView ->
            object : GMSTileLayer() {
                override fun requestTileForX(
                    x: ULong,
                    y: ULong,
                    zoom: ULong,
                    receiver: GMSTileReceiverProtocol
                ) {
                    scope.launch {
                        val tile = mapTileProvider.getTileAsync(x.toInt(), y.toInt(), zoom.toInt())
                        val image: UIImage? = tile.data?.toUIImageOrNull()
                        receiver.receiveTileWithX(x, y, zoom, image)
                    }
                }
            }.map = mapView
            mapView.mapType = kGMSTypeNone
            config?.run {
                mapView.setMinZoom(minZoom.toFloat(), maxZoom.toFloat())
                mapView.animateToCameraPosition(GMSCameraPosition(startLat.toDouble(), startLng.toDouble(), initialZoom.toFloat()))
            }
            mapView.settings
            mapView.myLocationEnabled = true
            mapView.settings.compassButton = true
            mapView.settings.myLocationButton = true
        }
    )
}
