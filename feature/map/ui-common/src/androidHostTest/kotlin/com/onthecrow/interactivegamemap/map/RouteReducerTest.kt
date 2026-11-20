package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.FastTravelPoint
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapMarkerCategory
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup
import com.onthecrow.interactivegamemap.map.model.MarkerCategory
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.map.model.MarkerPin
import com.onthecrow.interactivegamemap.map.model.Route
import com.onthecrow.interactivegamemap.map.model.RouteBuildStage
import com.onthecrow.interactivegamemap.map.model.RouteFastTravel
import com.onthecrow.interactivegamemap.map.model.RoutePlan
import com.onthecrow.interactivegamemap.map.model.RouteStop
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteReducerTest {
    private val reducer = MapReducer()

    private fun MapScreenState.after(event: MapEvent) = runBlocking { reducer.reduce(this@after, event) }

    private val locations = (1..4).map { MapLocation.EMPTY.copy(id = it, name = "L$it", lat = it.toDouble()) }
    private val plan = RoutePlan(
        mapId = 1,
        stops = listOf(RouteStop.FastTravel(100), RouteStop.Location(1), RouteStop.Location(2)) +
            listOf(RouteStop.FastTravel(200), RouteStop.Location(3), RouteStop.Location(4)),
    )
    private val route = Route(
        plan = plan,
        fastTravels = listOf(
            RouteFastTravel(1, FastTravelPoint(100, "A", 0.0, 0.0)),
            RouteFastTravel(2, FastTravelPoint(200, "B", 0.0, 0.0)),
        ),
        paths = emptyList(),
    )
    private val built = MapScreenState(isRouteAvailable = true, routeCandidateCount = 4)
        .after(MapEvent.OnRouteBuildProgress(RouteBuildProgress(RouteBuildStage.IMPROVING, 0.5f, 4)))
        .after(MapEvent.OnRouteLoaded(route, locations))

    @Test
    fun `a built route opens its first location and closes the progress`() {
        assertNull(built.routeBuild)
        assertEquals(0, built.routePage)
        assertEquals(locations[0], built.selectedLocation)
        assertEquals(locations[0], built.focusedLocation)
        assertTrue(built.isRoutePagerVisible)
    }

    @Test
    fun `a page change takes the camera to its location`() {
        val state = built.after(MapEvent.OnRoutePageChange(2))
        assertEquals(2, state.routePage)
        assertEquals(locations[2], state.selectedLocation)
        assertEquals(locations[2], state.focusedLocation)
        // Past the end nothing changes
        assertEquals(state, state.after(MapEvent.OnRoutePageChange(4)))
    }

    @Test
    fun `found on the current page goes on to the next one, unfound stays`() {
        val next = built.after(MapEvent.OnFoundClick(1))
        assertEquals(1, next.routePage)
        assertEquals(locations[1], next.selectedLocation)

        val found = built.copy(foundLocationIds = setOf(1)).after(MapEvent.OnFoundClick(1))
        assertEquals(0, found.routePage)
    }

    @Test
    fun `a tap on a location of the route opens its page, another one its own popup`() {
        val onRoute = built.after(MapEvent.OnLocationDismiss).after(MapEvent.OnLocationClick(locations[3]))
        assertEquals(3, onRoute.routePage)
        assertTrue(onRoute.isRoutePagerVisible)

        val other = built.after(MapEvent.OnLocationClick(MapLocation.EMPTY.copy(id = 99)))
        assertFalse(other.isRoutePagerVisible)
        assertEquals(0, other.routePage)
    }

    @Test
    fun `the route button offers to end the route, confirming drops it`() {
        val asked = built.after(MapEvent.OnRouteClick)
        assertTrue(asked.isRouteExitConfirmVisible)
        assertTrue(asked.after(MapEvent.OnRouteExitDismiss).route != null)

        val ended = asked.after(MapEvent.OnRouteExitConfirm)
        assertNull(ended.route)
        assertTrue(ended.routeLocations.isEmpty())
        assertNull(ended.selectedLocation)
        assertFalse(ended.isRouteExitConfirmVisible)
    }

    @Test
    fun `keep going opens the first location not found yet, a tap outside only closes`() {
        val asked = built.copy(foundLocationIds = setOf(1, 2))
            .after(MapEvent.OnRoutePageChange(3))
            .after(MapEvent.OnLocationDismiss)
            .after(MapEvent.OnRouteClick)

        val continued = asked.after(MapEvent.OnRouteContinue)
        assertFalse(continued.isRouteExitConfirmVisible)
        assertEquals(2, continued.routePage)
        assertEquals(locations[2], continued.selectedLocation)
        assertEquals(locations[2], continued.focusedLocation)
        assertTrue(continued.isRoutePagerVisible)

        val closed = asked.after(MapEvent.OnRouteExitDismiss)
        assertNull(closed.selectedLocation)
        assertEquals(3, closed.routePage)

        val allFound = asked.copy(foundLocationIds = setOf(1, 2, 3, 4)).after(MapEvent.OnRouteContinue)
        assertFalse(allFound.isRouteExitConfirmVisible)
        assertNull(allFound.selectedLocation)
    }

    @Test
    fun `a search hides the route and its button until it's cleared`() {
        val shown = built.copy(isRouteAvailable = true).after(MapEvent.OnLocationDismiss)
        assertTrue(shown.isRouteButtonVisible)
        assertEquals(route, shown.displayedRoute)

        val searching = shown.after(MapEvent.OnSearchValueChange("bone"))
        assertNull(searching.displayedRoute)
        assertFalse(searching.isRouteButtonVisible)
        // The route is kept for when the search is cleared
        assertEquals(route, searching.route)
        val found = searching.after(MapEvent.OnLocationClick(locations[2]))
        assertFalse(found.isRoutePagerVisible)

        val cleared = found.after(MapEvent.OnClearSearchClick)
        assertEquals(route, cleared.displayedRoute)
        assertTrue(cleared.isRoutePagerVisible)
        assertEquals(2, cleared.routePage)
    }

    @Test
    fun `no route is built from the results of a search`() {
        val state = MapScreenState(isRouteAvailable = true, routeCandidateCount = 10)
        assertTrue(state.canBuildRoute)
        assertTrue(state.isRouteButtonVisible)
        val searching = state.after(MapEvent.OnSearchValueChange("bone"))
        assertFalse(searching.canBuildRoute)
        assertFalse(searching.isRouteButtonVisible)
    }

    @Test
    fun `a restored route opens at its page`() {
        val state = MapScreenState().after(MapEvent.OnRouteLoaded(route, locations, page = 2))
        assertEquals(2, state.routePage)
        assertEquals(locations[2], state.selectedLocation)
    }

    @Test
    fun `a cancelled build closes the progress`() {
        val building = MapScreenState()
            .after(MapEvent.OnRouteBuildProgress(RouteBuildProgress(RouteBuildStage.PREPARING, 0f, 6000)))
        assertNull(building.after(MapEvent.OnRouteBuildCancel).routeBuild)
    }

    private fun category(id: String, hasCycles: Boolean = false) = MapMarkerCategory(
        category = MarkerCategory(
            id = id,
            name = id,
            icon = MarkerIcon(id),
            pin = MarkerPin(icon = MarkerIcon(id), width = 30, height = 30),
            visibleByDefault = true,
            hasCycles = hasCycles,
        ),
        count = 2,
        countByCycle = emptyMap(),
    )

    @Test
    fun `hiding a category or a cycle of the route's markers hides the route, others don't`() {
        // Two bones and two Nazar items of cycle 1
        val routed = built.copy(
            isMarkerListSettingsLoaded = true,
            routeLocations = locations.mapIndexed { i, location ->
                if (i < 2) location.copy(categoryId = "bone") else location.copy(categoryId = "egg", cycle = 1)
            },
            markerGroups = listOf(
                MapMarkerGroup("g", "G", null, listOf(category("bone"), category("egg", hasCycles = true), category("card"))),
            ),
        )
        assertFalse(routed.hasHiddenRouteLocations)
        assertFalse(routed.after(MapEvent.OnCategoryClick("card")).hasHiddenRouteLocations)
        assertTrue(routed.after(MapEvent.OnCategoryClick("bone")).hasHiddenRouteLocations)
        assertTrue(routed.after(MapEvent.OnCycleClick("egg", 2)).hasHiddenRouteLocations)
        assertTrue(routed.after(MapEvent.OnCategoriesVisibilityChange(false)).hasHiddenRouteLocations)
        // Before the groups and the user's choices come nothing is known to be hidden
        assertFalse(routed.copy(markerGroups = emptyList(), categoryVisibility = mapOf("bone" to false)).hasHiddenRouteLocations)
        assertFalse(routed.copy(isMarkerListSettingsLoaded = false, categoryVisibility = mapOf("bone" to false)).hasHiddenRouteLocations)

        val dropped = routed.after(MapEvent.OnCategoryClick("bone")).after(MapEvent.OnRouteHidden)
        assertNull(dropped.route)
        assertNull(dropped.selectedLocation)
    }

    @Test
    fun `a tap on a fast travel point shows its bubble until the map or a marker is tapped`() {
        val bubble = built.after(MapEvent.OnRouteStopClick(2))
        assertEquals(2, bubble.selectedRouteStop)
        // The page of the route stays
        assertEquals(locations[0], bubble.selectedLocation)
        assertNull(bubble.after(MapEvent.OnLocationDismiss).selectedRouteStop)
        assertNull(bubble.after(MapEvent.OnLocationClick(locations[3])).selectedRouteStop)
        assertNull(bubble.after(MapEvent.OnRouteClick).after(MapEvent.OnRouteExitConfirm).selectedRouteStop)
    }

    @Test
    fun `the saved route keeps the jumps before their locations`() {
        val saved = SavedRoute.of(plan, page = 3)
        assertEquals(listOf(1, 2, 3, 4), saved.locationIds)
        assertEquals(listOf(100, -1, 200, -1), saved.fastTravelBefore)
        assertEquals(plan, saved.toPlan())
    }
}
