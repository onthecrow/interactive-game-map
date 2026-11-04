package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import kotlin.math.max
import kotlin.math.roundToInt

private val ClusterHeight = 40.dp
private val ClusterBorder = 2.dp
private val ClusterTextPadding = 10.dp

/** The exact number of a small cluster, a rounded down one of a big cluster, so few images are drawn. */
internal val MarkerCluster.label: String
    get() = when {
        count < 100 -> "$count"
        count < 1000 -> "${count / 100 * 100}+"
        else -> "${count / 1000 * 1000}+"
    }

/** The images of the clusters by their labels, a round badge in the cluster colors of the theme. */
@Composable
internal fun rememberClusterImages(clusters: List<MarkerCluster>): Map<String, ImageBitmap> {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val colors = GameMapTheme.colors
    val background = colors.cluster
    val border = colors.clusterBorder
    val style = GameMapTheme.typography.label.copy(color = colors.onCluster)
    val images = remember(textMeasurer, density, background, border, style) { mutableMapOf<String, ImageBitmap>() }

    return remember(clusters, images) {
        clusters.map { it.label }.distinct().associateWith { label ->
            images.getOrPut(label) { drawCluster(label, textMeasurer, style, density, background, border) }
        }
    }
}

private fun drawCluster(
    label: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    density: Density,
    background: Color,
    border: Color,
): ImageBitmap {
    val text = textMeasurer.measure(label, style)
    val height = with(density) { ClusterHeight.toPx() }
    val borderWidth = with(density) { ClusterBorder.toPx() }
    // A circle, or a pill when the number is long
    val width = max(height, text.size.width + 2 * with(density) { ClusterTextPadding.toPx() })
    val image = ImageBitmap(width.roundToInt(), height.roundToInt())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
        drawRoundRect(color = border, cornerRadius = CornerRadius(size.height / 2))
        drawRoundRect(
            color = background,
            topLeft = Offset(borderWidth, borderWidth),
            size = Size(size.width - 2 * borderWidth, size.height - 2 * borderWidth),
            cornerRadius = CornerRadius(size.height / 2 - borderWidth),
        )
        drawText(
            textLayoutResult = text,
            topLeft = Offset((size.width - text.size.width) / 2, (size.height - text.size.height) / 2),
        )
    }
    return image
}
