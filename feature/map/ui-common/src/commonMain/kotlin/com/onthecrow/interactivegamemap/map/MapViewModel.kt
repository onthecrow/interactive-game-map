package com.onthecrow.interactivegamemap.map

import androidx.lifecycle.viewModelScope
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.analytics.MarkerFoundChangeEvent
import com.onthecrow.interactivegamemap.analytics.RouteBuildEvent
import com.onthecrow.interactivegamemap.analytics.RouteBuildResult
import com.onthecrow.interactivegamemap.analytics.RouteEndReason
import com.onthecrow.interactivegamemap.analytics.RouteExitEvent
import com.onthecrow.interactivegamemap.analytics.SearchEvent
import com.onthecrow.interactivegamemap.analytics.SearchType
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.Route
import com.onthecrow.interactivegamemap.map.model.RouteBuildStage
import com.onthecrow.interactivegamemap.map.model.RouteBuildState
import com.onthecrow.interactivegamemap.settings.AppLanguageProvider
import com.onthecrow.interactivegamemap.settings.SettingsDestination
import com.onthecrow.interactivegamemap.settings.SettingsRepository
import com.onthecrow.interactivegamemap.ui.components.GameSnackbar
import com.onthecrow.interactivegamemap.ui.components.GameSnackbars
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.route_finished
import com.onthecrow.nomadrates.navigation.Navigator
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

// A search is logged once the user stops typing
private val SEARCH_LOG_DELAY = 1500.milliseconds
private const val MIN_SEARCH_TERM_LENGTH = 2

private data class SearchInput(val type: SearchType, val term: String)

// The keys of the crash reports about the route
private const val KEY_ROUTE_MARKERS = "route_markers"
private const val KEY_ROUTE_LINES = "route_lines"
private const val KEY_ROUTE_BUILDING = "route_building"

/** How long the route on the map is, in markers and in lines on the map, and how many markers one being built has. */
private data class RouteSize(val markers: Int, val lines: Int, val building: Int)

/** What the markers of a route are found by: the filters on the whole map, without the found markers. */
private data class RouteCandidatesRequest(val filter: LocationsFilter, val foundLocationIds: Set<Int>)

@OptIn(ExperimentalCoroutinesApi::class)
internal class MapViewModel(
    private val getLocationsUseCase: GetLocationsUseCase,
    private val getLocationsByIdsUseCase: GetLocationsByIdsUseCase,
    private val getMarkerGroupsUseCase: GetMarkerGroupsUseCase,
    getMapsUseCase: GetMapsUseCase,
    observeContentUpdatesUseCase: ObserveContentUpdatesUseCase,
    private val setLocationFoundUseCase: SetLocationFoundUseCase,
    getFoundLocationIdsUseCase: GetFoundLocationIdsUseCase,
    private val isRouteAvailableUseCase: IsRouteAvailableUseCase,
    private val buildRouteUseCase: BuildRouteUseCase,
    private val restoreRouteUseCase: RestoreRouteUseCase,
    private val features: MapFeatures,
    private val preferences: MapPreferences,
    settingsRepository: SettingsRepository,
    private val languageProvider: AppLanguageProvider,
    navigator: Navigator,
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
    private val snackbars: GameSnackbars,
    reducer: MapReducer,
): BaseViewModel<MapEvent, MapScreenState, MapReducer>(reducer) {
    private var getLocationJob: Job? = null
    private var getEntrancesJob: Job? = null
    private var getMarkerGroupsJob: Job? = null
    private var buildRouteJob: Job? = null
    private var routeCandidates: List<MapLocation> = emptyList()
    private var fitJob: Job? = null
    // The categories shown when the marker list was opened
    private var visibleOnListOpen: Set<String> = emptySet()

    override val initialState: MapScreenState get() = MapScreenState()

    init {
        // Read before the config comes, so the map of the last launch opens instead of the default one
        val savedMapView = preferences.getMapViewSettings()
        // The names of the maps are translated with the markers
        languageProvider.language
            .flatMapLatest { language -> getMapsUseCase(language) }
            .onEach { onEvent(MapEvent.OnConfigLoaded(it, savedMapView)) }
            .launchIn(viewModelScope)
        languageProvider.language
            .onEach { onEvent(MapEvent.OnLanguageChange(it)) }
            .launchIn(viewModelScope)
        // New data in the background: the same view shows it
        observeContentUpdatesUseCase()
            .onEach { reloadContent() }
            .launchIn(viewModelScope)
        settingsRepository.settings
            .onEach { onEvent(MapEvent.OnSettingsChange(it.hideMarkersWhileMoving, it.clusterMarkers)) }
            .launchIn(viewModelScope)
        state
            .map { it.locationsRequest }
            .distinctUntilChanged()
            .onEach { request -> request?.let(::getLocations) }
            .launchIn(viewModelScope)
        // Where the markers the filters show are on the whole map: the recenter button is there with them and fits them
        state
            .map { state -> state.locationsFilter?.copy(bounds = null, limit = Int.MAX_VALUE) to state.currentMap?.bounds }
            .distinctUntilChanged()
            .mapLatest { (filter, mapBounds) ->
                filter?.let { withContext(Dispatchers.IO) { locationsBounds(getLocationsUseCase(it), mapBounds) } }
            }
            .onEach { onEvent(MapEvent.OnMarkersBoundsChange(it)) }
            .launchIn(viewModelScope)
        state
            .map { it.selectedLocation }
            .distinctUntilChanged()
            .onEach(::getEntrances)
            .launchIn(viewModelScope)
        state
            .map { state -> state.currentMap?.id?.let { it to state.language } }
            .distinctUntilChanged()
            .onEach { it?.let { (mapId, language) -> getMarkerGroups(mapId, language) } }
            .launchIn(viewModelScope)
        // Launched after the base class has started to listen for events
        viewModelScope.launch {
            onEvent(MapEvent.OnMarkerListSettingsLoaded(preferences.getMarkerListSettings()))
        }
        state
            .map { it.markerListSettings }
            .distinctUntilChanged()
            .drop(1) // the value before the preferences are read
            .onEach(preferences::setMarkerListSettings)
            .launchIn(viewModelScope)
        state
            .mapNotNull { it.mapViewSettings }
            .distinctUntilChanged()
            .onEach(preferences::setMapViewSettings)
            .launchIn(viewModelScope)
        getFoundLocationIdsUseCase()
            .onEach { onEvent(MapEvent.OnFoundLocationsLoaded(it)) }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnFoundClick>()
            .onEach { setLocationFound(it.locationId) }
            .launchIn(viewModelScope)
        logSearches()
        eventFlow
            .filterIsInstance<MapEvent.OnLocationLinkClick>()
            .onEach { openLocationLink(it.locationId) }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnSettingsClick>()
            .onEach { navigator.navigate(SettingsDestination) }
            .launchIn(viewModelScope)
        // Categories turned on in the marker list: the camera shows all the markers once it's closed
        eventFlow
            .filterIsInstance<MapEvent.OnMarkerListClick>()
            .onEach { visibleOnListOpen = state.value.visibleCategoryIds() }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnMarkerListDismiss>()
            .onEach { if ((state.value.visibleCategoryIds() - visibleOnListOpen).isNotEmpty()) fitLocations() }
            .launchIn(viewModelScope)
        if (features.routes) observeRoutes()
        showHints()
    }

    private fun MapScreenState.visibleCategoryIds(): Set<String> {
        return markerGroups.asSequence()
            .flatMap { it.categories }
            .map { it.category }
            .filter { isCategoryVisible(it) }
            .map { it.id }
            .toSet()
    }

    private fun fitLocations() {
        val state = state.value
        val filter = state.locationsFilter?.copy(bounds = null, limit = Int.MAX_VALUE) ?: return
        fitJob?.cancel()
        fitJob = viewModelScope.launch(Dispatchers.IO) {
            val locations = getLocationsUseCase(filter)
            locationsBounds(locations, state.currentMap?.bounds)?.let { onEvent(MapEvent.OnFitBounds(it)) }
        }
    }

    private fun observeRoutes() {
        state
            .map { it.currentMap?.id }
            .distinctUntilChanged()
            .mapLatest { mapId -> mapId != null && isRouteAvailableUseCase(mapId) }
            .onEach { onEvent(MapEvent.OnRouteAvailabilityChange(it)) }
            .launchIn(viewModelScope)
        state
            .map { state ->
                state.locationsFilter
                    ?.takeIf { state.isRouteAvailable }
                    // The markers the filters show, a search doesn't change them
                    ?.let { RouteCandidatesRequest(it.copy(query = "", bounds = null, limit = Int.MAX_VALUE), state.foundLocationIds) }
            }
            .distinctUntilChanged()
            .mapLatest { request ->
                request?.let {
                    withContext(Dispatchers.IO) {
                        getLocationsUseCase(it.filter).filter { location -> location.id !in it.foundLocationIds }
                    }
                }.orEmpty()
            }
            .onEach {
                routeCandidates = it
                onEvent(MapEvent.OnRouteCandidatesCounted(it.size))
            }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnRouteClick>()
            .onEach { if (state.value.route == null) buildRoute() }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnRouteBuildCancel>()
            .onEach { buildRouteJob?.cancel() }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnRouteExitConfirm>()
            .onEach { logRouteExit(RouteEndReason.ENDED) }
            .launchIn(viewModelScope)
        // A route goes through the markers shown when it was built: hiding some of them drops it
        state
            .map { it.hasHiddenRouteLocations }
            .distinctUntilChanged()
            .filter { it }
            .onEach {
                logRouteExit(RouteEndReason.MARKERS_HIDDEN)
                onEvent(MapEvent.OnRouteHidden)
            }
            .launchIn(viewModelScope)
        // The names of the route's locations follow the language
        state
            .map { it.language }
            .distinctUntilChanged()
            .drop(1)
            .onEach { language -> state.value.route?.let { reloadRouteLocations(it, language) } }
            .launchIn(viewModelScope)
        // A crash report tells how long a route is on the map or being built: a long one takes memory
        state
            .map { RouteSize(markers = it.route?.locationIds?.size ?: 0, lines = it.route?.paths?.size ?: 0, building = it.routeBuild?.count ?: 0) }
            .distinctUntilChanged()
            .onEach { size ->
                crashReporter.setKeys(
                    mapOf(KEY_ROUTE_MARKERS to size.markers, KEY_ROUTE_LINES to size.lines, KEY_ROUTE_BUILDING to size.building)
                )
                crashReporter.log("Route: ${size.markers} markers, ${size.lines} lines, building ${size.building}")
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            restoreRoute()
            // Kept from here on, so the route read back isn't forgotten before it is restored
            state
                .map { it.route to it.routePage }
                .distinctUntilChanged()
                .onEach { (route, page) -> preferences.setSavedRoute(route?.let { SavedRoute.of(it.plan, page) }) }
                .launchIn(viewModelScope)
        }
    }

    /**
     * The hints at the buttons, each once ever and one at a time, see [HintManager]. The route one only in an app with
     * routes; someone who has a route kept from before knows the route button already.
     */
    private fun showHints() {
        if (preferences.getSavedRoute() != null) preferences.setHintShown(MapHint.ROUTE)
        val hints = HintManager(
            hints = MapHint.entries.filter { it != MapHint.ROUTE || features.routes },
            priority = { it.priority },
            isShown = preferences::isHintShown,
            setShown = preferences::setHintShown,
        )
        viewModelScope.launch {
            hints.run(
                states = state,
                current = { it.hint },
                canShow = { state, hint -> state.canShowHint(hint) },
                isKnown = { state, hint -> state.knowsHint(hint) },
                show = { onEvent(MapEvent.OnHintShow(it)) },
            )
        }
    }

    private fun buildRoute() {
        val state = state.value
        val mapId = state.currentMap?.id ?: return
        if (!state.canBuildRoute || buildRouteJob?.isActive == true) return
        val candidates = routeCandidates
        val start = TimeSource.Monotonic.markNow()
        onEvent(MapEvent.OnRouteBuildProgress(RouteBuildProgress(RouteBuildStage.PREPARING, 0f, candidates.size)))
        buildRouteJob = viewModelScope.launch {
            try {
                buildRouteUseCase(mapId, candidates).collect { build ->
                    when (build) {
                        is RouteBuildState.Building -> onEvent(
                            MapEvent.OnRouteBuildProgress(RouteBuildProgress(build.stage, build.progress, candidates.size))
                        )
                        is RouteBuildState.Built -> {
                            val byId = candidates.associateBy { it.id }
                            onEvent(MapEvent.OnRouteLoaded(build.route, build.route.locationIds.mapNotNull { byId[it] }))
                            logRouteBuild(RouteBuildResult.BUILT, candidates.size, start, build.route)
                        }
                        is RouteBuildState.Unavailable -> {
                            onEvent(MapEvent.OnRouteBuildFailed)
                            logRouteBuild(RouteBuildResult.UNAVAILABLE, candidates.size, start)
                        }
                    }
                }
            } catch (e: CancellationException) {
                logRouteBuild(RouteBuildResult.CANCELLED, candidates.size, start)
                throw e
            } catch (e: Exception) {
                onEvent(MapEvent.OnRouteBuildFailed)
                logRouteBuild(RouteBuildResult.FAILED, candidates.size, start)
            }
        }
    }

    private fun logRouteBuild(result: RouteBuildResult, count: Int, start: TimeSource.Monotonic.ValueTimeMark, route: Route? = null) {
        analytics.log(
            RouteBuildEvent(
                result = result,
                locationsCount = count,
                fastTravels = route?.fastTravels?.size,
                buildTime = start.elapsedNow(),
            )
        )
    }

    private fun logRouteExit(reason: RouteEndReason) {
        val state = state.value
        val locations = state.routeLocations
        analytics.log(
            RouteExitEvent(
                reason = reason,
                locationsCount = locations.size,
                foundCount = locations.count { it.id in state.foundLocationIds },
                page = state.routePage,
            )
        )
    }

    private suspend fun restoreRoute() {
        val saved = preferences.getSavedRoute() ?: return
        val route = withContext(Dispatchers.IO) { restoreRouteUseCase(saved.toPlan()) } ?: return
        val locations = routeLocations(route, languageProvider.language.first())
        onEvent(MapEvent.OnRouteLoaded(route, locations, saved.page))
    }

    private fun reloadRouteLocations(route: Route, language: String) {
        viewModelScope.launch {
            onEvent(MapEvent.OnRouteLocationsLoaded(routeLocations(route, language)))
        }
    }

    private suspend fun routeLocations(route: Route, language: String): List<MapLocation> {
        val byId = withContext(Dispatchers.IO) { getLocationsByIdsUseCase(route.locationIds, language) }.associateBy { it.id }
        return route.locationIds.mapNotNull { byId[it] }
    }

    private suspend fun setLocationFound(locationId: Int) {
        val state = state.value
        val isFound = locationId !in state.foundLocationIds
        // The reducer has shown all of the route already
        val finishesRoute = state.finishesRoute(locationId)
        setLocationFoundUseCase(locationId, isFound)
        if (finishesRoute) snackbars.show(GameSnackbar(Res.string.route_finished))
        // On a page of the route the selection may have gone on to the next location already
        val location = state.selectedLocation?.takeIf { it.id == locationId }
            ?: state.routeLocations.firstOrNull { it.id == locationId }
        analytics.log(
            MarkerFoundChangeEvent(
                locationId = locationId,
                categoryId = location?.categoryId,
                isFound = isFound,
                foundTotal = state.foundLocationIds.size + if (isFound) 1 else -1,
            )
        )
    }

    @OptIn(FlowPreview::class)
    private fun logSearches() {
        merge(
            searchInputs<MapEvent.OnSearchValueChange>(SearchType.MAP) { it.value },
            searchInputs<MapEvent.OnClearSearchClick>(SearchType.MAP) { "" },
            searchInputs<MapEvent.OnSearchLinkClick>(SearchType.LINK) { it.query },
            searchInputs<MapEvent.OnMarkerListQueryChange>(SearchType.MARKER_LIST) { it.value },
            searchInputs<MapEvent.OnMarkerListDismiss>(SearchType.MARKER_LIST) { "" },
        )
            .debounce(SEARCH_LOG_DELAY)
            .distinctUntilChanged()
            .filter { it.term.length >= MIN_SEARCH_TERM_LENGTH }
            .onEach { input -> analytics.log(SearchEvent(input.type, input.term, searchResultsCount(input.type))) }
            .launchIn(viewModelScope)
    }

    private inline fun <reified T : MapEvent> searchInputs(type: SearchType, crossinline term: (T) -> String): Flow<SearchInput> {
        return eventFlow.filterIsInstance<T>().map { SearchInput(type, term(it).trim().lowercase()) }
    }

    // The locations of the term are loaded by the time it's logged
    private fun searchResultsCount(type: SearchType): Int {
        val state = state.value
        return when (type) {
            SearchType.MARKER_LIST -> state.foundCategories.size
            SearchType.MAP, SearchType.LINK -> state.locations.size + state.clusters.sumOf { it.count }
        }
    }

    private fun openLocationLink(locationId: Int) {
        val language = state.value.language
        viewModelScope.launch(Dispatchers.IO) {
            getLocationsByIdsUseCase(listOf(locationId), language).firstOrNull()?.let {
                onEvent(MapEvent.OnLocationLinkLoaded(it))
            }
        }
    }

    private fun getLocations(request: LocationsRequest) {
        getLocationJob?.cancel()
        getLocationJob = viewModelScope.launch(Dispatchers.IO) {
            val locations = getLocationsUseCase(request.filter)
            val clustered = request.clusterZoom?.let { clusterLocations(locations, it) }
            onEvent(
                MapEvent.OnLocationsLoaded(
                    locations = clustered?.locations ?: locations,
                    clusters = clustered?.clusters.orEmpty(),
                )
            )
        }
    }

    private fun getEntrances(location: MapLocation?) {
        getEntrancesJob?.cancel()
        val entranceIds = location?.dungeon?.entranceIds.orEmpty()
        if (entranceIds.isEmpty()) return
        val language = state.value.language
        getEntrancesJob = viewModelScope.launch(Dispatchers.IO) {
            val entrances = getLocationsByIdsUseCase(entranceIds, language)
            onEvent(MapEvent.OnEntrancesLoaded(entrances))
        }
    }

    private fun reloadContent() {
        val state = state.value
        state.locationsRequest?.let(::getLocations)
        state.currentMap?.let { getMarkerGroups(it.id, state.language) }
    }

    private fun getMarkerGroups(mapId: Int, language: String) {
        getMarkerGroupsJob?.cancel()
        getMarkerGroupsJob = viewModelScope.launch(Dispatchers.IO) {
            val groups = getMarkerGroupsUseCase(mapId, language)
            onEvent(MapEvent.OnMarkerGroupsLoaded(groups))
        }
    }
}
