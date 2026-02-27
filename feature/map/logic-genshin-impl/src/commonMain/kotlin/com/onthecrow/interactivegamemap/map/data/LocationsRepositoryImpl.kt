package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.model.MapLocation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class LocationsRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
) : LocationsRepository {

    private val locationsFlow = MutableStateFlow<List<MapLocation>>(emptyList())

    init {
        // todo inject
        MainScope().launch {
            val locale = remoteDataSource.getLocale()
            locationsFlow.emit(
                remoteDataSource.getLocations().data
                    .map {
                        MapLocation(
                            id = it.id,
                            mapId = it.mapId,
                            name = locale.en?.get(it.type) ?: "",
                            description = "",
                            lat = it.lat,
                            lng = it.lng,
                        )
                    }
            )
        }
    }

    override suspend fun getLocations(): List<MapLocation> {
        return locationsFlow.value
    }
}