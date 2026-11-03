package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.withFrameNanos
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.analytics.MapLoadSource
import com.onthecrow.interactivegamemap.analytics.MapReadyEvent
import com.onthecrow.interactivegamemap.analytics.MapTimeEvent
import com.onthecrow.interactivegamemap.analytics.PARAM_MAP_ID
import com.onthecrow.interactivegamemap.analytics.ScreenTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// The map draws a tile on the frame after it gets it, the second one is surely after that
private const val FRAMES_TO_DRAW_TILE = 2

// A map takes a second or two, a visit this long without it is reported
private val NOT_DRAWN_VISIT = 10.seconds

// The keys of the crash reports, with map_id
private const val KEY_MAP_READY = "map_ready"
private const val KEY_TILE_ERROR = "tile_error"

/**
 * Logs map_ready when the first tiles of a map are drawn and map_time when the user leaves the map screen:
 * how long the map takes to load, and the visits it never loads in. The crash reports get the open map, the
 * errors of its tiles, and a non-fatal error for a long visit without a map.
 * It's created at the launch with Koin, the first map is measured from then. Called on the main thread,
 * except [MapLoad.onTile].
 */
internal class MapAnalytics(
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val launch = timeSource.markNow()
    private var load: MapLoad? = null
    private var lastMapId: Int? = null
    private var visitStart: TimeMark? = null
    private var visits = 0
    private var isNoMapReported = false

    /** A map gets its tile layer, the tiles it gets go to the returned load. */
    fun startLoad(mapId: Int): MapLoad {
        val source = when (lastMapId) {
            null -> MapLoadSource.LAUNCH
            mapId -> MapLoadSource.RELOAD
            else -> MapLoadSource.MAP_SWITCH
        }
        lastMapId = mapId
        analytics.setDefaultParams(mapOf(PARAM_MAP_ID to mapId))
        crashReporter.setKeys(mapOf(PARAM_MAP_ID to mapId, KEY_MAP_READY to false, KEY_TILE_ERROR to null))
        crashReporter.log("Map $mapId: loading, ${source.name.lowercase()}")
        return MapLoad(mapId, source, timeSource.markNow()) { onTileError(mapId, it) }.also { load = it }
    }

    /** The first tile of [load] is drawn. */
    fun onFirstTileDrawn(load: MapLoad) {
        if (load !== this.load || load.readyMark != null) return
        load.readyMark = timeSource.markNow()
        val loadTime = load.start.elapsedNow()
        analytics.log(
            MapReadyEvent(
                source = load.source,
                loadTime = loadTime,
                launchTime = launch.elapsedNow().takeIf { load.source == MapLoadSource.LAUNCH },
                tilesFailed = load.stats.failed,
                wasHidden = load.wasHidden,
            )
        )
        crashReporter.setKeys(mapOf(KEY_MAP_READY to true))
        crashReporter.log("Map ${load.mapId}: drawn in ${loadTime.inWholeMilliseconds} ms")
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
        if (readyTime == null && screenTime >= NOT_DRAWN_VISIT) recordNotDrawn(load, screenTime)
    }

    /** Once a load: the report has the log, the keys and the events of the session. */
    private fun recordNotDrawn(load: MapLoad?, screenTime: Duration) {
        val message = if (load == null) {
            if (isNoMapReported) return
            isNoMapReported = true
            "No map after ${screenTime.inWholeSeconds} s, the remote config hasn't come"
        } else {
            if (load.isReported) return
            load.isReported = true
            val stats = load.stats
            "Map ${load.mapId} not drawn after ${screenTime.inWholeSeconds} s, ${load.source.name.lowercase()}: " +
                "${stats.loaded} tiles loaded, ${stats.failed} failed, the last error ${stats.lastError}"
        }
        crashReporter.recordNonFatal(MapNotDrawnException(message))
    }

    /** Called from the threads of the map. */
    private fun onTileError(mapId: Int, error: String) {
        crashReporter.setKeys(mapOf(KEY_TILE_ERROR to error))
        crashReporter.log("Map $mapId: a tile failed, $error")
    }
}

/** The user stayed on the map screen for long without the map. */
internal class MapNotDrawnException(message: String) : Exception(message)

/**
 * The tiles a map got since its tile layer was added.
 * @param onNewError gets each reason a tile wasn't loaded for when it's another than the last one
 */
internal class MapLoad(
    val mapId: Int,
    val source: MapLoadSource,
    val start: TimeMark,
    private val onNewError: (String) -> Unit,
) {
    private val _stats = MutableStateFlow(TileStats())
    val stats: TileStats get() = _stats.value

    var readyMark: TimeMark? = null
    var wasHidden = false
    var isReported = false

    /**
     * Called from the threads of the map for each tile it gets.
     * @param hasImage false for a tile without an image: one past the edges of the map, or not loaded
     * @param error why the tile wasn't loaded, null when it wasn't an error
     */
    fun onTile(hasImage: Boolean, error: String?) {
        val old = _stats.getAndUpdate { stats ->
            when {
                hasImage -> stats.copy(loaded = stats.loaded + 1)
                error != null -> stats.copy(failed = stats.failed + 1, lastError = error)
                else -> stats
            }
        }
        // A tile failing again for the same reason tells nothing new
        if (!hasImage && error != null && error != old.lastError) onNewError(error)
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
