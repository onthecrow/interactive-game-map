package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.data.RemoteDataSource
import com.onthecrow.interactivegamemap.map.mapper.toMapLocation
import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class LocationsRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
) : LocationsRepository {
    override fun getLocations(): Flow<List<MapLocation>> {
        return flow {
            emit(
                remoteDataSource.getLocations()
                    .map { location -> location.toMapLocation() }
            )
        }
    }
}
