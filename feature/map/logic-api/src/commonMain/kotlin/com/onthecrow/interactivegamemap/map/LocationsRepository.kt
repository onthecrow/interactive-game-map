package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog

interface LocationsRepository {
    suspend fun getLocations(): List<MapLocation>
    suspend fun getMarkerCatalog(): MarkerCatalog
}
