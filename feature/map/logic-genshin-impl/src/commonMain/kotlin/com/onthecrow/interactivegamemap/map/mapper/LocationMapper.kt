package com.onthecrow.interactivegamemap.map.mapper

import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.LocationResponse
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerLocale

private const val META_DUNGEON_PREFIX = "l="

internal fun LocationResponse.toMapLocations(catalog: MarkerCatalog): List<MapLocation> {
    val ids = data.id.runningReduce(Int::plus) // ids are delta encoded
    return ids.indices
        // Only a limited number of markers can be displayed at once, so the main ones go first
        .sortedBy { catalog.categories[data.type[it]]?.visibleByDefault != true }
        .map { i ->
            MapLocation(
                id = ids[i],
                mapId = data.mid[i],
                categoryId = data.type[i],
                name = catalog.categories[data.type[i]]?.name ?: "",
                description = "",
                lat = data.lat[i],
                lng = data.lng[i],
                dungeon = parseDungeon(data.meta[i]),
                pin = catalog.categories[data.type[i]]?.pin,
            )
        }
}

// Meta format: "[i;]l=<entrance ids separated with _>|<dungeon id>|<floor id>"
private fun parseDungeon(meta: String): LocationDungeon? {
    val dungeon = meta.split(';')
        .firstOrNull { it.startsWith(META_DUNGEON_PREFIX) }
        ?.removePrefix(META_DUNGEON_PREFIX)
        ?: return null
    val (entranceIds, dungeonId, floorId) = dungeon.split('|')
    return LocationDungeon(
        dungeonId = dungeonId.toInt(),
        floorId = floorId.toIntOrNull(),
        entranceIds = entranceIds.split('_').mapNotNull { it.toIntOrNull() },
    )
}

/** The names of [locale] in place of the English ones, the untranslated ones stay. */
internal fun MarkerCatalog.localized(locale: MarkerLocale): MarkerCatalog {
    return copy(
        categories = categories.mapValues { (id, category) ->
            locale.categories[id]?.let { category.copy(name = it) } ?: category
        },
        groupsByMap = groupsByMap.mapValues { (_, groups) ->
            groups.map { group -> locale.groups[group.id]?.let { group.copy(name = it) } ?: group }
        },
        mapNames = locale.maps,
    )
}
