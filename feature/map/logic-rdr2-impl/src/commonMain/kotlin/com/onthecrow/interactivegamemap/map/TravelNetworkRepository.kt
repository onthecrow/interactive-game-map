package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.data.LocalDataSource
import com.onthecrow.interactivegamemap.map.model.FastTravelPoint
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.Road
import com.onthecrow.interactivegamemap.map.model.Roads
import com.onthecrow.interactivegamemap.map.model.TravelCosts
import com.onthecrow.interactivegamemap.map.model.TravelNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/** The roads of the world map from the build, its fast travel points are the markers of their category. */
internal class TravelNetworkRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val locationsRepository: LocationsRepository,
) : TravelNetworkRepository {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val roads = scope.async(start = CoroutineStart.LAZY) { localDataSource.getRoads() }

    private val network = scope.async(start = CoroutineStart.LAZY) {
        val roads = roads.await()
        val fastTravelPoints = locationsRepository.getLocations(LocationsFilter.DEFAULT_LANGUAGE)
            .filter { it.mapId == roads.mapId && it.categoryId == roads.fastTravelCategoryId }
            .map { FastTravelPoint(id = it.id, name = it.name, lat = it.lat, lng = it.lng) }
        roads.toTravelNetwork(fastTravelPoints)
    }

    override suspend fun getTravelNetwork(mapId: Int): TravelNetwork? {
        return network.await().takeIf { roads.await().mapId == mapId }
    }
}

internal fun Roads.toTravelNetwork(fastTravelPoints: List<FastTravelPoint>): TravelNetwork {
    return TravelNetwork(
        junctions = junctions.toGeoPoints(),
        roads = roads.map { Road(from = it.from, to = it.to, path = it.path.toGeoPoints()) },
        fastTravelPoints = fastTravelPoints,
        costs = TravelCosts(
            offRoadFactor = costs.offRoadFactor,
            fastTravel = costs.fastTravel,
            directReach = costs.directReach,
        ),
    )
}

private fun List<Double>.toGeoPoints(): List<GeoPoint> = chunked(2) { (lat, lng) -> GeoPoint(lat, lng) }
