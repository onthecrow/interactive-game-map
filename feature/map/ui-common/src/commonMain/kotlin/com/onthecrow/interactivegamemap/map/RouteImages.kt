package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onthecrow.interactivegamemap.map.model.RouteFastTravel
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.route_fast_travel_point
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

// Smaller than the 30 dp pins of the markers
private val RouteStopSize = 20.dp
private val RouteStopBorder = 1.5.dp
private val RouteStopTextPadding = 4.dp

// The bubble over a fast travel point of a route: its name and what it is, a pointer down to the point
private val BubblePadding = 10.dp
private val BubbleLineGap = 2.dp
private val BubbleCorner = 6.dp
private val BubbleBorder = 1.dp
private val BubblePointer = 7.dp
// The bubble's pointer ends this far above the middle of the point, clear of its disc
internal val RouteStopBubbleLift = RouteStopSize / 2 + 2.dp

/**
 * The bubble over the fast travel point [stop] of a route, in the colors of the popups; its bottom middle is the tip of
 * its pointer, [RouteStopBubbleLift] above the point. Null without a stop.
 */
@Composable
internal fun rememberRouteStopBubble(stop: RouteFastTravel?): ImageBitmap? {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val colors = GameMapTheme.colors
    val typography = GameMapTheme.typography
    val caption = stringResource(Res.string.route_fast_travel_point)
    val nameStyle = typography.label.copy(color = colors.content, fontWeight = FontWeight.Bold)
    val captionStyle = typography.caption.copy(color = colors.contentSecondary)
    return remember(stop, caption, textMeasurer, density, colors, nameStyle, captionStyle) {
        stop?.let {
            drawBubble(it.point.name, caption, textMeasurer, nameStyle, captionStyle, density, colors.panel, colors.outline)
        }
    }
}

private fun drawBubble(
    name: String,
    caption: String,
    textMeasurer: TextMeasurer,
    nameStyle: TextStyle,
    captionStyle: TextStyle,
    density: Density,
    background: Color,
    border: Color,
): ImageBitmap {
    val nameText = textMeasurer.measure(name, nameStyle)
    val captionText = textMeasurer.measure(caption, captionStyle)
    with(density) {
        val padding = BubblePadding.toPx()
        val pointer = BubblePointer.toPx()
        val borderWidth = BubbleBorder.toPx()
        val boxWidth = max(nameText.size.width, captionText.size.width) + 2 * padding
        val boxHeight = nameText.size.height + BubbleLineGap.toPx() + captionText.size.height + 2 * padding
        val width = boxWidth.roundToInt()
        val height = (boxHeight + pointer).roundToInt()
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
            val middle = size.width / 2
            val inset = borderWidth / 2
            val box = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = inset,
                        top = inset,
                        right = boxWidth - inset,
                        bottom = boxHeight - inset,
                        cornerRadius = CornerRadius(BubbleCorner.toPx()),
                    )
                )
            }
            val tip = Path().apply {
                moveTo(middle - pointer, boxHeight - borderWidth * 2)
                lineTo(middle, boxHeight + pointer - inset)
                lineTo(middle + pointer, boxHeight - borderWidth * 2)
                close()
            }
            // One outline of the box and its pointer
            val bubble = Path.combine(PathOperation.Union, box, tip)
            drawPath(bubble, background)
            drawPath(bubble, border, style = Stroke(borderWidth))
            drawText(nameText, topLeft = Offset((size.width - nameText.size.width) / 2, padding))
            drawText(
                captionText,
                topLeft = Offset((size.width - captionText.size.width) / 2, padding + nameText.size.height + BubbleLineGap.toPx()),
            )
        }
        return image
    }
}

/** The images of the numbered fast travel points of a route by their numbers: a white disc with a black number. */
@Composable
internal fun rememberRouteStopImages(numbers: List<Int>): Map<Int, ImageBitmap> {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = GameMapTheme.typography.label.copy(color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val images = remember(textMeasurer, density, style) { mutableMapOf<Int, ImageBitmap>() }

    return remember(numbers, images) {
        numbers.distinct().associateWith { number ->
            images.getOrPut(number) { drawRouteStop(number, textMeasurer, style, density) }
        }
    }
}

private fun drawRouteStop(number: Int, textMeasurer: TextMeasurer, style: TextStyle, density: Density): ImageBitmap {
    val text = textMeasurer.measure("$number", style)
    val height = with(density) { RouteStopSize.toPx() }
    val border = with(density) { RouteStopBorder.toPx() }
    // A disc, or a pill when the number is long
    val width = max(height, text.size.width + 2 * with(density) { RouteStopTextPadding.toPx() })
    val image = ImageBitmap(width.roundToInt(), height.roundToInt())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = Color.Black, cornerRadius = radius)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(border, border),
            size = Size(size.width - 2 * border, size.height - 2 * border),
            cornerRadius = CornerRadius(size.height / 2 - border),
        )
        drawText(
            textLayoutResult = text,
            topLeft = Offset((size.width - text.size.width) / 2, (size.height - text.size.height) / 2),
        )
    }
    return image
}
