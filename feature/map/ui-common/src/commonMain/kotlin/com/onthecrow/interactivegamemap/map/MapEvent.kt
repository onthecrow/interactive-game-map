package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapBounds
import com.onthecrow.interactivegamemap.map.model.MapsConfig
import com.onthecrow.interactivegamemap.map.model.Route
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

    /** All of [bounds] are to be shown: the markers the list has just turned on with the ones shown before. */
    data class OnFitBounds(val bounds: MapBounds) : MapEvent

    /** The hint at the route button comes up, once for good. */
    data object OnRouteHintShow : MapEvent

    /** The route button: builds a route, or offers to end the one there is. */
    data object OnRouteClick : MapEvent
    data object OnRouteBuildCancel : MapEvent
    data object OnRouteExitConfirm : MapEvent
    data object OnRouteExitDismiss : MapEvent
    /** Keep going: the route goes on at its first location not found yet. */
    data object OnRouteContinue : MapEvent
    data class OnRouteAvailabilityChange(val isAvailable: Boolean) : MapEvent
    data class OnRouteCandidatesCounted(val count: Int) : MapEvent
    data class OnRouteBuildProgress(val progress: RouteBuildProgress) : MapEvent
    /** The build stopped without a route. */
    data object OnRouteBuildFailed : MapEvent
    /** [locations] are the ones of [route] in its order; [page] is where the route has come to, and is selected. */
    data class OnRouteLoaded(val route: Route, val locations: List<MapLocation>, val page: Int = 0) : MapEvent
    /** The locations of the route again, in another language. */
    data class OnRouteLocationsLoaded(val locations: List<MapLocation>) : MapEvent
    /** Markers of the route were hidden in the marker list: the route is dropped. */
    data object OnRouteHidden : MapEvent
    /** A tap on the fast travel point of the route numbered [number]. */
    data class OnRouteStopClick(val number: Int) : MapEvent
    /** A swipe or an arrow of the popup of the route. */
    data class OnRoutePageChange(val page: Int) : MapEvent
}
