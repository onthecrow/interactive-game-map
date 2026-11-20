package com.onthecrow.interactivegamemap.map.route

import com.onthecrow.interactivegamemap.map.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh

/** A point of the Web Mercator world at zoom 0, 1 wide; the game's map is laid on it flat, so distances are its own. */
internal class WorldPoint(val x: Double, val y: Double) {
    fun distanceTo(other: WorldPoint): Double = hypot(x - other.x, y - other.y)

    override fun equals(other: Any?): Boolean = other is WorldPoint && other.x == x && other.y == y

    override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()
}

internal fun worldPoint(lat: Double, lng: Double): WorldPoint {
    val s = sin(lat * PI / 180)
    return WorldPoint((lng + 180) / 360, 0.5 - ln((1 + s) / (1 - s)) / (4 * PI))
}

internal fun GeoPoint.toWorld(): WorldPoint = worldPoint(lat, lng)

internal fun WorldPoint.toGeoPoint(): GeoPoint {
    val lat = atan(sinh(PI - 2 * PI * y)) * 180 / PI
    return GeoPoint(lat = lat, lng = x * 360 - 180)
}
