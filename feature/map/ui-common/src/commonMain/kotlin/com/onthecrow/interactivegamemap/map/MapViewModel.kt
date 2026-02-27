package com.onthecrow.interactivegamemap.map

import androidx.lifecycle.viewModelScope
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class MapViewModel(
    private val getLocationsUseCase: GetLocationsUseCase,
    reducer: MapReducer,
): BaseViewModel<MapEvent, MapScreenState, MapReducer>(reducer) {
    private var getLocationJob: Job? = null

    override val initialState: MapScreenState get() = MapScreenState()

    init {
        getLocations("")
        eventFlow
            .onEach { event ->
                when (event) {
                    MapEvent.OnClearSearchClick -> getLocations("")
                    is MapEvent.OnSearchValueChange -> getLocations(event.value)
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getLocations(query: String) {
        getLocationJob?.cancel()
        getLocationJob = viewModelScope.launch(Dispatchers.IO) {
            val locations = getLocationsUseCase(query)
            onEvent(MapEvent.OnLocationsLoaded(locations))
        }
    }

//    val state: StateFlow<MapScreenState> = getLocationsUseCase()
//        .map { locations -> MapScreenState(locations = locations) }
//        .stateIn(viewModelScope, SharingStarted.Lazily, MapScreenState())

}