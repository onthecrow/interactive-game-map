package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.data.RemoteDataSource
import com.onthecrow.interactivegamemap.map.mapper.toMapLocation
import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

internal class LocationsRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
) : LocationsRepository {

    private val locationsFlow = MutableStateFlow<List<MapLocation>>(emptyList())

    init {
        // todo inject
        MainScope().launch {
            locationsFlow.emit(
                remoteDataSource.getLocations()
                    .map { location -> location.toMapLocation() }
            )
        }
    }

    override suspend fun getLocations(): List<MapLocation> {
        return locationsFlow.value
    }
}
