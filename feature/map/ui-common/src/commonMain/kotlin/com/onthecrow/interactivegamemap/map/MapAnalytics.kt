package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.withFrameNanos
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.MapLoadSource
import com.onthecrow.interactivegamemap.analytics.MapReadyEvent
import com.onthecrow.interactivegamemap.analytics.MapTimeEvent
import com.onthecrow.interactivegamemap.analytics.PARAM_MAP_ID
import com.onthecrow.interactivegamemap.analytics.ScreenTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// The map draws a tile on the frame after it gets it, the second one is surely after that
private const val FRAMES_TO_DRAW_TILE = 2

/**
 * Logs map_ready when the first tiles of a map are drawn and map_time when the user leaves the map screen:
 * how long the map takes to load, and the visits it never loads in.
 * It's created at the launch with Koin, the first map is measured from then. Called on the main thread,
 * except [MapLoad.onTile].
 */
internal class MapAnalytics(
    private val analytics: Analytics,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val launch = timeSource.markNow()
    private var load: MapLoad? = null
    private var lastMapId: Int? = null
    private var visitStart: TimeMark? = null
    private var visits = 0

    /** A map gets its tile layer, the tiles it gets go to the returned load. */
    fun startLoad(mapId: Int): MapLoad {
        val source = when (lastMapId) {
            null -> MapLoadSource.LAUNCH
            mapId -> MapLoadSource.RELOAD
            else -> MapLoadSource.MAP_SWITCH
        }
        lastMapId = mapId
        analytics.setDefaultParams(mapOf(PARAM_MAP_ID to mapId))
        return MapLoad(source, timeSource.markNow()).also { load = it }
    }

    /** The first tile of [load] is drawn. */
    fun onFirstTileDrawn(load: MapLoad) {
        if (load !== this.load || load.readyMark != null) return
        load.readyMark = timeSource.markNow()
        analytics.log(
            MapReadyEvent(
                source = load.source,
                loadTime = load.start.elapsedNow(),
                launchTime = launch.elapsedNow().takeIf { load.source == MapLoadSource.LAUNCH },
                tilesFailed = load.stats.failed,
                wasHidden = load.wasHidden,
            )
        )
    }

    /** @param screen the screen the user sees now, null in the background, see [ScreenTracker] */
    fun onScreenChange(screen: String?) {
        val isMap = screen == MapDestination.screenName
        val start = visitStart
        if (isMap && start == null) {
            visitStart = timeSource.markNow()
            visits++
        } else if (!isMap && start != null) {
            visitStart = null
            logMapTime(start, exitTo = screen ?: ScreenTracker.BACKGROUND)
        }
    }

    private fun logMapTime(visitStart: TimeMark, exitTo: String) {
        val load = load
        val screenTime = visitStart.elapsedNow()
        // A map ready before the visit counts from its start
        val readyTime = load?.readyMark?.elapsedNow()?.coerceAtMost(screenTime)
        if (load != null && readyTime == null) load.wasHidden = true
        val stats = load?.stats ?: TileStats()
        analytics.log(
            MapTimeEvent(
                screenTime = screenTime,
                readyTime = readyTime,
                exitTo = exitTo,
                hasMap = load != null,
                isFirstVisit = visits == 1,
                tilesLoaded = stats.loaded,
                tilesFailed = stats.failed,
                tileError = stats.lastError,
            )
        )
    }
}

/** The tiles a map got since its tile layer was added. */
internal class MapLoad(val source: MapLoadSource, val start: TimeMark) {
    private val _stats = MutableStateFlow(TileStats())
    val stats: TileStats get() = _stats.value

    var readyMark: TimeMark? = null
    var wasHidden = false

    /**
     * Called from the threads of the map for each tile it gets.
     * @param hasImage false for a tile without an image: one past the edges of the map, or not loaded
     * @param error why the tile wasn't loaded, null when it wasn't an error
     */
    fun onTile(hasImage: Boolean, error: String?) {
        _stats.update { stats ->
            when {
                hasImage -> stats.copy(loaded = stats.loaded + 1)
                error != null -> stats.copy(failed = stats.failed + 1, lastError = error)
                else -> stats
            }
        }
    }

    suspend fun awaitImage() {
        _stats.first { it.loaded > 0 }
    }
}

internal data class TileStats(
    val loaded: Int = 0,
    val failed: Int = 0,
    val lastError: String? = null,
)

/**
 * Logs map_ready once the first tile of [load] is on the screen. The map doesn't tell when it has drawn a tile,
 * it's the frame after the one the tile came in.
 */
internal suspend fun MapAnalytics.trackFirstTile(load: MapLoad) {
    load.awaitImage()
    repeat(FRAMES_TO_DRAW_TILE) { withFrameNanos { } }
    onFirstTileDrawn(load)
}
