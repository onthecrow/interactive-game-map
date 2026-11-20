package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.Route
import com.onthecrow.interactivegamemap.map.model.RouteBuildStage
import com.onthecrow.interactivegamemap.map.model.RouteBuildState
import com.onthecrow.interactivegamemap.map.model.RoutePlan
import com.onthecrow.interactivegamemap.map.model.RouteStop
import com.onthecrow.interactivegamemap.map.route.RouteBuildListener
import com.onthecrow.interactivegamemap.map.route.RouteEngine
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

// Where each stage of a build is within its whole progress
private const val PREPARING_END = 0.15f
private const val ORDERING_END = 0.45f
private const val IMPROVING_END = 0.95f
// Smaller steps of the progress aren't sent
private const val PROGRESS_STEP = 0.005f

/** Whether routes can be built on a map: it needs roads. */
interface IsRouteAvailableUseCase {
    suspend operator fun invoke(mapId: Int): Boolean
}

class IsRouteAvailableUseCaseImpl(private val repository: TravelNetworkRepository) : IsRouteAvailableUseCase {
    override suspend operator fun invoke(mapId: Int): Boolean = repository.getTravelNetwork(mapId) != null
}

/**
 * The shortest route through locations the time allows, along the roads and with fast travel, see [Route].
 *
 * The work runs off the main thread; it stops when the collection is cancelled.
 */
interface BuildRouteUseCase {
    /** The progress, then the route, or [RouteBuildState.Unavailable] when the map has no roads. */
    operator fun invoke(mapId: Int, locations: List<MapLocation>): Flow<RouteBuildState>
}

class BuildRouteUseCaseImpl internal constructor(private val engine: RouteEngine) : BuildRouteUseCase {
    constructor(repository: TravelNetworkRepository) : this(RouteEngine(repository))

    override operator fun invoke(mapId: Int, locations: List<MapLocation>): Flow<RouteBuildState> = channelFlow {
        var lastStage: RouteBuildStage? = null
        var lastProgress = -1f

        fun report(stage: RouteBuildStage, progress: Float) {
            if (stage == lastStage && abs(progress - lastProgress) < PROGRESS_STEP) return
            lastStage = stage
            lastProgress = progress
            trySend(RouteBuildState.Building(stage, progress.coerceIn(0f, 1f)))
        }

        report(RouteBuildStage.PREPARING, 0f)
        val route = engine.build(
            mapId = mapId,
            locations = locations,
            ensureActive = { ensureActive() },
            listener = object : RouteBuildListener {
                override fun onPreparing(progress: Float) =
                    report(RouteBuildStage.PREPARING, PREPARING_END * progress)

                override fun onOrdering(progress: Float) =
                    report(RouteBuildStage.ORDERING, PREPARING_END + (ORDERING_END - PREPARING_END) * progress)

                override fun onImproving(progress: Float) =
                    report(RouteBuildStage.IMPROVING, ORDERING_END + (IMPROVING_END - ORDERING_END) * progress)

                override fun onDrawing() = report(RouteBuildStage.DRAWING, IMPROVING_END)
            },
        )
        send(route?.let { RouteBuildState.Built(it) } ?: RouteBuildState.Unavailable)
    }
        // Only the latest progress matters to the one who waits, and the result is the latest of all
        .buffer(Channel.CONFLATED)
        .flowOn(Dispatchers.Default)
}

/** A route kept from before, its lines laid again; null when its map has no roads any more. */
interface RestoreRouteUseCase {
    suspend operator fun invoke(plan: RoutePlan): Route?
}

class RestoreRouteUseCaseImpl internal constructor(
    private val engine: RouteEngine,
    private val locationsRepository: LocationsRepository,
) : RestoreRouteUseCase {
    constructor(repository: TravelNetworkRepository, locationsRepository: LocationsRepository) :
        this(RouteEngine(repository), locationsRepository)

    override suspend operator fun invoke(plan: RoutePlan): Route? = withContext(Dispatchers.Default) {
        val ids = plan.stops.filterIsInstance<RouteStop.Location>().map { it.locationId }.toSet()
        // Only where they are matters, which is the same in every language
        val locations = locationsRepository.getLocations(LocationsFilter.DEFAULT_LANGUAGE).filter { it.id in ids }
        engine.restore(plan, locations)
    }
}
