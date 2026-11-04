package com.onthecrow.interactivegamemap.map.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.UIKitView
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.MarkerPinImage
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.pinImage
import com.onthecrow.interactivegamemap.map.rememberMarkerPinImages
import com.onthecrow.interactivegamemap.network.GameMap
import com.onthecrow.interactivegamemap.network.MapBounds
import com.onthecrow.interactivegamemap.network.MapFloor
import googlemaps.GMSCameraPosition
import googlemaps.GMSCoordinateBounds
import googlemaps.GMSGroundOverlay
import googlemaps.GMSMapView
import googlemaps.GMSMapViewDelegateProtocol
import googlemaps.GMSMarker
import googlemaps.GMSMutablePath
import googlemaps.GMSPolygon
import googlemaps.GMSPolyline
import googlemaps.GMSTileLayer
import googlemaps.GMSTileReceiverProtocol
import googlemaps.animateToLocation
import googlemaps.kGMSTileLayerNoTile
import googlemaps.kGMSTypeNone
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
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
    val scope = rememberCoroutineScope()
    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()

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

            override fun mapView(mapView: GMSMapView, idleAtCameraPosition: GMSCameraPosition) {
                onEvent(MapEvent.OnCameraIdle(mapView.visibleBounds()))
            }
        }
    }
    val tileLayer = remember(map?.tiles) {
        map?.tiles?.let { tiles ->
            object : GMSTileLayer() {
                override fun requestTileForX(
                    x: ULong,
                    y: ULong,
                    zoom: ULong,
                    receiver: GMSTileReceiverProtocol
                ) {
                    scope.launch {
                        val tile = mapTileProvider.getTileAsync(tiles, x.toInt(), y.toInt(), zoom.toInt())
                        val image: UIImage? = tile?.let { it.data.toUIImageOrNull() ?: kGMSTileLayerNoTile }
                        receiver.receiveTileWithX(x, y, zoom, image)
                    }
                }
            }
        }
    }

    val camera: GMSCameraPosition = remember {
        val target = CLLocationCoordinate2DMake(55.751244, 37.618423) // Москва
        GMSCameraPosition.cameraWithTarget(target = target, zoom = 12f)
    }

    DisposableEffect(mapView, tileLayer) {
        tileLayer?.map = mapView
        onDispose { tileLayer?.map = null }
    }

    LaunchedEffect(mapView, map) {
        val view = mapView ?: return@LaunchedEffect
        val gameMap = map ?: return@LaunchedEffect

        view.cameraTargetBounds = gameMap.bounds?.toCoordinateBounds()
        view.setMinZoom(gameMap.minZoom.toFloat(), gameMap.maxZoom.toFloat())
        view.camera = GMSCameraPosition(
            gameMap.camera.lat,
            gameMap.camera.lng,
            gameMap.camera.zoom.toFloat(),
        )
    }

    val pinImages = rememberMarkerPinImages(locations)
    val density = LocalDensity.current.density
    val pinIcons = remember { mutableMapOf<MarkerPinImage, UIImage?>() }
    val markers = remember { mutableMapOf<String, GMSMarker>() }
    val markerImages = remember { mutableMapOf<String, ImageBitmap?>() }

    LaunchedEffect(mapView, locations, pinImages) {
        val view = mapView ?: return@LaunchedEffect
        val actualIds = locations.map { it.id.toString() }.toSet()

        markers.entries.removeAll { entry ->
            val shouldRemove = entry.key !in actualIds
            if (shouldRemove) {
                entry.value.map = null
                markerImages.remove(entry.key)
            }
            shouldRemove
        }

        locations.forEach { location ->
            val id = location.id.toString()
            val marker = markers.getOrPut(id) {
                GMSMarker().apply {
                    position = CLLocationCoordinate2DMake(location.lat, location.lng)
                    title = location.name
                    snippet = location.description
                    userData = location
                }
            }
            val pinImage = location.pinImage
            if (pinImage != null && pinImage !in pinImages) return@forEach

            val image = pinImage?.let { pinImages[it] }
            if (id !in markerImages || markerImages[id] !== image) {
                // The default pin is displayed when there is no image
                marker.icon = image?.let {
                    pinIcons.getOrPut(pinImage) { it.toPinIcon(density) }
                }
                // A marker stays hidden until the load of its image is finished
                marker.map = view
                markerImages[id] = image
            }
        }
    }

    LaunchedEffect(mapView, locations, foundLocationIds) {
        locations.forEach { location ->
            markers[location.id.toString()]?.opacity = if (location.id in foundLocationIds) FOUND_MARKER_ALPHA else 1f
        }
    }

    LaunchedEffect(mapView, focusedLocation) {
        val location = focusedLocation ?: return@LaunchedEffect
        mapView?.animateToLocation(CLLocationCoordinate2DMake(location.lat, location.lng))
    }

    var dungeonDim by remember { mutableStateOf<GMSPolygon?>(null) }
    val floorOverlays = remember { mutableMapOf<Int, GMSGroundOverlay>() }
    val hasFloors = floors.isNotEmpty()

    LaunchedEffect(mapView, map, hasFloors) {
        dungeonDim?.map = null
        dungeonDim = null

        val view = mapView ?: return@LaunchedEffect
        val gameMap = map ?: return@LaunchedEffect
        val bounds = gameMap.bounds ?: return@LaunchedEffect

        if (hasFloors && gameMap.dungeonBaseOpacity < 1f) {
            val dimPath = GMSMutablePath().apply {
                addCoordinate(CLLocationCoordinate2DMake(bounds.south, bounds.west))
                addCoordinate(CLLocationCoordinate2DMake(bounds.north, bounds.west))
                addCoordinate(CLLocationCoordinate2DMake(bounds.north, bounds.east))
                addCoordinate(CLLocationCoordinate2DMake(bounds.south, bounds.east))
            }
            dungeonDim = GMSPolygon.polygonWithPath(dimPath).apply {
                fillColor = Color.Black.copy(alpha = 1f - gameMap.dungeonBaseOpacity).toUIColor()
                strokeWidth = 0.0
                zIndex = Z_INDEX_DUNGEON_DIM
                this.map = view
            }
        }
    }

    LaunchedEffect(mapView, floors) {
        val actualIds = floors.map { it.id }.toSet()

        floorOverlays.entries.removeAll { entry ->
            val shouldRemove = entry.key !in actualIds
            if (shouldRemove) {
                entry.value.map = null
            }
            shouldRemove
        }

        val view = mapView ?: return@LaunchedEffect

        floors.filter { it.id !in floorOverlays }.forEach { floor ->
            launch {
                val image = mapTileProvider.getImage(floor.imageUrl).toUIImageOrNull()
                    ?: return@launch

                floorOverlays[floor.id] = GMSGroundOverlay.groundOverlayWithBounds(
                    floor.bounds.toCoordinateBounds(),
                    image,
                ).apply {
                    zIndex = Z_INDEX_DUNGEON_FLOOR
                    this.map = view
                }
            }
        }
    }

    val entrancePaths = remember { mutableListOf<GMSPolyline>() }

    LaunchedEffect(mapView, selectedLocation, entrances) {
        entrancePaths.forEach { it.map = null }
        entrancePaths.clear()

        val view = mapView ?: return@LaunchedEffect
        val location = selectedLocation ?: return@LaunchedEffect

        entrances.forEach { entrance ->
            val path = GMSMutablePath().apply {
                addCoordinate(CLLocationCoordinate2DMake(location.lat, location.lng))
                addCoordinate(CLLocationCoordinate2DMake(entrance.lat, entrance.lng))
            }
            entrancePaths += GMSPolyline.polylineWithPath(path).apply {
                strokeColor = EntrancePathColor.toUIColor()
                zIndex = Z_INDEX_ENTRANCE_PATH
                this.map = view
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            mapView = GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera).apply {
                mapType = kGMSTypeNone
                myLocationEnabled = true
                settings.compassButton = true
                settings.myLocationButton = true
            }
            mapView!!
        },
        update = { view ->
            // GMSMapView keeps a weak reference to its delegate
            view.delegate = mapDelegate
        }
    )
}

// A marker is drawn with the size of its image in points
@OptIn(ExperimentalForeignApi::class)
private fun ImageBitmap.toPinIcon(density: Float): UIImage? {
    val bytes = Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
    val image = bytes.toUIImageOrNull() ?: return null
    return UIImage.imageWithCGImage(
        image.CGImage,
        scale = density.toDouble(),
        orientation = UIImageOrientation.UIImageOrientationUp,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun MapBounds.toCoordinateBounds(): GMSCoordinateBounds {
    return GMSCoordinateBounds(
        CLLocationCoordinate2DMake(south, west),
        CLLocationCoordinate2DMake(north, east),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun GMSMapView.visibleBounds(): MapBounds {
    return projection.visibleRegion().useContents {
        val corners = listOf(nearLeft, nearRight, farLeft, farRight)
        MapBounds(
            west = corners.minOf { it.longitude },
            south = corners.minOf { it.latitude },
            east = corners.maxOf { it.longitude },
            north = corners.maxOf { it.latitude },
        )
    }
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
