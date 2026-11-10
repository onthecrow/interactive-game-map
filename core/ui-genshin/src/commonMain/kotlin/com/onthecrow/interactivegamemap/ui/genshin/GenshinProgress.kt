package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ProgressIndicatorComponent
import com.onthecrow.interactivegamemap.ui.components.progressSegment
import com.onthecrow.interactivegamemap.ui.components.rememberIndeterminateSegment

/** A thin gold bar in an ornament frame, as the loading bar of the game. */
object GenshinProgressIndicator : ProgressIndicatorComponent {
    @Composable
    override fun Content(progress: Float?, modifier: Modifier) {
        val colors = Genshin.colors
        val segment = progressSegment(progress, rememberIndeterminateSegment())
        Canvas(modifier.height(8.dp)) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(colors.surfaceRowTrail, cornerRadius = radius)
            val start = segment.start * size.width
            val end = segment.end * size.width
            if (end > start) {
                drawRoundRect(
                    color = colors.iconAccent,
                    topLeft = Offset(start, 0f),
                    size = Size(end - start, size.height),
                    cornerRadius = radius,
                )
            }
            drawRoundRect(colors.borderOrnament, cornerRadius = radius, style = Stroke(1.dp.toPx()))
        }
    }
}
