package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.Flow

// todo make it common for every game
internal class GetLocationsUseCaseImpl(private val repository: LocationsRepository): GetLocationsUseCase {
    override suspend operator fun invoke(query: String?): List<MapLocation> {
        val query = query?.trim() ?: ""
        return repository.getLocations()
            .filter { it.mapId == 2 }
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(1000)
    }
}