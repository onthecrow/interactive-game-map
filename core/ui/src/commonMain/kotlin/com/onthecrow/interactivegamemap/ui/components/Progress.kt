package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

/** A bar of how much of a load is done: of the first load of the data, of an update. */
interface ProgressIndicatorComponent {
    /** @param progress from 0 to 1, null while unknown: then the bar runs on its own */
    @Composable
    fun Content(progress: Float?, modifier: Modifier)
}

@Composable
fun GameProgressIndicator(progress: Float?, modifier: Modifier = Modifier) {
    GameMapTheme.components.progressIndicator.Content(progress, modifier)
}

/**
 * Where the moving part of a bar of unknown progress is, as fractions of its length from its start to its end; one
 * pass every [periodMillis].
 */
@Composable
fun rememberIndeterminateSegment(periodMillis: Int = 1400): ProgressSegment {
    val transition = rememberInfiniteTransition()
    val position by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
    )
    // The part runs in from before the start and out past the end
    val start = (position * (1f + SEGMENT_LENGTH) - SEGMENT_LENGTH).coerceIn(0f, 1f)
    val end = (position * (1f + SEGMENT_LENGTH)).coerceIn(0f, 1f)
    return ProgressSegment(start, end)
}

/** The filled part of a bar, as fractions of its length. */
data class ProgressSegment(val start: Float, val end: Float)

/** The filled part of a bar of [progress], or [indeterminate] while it's unknown. */
fun progressSegment(progress: Float?, indeterminate: ProgressSegment): ProgressSegment {
    return progress?.let { ProgressSegment(0f, it.coerceIn(0f, 1f)) } ?: indeterminate
}

private const val SEGMENT_LENGTH = 0.35f
