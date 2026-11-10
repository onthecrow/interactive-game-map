package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.MapsRepository
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabProjection
import com.onthecrow.interactivegamemap.map.hoyolab.TILE_PARAM_COLUMNS
import com.onthecrow.interactivegamemap.map.hoyolab.TILE_PARAM_MIN_LEVEL
import com.onthecrow.interactivegamemap.map.hoyolab.TILE_PARAM_ROWS
import com.onthecrow.interactivegamemap.map.hoyolab.TILE_TOKEN_VERSION
import com.onthecrow.interactivegamemap.map.model.GameMap
import com.onthecrow.interactivegamemap.map.model.MapDungeon
import com.onthecrow.interactivegamemap.map.model.MapFloor
import com.onthecrow.interactivegamemap.map.model.MapsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val TILE_SIZE = 256

/**
 * The maps of Remote Config with what the last update knows better: the version of the tiles, the size of their
 * grid, the bounds of the picture and the underground places.
 */
internal class GenshinMapsRepository(
    private val remoteConfigMaps: MapsRepository,
    private val content: GenshinContent,
) : MapsRepository {

    // Again on every saved update even with the same maps: their names come from the names of the update
    override fun maps(): Flow<MapsConfig> {
        return combine(remoteConfigMaps.maps(), content.revision) { config, _ ->
            val snapshot = content.snapshot() ?: return@combine config
            config.copy(
                maps = config.maps.map { map ->
                    snapshot.maps.firstOrNull { it.id == map.id }
                        ?.let { map.withSource(it, snapshot.groups.filter { group -> group.mapId == map.id }) }
                        ?: map
                },
            )
        }
    }
}

internal fun GameMap.withSource(source: SnapshotMap, groups: List<SnapshotGroup>): GameMap {
    val detail = source.detail
    val projection = HoyolabProjection(detail)
    return copy(
        tiles = tiles.copy(
            urlTemplate = tiles.urlTemplate.replace(TILE_TOKEN_VERSION, detail.mapVersion),
            params = tiles.params + mapOf(
                TILE_PARAM_COLUMNS to (detail.totalSize[0] / TILE_SIZE).toString(),
                TILE_PARAM_ROWS to (detail.totalSize[1] / TILE_SIZE).toString(),
                TILE_PARAM_MIN_LEVEL to detail.minZoom.toString(),
            ),
        ),
        bounds = projection.mapBounds(),
        dungeons = groups.map { group ->
            MapDungeon(
                id = group.id,
                floors = group.floors.map { floor ->
                    MapFloor(
                        id = floor.id,
                        // "Abandoned Station_B1": the place, then the floor
                        name = floor.shortName.ifEmpty { floor.name.substringAfterLast('_') },
                        imageUrl = floor.overlay.url,
                        bounds = projection.bounds(
                            left = floor.overlay.left,
                            top = floor.overlay.top,
                            right = floor.overlay.right,
                            bottom = floor.overlay.bottom,
                        ),
                    )
                },
            )
        },
    )
}
