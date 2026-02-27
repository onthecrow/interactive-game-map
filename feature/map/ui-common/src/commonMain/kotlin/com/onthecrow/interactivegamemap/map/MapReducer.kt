package com.onthecrow.interactivegamemap.map

import com.onthecrow.nomadrates.uicore.Reducer

internal class MapReducer: Reducer<MapScreenState, MapEvent> {
    override suspend fun reduce(
        state: MapScreenState,
        event: MapEvent
    ): MapScreenState {
        return when (event) {
            is MapEvent.OnClearSearchClick -> state.copy(searchValue = "")
            is MapEvent.OnSearchValueChange -> state.copy(searchValue = event.value)
            is MapEvent.OnLocationsLoaded -> state.copy(locations = event.locations)
            is MapEvent.OnLocationClick -> state.copy(selectedLocation = event.location)
            is MapEvent.OnLocationDismiss -> state.copy(selectedLocation = null)
            else -> state
        }
    }
}
