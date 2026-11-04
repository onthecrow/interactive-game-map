package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog

/** The names come in [language], a language tag, or in English when they aren't translated to it. */
interface LocationsRepository {
    suspend fun getLocations(language: String): List<MapLocation>
    suspend fun getMarkerCatalog(language: String): MarkerCatalog
}
