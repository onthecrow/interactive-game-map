package com.onthecrow.interactivegamemap.map.hoyolab

import com.onthecrow.interactivegamemap.map.model.MapBounds
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

/** The zoom of the map view at which a pixel of a map's zoom 0 is a pixel of the view's world. */
internal const val HOYOLAB_BASE_ZOOM = 15

private const val WORLD_SIZE = 256.0 * (1 shl HOYOLAB_BASE_ZOOM)

/**
 * Where the points of a map lie on the map view's world: the tile grid of the map starts in the middle of the world
 * at [HOYOLAB_BASE_ZOOM], so its tiles are the view's tiles there and at every lower zoom, down and to the right of
 * the middle. A point's position is in the pixels of the map's zoom 0 counted from its origin.
 */
internal class HoyolabProjection(private val detail: MapDetailDto) {

    fun lat(y: Double): Double {
        val worldY = WORLD_SIZE / 2 + y + detail.origin[1]
        return atan(sinh(PI * (1 - 2 * worldY / WORLD_SIZE))) * 180 / PI
    }

    fun lng(x: Double): Double {
        val worldX = WORLD_SIZE / 2 + x + detail.origin[0]
        return worldX / WORLD_SIZE * 360 - 180
    }

    /** The picture of the map in the grid, without the padding around it. */
    fun mapBounds(): MapBounds {
        val left = (detail.padding[0] - detail.origin[0]).toDouble()
        val top = (detail.padding[1] - detail.origin[1]).toDouble()
        return bounds(left, top, left + detail.originalSize[0], top + detail.originalSize[1])
    }

    /** The bounds of a rectangle given in the points' coordinates, y growing down. */
    fun bounds(left: Double, top: Double, right: Double, bottom: Double) = MapBounds(
        west = lng(left),
        south = lat(bottom),
        east = lng(right),
        north = lat(top),
    )
}
