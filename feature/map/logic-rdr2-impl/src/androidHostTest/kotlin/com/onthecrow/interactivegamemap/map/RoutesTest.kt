package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.mapper.toMapLocation
import com.onthecrow.interactivegamemap.map.model.FastTravelPoint
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.Roads
import com.onthecrow.interactivegamemap.map.model.Route
import com.onthecrow.interactivegamemap.map.model.RouteBuildState
import com.onthecrow.interactivegamemap.map.model.RoutePathKind
import com.onthecrow.interactivegamemap.map.model.RouteStop
import com.onthecrow.interactivegamemap.map.model.TravelNetwork
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlin.time.measureTimedValue

class RoutesTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val catalog: MarkerCatalog = json.decodeFromString(read("marker_catalog.json"))
    private val locations = json.decodeFromString<List<Location>>(read("markers.json")).map { it.toMapLocation(catalog) }
    private val roads: Roads = json.decodeFromString(read("roads.json"))
    private val network: TravelNetwork = roads.toTravelNetwork(
        locations.filter { it.categoryId == roads.fastTravelCategoryId }
            .map { FastTravelPoint(it.id, it.name, it.lat, it.lng) }
    )
    private val repository = object : TravelNetworkRepository {
        override suspend fun getTravelNetwork(mapId: Int) = network.takeIf { mapId == roads.mapId }
    }

    private fun read(name: String) = File("src/commonMain/composeResources/files/$name").readText()

    private fun build(locations: List<MapLocation>): Route = runBlocking {
        val (states, time) = measureTimedValue { BuildRouteUseCaseImpl(repository)(1, locations).toList() }
        println("Route through ${locations.size} locations: $time, ${states.size} progress updates")
        assertIs<RouteBuildState.Built>(states.last()).route.also { write(it, locations.size) }
    }

    // For a look at the route over the tiles, see tools/rdr2-roads
    private fun write(route: Route, size: Int) {
        val out = File("build/routes/route_$size.json")
        out.parentFile?.mkdirs()
        out.writeText(
            buildJsonObject {
                putJsonArray("paths") {
                    route.paths.forEach { path ->
                        add(buildJsonObject {
                            put("kind", path.kind.name)
                            putJsonArray("points") { path.points.forEach { add(kotlinx.serialization.json.JsonArray(listOf(
                                kotlinx.serialization.json.JsonPrimitive(it.lat), kotlinx.serialization.json.JsonPrimitive(it.lng)))) } }
                        })
                    }
                }
                putJsonArray("fastTravels") {
                    route.fastTravels.forEach { add(buildJsonObject { put("number", it.number); put("lat", it.point.lat); put("lng", it.point.lng) }) }
                }
                putJsonArray("locations") {
                    val byId = locations.associateBy { it.id }
                    route.locationIds.forEach { id -> byId[id]?.let { add(kotlinx.serialization.json.JsonArray(listOf(
                        kotlinx.serialization.json.JsonPrimitive(it.lat), kotlinx.serialization.json.JsonPrimitive(it.lng)))) } }
                }
            }.toString()
        )
    }

    @Test
    fun `the roads have the fast travel points of the map`() {
        // Every place a fast travel from a campfire goes to
        assertEquals(23, network.fastTravelPoints.size)
        assertTrue(network.junctions.size > 1000 && network.roads.size > 1000)
        assertTrue(network.roads.all { it.from in network.junctions.indices && it.to in network.junctions.indices })
    }

    @Test
    fun `a route through the cigarette cards starts at a fast travel point and visits every card`() {
        val cards = locations.filter { it.categoryId == "36" }
        val route = build(cards)
        assertIs<RouteStop.FastTravel>(route.plan.stops.first())
        assertEquals(cards.map { it.id }.sorted(), route.locationIds.sorted())
        assertEquals(route.fastTravels.size, route.plan.stops.count { it is RouteStop.FastTravel })
        assertEquals(route.fastTravels.size - 1, route.paths.count { it.kind == RoutePathKind.FAST_TRAVEL })
    }

    @Test
    fun `a route through the dinosaur bones`() {
        val bones = locations.filter { it.categoryId == "29" }
        assertEquals(bones.size, build(bones).locationIds.size)
    }

    @Test
    fun `two dreamcatchers by Annesburg are ridden by the road between them, not across the mountain`() {
        // 254 and 252: the road is 37% longer than the straight line, the line crosses a mountain
        val pair = locations.filter { it.id == 254 || it.id == 252 }
        val route = build(pair)
        fun GeoPoint.isAt(location: MapLocation) = abs(lat - location.lat) < 1e-6 && abs(lng - location.lng) < 1e-6
        // No straight line from one to the other: each one is ridden to from the road
        val direct = route.paths.filter { it.kind == RoutePathKind.OFF_ROAD }.filter { path ->
            pair.all { location -> path.points.first().isAt(location) || path.points.last().isAt(location) }
        }
        assertTrue(direct.isEmpty(), "${direct.size} straight lines between them")
        assertTrue(route.paths.any { it.kind == RoutePathKind.RIDE })
    }

    @Test
    fun `a route through every marker is built`() {
        assertEquals(locations.size, build(locations).locationIds.size)
    }

    @Test
    fun `a cancelled build stops at once`() = runBlocking {
        val (_, time) = measureTimedValue {
            kotlinx.coroutines.withTimeoutOrNull(300) { BuildRouteUseCaseImpl(repository)(1, locations).last() }
        }
        println("Cancelled after $time")
        assertTrue(time.inWholeMilliseconds < 1_000, "$time")
    }
}
