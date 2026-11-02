package com.onthecrow.interactivegamemap.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The colors the shared screens draw with. The components of a game may use more colors of their own,
 * [material] colors the Material components which aren't part of the design system.
 */
@Immutable
data class GameMapColors(
    val isDark: Boolean,
    /** The background of the screens. */
    val background: Color,
    /** Popups, dialogs and sheets. */
    val panel: Color,
    /** The text and the icons on [background] and [panel]. */
    val content: Color,
    val contentSecondary: Color,
    /** Titles of the sections, highlighted words, values. */
    val accent: Color,
    val link: Color,
    val outline: Color,
    val scrim: Color,
    /** The buttons over the map. */
    val mapControl: Color,
    /** The badges of the merged markers on the map. */
    val cluster: Color,
    val onCluster: Color,
    val clusterBorder: Color,
    val material: ColorScheme,
)
