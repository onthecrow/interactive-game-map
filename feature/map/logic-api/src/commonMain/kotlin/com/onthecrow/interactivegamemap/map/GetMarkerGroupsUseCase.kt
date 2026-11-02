package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapMarkerCategory
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup

interface GetMarkerGroupsUseCase {
    suspend operator fun invoke(mapId: Int, language: String): List<MapMarkerGroup>
}

class GetMarkerGroupsUseCaseImpl(private val repository: LocationsRepository) : GetMarkerGroupsUseCase {
    override suspend operator fun invoke(mapId: Int, language: String): List<MapMarkerGroup> {
        val catalog = repository.getMarkerCatalog(language)
        val locations = repository.getLocations(language)
            .filter { it.mapId == mapId }
            .groupBy { it.categoryId }
        return catalog.groupsByMap[mapId].orEmpty().mapNotNull { group ->
            val categories = group.categoryIds.mapNotNull { id ->
                val category = catalog.categories[id]
                val categoryLocations = locations[id]
                if (category == null || categoryLocations == null) {
                    null
                } else {
                    MapMarkerCategory(
                        category = category,
                        count = categoryLocations.size,
                        countByCycle = categoryLocations
                            .mapNotNull { it.cycle }
                            .groupingBy { it }
                            .eachCount(),
                    )
                }
            }
            categories.takeIf { it.isNotEmpty() }?.let {
                MapMarkerGroup(
                    id = group.id,
                    name = group.name,
                    description = group.description,
                    categories = it,
                )
            }
        }
    }
}
