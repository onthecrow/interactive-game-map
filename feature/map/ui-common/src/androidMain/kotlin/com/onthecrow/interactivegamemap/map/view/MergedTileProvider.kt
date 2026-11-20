package com.onthecrow.interactivegamemap.map.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.onthecrow.interactivegamemap.map.MapLoad
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.TileColorMatrix
import com.onthecrow.interactivegamemap.map.model.MapTile
import com.onthecrow.interactivegamemap.map.model.MapTiles
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream

private const val MERGED_TILE_JPEG_QUALITY = 90

/** A tile of the next zoom and its position in the merged tile. */
internal data class MergedTilePart(
    val x: Int,
    val y: Int,
    val column: Int,
    val row: Int,
)

internal fun mergedTileParts(x: Int, y: Int): List<MergedTilePart> {
    return listOf(0, 1).flatMap { row ->
        listOf(0, 1).map { column ->
            MergedTilePart(x = 2 * x + column, y = 2 * y + row, column = column, row = row)
        }
    }
}

/**
 * The map draws a tile on 256 dp, so a 256 px tile is stretched on high density screens.
 * The four tiles of the next zoom cover the same part of the world and have twice as many pixels.
 * Each tile the map gets goes to [load].
 *
 * @param colors recolour the tiles as they are merged, or as they come for the last zoom; null leaves them as they are
 */
internal class MergedTileProvider(
    private val mapTileProvider: MapTileProvider,
    private val tiles: MapTiles,
    private val maxZoom: Int,
    private val load: MapLoad,
    private val colors: TileColorMatrix? = null,
) : TileProvider {
    // The map asks for tiles on several threads at once, each merge takes a paint of its own
    private val colorFilter = colors?.let { ColorMatrixColorFilter(it.values) }

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        var error: String? = null
        val tile = runBlocking {
            val onError = { reason: String -> error = reason }
            if (zoom < maxZoom) getMergedTile(x, y, zoom, onError) else getSingleTile(x, y, zoom, onError)
        }
        load.onTile(
            hasImage = tile != null && tile !== TileProvider.NO_TILE,
            error = error ?: "unknown".takeIf { tile == null },
        )
        return tile
    }

    private suspend fun getSingleTile(x: Int, y: Int, zoom: Int, onError: (String) -> Unit): Tile? {
        val tile = mapTileProvider.getTileAsync(tiles, x, y, zoom, onError) ?: return null
        val image = tile.data?.takeIf { colorFilter != null }?.toBitmap() ?: return tile.toGoogleTile()
        return merge(listOf(MergedTilePart(x, y, column = 0, row = 0) to image), isComplete = true, parts = 1)
    }

    private suspend fun getMergedTile(x: Int, y: Int, zoom: Int, onError: (String) -> Unit): Tile? {
        val parts = coroutineScope {
            mergedTileParts(x, y).map { part ->
                async { part to mapTileProvider.getTileAsync(tiles, part.x, part.y, zoom + 1, onError) }
            }.awaitAll()
        }
        // The map requests a tile again when it can't be loaded right now
        if (parts.any { (_, tile) -> tile == null }) return null

        val images = parts.mapNotNull { (part, tile) ->
            tile?.data?.toBitmap()?.let { part to it }
        }
        return if (images.isEmpty()) {
            getSingleTile(x, y, zoom, onError)
        } else {
            merge(images, isComplete = images.size == parts.size, parts = 2)
        }
    }

    /** [images] drawn on one tile of [parts] x [parts] of them, through the [colors] if there are any. */
    private fun merge(images: List<Pair<MergedTilePart, Bitmap>>, isComplete: Boolean, parts: Int): Tile {
        val partWidth = images.first().second.width
        val partHeight = images.first().second.height
        val merged = Bitmap.createBitmap(partWidth * parts, partHeight * parts, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(merged)
        val paint = colorFilter?.let { Paint().apply { colorFilter = it } }
        images.forEach { (part, image) ->
            canvas.drawBitmap(image, part.column * partWidth.toFloat(), part.row * partHeight.toFloat(), paint)
            image.recycle()
        }
        // Only PNG keeps the missing parts at the edges of the map transparent
        val format = if (isComplete) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val output = ByteArrayOutputStream()
        merged.compress(format, MERGED_TILE_JPEG_QUALITY, output)
        merged.recycle()
        return Tile(partWidth * parts, partHeight * parts, output.toByteArray())
    }

    private fun ByteArray.toBitmap(): Bitmap? {
        return BitmapFactory.decodeByteArray(this, 0, size)
    }

    private fun MapTile.toGoogleTile(): Tile {
        return data?.let { Tile(width, height, it) } ?: TileProvider.NO_TILE
    }
}

/** A copy of the image recoloured by [colors]. */
internal fun Bitmap.recoloured(colors: TileColorMatrix): Bitmap {
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(this, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(colors.values) })
    return result
}
