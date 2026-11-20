package com.onthecrow.interactivegamemap.map.hoyolab

import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The key in Remote Config of where the data of the game comes from. */
internal const val REMOTE_CONFIG_KEY_CONTENT = "content"

/**
 * Where HoYoLAB's map is, from Remote Config: a changed address is fixed there without a release. The defaults are
 * the addresses of November 2026.
 *
 * @param enabled false stops the requests, the app goes on with the data it has
 * @param fullRefreshDays how often everything is downloaded again, even when the changelog says nothing changed
 */
@Serializable
internal data class HoyolabContentConfig(
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("baseUrl") val baseUrl: String = "https://sg-public-api-static.hoyolab.com/common/map_user/ys_obc",
    @SerialName("params") val params: Map<String, String> = mapOf("app_sn" to "ys_obc"),
    @SerialName("mapIds") val mapIds: List<Int> = listOf(2, 7, 9, 34, 36, 37, 40),
    @SerialName("fullRefreshDays") val fullRefreshDays: Int = 7,
)

internal class HoyolabConfigProvider(
    remoteConfig: RemoteConfigProvider,
    crashReporter: CrashReporter,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** The defaults until Remote Config has its values, and when it has none. */
    val config: StateFlow<HoyolabContentConfig> = remoteConfig.observe(REMOTE_CONFIG_KEY_CONTENT)
        .mapNotNull { value ->
            try {
                json.decodeFromString<HoyolabContentConfig>(value)
            } catch (e: SerializationException) {
                crashReporter.log("Remote config: can't parse the content, ${e.message}")
                crashReporter.recordNonFatal(e)
                null
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, HoyolabContentConfig())
}
