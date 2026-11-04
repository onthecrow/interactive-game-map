package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Keeps the loaded images, so a sprite shared by many icons is loaded once. Must be used from one thread. */
internal class MarkerIconLoader(
    private val mapTileProvider: MapTileProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val images = mutableMapOf<String, Deferred<ImageBitmap?>>()

    suspend fun load(url: String): ImageBitmap? {
        val image = images.getOrPut(url) {
            scope.async {
                mapTileProvider.getImage(url)?.let { bytes ->
                    runCatching { bytes.decodeToImageBitmap() }.getOrNull()
                }
            }
        }
        return image.await().also { if (it == null) images.remove(url) }
    }
}
