package com.onthecrow.interactivegamemap.map

import com.onthecrow.nomadrates.database.FoundMarkerDao
import com.onthecrow.nomadrates.database.FoundMarkerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FoundLocationsRepository {
    fun getFoundLocationIds(): Flow<Set<Int>>
    suspend fun setLocationFound(locationId: Int, isFound: Boolean)

    /** Marks [locationIds] as found, the number of the ones that weren't found before. */
    suspend fun addFoundLocations(locationIds: Collection<Int>): Int
}

internal class FoundLocationsRepositoryImpl(private val dao: FoundMarkerDao) : FoundLocationsRepository {
    override fun getFoundLocationIds(): Flow<Set<Int>> {
        return dao.getMarkerIds().map { it.toSet() }
    }

    override suspend fun addFoundLocations(locationIds: Collection<Int>): Int {
        if (locationIds.isEmpty()) return 0
        return dao.insertAll(locationIds.distinct().map { FoundMarkerEntity(it) }).count { it != -1L }
    }

    override suspend fun setLocationFound(locationId: Int, isFound: Boolean) {
        if (isFound) {
            dao.insert(FoundMarkerEntity(locationId))
        } else {
            dao.delete(locationId)
        }
    }
}
