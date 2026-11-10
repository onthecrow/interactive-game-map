package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.hoyolab.FloorDto
import com.onthecrow.interactivegamemap.map.hoyolab.LabelDto
import com.onthecrow.interactivegamemap.map.hoyolab.MapDetailDto
import com.onthecrow.interactivegamemap.map.hoyolab.PointDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

/**
 * HoYoLAB's points as the app keeps them: in columns, a few megabytes instead of the tens of the answers, read
 * fast at every launch. The coordinates stay the source's ones, the projection is applied on reading.
 */
@Serializable
internal data class GenshinSnapshot(
    @SerialName("version") val version: String,
    @SerialName("maps") val maps: List<SnapshotMap>,
    @SerialName("points") val points: SnapshotPoints,
    @SerialName("groups") val groups: List<SnapshotGroup>,
)

@Serializable
internal data class SnapshotMap(
    @SerialName("id") val id: Int,
    @SerialName("detail") val detail: MapDetailDto,
)

/** One column per field, a point is an index; 0 in [groupIds] and [floorIds] is none. */
@Serializable
internal data class SnapshotPoints(
    @SerialName("ids") val ids: List<Int>,
    @SerialName("mapIds") val mapIds: List<Int>,
    @SerialName("labelIds") val labelIds: List<Int>,
    @SerialName("xs") val xs: List<Double>,
    @SerialName("ys") val ys: List<Double>,
    @SerialName("groupIds") val groupIds: List<Int>,
    @SerialName("floorIds") val floorIds: List<Int>,
) {
    val size: Int get() = ids.size
}

@Serializable
internal data class SnapshotGroup(
    @SerialName("id") val id: Int,
    @SerialName("mapId") val mapId: Int,
    @SerialName("floors") val floors: List<FloorDto>,
)

/** The names of the categories and of the maps in one language of the app. */
@Serializable
internal data class SnapshotLabels(
    @SerialName("language") val language: String,
    @SerialName("groups") val groups: List<LabelDto>,
    @SerialName("mapNames") val mapNames: Map<Int, String>,
)

/**
 * What the device has, read at every launch and every check, so small.
 *
 * @param version what the check compares: the newest changelog of every map and the versions of their tiles
 * @param languages the languages the names were downloaded in, the first one with the points
 */
@Serializable
internal data class SnapshotMeta(
    @SerialName("version") val version: String,
    @SerialName("fetchedAt") val fetchedAtEpochMillis: Long,
    @SerialName("pointCount") val pointCount: Int,
    @SerialName("languages") val languages: List<String>,
)

/** The points of every map in columns, a hundredth of a pixel is plenty. */
internal fun snapshotPoints(pointsByMap: Map<Int, List<PointDto>>): SnapshotPoints {
    val points = pointsByMap.flatMap { (mapId, points) -> points.map { mapId to it } }
    return SnapshotPoints(
        ids = points.map { it.second.id },
        mapIds = points.map { it.first },
        labelIds = points.map { it.second.labelId },
        xs = points.map { it.second.x.toHundredths() },
        ys = points.map { it.second.y.toHundredths() },
        groupIds = points.map { it.second.group?.groupId ?: 0 },
        floorIds = points.map { it.second.group?.floorId ?: 0 },
    )
}

private fun Double.toHundredths(): Double = (this * 100).roundToLong() / 100.0
