package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt

/**
 * Recolours an image pixel by pixel, as Android's ColorMatrix does: 4 rows of 5 [values] give the red, green, blue
 * and alpha out of the red, green, blue and alpha in, plus an offset. The channels and the offset are in 0..255, and
 * the colors aren't premultiplied by their alpha.
 */
class TileColorMatrix(val values: FloatArray) {

    init {
        require(values.size == SIZE) { "A color matrix has $SIZE values, not ${values.size}" }
    }

    /** [color] as the matrix paints it, its channels kept in 0..255. */
    fun apply(color: Color): Color {
        val input = floatArrayOf(color.red, color.green, color.blue, color.alpha).map { it * MAX_CHANNEL }
        val output = (0 until CHANNELS).map { row ->
            val offset = values[row * COLUMNS + CHANNELS]
            val sum = (0 until CHANNELS).sumOf { column -> (values[row * COLUMNS + column] * input[column]).toDouble() }
            (sum.toFloat() + offset).roundToInt().coerceIn(0, MAX_CHANNEL.toInt())
        }
        return Color(red = output[0], green = output[1], blue = output[2], alpha = output[3])
    }

    override fun equals(other: Any?): Boolean = other is TileColorMatrix && values.contentEquals(other.values)

    override fun hashCode(): Int = values.contentHashCode()

    companion object {
        const val COLUMNS = 5
        const val CHANNELS = 4
        private const val SIZE = CHANNELS * COLUMNS
        private const val MAX_CHANNEL = 255f

        // The weights of the channels in the brightness of a color, as the luma of Rec. 601
        private const val LUMA_RED = 0.299f
        private const val LUMA_GREEN = 0.587f
        private const val LUMA_BLUE = 0.114f

        /**
         * Paints every pixel by its brightness alone, on a scale through two colors: a pixel as bright as the first of
         * [light] gets its second color, one as bright as the first of [dark] the second of [dark], the others lie on
         * the same line, beyond the two too. A map of light paper and dark ink turns into one of dark paper and light
         * ink when [light] is its paper and [dark] its ink.
         */
        fun byBrightness(light: Pair<Color, Color>, dark: Pair<Color, Color>): TileColorMatrix {
            val lightBrightness = light.first.brightness
            val darkBrightness = dark.first.brightness
            require(lightBrightness != darkBrightness) { "The two colors are as bright, there is no scale through them" }

            fun row(lightTo: Float, darkTo: Float): List<Float> {
                // Per unit of the brightness in 0..255, the channel's targets are in 0..255 too
                val scale = (lightTo - darkTo) * MAX_CHANNEL / (lightBrightness - darkBrightness)
                val offset = lightTo * MAX_CHANNEL - scale * lightBrightness
                return listOf(LUMA_RED * scale, LUMA_GREEN * scale, LUMA_BLUE * scale, 0f, offset)
            }

            return TileColorMatrix(
                (
                    row(light.second.red, dark.second.red) +
                        row(light.second.green, dark.second.green) +
                        row(light.second.blue, dark.second.blue) +
                        listOf(0f, 0f, 0f, 1f, 0f)
                    ).toFloatArray()
            )
        }

        /** In 0..255, as the matrix sees the channels. */
        private val Color.brightness: Float
            get() = (LUMA_RED * red + LUMA_GREEN * green + LUMA_BLUE * blue) * MAX_CHANNEL
    }
}

/**
 * Draws [block] recoloured by [colors], as they recolour the map under it, or as it is without them. The markers are
 * printed on the map, so the dark map turns their dark discs light and their light pictograms dark too.
 */
internal inline fun DrawScope.withColors(colors: TileColorMatrix?, block: DrawScope.() -> Unit) {
    if (colors == null) {
        block()
        return
    }
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix(colors.values.copyOf())) }
    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), paint)
    block()
    drawContext.canvas.restore()
}
