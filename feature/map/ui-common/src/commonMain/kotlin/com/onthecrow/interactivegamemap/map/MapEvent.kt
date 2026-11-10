package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapsConfig
import com.onthecrow.nomadrates.uicore.Event

internal sealed interface MapEvent : Event {
    data object OnClearSearchClick : MapEvent
    data object OnLocationDismiss : MapEvent
    data object OnDungeonsClick : MapEvent
    data object OnMarkerListClick : MapEvent
    data object OnMarkerListDismiss : MapEvent
    data object OnSettingsClick : MapEvent
    /** [savedMapView] is opened when no map is open yet. */
    data class OnConfigLoaded(
        val config: MapsConfig,
        val savedMapView: MapViewSettings = MapViewSettings(),
    ) : MapEvent
    data class OnMapSelect(val map: GameMap) : MapEvent
    data class OnCameraIdle(val viewport: MapBounds, val camera: CameraPosition) : MapEvent
    data class OnSearchValueChange(val value: String) : MapEvent
    /** A search link in the description of a marker. */
    data class OnSearchLinkClick(val query: String) : MapEvent
    data class OnLocationsLoaded(val locations: List<MapLocation>, val clusters: List<MarkerCluster>) : MapEvent
    data class OnMarkerGroupsLoaded(val groups: List<MapMarkerGroup>) : MapEvent
    data class OnLocationClick(val location: MapLocation) : MapEvent
    data class OnEntrancesLoaded(val entrances: List<MapLocation>) : MapEvent
    data class OnFloorClick(val floorId: Int) : MapEvent
    data class OnCategoryClick(val categoryId: String) : MapEvent
    data class OnCategoriesVisibilityChange(val visible: Boolean) : MapEvent
    data class OnCycleClick(val categoryId: String, val cycle: Int) : MapEvent
    data class OnMarkerListQueryChange(val value: String) : MapEvent
    data class OnGroupClick(val groupId: String) : MapEvent
    data class OnMarkerListSettingsLoaded(val settings: MarkerListSettings) : MapEvent
    data class OnFoundClick(val locationId: Int) : MapEvent
    data class OnFoundLocationsLoaded(val locationIds: Set<Int>) : MapEvent
    data class OnLocationLinkClick(val locationId: Int) : MapEvent
    data class OnLocationLinkLoaded(val location: MapLocation) : MapEvent
    data class OnLanguageChange(val language: String) : MapEvent
    data class OnSettingsChange(val hideMarkersWhileMoving: Boolean, val clusterMarkers: Boolean) : MapEvent
}
