package com.onthecrow.interactivegamemap.map.model

/** Where a route goes, in order: the locations it visits and the fast travel points it jumps to on the way. */
data class RoutePlan(
    val mapId: Int,
    val stops: List<RouteStop>,
)

sealed interface RouteStop {
    data class Location(val locationId: Int) : RouteStop

    /** [pointId] is the id of a [FastTravelPoint]. */
    data class FastTravel(val pointId: Int) : RouteStop
}

/**
 * A route ready to be drawn. It starts at a fast travel point, as where the player is isn't known, and rides from
 * location to location; when a jump to a fast travel point is shorter than the ride, it jumps there and rides on.
 *
 * @param fastTravels the fast travel points it jumps to, numbered from 1 in the order of the route
 * @param paths the rides along the roads and the jumps, in the order of the route
 */
data class Route(
    val plan: RoutePlan,
    val fastTravels: List<RouteFastTravel>,
    val paths: List<RoutePath>,
) {
    /** The locations of the route in its order. */
    val locationIds: List<Int> = plan.stops.filterIsInstance<RouteStop.Location>().map { it.locationId }
}

data class RouteFastTravel(
    val number: Int,
    val point: FastTravelPoint,
)

data class RoutePath(
    val kind: RoutePathKind,
    val points: List<GeoPoint>,
)

enum class RoutePathKind {
    /** Along the roads, a smooth curve. */
    RIDE,

    /** Straight across country: from a location to the road it is ridden from, or between locations close together. */
    OFF_ROAD,

    /** A jump to a fast travel point, a straight line from where it's made. */
    FAST_TRAVEL,
}

sealed interface RouteBuildState {
    /** @param progress from 0 to 1 over the whole build */
    data class Building(val stage: RouteBuildStage, val progress: Float) : RouteBuildState

    data class Built(val route: Route) : RouteBuildState

    /** The map has no roads to build a route on. */
    data object Unavailable : RouteBuildState
}

enum class RouteBuildStage {
    /** The roads are made ready and the locations are put on them. */
    PREPARING,

    /** A first order of the locations is found. */
    ORDERING,

    /** The order is made shorter while there is time for it. */
    IMPROVING,

    /** The lines of the route are laid along the roads. */
    DRAWING,
}
