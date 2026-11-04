package com.onthecrow.interactivegamemap.map

import androidx.lifecycle.viewModelScope
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class MapViewModel(
    private val getLocationsUseCase: GetLocationsUseCase,
    private val getLocationsByIdsUseCase: GetLocationsByIdsUseCase,
    private val getMarkerGroupsUseCase: GetMarkerGroupsUseCase,
    preferences: MapPreferences,
    configProvider: RemoteConfigProvider,
    reducer: MapReducer,
): BaseViewModel<MapEvent, MapScreenState, MapReducer>(reducer) {
    private var getLocationJob: Job? = null
    private var getEntrancesJob: Job? = null
    private var getMarkerGroupsJob: Job? = null

    override val initialState: MapScreenState get() = MapScreenState()

    init {
        configProvider.getRemoteConfigFlow()
            .onEach { onEvent(MapEvent.OnConfigLoaded(it)) }
            .launchIn(viewModelScope)
        state
            .map { it.locationsFilter }
            .distinctUntilChanged()
            .onEach { filter -> filter?.let(::getLocations) }
            .launchIn(viewModelScope)
        state
            .map { it.selectedLocation }
            .distinctUntilChanged()
            .onEach(::getEntrances)
            .launchIn(viewModelScope)
        state
            .map { it.currentMap?.id }
            .distinctUntilChanged()
            .onEach { mapId -> mapId?.let(::getMarkerGroups) }
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
    }

    private fun getLocations(filter: LocationsFilter) {
        getLocationJob?.cancel()
        getLocationJob = viewModelScope.launch(Dispatchers.IO) {
            val locations = getLocationsUseCase(filter)
            onEvent(MapEvent.OnLocationsLoaded(locations))
        }
    }

    private fun getEntrances(location: MapLocation?) {
        getEntrancesJob?.cancel()
        val entranceIds = location?.dungeon?.entranceIds.orEmpty()
        if (entranceIds.isEmpty()) return
        getEntrancesJob = viewModelScope.launch(Dispatchers.IO) {
            val entrances = getLocationsByIdsUseCase(entranceIds)
            onEvent(MapEvent.OnEntrancesLoaded(entrances))
        }
    }

    private fun getMarkerGroups(mapId: Int) {
        getMarkerGroupsJob?.cancel()
        getMarkerGroupsJob = viewModelScope.launch(Dispatchers.IO) {
            val groups = getMarkerGroupsUseCase(mapId)
            onEvent(MapEvent.OnMarkerGroupsLoaded(groups))
        }
    }
}
