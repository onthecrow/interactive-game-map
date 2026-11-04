package com.onthecrow.interactivegamemap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

/** The text styles the shared screens use, [material] styles the Material components. */
@Immutable
data class GameMapTypography(
    /** The title of a screen. */
    val headline: TextStyle,
    /** The title of a popup or of a group. */
    val title: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    /** Short labels on badges, like the number of merged markers. */
    val label: TextStyle,
    /** How the **bold** words of the descriptions stand out. */
    val emphasis: SpanStyle,
    val material: Typography,
)
