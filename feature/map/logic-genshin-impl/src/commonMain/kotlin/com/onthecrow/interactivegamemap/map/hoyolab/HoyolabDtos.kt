package com.onthecrow.interactivegamemap.map.hoyolab

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Every answer of the map's API: `retcode` 0 and the data, or an error code. */
@Serializable
internal data class HoyolabResponse<T>(
    @SerialName("retcode") val retcode: Int,
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: T? = null,
)

@Serializable
internal data class MapInfoData(
    @SerialName("info") val info: MapInfoDto,
)

@Serializable
internal data class MapInfoDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String = "",
    @SerialName("detail_v2") val detail: MapDetailDto,
)

/**
 * The tile grid of a map. The zoom 0 has the tiles of [totalSize] pixels, each lower zoom down to [minZoom] halves
 * them. A point's position is in the pixels of zoom 0 counted from [origin]; the picture of the map lies within
 * [padding] of the grid's edges.
 */
@Serializable
internal data class MapDetailDto(
    @SerialName("total_size") val totalSize: List<Int>,
    @SerialName("padding") val padding: List<Int>,
    @SerialName("origin") val origin: List<Int>,
    @SerialName("map_version") val mapVersion: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
    @SerialName("original_map_size") val originalSize: List<Int>,
)

@Serializable
internal data class LabelTreeData(
    @SerialName("tree") val tree: List<LabelDto>,
)

/** A group of the tree, or a category of the points: the leaves. */
@Serializable
internal data class LabelDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String = "",
    @SerialName("children") val children: List<LabelDto> = emptyList(),
)

@Serializable
internal data class PointListData(
    @SerialName("point_list") val points: List<PointDto>,
)

@Serializable
internal data class PointDto(
    @SerialName("id") val id: Int,
    @SerialName("label_id") val labelId: Int,
    @SerialName("x_pos") val x: Double,
    @SerialName("y_pos") val y: Double,
    @SerialName("point_group") val group: PointGroupRefDto? = null,
)

/** The floor of an underground place the point is on. */
@Serializable
internal data class PointGroupRefDto(
    @SerialName("group_id") val groupId: Int,
    @SerialName("floor_id") val floorId: Int,
)

@Serializable
internal data class PointGroupData(
    @SerialName("list") val groups: List<PointGroupDto>,
)

/** An underground place: its floors, each a picture over the map. */
@Serializable
internal data class PointGroupDto(
    @SerialName("id") val id: Int,
    @SerialName("floors") val floors: List<FloorDto> = emptyList(),
)

@Serializable
internal data class FloorDto(
    @SerialName("id") val id: Int,
    @SerialName("floor_name") val name: String = "",
    @SerialName("floor_name_short") val shortName: String = "",
    @SerialName("overlay") val overlay: OverlayDto,
    @SerialName("entrance_ids") val entranceIds: List<Int> = emptyList(),
)

/** The picture of a floor between its corners, in the points' coordinates. */
@Serializable
internal data class OverlayDto(
    @SerialName("url") val url: String,
    @SerialName("l_x") val left: Double,
    @SerialName("l_y") val top: Double,
    @SerialName("r_x") val right: Double,
    @SerialName("r_y") val bottom: Double,
)

@Serializable
internal data class ChangelogData(
    @SerialName("list") val entries: List<ChangelogEntryDto> = emptyList(),
)

/** The points changed in a day, by their categories' names. */
@Serializable
internal data class ChangelogEntryDto(
    @SerialName("timestamp") val timestamp: String,
    @SerialName("points") val points: List<ChangelogCountDto> = emptyList(),
)

@Serializable
internal data class ChangelogCountDto(
    @SerialName("name") val name: String,
    @SerialName("num") val count: Int,
)
