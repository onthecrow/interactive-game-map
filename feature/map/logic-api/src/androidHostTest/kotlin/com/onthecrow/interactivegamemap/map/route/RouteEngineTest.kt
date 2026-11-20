package com.onthecrow.interactivegamemap.map.route

import com.onthecrow.interactivegamemap.map.TravelNetworkRepository
import com.onthecrow.interactivegamemap.map.model.FastTravelPoint
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.Road
import com.onthecrow.interactivegamemap.map.model.RoutePathKind
import com.onthecrow.interactivegamemap.map.model.RouteStop
import com.onthecrow.interactivegamemap.map.model.TravelCosts
import com.onthecrow.interactivegamemap.map.model.TravelNetwork
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// A degree near the equator, in world units
private const val DEGREE = 1.0 / 360

class RouteEngineTest {

    // A U of roads: A (0,0) - B (0,1) - C (1,1) - D (1,0) as (lat, lng); nothing between D and A
    private val junctions = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0), GeoPoint(1.0, 1.0), GeoPoint(1.0, 0.0))
    private val roads = listOf(
        Road(0, 1, listOf(junctions[0], GeoPoint(0.0, 0.5), junctions[1])),
        Road(1, 2, listOf(junctions[1], junctions[2])),
        Road(2, 3, listOf(junctions[2], junctions[3])),
    )

    private fun network(fastTravelPoints: List<FastTravelPoint> = emptyList(), fastTravel: Double = 100.0) =
        TravelNetwork(junctions, roads, fastTravelPoints, TravelCosts(offRoadFactor = 2.0, fastTravel = fastTravel, directReach = 0.01 * DEGREE))

    private fun engine(network: TravelNetwork?) = RouteEngine(object : TravelNetworkRepository {
        override suspend fun getTravelNetwork(mapId: Int) = network
    })

    private fun location(id: Int, lat: Double, lng: Double) =
        MapLocation(id = id, mapId = 1, categoryId = "1", name = "$id", description = "", lat = lat, lng = lng)

    @Test
    fun `a ride follows the roads around`() {
        val model = TravelCostModel(RoadGraph(network()), network().costs)
        val nearA = model.place(worldPoint(0.0, -0.05))
        val nearD = model.place(worldPoint(1.0, -0.05))
        val ride = model.ride(nearA, nearD)
        // Three sides of the U and twice 0.05 across country, at twice the cost
        assertEquals((3.0 + 4 * 0.05) * DEGREE, ride, 0.01 * DEGREE)
        val legs = model.rideLegs(nearA, nearD)
        // Across country to the road, along it, across country again
        assertEquals(listOf(false, true, false), legs.map { it.onRoad })
        val geo = legs.flatMap { it.points }.map { it.toGeoPoint() }
        assertTrue(geo.any { it.lat in -0.01..0.01 && it.lng in 0.99..1.01 }, "passes B")
        assertTrue(geo.any { it.lat in 0.99..1.01 && it.lng in 0.99..1.01 }, "passes C")
        assertEquals(1.0, geo.last().lat, 1e-9)
    }

    @Test
    fun `two places on one road are ridden along it`() {
        val model = TravelCostModel(RoadGraph(network()), network().costs)
        val a = model.place(worldPoint(-0.1, 0.2))
        val b = model.place(worldPoint(-0.1, 0.7))
        assertEquals((0.5 + 0.4) * DEGREE, model.ride(a, b), 0.01 * DEGREE)
    }

    @Test
    fun `the route starts at a fast travel point and jumps between far clusters`() = runBlocking {
        val points = listOf(
            FastTravelPoint(id = 100, name = "Near A", lat = 0.0, lng = 0.0),
            FastTravelPoint(id = 200, name = "Near D", lat = 1.0, lng = 0.0),
        )
        // A jump is worth less than the ride around the U
        val engine = engine(network(points, fastTravel = 0.5 * DEGREE))
        val locations = listOf(
            location(1, 0.0, 0.1), location(2, 0.0, 0.2), location(3, 0.0, 0.15),
            location(4, 1.0, 0.1), location(5, 1.0, 0.2),
        )
        val route = assertNotNull(engine.build(1, locations, {}, object : RouteBuildListener {}))
        val stops = route.plan.stops
        assertIs<RouteStop.FastTravel>(stops.first())
        assertEquals(2, stops.count { it is RouteStop.FastTravel })
        assertEquals(locations.map { it.id }.toSet(), route.locationIds.toSet())
        assertEquals(listOf(1, 2), route.fastTravels.map { it.number })
        assertEquals(1, route.paths.count { it.kind == RoutePathKind.FAST_TRAVEL })
        // The locations of a cluster are next to each other
        val firstCluster = route.locationIds.take(3).toSet()
        assertTrue(firstCluster == setOf(1, 2, 3) || firstCluster == setOf(4, 5, 1), "$firstCluster")
    }

    @Test
    fun `a restored route is the same route`() = runBlocking {
        val points = listOf(FastTravelPoint(id = 100, name = "Near A", lat = 0.0, lng = 0.0))
        val engine = engine(network(points))
        val locations = listOf(location(1, 0.0, 0.3), location(2, 0.5, 1.0), location(3, 1.0, 0.4))
        val route = assertNotNull(engine.build(1, locations, {}, object : RouteBuildListener {}))
        val restored = assertNotNull(engine.restore(route.plan, locations))
        assertEquals(route, restored)
    }

    @Test
    fun `a map without roads has no routes`() = runBlocking {
        assertNull(engine(null).build(1, listOf(location(1, 0.0, 0.0)), {}, object : RouteBuildListener {}))
    }
}
