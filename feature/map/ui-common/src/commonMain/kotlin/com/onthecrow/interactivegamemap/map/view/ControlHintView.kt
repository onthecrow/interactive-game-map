package com.onthecrow.interactivegamemap.map.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.BubbleBorder
import com.onthecrow.interactivegamemap.map.BubbleCorner
import com.onthecrow.interactivegamemap.map.BubbleLineGap
import com.onthecrow.interactivegamemap.map.BubblePadding
import com.onthecrow.interactivegamemap.map.BubblePointer
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

private val HintMaxWidth = 240.dp
// How far the hint nudges toward the button now and then, to be noticed
private val NudgeDistance = 6.dp
private const val NUDGE_PERIOD_MILLIS = 2400

/**
 * A bubble that tells about a button, at its left with a pointer to it: the look of the bubble of a fast travel point,
 * the pointer at the right. It grows out of the pointer, nudges toward the button now and then, and shrinks back into
 * it.
 */
@Composable
internal fun ControlHintView(visible: Boolean, title: String, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(tween(durationMillis = 250)) +
            scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.6f, transformOrigin = PointerOrigin),
        exit = fadeOut(tween(durationMillis = 200)) +
            scaleOut(tween(durationMillis = 200), targetScale = 0.6f, transformOrigin = PointerOrigin),
    ) {
        val nudge by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = NUDGE_PERIOD_MILLIS
                    0f at 0
                    0f at 1600
                    1f at 1800 using FastOutSlowInEasing
                    0f at 2000 using FastOutSlowInEasing
                    1f at 2200 using FastOutSlowInEasing
                    0f at NUDGE_PERIOD_MILLIS
                },
                repeatMode = RepeatMode.Restart,
            ),
        )
        val distance = with(LocalDensity.current) { NudgeDistance.toPx() }
        HintBubble(title = title, text = text, modifier = Modifier.graphicsLayer { translationX = nudge * distance })
    }
}

@Composable
private fun HintBubble(title: String, text: String, modifier: Modifier = Modifier) {
    val colors = GameMapTheme.colors
    val typography = GameMapTheme.typography
    val shape = BubbleWithPointerAtRight
    Column(
        modifier = modifier
            .widthIn(max = HintMaxWidth + BubblePointer)
            .background(colors.panel, shape)
            .border(BubbleBorder, colors.outline, shape)
            .padding(start = BubblePadding, top = BubblePadding, bottom = BubblePadding, end = BubblePadding + BubblePointer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BubbleLineGap),
    ) {
        Text(
            text = title,
            style = typography.label.copy(fontWeight = FontWeight.Bold),
            color = colors.content,
            textAlign = TextAlign.Center,
        )
        Text(
            text = text,
            style = typography.caption,
            color = colors.contentSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// The bubble grows out of the tip of its pointer, at its right in the middle
private val PointerOrigin = TransformOrigin(1f, 0.5f)

/** A rounded box with a pointer in the middle of its right side; the box leaves room for the pointer at its right. */
private object BubbleWithPointerAtRight : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val pointer = with(density) { BubblePointer.toPx() }
        val corner = with(density) { BubbleCorner.toPx() }
        val boxRight = size.width - pointer
        val middle = size.height / 2
        val box = Path().apply { addRoundRect(RoundRect(0f, 0f, boxRight, size.height, CornerRadius(corner))) }
        // The pointer starts a hair inside the box, so the two make one outline
        val tip = Path().apply {
            moveTo(boxRight - 1f, middle - pointer)
            lineTo(size.width, middle)
            lineTo(boxRight - 1f, middle + pointer)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, box, tip))
    }
}
