package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapTile
import com.onthecrow.interactivegamemap.network.RemoteConfig
import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

abstract class MapTileProvider(
    private val configProvider: RemoteConfigProvider,
    private val httpClient: HttpClient,
) {
    private var remoteConfig: RemoteConfig? = null

    init {
        configProvider.getRemoteConfigFlow()
            .onEach { remoteConfig = it }
            .launchIn(MainScope())
    }

    abstract fun getXYZ(x: Int, y: Int, zoom: Int): Triple<String, String, String>

    fun getTile(x: Int, y: Int, zoom: Int): MapTile {
        return MapTile(
            width = SIZE_TILE_PX,
            height = SIZE_TILE_PX,
            data = runBlocking { downloadTile(getUrl(x, y, zoom)) },
        )
    }

    suspend fun getTileAsync(x: Int, y: Int, zoom: Int): MapTile {
        return MapTile(
            width = SIZE_TILE_PX,
            height = SIZE_TILE_PX,
            data = downloadTile(getUrl(x, y, zoom)),
        )
    }

    private fun getUrl(x: Int, y: Int, zoom: Int): String? {
        val remoteConfig = remoteConfig ?: return null

        val (tileX, tileY, tileZ) = getXYZ(x, y, zoom)

        return remoteConfig.tilesUrl
            .replace(KEY_TILE_X, tileX)
            .replace(KEY_TILE_Y, tileY)
            .replace(KEY_TILE_Z, tileZ)
    }

    private suspend fun downloadTile(url: String?): ByteArray? {
        if (url == null) {
            return null
        }
        val response = httpClient.get(url)

        if (!response.status.isSuccess()) {
            println("HTTP ${response.status.value} ${response.status.description} for $url")
        }
        return response.body<ByteArray>()
    }

    private companion object {
        private const val KEY_TILE_X = "{{x}}"
        private const val KEY_TILE_Y = "{{y}}"
        private const val KEY_TILE_Z = "{{z}}"
        private const val SIZE_TILE_PX = 256
    }
}
