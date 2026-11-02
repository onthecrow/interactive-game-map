package com.onthecrow.interactivegamemap.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

enum class ShadePosition {
    TOP, BOTTOM;
}

private data class SystemBarShadeViewState(
    val alignment: Alignment,
    val height: Dp,
    val colors: List<Color>,
)

@Composable
fun BoxScope.SystemBarShadeView(
    position: ShadePosition,
    modifier: Modifier = Modifier,
) {
    val shadeColor = GameMapTheme.colors.background.copy(alpha = 0.9f)
    val systemBarsPaddings = WindowInsets.systemBars.asPaddingValues()
    var state by remember { mutableStateOf(SystemBarShadeViewState(Alignment.TopCenter, height = 0.dp, colors = emptyList())) }
    LaunchedEffect(position, systemBarsPaddings, shadeColor) {
        snapshotFlow { systemBarsPaddings }
            .collect { currentList ->
                state = when (position) {
                    ShadePosition.TOP -> SystemBarShadeViewState(
                        alignment = Alignment.TopCenter,
                        height = currentList.calculateTopPadding(),
                        colors = listOf(shadeColor, Color.Transparent),
                    )
                    ShadePosition.BOTTOM -> SystemBarShadeViewState(
                        alignment = Alignment.BottomCenter,
                        height = currentList.calculateBottomPadding(),
                        colors = listOf(Color.Transparent, shadeColor),
                    )
                }
            }
    }

    Box(
        modifier = modifier
            .height(state.height)
            .align(state.alignment)
            .background(brush = Brush.verticalGradient(colors = state.colors)),
    )
}
