package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val LINE_WIDTH = 3f

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

// A line with the rounded 3px stroke all the line icons share
private fun ImageVector.Builder.line(pathData: String, width: Float = LINE_WIDTH) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

private fun ImageVector.Builder.shape(pathData: String) {
    addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx + r} ${cy}A$r $r 0 1 1 ${cx - r} ${cy}A$r $r 0 1 1 ${cx + r} ${cy}Z"

/** The icons of the Genshin Impact look, as the GI/Icon components of the Figma file. */
internal object GenshinIcons {

    val Check = icon("Check") { line("M5.5 12.5L10 17L18.5 7.5") }

    val Cross = icon("Cross") { line("M6.5 6.5L17.5 17.5M17.5 6.5L6.5 17.5") }

    val ChevronRight = icon("ChevronRight") { line("M9.5 5.5L16 12L9.5 18.5") }

    // A sheet over another one
    val Copy = icon("Copy") {
        line("M9 4.5H17.5A2 2 0 0 1 19.5 6.5V15")
        line("M6.5 8H14A2 2 0 0 1 16 10V18A2 2 0 0 1 14 20H6.5A2 2 0 0 1 4.5 18V10A2 2 0 0 1 6.5 8Z")
    }

    val ExpandMore = icon("ExpandMore") { line("M6 9.5L12 15.5L18 9.5") }

    val Add = icon("Add") { line("M12 5.5V18.5M5.5 12H18.5") }

    val Remove = icon("Remove") { line("M5.5 12H18.5") }

    val Search = icon("Search") {
        line(circle(10.5f, 10.5f, 6f))
        line("M15.2 15.2L19.5 19.5")
    }

    val List = icon("List") {
        shape(circle(5f, 6.5f, 1.9f))
        shape(circle(5f, 12f, 1.9f))
        shape(circle(5f, 17.5f, 1.9f))
        line("M10 6.5H19.5M10 12H19.5M10 17.5H19.5")
    }

    val Layers = icon("Layers") {
        addPath(
            pathData = addPathNodes("M12 4.5L19.5 9.2L12 13.9L4.5 9.2Z"),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineJoin = StrokeJoin.Round,
        )
        line("M4.5 14.2L12 18.9L19.5 14.2")
    }

    val Settings = icon("Settings") {
        shape(
            "M10.83,22q-0.68,0-1.16-0.45t-0.59-1.1L8.85,18.8q-0.32-0.13-0.61-0.3T7.68,18.13L6.13,18.77Q5.5,19.05 " +
                "4.88,18.83T3.9,18.02L2.73,15.98q-0.35-0.57-0.2-1.22T3.2,13.68l1.33-1Q4.5,12.5 4.5,12.34q0-0.16 " +
                "0-0.34t0-0.34t0.03-0.34l-1.33-1Q2.68,9.9 2.53,9.25t0.2-1.23L3.9,5.97Q4.25,5.4 4.88,5.18T6.13,5.22L7.68," +
                "5.88Q7.95,5.68 8.25,5.5t0.6-0.3L9.08,3.55q0.1-0.65 0.59-1.1T10.83,2h2.35q0.68,0 1.16,0.45t0.59,1.1L15.15," +
                "5.2q0.33,0.13 0.61,0.3t0.56,0.38l1.55-0.65q0.63-0.27 1.25-0.05t0.98,0.8l1.18,2.05q0.35,0.58 0.2,1.23T20.8," +
                "10.33l-1.32,1q0.02,0.18 0.02,0.34t0,0.34t0,0.34t-0.05,0.34l1.32,1q0.52,0.43 0.68,1.08t-0.2,1.22l-1.2," +
                "2.05q-0.35,0.58-0.98,0.8t-1.25-0.05l-1.5-0.65q-0.27,0.2-0.57,0.38t-0.6,0.3l-0.22,1.65q-0.1,0.65-0.59," +
                "1.1T13.18,22H10.83Zm1.22-6.5q1.45,0 2.47-1.03T15.55,12T14.53,9.52T12.05,8.5q-1.47,0-2.49,1.02T8.55,12t1.01," +
                "2.47t2.49,1.03Z",
        )
    }

    /** The game's cross: four thick arms ending in arrowheads, the round join softens the tips. */
    val Close = icon("Close") {
        addPath(
            pathData = addPathNodes(
                "M6.13 6.13L10.23 6.56L9.21 7.58L16.42 14.79L17.44 13.77L17.87 17.87L13.77 17.44L14.79 16.42L7.58 9.21" +
                    "L6.56 10.23ZM6.13 17.87L6.56 13.77L7.58 14.79L14.79 7.58L13.77 6.56L17.87 6.13L17.44 10.23" +
                    "L16.42 9.21L9.21 16.42L10.23 17.44Z",
            ),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.1f,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    val DiamondFilled = icon("DiamondFilled") { shape("M12 3L21 12L12 21L3 12Z") }
}
