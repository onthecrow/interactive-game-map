package com.onthecrow.interactivegamemap.map

import androidx.lifecycle.viewModelScope
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import com.onthecrow.interactivegamemap.settings.AppLanguageProvider
import com.onthecrow.interactivegamemap.settings.SettingsDestination
import com.onthecrow.interactivegamemap.settings.SettingsRepository
import com.onthecrow.nomadrates.navigation.Navigator
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
    reducer: MapReducer,
): BaseViewModel<MapEvent, MapScreenState, MapReducer>(reducer) {
    private var getLocationJob: Job? = null
    private var getEntrancesJob: Job? = null
    private var getMarkerGroupsJob: Job? = null

    override val initialState: MapScreenState get() = MapScreenState()

    init {
        // The names of the maps are translated with the markers
        combine(configProvider.getRemoteConfigFlow(), languageProvider.language) { config, language ->
            val names = getMapNamesUseCase(language)
            config.copy(maps = config.maps.map { map -> names[map.id]?.let { map.copy(name = it) } ?: map })
        }
            .onEach { onEvent(MapEvent.OnConfigLoaded(it)) }
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
        getFoundLocationIdsUseCase()
            .onEach { onEvent(MapEvent.OnFoundLocationsLoaded(it)) }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnFoundClick>()
            .onEach { setLocationFoundUseCase(it.locationId, it.locationId !in state.value.foundLocationIds) }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnLocationLinkClick>()
            .onEach { openLocationLink(it.locationId) }
            .launchIn(viewModelScope)
        eventFlow
            .filterIsInstance<MapEvent.OnSettingsClick>()
            .onEach { navigator.navigate(SettingsDestination) }
            .launchIn(viewModelScope)
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
