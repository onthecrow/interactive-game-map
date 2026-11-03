package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.data.LocalDataSource
import com.onthecrow.interactivegamemap.map.mapper.toMapLocation
import com.onthecrow.interactivegamemap.map.mapper.toMapLocations
import com.onthecrow.interactivegamemap.map.mapper.withAnimals
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

internal class LocationsRepositoryImpl(
    private val localDataSource: LocalDataSource,
) : LocationsRepository {

    // todo inject
    private val scope = CoroutineScope(Dispatchers.Default)

    private val animals = scope.async(start = CoroutineStart.LAZY) {
        localDataSource.getAnimals()
    }

    private val catalog = scope.async(start = CoroutineStart.LAZY) {
        localDataSource.getMarkerCatalog().withAnimals(animals.await(), localDataSource.getAnimalPinUrl())
    }

    private val locations = scope.async(start = CoroutineStart.LAZY) {
        val catalog = catalog.await()
        localDataSource.getLocations().map { location -> location.toMapLocation(catalog) } +
            animals.await().toMapLocations(catalog)
    }

    // The markers of the source are in English only
    override suspend fun getLocations(language: String): List<MapLocation> {
        return locations.await()
    }

    override suspend fun getMarkerCatalog(language: String): MarkerCatalog {
        return catalog.await()
    }
}
