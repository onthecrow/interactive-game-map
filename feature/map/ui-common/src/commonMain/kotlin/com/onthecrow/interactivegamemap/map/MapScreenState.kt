package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.nomadrates.uicore.State

data class MapScreenState(
    val searchValue: String = "",
    val selectedLocation: MapLocation? = null,
    val locations: List<MapLocation> = emptyList(),
): State
