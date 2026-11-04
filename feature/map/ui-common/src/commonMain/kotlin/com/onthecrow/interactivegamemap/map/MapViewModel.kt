package com.onthecrow.interactivegamemap.map

import androidx.lifecycle.viewModelScope
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.MarkerFoundChangeEvent
import com.onthecrow.interactivegamemap.analytics.SearchEvent
import com.onthecrow.interactivegamemap.analytics.SearchType
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import com.onthecrow.interactivegamemap.settings.AppLanguageProvider
import com.onthecrow.interactivegamemap.settings.SettingsDestination
import com.onthecrow.interactivegamemap.settings.SettingsRepository
import com.onthecrow.nomadrates.navigation.Navigator
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// A search is logged once the user stops typing
private val SEARCH_LOG_DELAY = 1500.milliseconds
private const val MIN_SEARCH_TERM_LENGTH = 2

private data class SearchInput(val type: SearchType, val term: String)

internal class MapViewModel(
    private val getLocationsUseCase: GetLocationsUseCase,
    private val getLocationsByIdsUseCase: GetLocationsByIdsUseCase,
    private val getMarkerGroupsUseCase: GetMarkerGroupsUseCase,
    private val getMapNamesUseCase: GetMapNamesUseCase,
    private val setLocationFoundUseCase: SetLocationFoundUseCase,
    getFoundLocationIdsUseCase: GetFoundLocationIdsUseCase,
    preferences: MapPreferences,
    configProvider: RemoteConfigProvider,
    settingsRepository: SettingsRepository,
    languageProvider: AppLanguageProvider,
    navigator: Navigator,
    private val analytics: Analytics,
    reducer: MapReducer,
): BaseViewModel<MapEvent, MapScreenState, MapReducer>(reducer) {
    private var getLocationJob: Job? = null
    private var getEntrancesJob: Job? = null
    private var getMarkerGroupsJob: Job? = null

    override val initialState: MapScreenState get() = MapScreenState()

    init {
        // Read before the config comes, so the map of the last launch opens instead of the default one
        val savedMapView = preferences.getMapViewSettings()
        // The names of the maps are translated with the markers
        combine(configProvider.getRemoteConfigFlow(), languageProvider.language) { config, language ->
            val names = getMapNamesUseCase(language)
            config.copy(maps = config.maps.map { map -> names[map.id]?.let { map.copy(name = it) } ?: map })
        }
            .onEach { onEvent(MapEvent.OnConfigLoaded(it, savedMapView)) }
            .launchIn(viewModelScope)
        languageProvider.language
            .onEach { onEvent(MapEvent.OnLanguageChange(it)) }
            .launchIn(viewModelScope)
        settingsRepository.settings
            .onEach { onEvent(MapEvent.OnSettingsChange(it.hideMarkersWhileMoving, it.clusterMarkers)) }
            .launchIn(viewModelScope)
        state
            .map { it.locationsRequest }
            .distinctUntilChanged()
            .onEach { request -> request?.let(::getLocations) }
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
    }

    private suspend fun setLocationFound(locationId: Int) {
        val state = state.value
        val isFound = locationId !in state.foundLocationIds
        setLocationFoundUseCase(locationId, isFound)
        analytics.log(
            MarkerFoundChangeEvent(
                locationId = locationId,
                categoryId = state.selectedLocation?.takeIf { it.id == locationId }?.categoryId,
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

    private fun getMarkerGroups(mapId: Int, language: String) {
        getMarkerGroupsJob?.cancel()
        getMarkerGroupsJob = viewModelScope.launch(Dispatchers.IO) {
            val groups = getMarkerGroupsUseCase(mapId, language)
            onEvent(MapEvent.OnMarkerGroupsLoaded(groups))
        }
    }
}
