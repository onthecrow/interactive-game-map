package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.Flow

interface LocationsRepository {
    fun getLocations(): Flow<List<MapLocation>>
}
