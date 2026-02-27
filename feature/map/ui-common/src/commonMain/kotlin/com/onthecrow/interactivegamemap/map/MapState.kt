package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation

data class MapState(
    val locations: List<MapLocation> = emptyList(),
)
