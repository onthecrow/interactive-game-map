package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MapBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkerClusteringTest {
    // At zoom 0 the world is 256 dp wide, a degree of longitude is about 0.71 dp
    private fun location(id: Int, lng: Double, lat: Double = 0.0) = MapLocation.EMPTY.copy(id = id, lat = lat, lng = lng)

    @Test
    fun `close locations go to one cluster at the first of them`() {
        val locations = listOf(location(1, 0.0), location(2, 10.0), location(3, 20.0), location(4, 150.0))

        val result = clusterLocations(locations, zoom = 0f)

        assertEquals(listOf(4), result.locations.map { it.id })
        val cluster = result.clusters.single()
        assertEquals(1, cluster.id)
        assertEquals(3, cluster.count)
        assertEquals(0.0, cluster.bounds.west)
        assertEquals(20.0, cluster.bounds.east)
    }

    @Test
    fun `small groups stay apart`() {
        val locations = listOf(location(1, 0.0), location(2, 10.0))

        val result = clusterLocations(locations, zoom = 0f)

        assertTrue(result.clusters.isEmpty())
        assertEquals(listOf(1, 2), result.locations.map { it.id })
    }

    @Test
    fun `clusters split when the map is zoomed in`() {
        val locations = listOf(location(1, 0.0), location(2, 10.0), location(3, 20.0))

        assertEquals(1, clusterLocations(locations, zoom = 0f).clusters.size)
        assertEquals(3, clusterLocations(locations, zoom = 3f).locations.size)
    }

    @Test
    fun `labels of big clusters are rounded down`() {
        val cluster = MarkerCluster(id = 1, lat = 0.0, lng = 0.0, count = 0, bounds = MapBounds(0.0, 0.0, 0.0, 0.0))

        assertEquals("42", cluster.copy(count = 42).label)
        assertEquals("200+", cluster.copy(count = 250).label)
        assertEquals("3000+", cluster.copy(count = 3999).label)
    }
}
