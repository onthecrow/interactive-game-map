package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.TravelNetworkRepository
import com.onthecrow.interactivegamemap.map.model.TravelNetwork

/** HoYoLAB's map has no roads, so Genshin has no routes yet; its teleport waypoints could be the fast travel points. */
internal class GenshinTravelNetworkRepository : TravelNetworkRepository {
    override suspend fun getTravelNetwork(mapId: Int): TravelNetwork? = null
}
