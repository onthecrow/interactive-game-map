package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsEvent
import com.onthecrow.interactivegamemap.analytics.MapLoadSource
import com.onthecrow.interactivegamemap.analytics.MapReadyEvent
import com.onthecrow.interactivegamemap.analytics.MapTimeEvent
import com.onthecrow.interactivegamemap.analytics.ScreenTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private class RecordingAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    override fun setUserProperty(name: String, value: String?) = Unit

    override fun setDefaultParams(params: Map<String, Any?>) = Unit
}

class MapAnalyticsTest {
    private val time = TestTimeSource()
    private val analytics = RecordingAnalytics()
    private val mapAnalytics = MapAnalytics(analytics, time)

    private val map = MapDestination.screenName

    @Test
    fun `the first map is ready from the launch and the time on the screen counts from then`() {
        time += 1.seconds
        mapAnalytics.onScreenChange(map)
        val load = mapAnalytics.startLoad(mapId = 2)
        load.onTile(hasImage = false, error = "timeout")
        time += 2.seconds
        load.onTile(hasImage = true, error = null)
        mapAnalytics.onFirstTileDrawn(load)
        time += 10.seconds
        mapAnalytics.onScreenChange("settings")

        assertEquals(
            listOf(
                MapReadyEvent(MapLoadSource.LAUNCH, 2.seconds, 3.seconds, tilesFailed = 1, wasHidden = false),
                MapTimeEvent(
                    screenTime = 12.seconds,
                    readyTime = 10.seconds,
                    exitTo = "settings",
                    hasMap = true,
                    isFirstVisit = true,
                    tilesLoaded = 1,
                    tilesFailed = 1,
                    tileError = "timeout",
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `a map that doesn't load is left not ready`() {
        mapAnalytics.onScreenChange(map)
        val load = mapAnalytics.startLoad(mapId = 2)
        load.onTile(hasImage = false, error = "no_network")
        time += 30.seconds
        mapAnalytics.onScreenChange(null)
        time += 60.seconds
        mapAnalytics.onScreenChange(map)
        load.onTile(hasImage = true, error = null)
        mapAnalytics.onFirstTileDrawn(load)

        val (notReady, ready) = analytics.events
        assertEquals(
            MapTimeEvent(30.seconds, null, ScreenTracker.BACKGROUND, true, true, 0, 1, "no_network"),
            notReady,
        )
        assertEquals(MapReadyEvent(MapLoadSource.LAUNCH, 90.seconds, 90.seconds, 1, wasHidden = true), ready)
    }

    @Test
    fun `a map ready before the visit is ready from its start`() {
        mapAnalytics.onScreenChange(map)
        val load = mapAnalytics.startLoad(mapId = 2)
        load.onTile(hasImage = true, error = null)
        mapAnalytics.onFirstTileDrawn(load)
        mapAnalytics.onScreenChange("settings")
        time += 60.seconds
        mapAnalytics.onScreenChange(map)
        time += 5.seconds
        mapAnalytics.onScreenChange(null)

        val last = analytics.events.last() as MapTimeEvent
        assertEquals(5.seconds, last.screenTime)
        assertEquals(5.seconds, last.readyTime)
        assertEquals(false, last.isFirstVisit)
    }

    @Test
    fun `the next loads are a switch or a reload, the old one is no longer logged`() {
        val first = mapAnalytics.startLoad(mapId = 2)
        val second = mapAnalytics.startLoad(mapId = 3)
        val third = mapAnalytics.startLoad(mapId = 3)
        first.onTile(hasImage = true, error = null)
        mapAnalytics.onFirstTileDrawn(first)

        assertEquals(listOf(MapLoadSource.LAUNCH, MapLoadSource.MAP_SWITCH, MapLoadSource.RELOAD), listOf(first, second, third).map { it.source })
        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `no map is open until the config comes`() {
        mapAnalytics.onScreenChange(map)
        time += 4.seconds
        mapAnalytics.onScreenChange(null)

        assertEquals(MapTimeEvent(4.seconds, null, ScreenTracker.BACKGROUND, false, true, 0, 0, null), analytics.events.single())
    }
}
