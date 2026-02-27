package com.onthecrow.interactivegamemap.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class MapViewModel(
    getLocationsUseCase: GetLocationsUseCase,
): ViewModel() {
    val state: StateFlow<MapState> = getLocationsUseCase()
        .map { locations -> MapState(locations = locations) }
        .stateIn(viewModelScope, SharingStarted.Lazily, MapState())
}