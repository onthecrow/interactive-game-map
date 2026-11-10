package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ProgressIndicatorComponent
import com.onthecrow.interactivegamemap.ui.components.progressSegment
import com.onthecrow.interactivegamemap.ui.components.rememberIndeterminateSegment

/**
 * A faint ink stroke filled with the red one of the selection. The red stroke spans the whole bar and is cut to the
 * progress, so it keeps its shape while it grows.
 */
object Rdr2ProgressIndicator : ProgressIndicatorComponent {
    @Composable
    override fun Content(progress: Float?, modifier: Modifier) {
        val colors = Rdr2.colors
        val segment = progressSegment(progress, rememberIndeterminateSegment())
        Box(modifier.height(12.dp)) {
            Box(Modifier.matchParentSize().brushStroke(colors.strokeRow, salt = TRACK_SALT))
            Box(
                Modifier
                    .matchParentSize()
                    .clip(
                        GenericShape { size, _ ->
                            addRect(Rect(segment.start * size.width, 0f, segment.end * size.width, size.height))
                        }
                    )
                    .brushStroke(colors.selection, salt = FILL_SALT)
            )
        }
    }

    private const val TRACK_SALT = 71
    private const val FILL_SALT = 72
}
