package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.nomadrates.uicore.Event

internal sealed interface MapEvent : Event {
    data object OnClearSearchClick : MapEvent
    data object OnLocationDismiss : MapEvent
    data class OnSearchValueChange(val value: String) : MapEvent
    data class OnLocationsLoaded(val locations: List<MapLocation>) : MapEvent
    data class OnLocationClick(val location: MapLocation) : MapEvent
}
