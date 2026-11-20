package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The maps with their names in [language] where the markers' catalog translates them. */
interface GetMapsUseCase {
    operator fun invoke(language: String): Flow<MapsConfig>
}

class GetMapsUseCaseImpl(
    private val mapsRepository: MapsRepository,
    private val locationsRepository: LocationsRepository,
) : GetMapsUseCase {
    override fun invoke(language: String): Flow<MapsConfig> {
        return mapsRepository.maps().map { config ->
            val names = locationsRepository.getMarkerCatalog(language).mapNames
            config.copy(maps = config.maps.map { map -> names[map.id]?.let { map.copy(name = it) } ?: map })
        }
    }
}
