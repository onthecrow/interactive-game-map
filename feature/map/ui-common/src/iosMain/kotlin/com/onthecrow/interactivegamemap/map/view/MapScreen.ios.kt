package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapScreenState
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.network.RemoteConfig
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import googlemaps.GMSCameraPosition
import googlemaps.GMSMapView
import googlemaps.GMSMapViewDelegateProtocol
import googlemaps.GMSMarker
import googlemaps.GMSTileLayer
import googlemaps.GMSTileReceiverProtocol
import googlemaps.animateToCameraPosition
import googlemaps.kGMSTypeNone
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray?.toUIImageOrNull(): UIImage? {
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
internal actual fun MapView(
    locations: List<MapLocation>,
    modifier: Modifier,
    onEvent: (MapEvent) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val configProvider: RemoteConfigProvider = koinInject<RemoteConfigProvider>()
    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()
    var config by remember { mutableStateOf<RemoteConfig?>(null) }

    var mapView by remember { mutableStateOf<GMSMapView?>(null) }

    val mapDelegate = remember(onEvent) {
        object : NSObject(), GMSMapViewDelegateProtocol {
            override fun mapView(mapView: GMSMapView, didTapMarker: GMSMarker): Boolean {
                (didTapMarker.userData as? MapLocation)?.let {
                    onEvent(MapEvent.OnLocationClick(it))
                }
                return true
            }

            override fun mapView(
                mapView: GMSMapView,
                didTapAtCoordinate: CValue<CLLocationCoordinate2D>
            ) {
                onEvent(MapEvent.OnLocationDismiss)
            }
        }
    }
    val tileLayer = remember {
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
        }
    }

    val camera: GMSCameraPosition = remember {
        val target = CLLocationCoordinate2DMake(55.751244, 37.618423) // Москва
        GMSCameraPosition.cameraWithTarget(target = target, zoom = 12f)
    }

    LaunchedEffect(configProvider) {
        configProvider.getRemoteConfigFlow()
            .collect {
                config = it
                config?.run {
                    mapView?.setMinZoom(minZoom.toFloat(), maxZoom.toFloat())
                    mapView?.animateToCameraPosition(
                        GMSCameraPosition(
                            startLat.toDouble(),
                            startLng.toDouble(),
                            initialZoom.toFloat()
                        )
                    )
                }
            }
    }

    val markers = remember { mutableMapOf<String, GMSMarker>() }

    LaunchedEffect(locations) {
        locations.forEach { location ->
            val actualIds = locations.map { it.id.toString() }.toSet()

            markers.entries.removeAll { entry ->
                val shouldRemove = entry.key !in actualIds
                if (shouldRemove) {
                    entry.value.map = null
                }
                shouldRemove
            }

            val existing = markers[location.id.toString()]
            if (existing == null) {
                val ann = GMSMarker().apply {
                    position = CLLocationCoordinate2DMake(location.lat, location.lng)
                    title = location.name
                    snippet = location.description
                    map = mapView
                    userData = location
                }
                markers[location.id.toString()] = ann
            } else {
                existing.position = CLLocationCoordinate2DMake(location.lat, location.lng)
                existing.title = location.name
                existing.snippet = location.description
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            mapView = GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera).apply {
                tileLayer.map = this
                mapType = kGMSTypeNone
                myLocationEnabled = true
                settings.compassButton = true
                settings.myLocationButton = true
                delegate = mapDelegate
            }
            mapView!!
        },
        update = { mapView ->

        }
    )
}
