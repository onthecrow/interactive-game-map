package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.map.model.MarkerWear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToImageVector
import kotlin.io.encoding.Base64

// An image shipped with the app instead of a downloaded one: data:<its MIME type>;base64,<the bytes>
private const val DATA_URL_PREFIX = "data:"
private const val BASE64_DATA_DELIMITER = ";base64,"

// A vector is drawn in the units of its viewport, the density would only scale its default size
private val VectorDensity = Density(1f)

/** A loaded icon: a bitmap, or a vector, which stays sharp at any size. */
internal sealed interface MarkerImage {
    data class Bitmap(val bitmap: ImageBitmap) : MarkerImage

    data class Vector(val vector: ImageVector) : MarkerImage
}

/** Keeps the loaded images, so a sprite shared by many icons is loaded once. Must be used from one thread. */
internal class MarkerIconLoader(
    private val mapTileProvider: MapTileProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val images = mutableMapOf<String, Deferred<MarkerImage?>>()

    suspend fun load(url: String): MarkerImage? {
        val image = images.getOrPut(url) { scope.async { decode(url) } }
        return image.await().also { if (it == null) images.remove(url) }
    }

    /** The texture of the wear, null when it can't be loaded or isn't a bitmap: the image is drawn unworn then. */
    suspend fun loadTexture(wear: MarkerWear): ImageBitmap? {
        return (load(wear.texture.url) as? MarkerImage.Bitmap)?.bitmap
    }

    private suspend fun decode(url: String): MarkerImage? {
        if (!url.startsWith(DATA_URL_PREFIX)) return mapTileProvider.getImage(url)?.toBitmap()
        val mimeType = url.removePrefix(DATA_URL_PREFIX).substringBefore(BASE64_DATA_DELIMITER)
        val data = url.substringAfter(BASE64_DATA_DELIMITER, missingDelimiterValue = "")
        val bytes = runCatching { Base64.decode(data) }.getOrNull() ?: return null
        return when (mimeType) {
            MarkerIcon.VECTOR_DRAWABLE_MIME_TYPE -> runCatching {
                MarkerImage.Vector(bytes.decodeToImageVector(VectorDensity))
            }.getOrNull()
            else -> bytes.toBitmap()
        }
    }

    private fun ByteArray.toBitmap(): MarkerImage? {
        return runCatching { MarkerImage.Bitmap(decodeToImageBitmap()) }.getOrNull()
    }
}
