package com.onthecrow.interactivegamemap.map.mapper

import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog

internal fun Location.toMapLocation(catalog: MarkerCatalog): MapLocation {
    return MapLocation(
        id = id,
        mapId = mapId,
        categoryId = categoryId.toString(),
        name = title,
        description = description ?: "",
        lat = lat,
        lng = lng,
        cycle = tags.firstOrNull { it in LocationsFilter.CYCLES },
        // Every interior has one floor with the same id
        dungeon = interiorId?.let { LocationDungeon(dungeonId = it, floorId = it, entranceIds = emptyList()) },
        pin = catalog.categories[categoryId.toString()]?.pin,
    )
}
