package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.Flow

interface GetLocationsUseCase {
    operator fun invoke(query: String? = null): Flow<List<MapLocation>>
}
