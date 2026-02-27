package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.Flow

interface GetLocationsUseCase {
    suspend operator fun invoke(query: String? = null): List<MapLocation>
}
