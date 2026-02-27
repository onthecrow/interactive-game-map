package com.onthecrow.interactivegamemap.map.mapper

import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.MapLocation

internal fun Location.toMapLocation(): MapLocation {
    return MapLocation(
        id = id,
        mapId = -1,
        name = title,
        description = description ?: "",
        lat = lat,
        lng = lng,
    )
}
