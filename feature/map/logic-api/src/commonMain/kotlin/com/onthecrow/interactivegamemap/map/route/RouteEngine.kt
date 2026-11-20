package com.onthecrow.interactivegamemap.map.route

import com.onthecrow.interactivegamemap.map.TravelNetworkRepository
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.Route
import com.onthecrow.interactivegamemap.map.model.RouteFastTravel
import com.onthecrow.interactivegamemap.map.model.RoutePath
import com.onthecrow.interactivegamemap.map.model.RoutePathKind
import com.onthecrow.interactivegamemap.map.model.RoutePlan
import com.onthecrow.interactivegamemap.map.model.RouteStop
import com.onthecrow.interactivegamemap.map.model.TravelNetwork
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// The time the order is improved for grows with the number of locations, up to a limit
private val BASE_IMPROVEMENT = 500.milliseconds
private val IMPROVEMENT_PER_LOCATION = 0.5.milliseconds
private val MAX_IMPROVEMENT = 5.seconds

/** A network made ready: its roads and its fast travel points put on them. */
internal class PreparedNetwork(
    val network: TravelNetwork,
    val model: TravelCostModel,
    val fastTravelPlaces: List<Place>,
) {
    private val fastTravelIndex = network.fastTravelPoints.withIndex().associate { (index, point) -> point.id to index }

    fun fastTravelIndex(pointId: Int): Int? = fastTravelIndex[pointId]
}

/** The phases of building a route report their progress through it, see [RouteEngine.build]. */
internal interface RouteBuildListener {
    fun onPreparing(progress: Float) {}
    fun onOrdering(progress: Float) {}
    fun onImproving(progress: Float) {}
    fun onDrawing() {}
}

/** Builds routes on the networks of the maps; a network is made ready once and kept. */
internal class RouteEngine(private val repository: TravelNetworkRepository) {
    private val mutex = Mutex()
    private val prepared = HashMap<Int, PreparedNetwork>()

    /** The network of [mapId] ready for routes, null when the map has none. */
    suspend fun prepare(mapId: Int, ensureActive: () -> Unit = {}, onProgress: (Float) -> Unit = {}): PreparedNetwork? {
        return mutex.withLock {
            prepared[mapId] ?: run {
                val network = repository.getTravelNetwork(mapId) ?: return@withLock null
                val model = TravelCostModel(RoadGraph(network, ensureActive, onProgress), network.costs)
                val places = network.fastTravelPoints.map { model.place(worldPoint(it.lat, it.lng)) }
                PreparedNetwork(network, model, places).also { prepared[mapId] = it }
            }
        }
    }

    /** The shortest route through [locations] that the time allows, null when [mapId] has no roads. */
    suspend fun build(
        mapId: Int,
        locations: List<MapLocation>,
        ensureActive: () -> Unit,
        listener: RouteBuildListener,
    ): Route? {
        val network = prepare(mapId, ensureActive) { listener.onPreparing(it * 0.7f) } ?: return null
        val model = network.model
        val places = locations.mapIndexed { index, location ->
            if (index % 64 == 0) {
                ensureActive()
                listener.onPreparing(0.7f + 0.3f * index / locations.size)
            }
            model.place(worldPoint(location.lat, location.lng))
        }
        val jumps = FastTravelCosts(network, places)
        val costs = object : TourCosts {
            override val size = locations.size
            override fun start(j: Int) = jumps.toLocation[j]
            override fun cost(i: Int, j: Int) = min(model.ride(places[i], places[j]), jumps.jumpTo(j))
        }
        val order = TourSolver(
            costs = costs,
            xs = DoubleArray(places.size) { places[it].point.x },
            ys = DoubleArray(places.size) { places[it].point.y },
            improvementBudget = improvementBudget(locations.size),
            ensureActive = ensureActive,
            onOrderProgress = listener::onOrdering,
            onImproveProgress = listener::onImproving,
        ).solve()

        val stops = mutableListOf<RouteStop>()
        var previous = -1
        for (j in order) {
            val point = jumps.nearestPoint[j]
            val jump = point >= 0 && (previous < 0 || jumps.jumpTo(j) < model.ride(places[previous], places[j]))
            if (jump) stops += RouteStop.FastTravel(network.network.fastTravelPoints[point].id)
            stops += RouteStop.Location(locations[j].id)
            previous = j
        }
        listener.onDrawing()
        val placesById = locations.withIndex().associate { (index, location) -> location.id to places[index] }
        return assemble(network, RoutePlan(mapId, stops), placesById, ensureActive)
    }

    /** The lines of [plan] laid again, null when the map has no roads any more. */
    suspend fun restore(plan: RoutePlan, locations: List<MapLocation>): Route? {
        val network = prepare(plan.mapId) ?: return null
        val places = locations.associate { it.id to network.model.place(worldPoint(it.lat, it.lng)) }
        return assemble(network, plan, places) {}
    }

    private fun improvementBudget(size: Int): Duration {
        return minOf(MAX_IMPROVEMENT, BASE_IMPROVEMENT + IMPROVEMENT_PER_LOCATION * size)
    }

    /** The paths of [plan]: rides between its stops, a straight jump to each fast travel point after the first one. */
    private fun assemble(
        network: PreparedNetwork,
        plan: RoutePlan,
        places: Map<Int, Place>,
        ensureActive: () -> Unit,
    ): Route {
        val paths = mutableListOf<RoutePath>()
        val fastTravels = mutableListOf<RouteFastTravel>()
        val stops = mutableListOf<RouteStop>()
        // The legs of the ride since the last jump, drawn when the next jump or the end comes
        val legs = mutableListOf<RideLeg>()
        var previous: Place? = null

        fun endRide() {
            for (leg in mergeLegs(legs)) {
                val points = leg.points.dedupe()
                if (points.size < 2) continue
                // The roads are drawn as smooth curves through their points, the legs across country straight
                val line = if (leg.onRoad) smoothLine(points, anchors = emptySet()) else points
                paths += RoutePath(if (leg.onRoad) RoutePathKind.RIDE else RoutePathKind.OFF_ROAD, line.map { it.toGeoPoint() })
            }
            legs.clear()
        }

        plan.stops.forEachIndexed { index, stop ->
            if (index % 64 == 0) ensureActive()
            when (stop) {
                is RouteStop.FastTravel -> {
                    val pointIndex = network.fastTravelIndex(stop.pointId) ?: return@forEachIndexed
                    val place = network.fastTravelPlaces[pointIndex]
                    fastTravels += RouteFastTravel(fastTravels.size + 1, network.network.fastTravelPoints[pointIndex])
                    previous?.let { from ->
                        endRide()
                        paths += RoutePath(RoutePathKind.FAST_TRAVEL, listOf(from.point.toGeoPoint(), place.point.toGeoPoint()))
                    }
                    previous = place
                    stops += stop
                }
                is RouteStop.Location -> {
                    val place = places[stop.locationId] ?: return@forEachIndexed
                    previous?.let { from -> legs += network.model.rideLegs(from, place) }
                    previous = place
                    stops += stop
                }
            }
        }
        endRide()
        return Route(RoutePlan(plan.mapId, stops), fastTravels, paths)
    }
}

/** How far each location is from the nearest fast travel point, and what a jump to it costs. */
private class FastTravelCosts(network: PreparedNetwork, places: List<Place>) {
    private val jumpCost = network.network.costs.fastTravel
    /** The index of the fast travel point nearest to each location, -1 without fast travel points. */
    val nearestPoint = IntArray(places.size) { -1 }
    /** The ride to each location from its nearest fast travel point, 0 without them: the route starts anywhere. */
    val toLocation = DoubleArray(places.size)

    init {
        places.forEachIndexed { j, place ->
            var best = Double.POSITIVE_INFINITY
            network.fastTravelPlaces.forEachIndexed { f, point ->
                val cost = network.model.ride(point, place)
                if (cost < best) {
                    best = cost
                    nearestPoint[j] = f
                }
            }
            toLocation[j] = if (best.isInfinite()) 0.0 else best
        }
    }

    /** A jump from anywhere to the fast travel point nearest to [j] and the ride on to it. */
    fun jumpTo(j: Int): Double = if (nearestPoint[j] < 0) Double.POSITIVE_INFINITY else jumpCost + toLocation[j]
}

/** Legs of one kind in a row joined into one, like the roads on both sides of a location on a road. */
private fun mergeLegs(legs: List<RideLeg>): List<RideLeg> {
    val merged = mutableListOf<RideLeg>()
    for (leg in legs) {
        val last = merged.lastOrNull()
        merged += if (last != null && last.onRoad == leg.onRoad && last.points.last() == leg.points.first()) {
            merged.removeAt(merged.lastIndex)
            RideLeg(leg.onRoad, last.points + leg.points.drop(1))
        } else {
            leg
        }
    }
    return merged
}

private fun List<WorldPoint>.dedupe(): List<WorldPoint> {
    val out = ArrayList<WorldPoint>(size)
    for (point in this) if (out.isEmpty() || out.last() != point) out += point
    return out
}
