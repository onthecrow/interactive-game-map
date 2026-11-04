package com.onthecrow.interactivegamemap.ui.theme

import androidx.compose.runtime.Composable
import com.onthecrow.interactivegamemap.ui.components.GameMapComponents

/**
 * The look of a game: its colors, type, icons and components. The app registers its style in Koin, and
 * [GameMapTheme] applies it to everything inside.
 */
interface GameMapStyle {

    /** Whether the colors can follow the system's ones, Android 12 and later have them. */
    val supportsDynamicColor: Boolean

    val components: GameMapComponents

    @Composable
    fun colors(darkTheme: Boolean, dynamicColor: Boolean): GameMapColors

    @Composable
    fun typography(colors: GameMapColors): GameMapTypography

    @Composable
    fun icons(): GameMapIcons

    /** Provides what the game's own components read, like its extra colors, around [content]. */
    @Composable
    fun Provide(colors: GameMapColors, content: @Composable () -> Unit)
}
