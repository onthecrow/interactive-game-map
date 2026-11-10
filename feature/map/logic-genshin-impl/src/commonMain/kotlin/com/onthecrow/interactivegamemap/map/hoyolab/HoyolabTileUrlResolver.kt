package com.onthecrow.interactivegamemap.map.hoyolab

import com.onthecrow.interactivegamemap.map.model.MapTiles
import com.onthecrow.interactivegamemap.map.tiles.TileUrl
import com.onthecrow.interactivegamemap.map.tiles.TileUrlResolver
import com.onthecrow.interactivegamemap.map.tiles.tileUrl

internal const val HOYOLAB_TILE_SCHEME = "hoyolab"
internal const val TILE_TOKEN_LEVEL = "{{level}}"
internal const val TILE_TOKEN_VERSION = "{{version}}"

// The size of the grid at the map's zoom 0 in tiles, and its lowest zoom: as the map's detail has them
internal const val TILE_PARAM_COLUMNS = "columns"
internal const val TILE_PARAM_ROWS = "rows"
internal const val TILE_PARAM_MIN_LEVEL = "minLevel"

/**
 * The tiles of HoYoLAB's map: `{{x}}_{{y}}_{{level}}`, the level P0 at the map's zoom 0 and N1, N2… below it, the
 * grid laid as [HoyolabProjection] lays it.
 */
internal class HoyolabTileUrlResolver : TileUrlResolver {
    override val scheme: String = HOYOLAB_TILE_SCHEME

    override fun resolve(tiles: MapTiles, x: Int, y: Int, zoom: Int): TileUrl {
        val columns = tiles.params[TILE_PARAM_COLUMNS]?.toIntOrNull() ?: return TileUrl.Outside
        val rows = tiles.params[TILE_PARAM_ROWS]?.toIntOrNull() ?: return TileUrl.Outside
        val minLevel = tiles.params[TILE_PARAM_MIN_LEVEL]?.toIntOrNull() ?: 0
        val level = zoom - HOYOLAB_BASE_ZOOM
        if (level > 0 || level < minLevel) return TileUrl.Outside
        val half = 1 shl (zoom - 1)
        val tileX = x - half
        val tileY = y - half
        val scale = 1 shl -level
        if (tileX !in 0 until ceilDiv(columns, scale) || tileY !in 0 until ceilDiv(rows, scale)) return TileUrl.Outside
        val url = tileUrl(tiles.urlTemplate, tileX, tileY, zoom).value
        return TileUrl.Url(url.replace(TILE_TOKEN_LEVEL, if (level == 0) "P0" else "N${-level}"))
    }

    private fun ceilDiv(value: Int, divisor: Int) = (value + divisor - 1) / divisor
}
