package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.TravelNetwork

/** The roads and fast travel points of the maps, for routes. A game without them has no routes. */
interface TravelNetworkRepository {
    /** Null when [mapId] has no roads: then no route is offered on it. */
    suspend fun getTravelNetwork(mapId: Int): TravelNetwork?
}
