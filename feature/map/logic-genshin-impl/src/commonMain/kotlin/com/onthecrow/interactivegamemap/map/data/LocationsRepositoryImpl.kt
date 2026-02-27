package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocationsRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
) : LocationsRepository {
    override fun getLocations(): Flow<List<MapLocation>> {
        return flow {
            val locale = remoteDataSource.getLocale()

            emit(
                remoteDataSource.getLocations().data
                    // todo tmp
                    .filter { it.mapId == 2 }
                    .takeLast(500)
                    .map {
                        MapLocation(
                            id = it.id,
                            name = locale.en?.get(it.type) ?: "",
                            description = "",
                            lat = it.lat,
                            lng = it.lng,
                        )
                    }
            )
        }
    }
}