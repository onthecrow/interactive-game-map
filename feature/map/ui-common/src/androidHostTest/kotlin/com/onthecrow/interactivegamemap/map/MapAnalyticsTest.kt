package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsEvent
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.analytics.MapLoadSource
import com.onthecrow.interactivegamemap.analytics.MapReadyEvent
import com.onthecrow.interactivegamemap.analytics.MapTimeEvent
import com.onthecrow.interactivegamemap.analytics.ScreenTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

private class RecordingCrashReporter : CrashReporter {
    val logs = mutableListOf<String>()
    val keys = mutableMapOf<String, Any?>()
    val nonFatals = mutableListOf<Throwable>()

    override fun log(message: String) {
        logs += message
    }

    override fun setKeys(keys: Map<String, Any?>) {
        this.keys += keys
    }

    override fun recordNonFatal(throwable: Throwable) {
        nonFatals += throwable
    }
}

class MapAnalyticsTest {
    private val time = TestTimeSource()
    private val analytics = RecordingAnalytics()
    private val crashReporter = RecordingCrashReporter()
    private val mapAnalytics = MapAnalytics(analytics, crashReporter, time)

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
        assertEquals<Map<String, Any?>>(mapOf("map_id" to 2, "map_ready" to true, "tile_error" to "timeout"), crashReporter.keys)
        assertEquals(
            listOf("Map 2: loading, launch", "Map 2: a tile failed, timeout", "Map 2: drawn in 2000 ms"),
            crashReporter.logs,
        )
        assertEquals(emptyList(), crashReporter.nonFatals)
    }

    @Test
    fun `a tile failing again for the same reason isn't logged again`() {
        val load = mapAnalytics.startLoad(mapId = 2)
        load.onTile(hasImage = false, error = "timeout")
        load.onTile(hasImage = false, error = "timeout")
        load.onTile(hasImage = false, error = "no_network")
        load.onTile(hasImage = false, error = null)

        assertEquals(
            listOf("Map 2: loading, launch", "Map 2: a tile failed, timeout", "Map 2: a tile failed, no_network"),
            crashReporter.logs,
        )
        assertEquals(TileStats(loaded = 0, failed = 3, lastError = "no_network"), load.stats)
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
        val notDrawn = assertIs<MapNotDrawnException>(crashReporter.nonFatals.single())
        assertEquals(
            "Map 2 not drawn after 30 s, launch: 0 tiles loaded, 1 failed, the last error no_network",
            notDrawn.message,
        )
    }

    @Test
    fun `a short visit without the map isn't reported, a long one is reported once`() {
        mapAnalytics.onScreenChange(map)
        mapAnalytics.startLoad(mapId = 2)
        time += 5.seconds
        mapAnalytics.onScreenChange("settings")
        repeat(2) {
            mapAnalytics.onScreenChange(map)
            time += 10.seconds
            mapAnalytics.onScreenChange(null)
        }

        assertEquals(1, crashReporter.nonFatals.size)
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
        assertEquals(emptyList(), crashReporter.nonFatals)

        mapAnalytics.onScreenChange(map)
        time += 20.seconds
        mapAnalytics.onScreenChange(null)

        assertEquals("No map after 20 s, the remote config hasn't come", crashReporter.nonFatals.single().message)
    }
}
