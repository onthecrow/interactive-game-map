package com.onthecrow.interactivegamemap.map.model

/**
 * How one gets around a map: its roads and its fast travel points, for routes through its locations.
 *
 * Distances are measured in world units: the Web Mercator world at zoom 0 is 1 wide. A game's map is a flat picture
 * laid on that world, so they are in proportion to the distances of the game.
 *
 * @param junctions where roads meet or end
 * @param roads each one from a junction to a junction, along its line
 */
class TravelNetwork(
    val junctions: List<GeoPoint>,
    val roads: List<Road>,
    val fastTravelPoints: List<FastTravelPoint>,
    val costs: TravelCosts,
)

/** @param path the line of the road, from the junction [from] to the junction [to] */
data class Road(
    val from: Int,
    val to: Int,
    val path: List<GeoPoint>,
)

/** A place a player can jump to at any moment; [id] is the location of the place, for the game that has one. */
data class FastTravelPoint(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)

/**
 * @param offRoadFactor how much longer a ride across country takes than the same distance on a road
 * @param fastTravel the distance of a ride a fast travel is worth, in world units
 * @param directReach locations closer than this, in world units, are ridden to straight instead of by the roads
 */
data class TravelCosts(
    val offRoadFactor: Double,
    val fastTravel: Double,
    val directReach: Double,
)
