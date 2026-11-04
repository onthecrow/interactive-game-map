package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.network.MapBounds
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

// Google Maps draws the world 256 dp wide at zoom 0 and twice as wide at each next zoom
private const val WORLD_SIZE_DP = 256.0

// The locations closer than this on the screen on both axes go to one cluster
private const val CLUSTER_SPAN_DP = 64.0

// Fewer locations stay apart, they don't hide each other much
internal const val MIN_CLUSTER_SIZE = 3

// The Web Mercator projection ends here
private const val MAX_LATITUDE = 85.05112878

/** Locations close to each other on the screen, displayed as one marker with their number. */
data class MarkerCluster(
    /** The id of the location the cluster is placed at. */
    val id: Int,
    val lat: Double,
    val lng: Double,
    val count: Int,
    val bounds: MapBounds,
)

internal class ClusteredLocations(
    val locations: List<MapLocation>,
    val clusters: List<MarkerCluster>,
)

private class WorldPoint(val x: Double, val y: Double)

/**
 * Puts the locations around each one into a cluster, the earlier locations first, so the clusters stay
 * where they are when the map moves. The locations of the small clusters stay apart.
 */
internal fun clusterLocations(locations: List<MapLocation>, zoom: Float): ClusteredLocations {
    val worldSize = WORLD_SIZE_DP * 2.0.pow(zoom.toDouble())
    val points = locations.map { it.toWorldPoint(worldSize) }
    // A cell is as big as a cluster, so the locations of a cluster are in the cells around its center
    val cells = HashMap<Long, MutableList<Int>>()
    points.forEachIndexed { index, point ->
        cells.getOrPut(cellKey(point.x.cell, point.y.cell)) { mutableListOf() } += index
    }

    val isTaken = BooleanArray(locations.size)
    val singles = mutableListOf<Int>()
    val clusters = mutableListOf<MarkerCluster>()
    val members = mutableListOf<Int>()
    points.forEachIndexed { index, center ->
        if (isTaken[index]) return@forEachIndexed
        members.clear()
        for (dx in -1..1) {
            for (dy in -1..1) {
                cells[cellKey(center.x.cell + dx, center.y.cell + dy)]?.forEach { other ->
                    val point = points[other]
                    if (!isTaken[other] &&
                        abs(point.x - center.x) <= CLUSTER_SPAN_DP / 2 &&
                        abs(point.y - center.y) <= CLUSTER_SPAN_DP / 2
                    ) {
                        members += other
                    }
                }
            }
        }
        members.forEach { isTaken[it] = true }
        if (members.size < MIN_CLUSTER_SIZE) {
            singles += members
        } else {
            clusters += cluster(locations[index], members.map { locations[it] })
        }
    }
    singles.sort()
    return ClusteredLocations(locations = singles.map { locations[it] }, clusters = clusters)
}

private fun cluster(center: MapLocation, members: List<MapLocation>): MarkerCluster {
    return MarkerCluster(
        id = center.id,
        lat = center.lat,
        lng = center.lng,
        count = members.size,
        bounds = MapBounds(
            west = members.minOf { it.lng },
            south = members.minOf { it.lat },
            east = members.maxOf { it.lng },
            north = members.maxOf { it.lat },
        ),
    )
}

private val Double.cell: Int get() = floor(this / CLUSTER_SPAN_DP).toInt()

private fun cellKey(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)

private fun MapLocation.toWorldPoint(worldSize: Double): WorldPoint {
    val latitude = lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE) * PI / 180
    return WorldPoint(
        x = (lng + 180) / 360 * worldSize,
        y = (1 - ln(tan(PI / 4 + latitude / 2)) / PI) / 2 * worldSize,
    )
}
