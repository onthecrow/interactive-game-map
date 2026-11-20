package com.onthecrow.interactivegamemap.map.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitView
import com.onthecrow.interactivegamemap.map.CameraCommand
import com.onthecrow.interactivegamemap.map.CameraPosition
import com.onthecrow.interactivegamemap.map.cameraTargetAbove
import com.onthecrow.interactivegamemap.map.focusZoom
import com.onthecrow.interactivegamemap.map.MapAnalytics
import com.onthecrow.interactivegamemap.map.MapCameraController
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapLoad
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
import com.onthecrow.interactivegamemap.map.TileColorMatrix
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import com.onthecrow.interactivegamemap.map.rememberClusterImages
import com.onthecrow.interactivegamemap.map.rememberMarkerPinImages
import com.onthecrow.interactivegamemap.map.trackFirstTile
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapFloor
import com.onthecrow.interactivegamemap.map.model.MapTiles
import googlemaps.GMSCameraPosition
import googlemaps.GMSCameraUpdate
import googlemaps.GMSCoordinateBounds
import googlemaps.GMSGroundOverlay
import googlemaps.GMSMapView
import googlemaps.GMSMapViewDelegateProtocol
import googlemaps.GMSMapViewOptions
import googlemaps.GMSMarker
import googlemaps.GMSMutablePath
import googlemaps.GMSPolygon
import googlemaps.GMSPolyline
import googlemaps.GMSStrokeStyle
import googlemaps.GMSStyleSpan
import googlemaps.GMSTextureStyle
import googlemaps.GMSTileLayer
import googlemaps.GMSTileReceiverProtocol
import googlemaps.animateWithCameraUpdate
import googlemaps.kGMSTileLayerNoTile
import googlemaps.kGMSTypeNone
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.compose.koinInject
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
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
    val scope = rememberCoroutineScope()
    val mapTileProvider: MapTileProvider = koinInject<MapTileProvider>()
    val mapStyle: MapStyle = koinInject<MapStyle>()
    // The tiles, the floors and the markers alike
    val tileColors = mapStyle.tileColors(isDarkMap)
    val mapAnalytics: MapAnalytics = koinInject<MapAnalytics>()

    var mapView by remember { mutableStateOf<GMSMapView?>(null) }
    var isCameraMoving by remember { mutableStateOf(false) }
    // The view starts somewhere else, that camera isn't the user's one
    var isCameraPlaced by remember { mutableStateOf(false) }
    val areMarkersHidden = hideMarkersWhileMoving && isCameraMoving

    // The lines of the route by the indices of its paths: they follow the zoom, see RouteLineWidths
    val routeLines = remember { mutableListOf<GMSPolyline>() }
    var routeWidths by remember { mutableStateOf<RouteLineWidths?>(null) }

    val mapDelegate = remember(onEvent) {
        object : NSObject(), GMSMapViewDelegateProtocol {
            override fun mapView(mapView: GMSMapView, didTapMarker: GMSMarker): Boolean {
                when (val data = didTapMarker.userData) {
                    is MapLocation -> onEvent(MapEvent.OnLocationClick(data))
                    is RouteFastTravel -> onEvent(MapEvent.OnRouteStopClick(data.number))
                    // Close enough for the locations of the cluster to be apart
                    is MarkerCluster -> {
                        mapView.animateWithCameraUpdate(
                            GMSCameraUpdate.fitBounds(data.bounds.toCoordinateBounds(), ClusterBoundsPadding.value.toDouble())
                        )
                        onEvent(MapEvent.OnClusterClick)
                    }
                }
                return true
            }

            override fun mapView(mapView: GMSMapView, willMove: Boolean) {
                isCameraMoving = true
            }

            override fun mapView(
                mapView: GMSMapView,
                didTapAtCoordinate: CValue<CLLocationCoordinate2D>
            ) {
                onEvent(MapEvent.OnLocationDismiss)
            }

            // The route grows and shrinks with the map while it zooms, not only once it stops
            @ObjCSignatureOverride
            override fun mapView(mapView: GMSMapView, didChangeCameraPosition: GMSCameraPosition) {
                routeWidths?.update(didChangeCameraPosition.zoom, mapView.visibleBounds()) { index, width ->
                    routeLines[index].strokeWidth = width.toDouble()
                }
            }

            @ObjCSignatureOverride
            override fun mapView(mapView: GMSMapView, idleAtCameraPosition: GMSCameraPosition) {
                isCameraMoving = false
                if (!isCameraPlaced) return
                val camera = mapView.camera
                val (lat, lng) = camera.target.useContents { latitude to longitude }
                onEvent(
                    MapEvent.OnCameraIdle(
                        viewport = mapView.visibleBounds(),
                        camera = CameraPosition(lat, lng, camera.zoom),
                    )
                )
            }
        }
    }
    val tileLayer = remember(map?.tiles, map?.maxZoom, tileColors) {
        map?.let { gameMap -> MapTileLayer(gameMap.tiles, gameMap.maxZoom, mapTileProvider, scope, tileColors) }
    }
    var tileLoad by remember { mutableStateOf<MapLoad?>(null) }

    val defaultCamera: GMSCameraPosition = remember {
        val target = CLLocationCoordinate2DMake(55.751244, 37.618423) // Москва
        GMSCameraPosition.cameraWithTarget(target = target, zoom = 12f)
    }

    DisposableEffect(mapView, tileLayer) {
        val view = mapView
        val gameMap = map
        if (view != null && tileLayer != null && gameMap != null) {
            // Before the layer asks for its first tiles
            tileLoad = mapAnalytics.startLoad(gameMap.id).also { tileLayer.load = it }
        }
        tileLayer?.map = view
        onDispose { tileLayer?.map = null }
    }

    LaunchedEffect(tileLoad) {
        tileLoad?.let { mapAnalytics.trackFirstTile(it) }
    }

    LaunchedEffect(mapView, map) {
        val view = mapView ?: return@LaunchedEffect
        val gameMap = map ?: return@LaunchedEffect

        view.cameraTargetBounds = gameMap.bounds?.toCoordinateBounds()
        view.setMinZoom(gameMap.minZoom.toFloat(), gameMap.maxZoom.toFloat())
        val position = camera ?: CameraPosition(gameMap.camera.lat, gameMap.camera.lng, gameMap.camera.zoom.toFloat())
        view.camera = GMSCameraPosition(position.lat, position.lng, position.zoom)
        isCameraPlaced = true
    }

    // The pin with the popup over it
    val selectedId = selectedLocation?.id
    val highlight = GameMapTheme.colors.accent
    val pinImages = rememberMarkerPinImages(locations, selectedId, highlight, tileColors)
    val density = LocalDensity.current.density
    val pinIcons = remember { mutableMapOf<MarkerPinImage, UIImage?>() }
    val markers = remember { mutableMapOf<String, GMSMarker>() }
    val markerImages = remember { mutableMapOf<String, ImageBitmap?>() }

    // With the colors: the images of the other style may be loaded already, and then pinImages stays the same
    LaunchedEffect(mapView, locations, pinImages, selectedId, highlight, tileColors) {
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
            val pinImage = location.pinImage(selectedId, highlight, tileColors)
            if (pinImage != null && pinImage !in pinImages) return@forEach

            val image = pinImage?.let { pinImages[it] }
            if (id !in markerImages || markerImages[id] !== image) {
                // The default pin is displayed when there is no image
                marker.icon = image?.let {
                    pinIcons.getOrPut(pinImage) { it.toPinIcon(density) }
                }
                // At its point by default, a disc lies on the location by its middle
                marker.groundAnchor = CGPointMake(0.5, (pinImage?.anchorY?.takeIf { image != null } ?: 1f).toDouble())
                // A marker stays hidden until the load of its image is finished
                marker.map = view
                markerImages[id] = image
            }
        }
    }

    LaunchedEffect(mapView, locations, foundLocationIds, areMarkersHidden, selectedId) {
        locations.forEach { location ->
            val marker = markers[location.id.toString()] ?: return@forEach
            marker.opacity = markerAlpha(
                location = location,
                isFound = location.id in foundLocationIds,
                areMarkersHidden = areMarkersHidden,
                selectedId = selectedId,
            )
            marker.zIndex = if (location.id == selectedId) Z_INDEX_SELECTED_MARKER else 0
        }
    }

    val clusterImages = rememberClusterImages(clusters, tileColors)
    val clusterIcons = remember { mutableMapOf<ImageBitmap, UIImage?>() }
    val clusterMarkers = remember { mutableMapOf<Int, GMSMarker>() }

    LaunchedEffect(mapView, clusters, clusterImages) {
        val view = mapView ?: return@LaunchedEffect
        val actualIds = clusters.map { it.id }.toSet()

        clusterMarkers.entries.removeAll { entry ->
            val shouldRemove = entry.key !in actualIds
            if (shouldRemove) {
                entry.value.map = null
            }
            shouldRemove
        }

        clusters.forEach { cluster ->
            val image = clusterImages[cluster.label] ?: return@forEach
            val marker = clusterMarkers.getOrPut(cluster.id) {
                GMSMarker().apply {
                    position = CLLocationCoordinate2DMake(cluster.lat, cluster.lng)
                    groundAnchor = CGPointMake(0.5, 0.5)
                    zIndex = Z_INDEX_CLUSTER
                    this.map = view
                }
            }
            marker.icon = clusterIcons.getOrPut(image) { image.toPinIcon(density) }
            marker.userData = cluster
        }
    }

    LaunchedEffect(mapView, clusters, areMarkersHidden) {
        clusterMarkers.values.forEach { it.opacity = if (areMarkersHidden) HIDDEN_MARKER_ALPHA else 1f }
    }

    val currentMap by rememberUpdatedState(map)
    val currentSelected by rememberUpdatedState(selectedLocation)

    LaunchedEffect(mapView, cameraController) {
        val view = mapView ?: return@LaunchedEffect
        cameraController.commands.collect { command ->
            view.animateWithCameraUpdate(
                when (command) {
                    CameraCommand.ZoomIn -> view.zoomBy(currentSelected, 1f)
                    CameraCommand.ZoomOut -> view.zoomBy(currentSelected, -1f)
                    is CameraCommand.Focus -> {
                        val camera = view.camera
                        val zoom = currentMap?.takeIf { !command.keepZoom }
                            ?.focusZoom(camera.zoom, command.points, command.areaWidth, command.areaHeight)
                            ?: camera.zoom
                        // The target is in the middle of the padding and the safe area, the points go above it
                        val centerY = view.projection.pointForCoordinate(camera.target).useContents { y }
                        val above = centerY - command.focusY.value
                        val (lat, lng) = cameraTargetAbove(command.points, zoom, above)
                        GMSCameraUpdate.setTarget(CLLocationCoordinate2DMake(lat, lng), zoom = zoom)
                    }
                    is CameraCommand.Fit -> GMSCameraUpdate.fitBounds(
                        command.bounds.toCoordinateBounds(),
                        FitBoundsPadding.value.toDouble(),
                    )
                }
            )
        }
    }

    var dungeonDim by remember { mutableStateOf<GMSPolygon?>(null) }
    val floorOverlays = remember { mutableMapOf<Int, GMSGroundOverlay>() }
    // The colors each floor's image is drawn with
    val floorColors = remember { mutableMapOf<Int, TileColorMatrix?>() }
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

    LaunchedEffect(mapView, isDarkMap) {
        // The background of the other style matches the edges of its tiles
        mapView?.backgroundColor = mapStyle.backgroundColor(isDarkMap).toUIColor()
    }

    LaunchedEffect(mapView, floors, tileColors) {
        val actualIds = floors.map { it.id }.toSet()

        floorOverlays.entries.removeAll { entry ->
            // The floors of the other style of the map are drawn again
            val shouldRemove = entry.key !in actualIds || floorColors[entry.key] != tileColors
            if (shouldRemove) {
                entry.value.map = null
            }
            shouldRemove
        }

        val view = mapView ?: return@LaunchedEffect

        floors.filter { it.id !in floorOverlays }.forEach { floor ->
            launch {
                val image = mapTileProvider.getImage(floor.imageUrl).toMapImage(tileColors)
                    ?: return@launch
                floorColors[floor.id] = tileColors

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

    val crayon = rememberCrayonTexture(mapStyle.routeColor)
    val crayonDash = rememberCrayonDashTexture(mapStyle.routeColor)
    // A crayon stroke: the texture is stamped over a transparent line, its ragged edges show the map
    val crayonStrokes = remember(crayon, crayonDash) {
        fun stroke(image: ImageBitmap) =
            image.toPinIcon(density)?.let { GMSStrokeStyle.transparentStrokeWithStampStyle(GMSTextureStyle.textureStyleWithImage(it)) }
        stroke(crayon) to stroke(crayonDash)
    }

    LaunchedEffect(mapView, route, crayonStrokes) {
        routeLines.forEach { it.map = null }
        routeLines.clear()
        routeWidths = null

        val view = mapView ?: return@LaunchedEffect
        val route = route ?: return@LaunchedEffect
        val (stroke, dashes) = crayonStrokes

        val widths = RouteLineWidths(mapStyle, route.paths)
        val zoom = view.camera.zoom
        route.paths.forEachIndexed { index, path ->
            val line = GMSMutablePath().apply {
                path.points.forEach { addCoordinate(CLLocationCoordinate2DMake(it.lat, it.lng)) }
            }
            GMSPolyline.polylineWithPath(line).apply {
                strokeColor = mapStyle.routeColor.toUIColor()
                // Along the roads a crayon stroke; across country and to the fast travel points crayon dashes, from
                // the texture: one span for the line
                val style = if (path.kind == RoutePathKind.RIDE) stroke else dashes
                if (style != null) spans = listOf(GMSStyleSpan.spanWithStyle(style))
                // In points, like the rest of the map
                strokeWidth = widths.initial(index, zoom).toDouble()
                zIndex = Z_INDEX_ROUTE
                this.map = view
                routeLines += this
            }
        }
        routeWidths = widths
    }

    val routeStopImages = rememberRouteStopImages(route?.fastTravels?.map { it.number }.orEmpty())
    val routeStops = remember { mutableListOf<GMSMarker>() }

    LaunchedEffect(mapView, route, routeStopImages) {
        routeStops.forEach { it.map = null }
        routeStops.clear()

        val view = mapView ?: return@LaunchedEffect

        route?.fastTravels.orEmpty().forEach { stop ->
            val image = routeStopImages[stop.number] ?: return@forEach
            routeStops += GMSMarker().apply {
                position = CLLocationCoordinate2DMake(stop.point.lat, stop.point.lng)
                icon = image.toPinIcon(density)
                groundAnchor = CGPointMake(0.5, 0.5)
                zIndex = Z_INDEX_ROUTE_STOP
                // A tap on it shows its bubble, see the delegate
                userData = stop
                this.map = view
            }
        }
    }

    val bubbleStop = route?.fastTravels?.firstOrNull { it.number == selectedRouteStop }
    val bubbleImage = rememberRouteStopBubble(bubbleStop)
    var bubble by remember { mutableStateOf<GMSMarker?>(null) }

    LaunchedEffect(mapView, bubbleStop, bubbleImage) {
        bubble?.map = null
        bubble = null

        val view = mapView ?: return@LaunchedEffect
        val stop = bubbleStop ?: return@LaunchedEffect
        val image = bubbleImage ?: return@LaunchedEffect
        // Its bottom is the tip of the pointer, a bit above the middle of the point
        val lift = RouteStopBubbleLift.value * density / image.height
        bubble = GMSMarker().apply {
            position = CLLocationCoordinate2DMake(stop.point.lat, stop.point.lng)
            icon = image.toPinIcon(density)
            groundAnchor = CGPointMake(0.5, 1.0 + lift)
            zIndex = Z_INDEX_ROUTE_BUBBLE
            this.map = view
        }
    }

    val areaPolygons = remember { mutableListOf<GMSPolygon>() }

    LaunchedEffect(mapView, selectedLocation) {
        areaPolygons.forEach { it.map = null }
        areaPolygons.clear()

        val view = mapView ?: return@LaunchedEffect

        selectedLocation?.areas.orEmpty().forEach { area ->
            val path = GMSMutablePath().apply {
                area.outline.forEach { addCoordinate(CLLocationCoordinate2DMake(it.lat, it.lng)) }
            }
            areaPolygons += GMSPolygon.polygonWithPath(path).apply {
                fillColor = mapStyle.areaColor.copy(alpha = AREA_FILL_ALPHA).toUIColor()
                strokeColor = mapStyle.areaColor.toUIColor()
                // In points, like the rest of the map
                strokeWidth = AreaStrokeWidth.value.toDouble()
                zIndex = Z_INDEX_AREA
                this.map = view
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val options = GMSMapViewOptions().apply {
                this.camera = defaultCamera
                backgroundColor = mapStyle.backgroundColor(isDarkMap).toUIColor()
            }
            mapView = GMSMapView(options = options).apply {
                mapType = kGMSTypeNone
                // The map view adds the safe area to the padding by itself
                padding = UIEdgeInsetsMake(
                    0.0,
                    GoogleLogoPadding.value.toDouble(),
                    (bottomPadding + GoogleLogoPadding).value.toDouble(),
                    0.0,
                )
                settings.rotateGestures = false
                settings.tiltGestures = false
                settings.compassButton = false
                settings.myLocationButton = false
            }
            mapView!!
        },
        update = { view ->
            // GMSMapView keeps a weak reference to its delegate
            view.delegate = mapDelegate
        }
    )
}

/**
 * Loads the tiles of [tiles] on the main thread, each one the map gets goes to [load].
 *
 * The layer draws 256 px tiles at their pixel size, so on a dense screen it asks for the zoom past the camera's.
 * Past [maxZoom] the server may have no tiles: then the part of the tile of [maxZoom] covering the same place,
 * enlarged, is drawn instead of nothing.
 */
@OptIn(ExperimentalForeignApi::class)
private class MapTileLayer(
    private val tiles: MapTiles,
    private val maxZoom: Int,
    private val mapTileProvider: MapTileProvider,
    private val scope: CoroutineScope,
    private val colors: TileColorMatrix?,
) : GMSTileLayer() {
    var load: MapLoad? = null

    override fun requestTileForX(x: ULong, y: ULong, zoom: ULong, receiver: GMSTileReceiverProtocol) {
        scope.launch {
            var error: String? = null
            val tile = mapTileProvider.getTileAsync(tiles, x.toInt(), y.toInt(), zoom.toInt()) { error = it }
            val decoded = tile?.data.toMapImage(colors)
                ?: tile?.takeIf { it.data == null && zoom.toInt() > maxZoom }
                    ?.let { enlargedPart(x.toInt(), y.toInt(), zoom.toInt()) }
            // No image is asked for again, the empty one isn't
            val image: UIImage? = decoded ?: tile?.let { kGMSTileLayerNoTile }
            receiver.receiveTileWithX(x, y, zoom, image)
            load?.onTile(
                hasImage = decoded != null,
                error = error ?: "decode".takeIf { tile?.data != null && decoded == null } ?: "unknown".takeIf { tile == null },
            )
        }
    }

    private suspend fun enlargedPart(x: Int, y: Int, zoom: Int): UIImage? {
        val depth = zoom - maxZoom
        val parentX = x shr depth
        val parentY = y shr depth
        val parent = mapTileProvider.getTileAsync(tiles, parentX, parentY, maxZoom)?.data.toMapImage(colors) ?: return null
        return parent.enlargedPart(column = x - (parentX shl depth), row = y - (parentY shl depth), parts = 1 shl depth)
    }
}

/**
 * The image of a tile or of a floor recoloured by [colors], decoded and recoloured off the main thread; without
 * colors it is decoded later, where the map draws it.
 */
private suspend fun ByteArray?.toMapImage(colors: TileColorMatrix?): UIImage? {
    colors ?: return toUIImageOrNull()
    val bytes = this ?: return null
    return withContext(Dispatchers.Default) { bytes.toUIImageOrNull()?.recoloured(colors) }
}

/** The part in [column] and [row] of the image cut in [parts] x [parts], at the size of the whole image. */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.enlargedPart(column: Int, row: Int, parts: Int): UIImage? {
    val image = CGImage ?: return null
    val width = CGImageGetWidth(image).toDouble()
    val height = CGImageGetHeight(image).toDouble()
    val partWidth = width / parts
    val partHeight = height / parts
    val cropped = CGImageCreateWithImageInRect(
        image,
        CGRectMake(column * partWidth, row * partHeight, partWidth, partHeight),
    ) ?: return null
    val part = UIImage.imageWithCGImage(cropped)
    CGImageRelease(cropped)
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    return UIGraphicsImageRenderer(size = CGSizeMake(width, height), format = format).imageWithActions {
        part.drawInRect(CGRectMake(0.0, 0.0, width, height))
    }
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

/**
 * A zoom step around [location] when it's on the screen, so it stays where it is; around the middle otherwise, as when
 * the map was moved away from it.
 */
@OptIn(ExperimentalForeignApi::class)
private fun GMSMapView.zoomBy(location: MapLocation?, amount: Float): GMSCameraUpdate {
    val coordinate = location?.let { CLLocationCoordinate2DMake(it.lat, it.lng) }
        ?.takeIf { projection.containsCoordinate(it) }
        ?: return GMSCameraUpdate.zoomBy(amount)
    return GMSCameraUpdate.zoomBy(amount, atPoint = projection.pointForCoordinate(coordinate))
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
    // The visible region of the projection doesn't include the padding
    val (width, height) = bounds.useContents { size.width to size.height }
    val (west, south) = projection.coordinateForPoint(CGPointMake(0.0, height)).useContents { longitude to latitude }
    val (east, north) = projection.coordinateForPoint(CGPointMake(width, 0.0)).useContents { longitude to latitude }
    return MapBounds(west = west, south = south, east = east, north = north)
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
