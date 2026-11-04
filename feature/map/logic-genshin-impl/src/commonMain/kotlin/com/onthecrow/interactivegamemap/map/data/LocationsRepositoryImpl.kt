package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.mapper.toMapLocations
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

    private val catalog = scope.async(start = CoroutineStart.LAZY) {
        localDataSource.getMarkerCatalog()
    }

    private val locations = scope.async(start = CoroutineStart.LAZY) {
        localDataSource.getLocations().toMapLocations(catalog.await())
    }

    override suspend fun getLocations(): List<MapLocation> {
        return locations.await()
    }

    override suspend fun getMarkerCatalog(): MarkerCatalog {
        return catalog.await()
    }
}
