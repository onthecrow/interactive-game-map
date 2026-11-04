package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapTile
import com.onthecrow.interactivegamemap.network.MapTiles
import com.onthecrow.interactivegamemap.network.TileScheme
import com.onthecrow.interactivegamemap.network.networkErrorType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class MapTileProvider(
    private val httpClient: HttpClient,
) {

    /**
     * @param onError gets why the tile wasn't loaded, for the analytics: an error of the network, or of the server
     * when it has the tile
     * @return null when the tile can't be loaded right now and should be requested again,
     * a tile without data when the server doesn't have it
     */
    suspend fun getTileAsync(
        tiles: MapTiles,
        x: Int,
        y: Int,
        zoom: Int,
        onError: (String) -> Unit = {},
    ): MapTile? {
        return nullOnFailure(onError) {
            MapTile(
                width = SIZE_TILE_PX,
                height = SIZE_TILE_PX,
                data = download(getUrl(tiles, x, y, zoom), onError),
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

    private suspend fun download(url: String, onError: (String) -> Unit = {}): ByteArray? {
        val response = httpClient.get(url)

        if (!response.status.isSuccess()) {
            println("HTTP ${response.status.value} ${response.status.description} for $url")
            // There are no tiles past the edges of a map, the server answers so there
            if (response.status != HttpStatusCode.NotFound) onError("http_${response.status.value}")
            return null
        }
        return response.body<ByteArray>()
    }

    private suspend fun <T> nullOnFailure(onError: (String) -> Unit = {}, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            onError(e.networkErrorType())
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
