package com.onthecrow.interactivegamemap.map.common

import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.map.MapsRepository
import com.onthecrow.interactivegamemap.map.model.MapsConfig
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The key of the maps in Remote Config. */
const val REMOTE_CONFIG_KEY_MAPS = "maps"

/** The maps as Remote Config has them, a game's repository can add what its source knows better. */
class RemoteConfigMapsRepository(
    private val remoteConfig: RemoteConfigProvider,
    private val crashReporter: CrashReporter,
) : MapsRepository {

    override fun maps(): Flow<MapsConfig> {
        return remoteConfig.observe(REMOTE_CONFIG_KEY_MAPS).mapNotNull { value ->
            try {
                parseMapsConfig(value)
            } catch (e: SerializationException) {
                // A malformed config leaves every user without the maps: the current one stays
                crashReporter.log("Remote config: can't parse the maps, ${e.message}")
                crashReporter.recordNonFatal(e)
                null
            }
        }
    }
}

private val mapsJson = Json { ignoreUnknownKeys = true }

/** @throws SerializationException for a malformed config */
fun parseMapsConfig(value: String): MapsConfig = mapsJson.decodeFromString(value)
