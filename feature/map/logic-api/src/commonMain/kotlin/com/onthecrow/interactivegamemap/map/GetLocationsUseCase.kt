package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation

interface GetLocationsUseCase {
    suspend operator fun invoke(filter: LocationsFilter): List<MapLocation>
}

class GetLocationsUseCaseImpl(private val repository: LocationsRepository) : GetLocationsUseCase {
    override suspend operator fun invoke(filter: LocationsFilter): List<MapLocation> {
        val query = filter.query.trim()
        val locations = repository.getLocations().asSequence().filter { it.mapId == filter.mapId }
        // A search looks through everything, including the hidden categories
        if (query.isNotEmpty()) {
            return locations
                .filter { it.name.contains(query, ignoreCase = true) }
                .take(MAX_LOCATIONS)
                .toList()
        }
        val categories = repository.getMarkerCatalog().categories
        return locations
            .filter { location ->
                filter.categoryVisibility[location.categoryId]
                    ?: categories[location.categoryId]?.visibleByDefault
                    ?: false
            }
            .filter { location ->
                val selectedCycle = filter.cycles[location.categoryId] ?: LocationsFilter.DEFAULT_CYCLE
                location.cycle == null || location.cycle == selectedCycle
            }
            .filter { location -> filter.bounds?.contains(location.lat, location.lng) ?: true }
            .take(MAX_LOCATIONS)
            .toList()
    }

    private companion object {
        private const val MAX_LOCATIONS = 1000
    }
}
