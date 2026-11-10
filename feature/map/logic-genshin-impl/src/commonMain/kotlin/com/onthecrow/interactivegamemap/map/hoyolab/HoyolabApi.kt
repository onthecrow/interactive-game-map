package com.onthecrow.interactivegamemap.map.hoyolab

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** The source answered, but not with its data: an HTTP error or an error code of the API. */
internal class HoyolabException(reason: String) : Exception(reason)

/** The map's API of HoYoLAB, at the addresses of [config]. */
internal class HoyolabApi(
    private val httpClient: HttpClient,
    private val config: HoyolabConfigProvider,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** The map's tile grid, and its name and icon in [language], a language of the app. */
    suspend fun mapInfo(mapId: Int, language: String): MapInfoDto {
        return get<MapInfoData>("v3/map/info", mapId, language).info
    }

    /** The groups of the categories with the categories in them, the same for every map. */
    suspend fun labelTree(mapId: Int, language: String): List<LabelDto> {
        return get<LabelTreeData>("v2/map/label/tree", mapId, language).tree
    }

    /**
     * Every point of the map, the biggest answer: tens of megabytes for Teyvat.
     *
     * @param expectedBytes how big the answer is likely to be, for [progress] when the server doesn't say
     */
    suspend fun points(mapId: Int, expectedBytes: Long, progress: (Float) -> Unit): List<PointDto> {
        return get<PointListData>("v3/map/point/list", mapId, ENGLISH, expectedBytes, progress).points
    }

    /** The underground places of the map with their floors. */
    suspend fun pointGroups(mapId: Int): List<PointGroupDto> {
        return get<PointGroupData>("v2/map/point_group", mapId, ENGLISH).groups
    }

    /** The points changed by day, the newest first: the cheap way to learn whether anything changed. */
    suspend fun changelog(mapId: Int): List<ChangelogEntryDto> {
        return get<ChangelogData>("v1/map/get_point_changelog", mapId, ENGLISH).entries
    }

    private suspend inline fun <reified T> get(
        path: String,
        mapId: Int,
        language: String,
        expectedBytes: Long? = null,
        noinline progress: ((Float) -> Unit)? = null,
    ): T {
        val config = config.config.value
        val response = httpClient.get("${config.baseUrl.trimEnd('/')}/$path") {
            parameter("map_id", mapId)
            parameter("lang", hoyolabLanguage(language))
            config.params.forEach { (key, value) -> parameter(key, value) }
            if (progress != null && expectedBytes != null) {
                onDownload { received, length ->
                    val total = if (length > 0) length else expectedBytes
                    progress((received.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        if (!response.status.isSuccess()) throw HoyolabException("http_${response.status.value}")
        val answer = json.decodeFromString<HoyolabResponse<T>>(response.body<ByteArray>().decodeToString())
        if (answer.retcode != 0) throw HoyolabException("retcode_${answer.retcode}: ${answer.message}")
        return answer.data ?: throw HoyolabException("no_data")
    }

    private companion object {
        const val ENGLISH = "en"
    }
}

/** The language of HoYoLAB for a language of the app. */
internal fun hoyolabLanguage(language: String): String = when (language) {
    "en" -> "en-us"
    "ja" -> "ja-jp"
    "ko" -> "ko-kr"
    "pt" -> "pt-pt"
    "zh" -> "zh-cn"
    else -> "$language-$language"
}
