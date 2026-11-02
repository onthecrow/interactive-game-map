package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.mapper.localized
import com.onthecrow.interactivegamemap.map.mapper.toMapLocations
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    // Only the last language is kept: the localized locations are a copy of every location
    private val locales = LastKeyCache(scope) { language -> localDataSource.getLocale(language) }

    private val localizedCatalogs = LastKeyCache(scope) { language ->
        val catalog = catalog.await()
        locales.get(language)?.let { catalog.localized(it) } ?: catalog
    }

    private val localizedLocations = LastKeyCache(scope) { language ->
        val locations = locations.await()
        val locale = locales.get(language) ?: return@LastKeyCache locations
        locations.map { location ->
            locale.categories[location.categoryId]?.let { location.copy(name = it) } ?: location
        }
    }

    override suspend fun getLocations(language: String): List<MapLocation> {
        return localizedLocations.get(language)
    }

    override suspend fun getMarkerCatalog(language: String): MarkerCatalog {
        return localizedCatalogs.get(language)
    }
}

/** Loads a value once for the last key it was asked for. */
private class LastKeyCache<T>(
    private val scope: CoroutineScope,
    private val load: suspend (String) -> T,
) {
    private val mutex = Mutex()
    private var cached: Pair<String, Deferred<T>>? = null

    suspend fun get(key: String): T {
        val value = mutex.withLock {
            cached?.takeIf { it.first == key }?.second
                ?: scope.async(start = CoroutineStart.LAZY) { load(key) }.also { cached = key to it }
        }
        return value.await()
    }
}
