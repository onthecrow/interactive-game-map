package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val LINE_WIDTH = 2.6f

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

// The glyphs of the game are cut square: butt ends, sharp corners
private fun ImageVector.Builder.line(pathData: String, width: Float = LINE_WIDTH) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
    )
}

private fun ImageVector.Builder.shape(pathData: String, evenOdd: Boolean = false) {
    addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
        pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
    )
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx + r} ${cy}A$r $r 0 1 1 ${cx - r} ${cy}A$r $r 0 1 1 ${cx + r} ${cy}Z"

private fun Double.f() = ((this * 100).toInt() / 100f).toString()

/** A gear of [teeth] square teeth with a round hole, as the gear of the game's settings menu. */
private fun gear(teeth: Int, outer: Double, inner: Double, hole: Float): String {
    val sb = StringBuilder()
    val step = 2 * PI / teeth
    for (i in 0 until teeth) {
        val a = i * step - PI / 2
        // The tooth spans 40% of its step at the tip, a bit more at its root
        listOf(
            inner to a - step * 0.3,
            outer to a - step * 0.18,
            outer to a + step * 0.18,
            inner to a + step * 0.3,
        ).forEachIndexed { k, (r, angle) ->
            sb.append(if (i == 0 && k == 0) "M" else "L")
            sb.append("${(12 + r * cos(angle)).f()} ${(12 + r * sin(angle)).f()}")
        }
    }
    sb.append("Z")
    sb.append(circle(12f, 12f, hole))
    return sb.toString()
}

/** The icons of the Red Dead Redemption 2 look, as the RDR2/Icon components of the Figma file. */
internal object Rdr2Icons {

    val Check = icon("Check") { line("M5 12.5L9.8 17.2L19 6.8") }

    /** The painted tick of the game's checklist: thin where the brush landed, thick where it lifted. */
    val PaintedCheck = icon("PaintedCheck") {
        shape("M3.6 12.4L6.2 10.6L9.9 15.1C12.4 10.6 15.9 6.4 21.2 2.4L22 3.4C17.3 8 13.9 13.3 11.3 20.4L9.1 20.6Z")
    }

    val Cross = icon("Cross") { line("M6.5 6.5L17.5 17.5M17.5 6.5L6.5 17.5") }

    val Add = icon("Add") { line("M12 5V19M5 12H19", width = 3.6f) }

    val Remove = icon("Remove") { line("M5 12H19", width = 3.6f) }

    val Search = icon("Search") {
        line(circle(10.2f, 10.2f, 5.6f))
        line("M14.3 14.3L19.8 19.8", width = 3.2f)
    }

    // The game's pointers: solid triangles
    val ArrowRight = icon("ArrowRight") { shape("M9 6L16.5 12L9 18Z") }

    val ArrowDown = icon("ArrowDown") { shape("M6 9L18 9L12 16.5Z") }

    val Back = icon("Back") {
        shape("M4 12L11 5.5V9.5H20V14.5H11V18.5Z")
    }

    // A sheet over another one
    val Copy = icon("Copy") {
        line("M9 4.5H19.5V15")
        line("M4.5 8.5H15.5V19.5H4.5Z")
    }

    /** Three entries of the map's index: a diamond and a line each. */
    val Index = icon("Index") {
        shape("M5 4.3L7.2 6.5L5 8.7L2.8 6.5Z")
        shape("M5 9.8L7.2 12L5 14.2L2.8 12Z")
        shape("M5 15.3L7.2 17.5L5 19.7L2.8 17.5Z")
        line("M10 6.5H21M10 12H21M10 17.5H21", width = 3.2f)
    }

    val Layers = icon("Layers") {
        shape("M12 3.5L21 8.6L12 13.7L3 8.6Z")
        line("M3.6 13.2L12 18L20.4 13.2", width = 2.2f)
    }

    val Settings = icon("Settings") { shape(gear(teeth = 8, outer = 10.2, inner = 7.4, hole = 3.2f), evenOdd = true) }

    val Diamond = icon("Diamond") { shape("M12 3L21 12L12 21L3 12Z") }
}
