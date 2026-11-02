package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.LocationBounds
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapMarkerCategory
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup
import com.onthecrow.interactivegamemap.map.model.MarkerCategory
import com.onthecrow.interactivegamemap.network.GameMap
import com.onthecrow.interactivegamemap.network.MapBounds
import com.onthecrow.interactivegamemap.network.MapDungeon
import com.onthecrow.interactivegamemap.network.MapFloor
import com.onthecrow.nomadrates.uicore.State

// The floor images are big, so only the ones closest to the center of the screen are displayed
private const val MAX_DISPLAYED_FLOORS = 8

// Markers just outside the screen are also loaded, so they don't pop in during small moves
private const val VIEWPORT_MARGIN = 0.25

// The clusters take few markers on the screen, so more locations are loaded for them
private const val CLUSTERED_LOCATIONS_LIMIT = 20_000

// The zoom of the camera is a float, it may stop a bit before the maximum one
private const val ZOOM_EPSILON = 0.01f

data class MapScreenState(
    val searchValue: String = "",
    val maps: List<GameMap> = emptyList(),
    val currentMap: GameMap? = null,
    val viewport: MapBounds? = null,
    val camera: CameraPosition? = null,
    val selectedLocation: MapLocation? = null,
    val selectedFloorId: Int? = null,
    val showAllDungeons: Boolean = false,
    /** The loaded locations which aren't in [clusters]. */
    val locations: List<MapLocation> = emptyList(),
    val clusters: List<MarkerCluster> = emptyList(),
    val entrances: List<MapLocation> = emptyList(),
    val isMarkerListVisible: Boolean = false,
    val markerGroups: List<MapMarkerGroup> = emptyList(),
    val categoryVisibility: Map<String, Boolean> = emptyMap(),
    val cycles: Map<String, Int> = emptyMap(),
    val markerListQuery: String = "",
    val collapsedGroupIds: Set<String> = emptySet(),
    val foundLocationIds: Set<Int> = emptySet(),
    val focusedLocation: MapLocation? = null,
    val language: String = LocationsFilter.DEFAULT_LANGUAGE,
    val hideMarkersWhileMoving: Boolean = false,
    val clusterMarkers: Boolean = true,
): State {
    internal val markerListSettings: MarkerListSettings
        get() = MarkerListSettings(collapsedGroupIds, categoryVisibility, cycles)

    val isMarkerListSearching: Boolean
        get() = markerListQuery.isNotBlank()

    // A category can be in several groups, it is found once
    val foundCategories: List<MapMarkerCategory>
        get() {
            val query = markerListQuery.trim()
            return markerGroups
                .flatMap { it.categories }
                .filter { it.category.name.contains(query, ignoreCase = true) }
                .distinctBy { it.category.id }
        }

    val selectedFloor: MapFloor?
        get() = currentMap.findDungeon(selectedLocation)?.floors?.firstOrNull { it.id == selectedFloorId }

    val hasDungeonFloors: Boolean
        get() = currentMap?.dungeons.orEmpty().any { it.floors.isNotEmpty() }

    val isDungeonsActive: Boolean
        get() = showAllDungeons || selectedFloorId != null

    val displayedFloors: List<MapFloor>
        get() {
            selectedFloor?.let { return listOf(it) }
            val viewport = viewport
            if (!showAllDungeons || viewport == null) return emptyList()
            return currentMap?.dungeons.orEmpty()
                .mapNotNull { it.floors.firstOrNull() }
                .filter { it.bounds.intersects(viewport) }
                .sortedBy { it.bounds.distanceTo(viewport) }
                .take(MAX_DISPLAYED_FLOORS)
        }

    val canZoomIn: Boolean
        get() = camera != null && currentMap != null && camera.zoom + ZOOM_EPSILON < currentMap.maxZoom

    val canZoomOut: Boolean
        get() = camera != null && currentMap != null && camera.zoom - ZOOM_EPSILON > currentMap.minZoom

    /** The zoom to cluster the locations at, null when they are displayed apart, like at the maximum zoom. */
    val clusterZoom: Float?
        get() = camera?.zoom?.takeIf { clusterMarkers && canZoomIn }

    val locationsFilter: LocationsFilter?
        get() {
            val mapId = currentMap?.id ?: return null
            return LocationsFilter(
                mapId = mapId,
                query = searchValue,
                categoryVisibility = categoryVisibility,
                cycles = cycles,
                bounds = viewport?.takeIf { searchValue.isBlank() }?.toLocationBounds(),
                language = language,
                limit = if (clusterZoom != null) CLUSTERED_LOCATIONS_LIMIT else LocationsFilter.DEFAULT_LIMIT,
            )
        }

    internal val locationsRequest: LocationsRequest?
        get() = locationsFilter?.let { LocationsRequest(it, clusterZoom) }

    fun isGroupCollapsed(groupId: String): Boolean {
        return groupId in collapsedGroupIds
    }

    fun isCategoryVisible(category: MarkerCategory): Boolean {
        return categoryVisibility[category.id] ?: category.visibleByDefault
    }

    fun selectedCycle(categoryId: String): Int {
        return cycles[categoryId] ?: LocationsFilter.DEFAULT_CYCLE
    }
}

internal data class LocationsRequest(val filter: LocationsFilter, val clusterZoom: Float?)

internal fun GameMap?.findDungeon(location: MapLocation?): MapDungeon? {
    val dungeonId = location?.dungeon?.dungeonId ?: return null
    return this?.dungeons?.firstOrNull { it.id == dungeonId }
}

// The viewport crosses the antimeridian when west is greater than east
private val MapBounds.crossesAntimeridian: Boolean get() = west > east

private fun MapBounds.intersects(viewport: MapBounds): Boolean {
    val latitudeOverlaps = south <= viewport.north && north >= viewport.south
    val longitudeOverlaps = if (viewport.crossesAntimeridian) {
        east >= viewport.west || west <= viewport.east
    } else {
        west <= viewport.east && east >= viewport.west
    }
    return latitudeOverlaps && longitudeOverlaps
}

private fun MapBounds.distanceTo(viewport: MapBounds): Double {
    val lat = (north + south) / 2 - (viewport.north + viewport.south) / 2
    val lng = (east + west) / 2 - (viewport.east + viewport.west) / 2
    return lat * lat + lng * lng
}

private fun MapBounds.toLocationBounds(): LocationBounds? {
    if (crossesAntimeridian) return null
    val latMargin = (north - south) * VIEWPORT_MARGIN
    val lngMargin = (east - west) * VIEWPORT_MARGIN
    return LocationBounds(
        west = west - lngMargin,
        south = south - latMargin,
        east = east + lngMargin,
        north = north + latMargin,
    )
}
