package com.onthecrow.interactivegamemap.map.model

/**
 * @param categoryVisibility the user's choice, other categories use [MarkerCategory.visibleByDefault]
 * @param cycles the selected cycle by category id, other categories use [DEFAULT_CYCLE]
 * @param bounds the area to look in, the whole map when null
 */
data class LocationsFilter(
    val mapId: Int,
    val query: String = "",
    val categoryVisibility: Map<String, Boolean> = emptyMap(),
    val cycles: Map<String, Int> = emptyMap(),
    val bounds: LocationBounds? = null,
) {
    companion object {
        const val DEFAULT_CYCLE = 1
        val CYCLES = 1..6
    }
}

data class LocationBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    fun contains(lat: Double, lng: Double): Boolean {
        return lat in south..north && lng in west..east
    }
}
