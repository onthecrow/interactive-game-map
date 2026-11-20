package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

/**
 * The roads of one map, files/roads.json, made by tools/rdr2-roads from jeanropke's RDR2CollectorsMap. Points are
 * flat lists of lat, lng, lat, lng...
 *
 * @param fastTravelCategoryId the markers of this category are the fast travel points: the places a fast travel from
 * a campfire goes to
 */
@Serializable
internal class Roads(
    val mapId: Int,
    val fastTravelCategoryId: String,
    val costs: RoadCosts,
    val junctions: List<Double>,
    val roads: List<RoadLine>,
)

/** See [TravelCosts]. */
@Serializable
internal class RoadCosts(
    val offRoadFactor: Double,
    val fastTravel: Double,
    val directReach: Double,
)

@Serializable
internal class RoadLine(
    val from: Int,
    val to: Int,
    val path: List<Double>,
)
