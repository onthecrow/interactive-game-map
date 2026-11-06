package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.map.model.MarkerWear
import kotlin.math.roundToInt
import kotlin.random.Random

/** The painter of the image, of the part of the sprite that [icon] sets for a bitmap. */
@Composable
internal fun MarkerImage.painter(icon: MarkerIcon? = null): Painter {
    return when (this) {
        is MarkerImage.Bitmap -> remember(this, icon) {
            BitmapPainter(
                image = bitmap,
                srcOffset = IntOffset(icon?.x ?: 0, icon?.y ?: 0),
                srcSize = IntSize(icon?.width ?: bitmap.width, icon?.height ?: bitmap.height),
            )
        }
        is MarkerImage.Vector -> rememberVectorPainter(vector)
    }
}

/** Draws the image into [bounds], only the part of the sprite that [icon] sets for a bitmap. */
internal fun DrawScope.drawMarkerImage(image: MarkerImage, icon: MarkerIcon, bounds: Rect) {
    when (image) {
        is MarkerImage.Bitmap -> drawImage(
            image = image.bitmap,
            srcOffset = IntOffset(icon.x ?: 0, icon.y ?: 0),
            srcSize = IntSize(icon.width ?: image.bitmap.width, icon.height ?: image.bitmap.height),
            dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
            dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()),
        )
        is MarkerImage.Vector -> translate(bounds.left, bounds.top) { drawVector(image.vector, bounds.size) }
    }
}

/**
 * Draws [draw] worn by [wear]: in a layer of [bounds], out of which the texture's part picked by [seed] is rubbed.
 * Without the wear or its [texture] it's drawn as it is.
 */
internal fun DrawScope.withWear(
    wear: MarkerWear?,
    texture: ImageBitmap?,
    bounds: Rect,
    seed: Int,
    draw: DrawScope.() -> Unit,
) {
    if (wear == null || texture == null) return draw()
    drawContext.canvas.saveLayer(bounds, Paint())
    draw()
    drawWear(wear, texture, bounds, seed)
    drawContext.canvas.restore()
}

/** The image of the modifier worn by [wear] as by [withWear]: in a layer of its own, so nothing under it is rubbed. */
internal fun Modifier.wear(wear: MarkerWear?, texture: ImageBitmap?, seed: Int): Modifier {
    if (wear == null || texture == null) return this
    return graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawWear(wear, texture, Rect(Offset.Zero, size), seed)
        }
}

/** Rubs the wear off what's drawn in [bounds]: the destination stays only where the texture is clear. */
private fun DrawScope.drawWear(wear: MarkerWear, texture: ImageBitmap, bounds: Rect, seed: Int) {
    val region = wearRegion(texture.width, texture.height, wear.size, bounds.size / density, seed)
    drawImage(
        image = texture,
        srcOffset = region.topLeft,
        srcSize = region.size,
        dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
        dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()),
        alpha = wear.strength.coerceIn(0f, 1f),
        blendMode = BlendMode.DstOut,
    )
}

/**
 * The part of a texture of [width] x [height] px spanning [textureSize] dp that lies over an image of [imageSize]
 * dp: as big as the image, at a place [seed] picks, so equal images wear alike and the others differ.
 */
internal fun wearRegion(width: Int, height: Int, textureSize: Int, imageSize: Size, seed: Int): IntRect {
    val pxPerDp = width.toFloat() / textureSize.coerceAtLeast(1)
    val w = (imageSize.width * pxPerDp).roundToInt().coerceIn(1, width)
    val h = (imageSize.height * pxPerDp).roundToInt().coerceIn(1, height)
    val random = Random(seed)
    val left = random.nextInt(width - w + 1)
    val top = random.nextInt(height - h + 1)
    return IntRect(left, top, left + w, top + h)
}

/**
 * Draws the vector stretched to [size], out of composition, where its painter can't be had. Draws what the vector
 * drawables of Android hold: the paths with their fills and strokes, in the groups with their transforms and clips.
 */
private fun DrawScope.drawVector(vector: ImageVector, size: Size) {
    scale(size.width / vector.viewportWidth, size.height / vector.viewportHeight, pivot = Offset.Zero) {
        drawGroup(vector.root)
    }
}

private fun DrawScope.drawGroup(group: VectorGroup) {
    withTransform(
        {
            translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
            rotate(group.rotation, pivot = Offset.Zero)
            scale(group.scaleX, group.scaleY, pivot = Offset.Zero)
            translate(-group.pivotX, -group.pivotY)
            if (group.clipPathData.isNotEmpty()) clipPath(group.clipPathData.toPath())
        },
    ) {
        group.forEach { node ->
            when (node) {
                is VectorGroup -> drawGroup(node)
                is VectorPath -> drawVectorPath(node)
            }
        }
    }
}

private fun DrawScope.drawVectorPath(node: VectorPath) {
    val path = node.pathData.toPath().apply { fillType = node.pathFillType }
    node.fill?.let { drawPath(path, it, alpha = node.fillAlpha) }
    node.stroke?.let {
        drawPath(
            path = path,
            brush = it,
            alpha = node.strokeAlpha,
            style = Stroke(
                width = node.strokeLineWidth,
                miter = node.strokeLineMiter,
                cap = node.strokeLineCap,
                join = node.strokeLineJoin,
            ),
        )
    }
}
