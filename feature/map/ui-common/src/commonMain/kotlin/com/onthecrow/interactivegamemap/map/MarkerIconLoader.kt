package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

// An image shipped with the app instead of a downloaded one: data:image/png;base64,<the bytes>
private const val DATA_URL_PREFIX = "data:"
private const val BASE64_DATA_DELIMITER = ";base64,"

/** Keeps the loaded images, so a sprite shared by many icons is loaded once. Must be used from one thread. */
internal class MarkerIconLoader(
    private val mapTileProvider: MapTileProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val images = mutableMapOf<String, Deferred<ImageBitmap?>>()

    suspend fun load(url: String): ImageBitmap? {
        val image = images.getOrPut(url) {
            scope.async {
                getBytes(url)?.let { bytes ->
                    runCatching { bytes.decodeToImageBitmap() }.getOrNull()
                }
            }
        }
        return image.await().also { if (it == null) images.remove(url) }
    }

    private suspend fun getBytes(url: String): ByteArray? {
        if (!url.startsWith(DATA_URL_PREFIX)) return mapTileProvider.getImage(url)
        val data = url.substringAfter(BASE64_DATA_DELIMITER, missingDelimiterValue = "")
        return runCatching { Base64.decode(data) }.getOrNull()
    }
}
