package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapTile
import com.onthecrow.interactivegamemap.network.MapTiles
import com.onthecrow.interactivegamemap.network.TileScheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class MapTileProvider(
    private val httpClient: HttpClient,
) {

    /**
     * @return null when the tile can't be loaded right now and should be requested again,
     * a tile without data when the server doesn't have it
     */
    suspend fun getTileAsync(tiles: MapTiles, x: Int, y: Int, zoom: Int): MapTile? {
        return nullOnFailure {
            MapTile(
                width = SIZE_TILE_PX,
                height = SIZE_TILE_PX,
                data = download(getUrl(tiles, x, y, zoom)),
            )
        }
    }

    suspend fun getImage(url: String): ByteArray? {
        return nullOnFailure { download(url) }
    }

    private fun getUrl(tiles: MapTiles, x: Int, y: Int, zoom: Int): String {
        val (tileX, tileY) = when (tiles.scheme) {
            TileScheme.XYZ -> x to y
            TileScheme.CENTERED -> {
                val half = 1 shl (zoom - 1)
                (x - half) to (y - half).inv()
            }
        }
        return tiles.urlTemplate
            .replace(KEY_TILE_X, tileX.toString())
            .replace(KEY_TILE_Y, tileY.toString())
            .replace(KEY_TILE_Z, zoom.toString())
    }

    private suspend fun download(url: String): ByteArray? {
        val response = httpClient.get(url)

        if (!response.status.isSuccess()) {
            println("HTTP ${response.status.value} ${response.status.description} for $url")
            return null
        }
        return response.body<ByteArray>()
    }

    private suspend fun <T> nullOnFailure(block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            null
        }
    }

    private companion object {
        private const val KEY_TILE_X = "{{x}}"
        private const val KEY_TILE_Y = "{{y}}"
        private const val KEY_TILE_Z = "{{z}}"
        private const val SIZE_TILE_PX = 256
    }
}
