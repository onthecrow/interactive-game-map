package com.onthecrow.interactivegamemap.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/** The icons the shared screens show, drawn in the style of the game. */
@Immutable
data class GameMapIcons(
    val layers: ImageVector,
    val markers: ImageVector,
    val settings: ImageVector,
)
