package com.onthecrow.interactivegamemap.map.route

import com.onthecrow.interactivegamemap.map.model.TravelCosts
import kotlin.math.abs
import kotlin.math.min

// A place is put on its nearest road and on the others about as near, the nearest one may lead the wrong way
private const val MAX_ATTACHMENTS = 3
private const val ATTACHMENT_SLACK = 0.5

/** A part of a ride: along the roads, or straight across country from a place to a road or to another place. */
internal class RideLeg(val onRoad: Boolean, val points: List<WorldPoint>)

/** A location or a fast travel point with the roads it is ridden to from. */
internal class Place(val point: WorldPoint, val attachments: List<Attachment>)

/** How long rides are: across country to the nearest roads, along them, across country again. */
internal class TravelCostModel(val graph: RoadGraph, val costs: TravelCosts) {

    fun place(point: WorldPoint): Place {
        return Place(point, graph.attach(point, MAX_ATTACHMENTS, ATTACHMENT_SLACK, costs.directReach / 2))
    }

    /** The cost of the shortest ride from [a] to [b]. */
    fun ride(a: Place, b: Place): Double {
        var best = Double.POSITIVE_INFINITY
        val straight = a.point.distanceTo(b.point)
        if (straight <= costs.directReach) best = straight * costs.offRoadFactor
        for (x in a.attachments) {
            for (y in b.attachments) {
                val offRoad = (x.offRoad + y.offRoad) * costs.offRoadFactor
                if (offRoad >= best) continue
                best = min(best, offRoad + alongRoads(x, y))
            }
        }
        // Without roads between them, across country all the way
        return if (best.isInfinite()) straight * costs.offRoadFactor else best
    }

    /** Along the roads from where [x] meets its road to where [y] meets its one. */
    private fun alongRoads(x: Attachment, y: Attachment): Double {
        val toStartX = x.along
        val toEndX = graph.length(x.road) - x.along
        val toStartY = y.along
        val toEndY = graph.length(y.road) - y.along
        var best = min(
            min(toStartX + graph.distance(graph.from(x.road), graph.from(y.road)) + toStartY,
                toStartX + graph.distance(graph.from(x.road), graph.to(y.road)) + toEndY),
            min(toEndX + graph.distance(graph.to(x.road), graph.from(y.road)) + toStartY,
                toEndX + graph.distance(graph.to(x.road), graph.to(y.road)) + toEndY),
        )
        if (x.road == y.road) best = min(best, abs(x.along - y.along))
        return best
    }

    /**
     * The legs of the shortest ride from [a] to [b]: across country to a road, along the roads, across country again;
     * or straight across country when that's shorter. A leg of no length is left out.
     */
    fun rideLegs(a: Place, b: Place): List<RideLeg> {
        val straight = a.point.distanceTo(b.point)
        var best = if (straight <= costs.directReach) straight * costs.offRoadFactor else Double.POSITIVE_INFINITY
        var bestX: Attachment? = null
        var bestY: Attachment? = null
        for (x in a.attachments) {
            for (y in b.attachments) {
                val cost = (x.offRoad + y.offRoad) * costs.offRoadFactor + alongRoads(x, y)
                if (cost < best) {
                    best = cost
                    bestX = x
                    bestY = y
                }
            }
        }
        if (bestX == null || bestY == null) return listOf(RideLeg(onRoad = false, points = listOf(a.point, b.point)))
        val road = mutableListOf(bestX.point)
        appendAlongRoads(road, bestX, bestY)
        return listOfNotNull(
            RideLeg(onRoad = false, points = listOf(a.point, bestX.point)).takeIf { a.point != bestX.point },
            RideLeg(onRoad = true, points = road),
            RideLeg(onRoad = false, points = listOf(bestY.point, b.point)).takeIf { bestY.point != b.point },
        )
    }

    private fun appendAlongRoads(out: MutableList<WorldPoint>, x: Attachment, y: Attachment) {
        val lengthX = graph.length(x.road)
        val lengthY = graph.length(y.road)
        val options = listOf(
            Triple(graph.from(x.road), graph.from(y.road), x.along + graph.distance(graph.from(x.road), graph.from(y.road)) + y.along),
            Triple(graph.from(x.road), graph.to(y.road), x.along + graph.distance(graph.from(x.road), graph.to(y.road)) + lengthY - y.along),
            Triple(graph.to(x.road), graph.from(y.road), lengthX - x.along + graph.distance(graph.to(x.road), graph.from(y.road)) + y.along),
            Triple(graph.to(x.road), graph.to(y.road), lengthX - x.along + graph.distance(graph.to(x.road), graph.to(y.road)) + lengthY - y.along),
        )
        val (u, v, viaJunctions) = options.minBy { it.third }
        if (x.road == y.road && abs(x.along - y.along) <= viaJunctions) {
            graph.appendRoadPart(out, x.road, x.along, y.along)
            return
        }
        graph.appendRoadPart(out, x.road, x.along, if (u == graph.from(x.road)) 0.0 else lengthX)
        graph.appendJunctionPath(out, u, v)
        graph.appendRoadPart(out, y.road, if (v == graph.from(y.road)) 0.0 else lengthY, y.along)
    }
}
