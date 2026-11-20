package com.onthecrow.interactivegamemap.map.tiles

import com.onthecrow.interactivegamemap.map.model.MapTiles
import com.onthecrow.interactivegamemap.map.model.TileSchemes

/** Where the tile the map asks for is. */
sealed interface TileUrl {
    /** Outside the map: there is nothing to ask a server for. */
    data object Outside : TileUrl

    data class Url(val value: String) : TileUrl
}

/** Places the tiles of one [scheme] on the tile grid of the map view: x, y and zoom are the map view's ones. */
interface TileUrlResolver {
    val scheme: String

    fun resolve(tiles: MapTiles, x: Int, y: Int, zoom: Int): TileUrl
}

/** Every resolver the app registered, by their schemes. A map of an unknown scheme has no tiles. */
class TileUrlResolvers(resolvers: List<TileUrlResolver>) {
    private val byScheme = resolvers.associateBy { it.scheme }

    fun resolve(tiles: MapTiles, x: Int, y: Int, zoom: Int): TileUrl {
        return byScheme[tiles.scheme]?.resolve(tiles, x, y, zoom) ?: TileUrl.Outside
    }
}

const val TILE_TOKEN_X = "{{x}}"
const val TILE_TOKEN_Y = "{{y}}"
const val TILE_TOKEN_Z = "{{z}}"

/** The url of [template] with the tile's coordinates in place of the tokens. */
fun tileUrl(template: String, x: Int, y: Int, z: Int): TileUrl.Url {
    return TileUrl.Url(
        template
            .replace(TILE_TOKEN_X, x.toString())
            .replace(TILE_TOKEN_Y, y.toString())
            .replace(TILE_TOKEN_Z, z.toString())
    )
}

/** The tiles around the middle of the world, the y of a tile counted up from it. */
class CenteredTileUrlResolver : TileUrlResolver {
    override val scheme: String = TileSchemes.CENTERED

    override fun resolve(tiles: MapTiles, x: Int, y: Int, zoom: Int): TileUrl {
        val half = 1 shl (zoom - 1)
        return tileUrl(tiles.urlTemplate, x - half, (y - half).inv(), zoom)
    }
}

/** The usual web tiles; with [MapTiles.zoomOffset] the grid of the map's zoom lies in the middle of the drawn one. */
class XyzTileUrlResolver : TileUrlResolver {
    override val scheme: String = TileSchemes.XYZ

    override fun resolve(tiles: MapTiles, x: Int, y: Int, zoom: Int): TileUrl {
        val tileZoom = zoom - tiles.zoomOffset
        if (tileZoom < 0) return TileUrl.Outside
        val count = 1 shl tileZoom
        val start = ((1 shl zoom) - count) / 2
        val tileX = x - start
        val tileY = y - start
        if (tileX !in 0 until count || tileY !in 0 until count) return TileUrl.Outside
        return tileUrl(tiles.urlTemplate, tileX, tileY, tileZoom)
    }
}
