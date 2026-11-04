package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation

interface GetLocationsByIdsUseCase {
    suspend operator fun invoke(ids: List<Int>): List<MapLocation>
}

class GetLocationsByIdsUseCaseImpl(private val repository: LocationsRepository) : GetLocationsByIdsUseCase {
    override suspend operator fun invoke(ids: List<Int>): List<MapLocation> {
        val ids = ids.toSet()
        return repository.getLocations().filter { it.id in ids }
    }
}
