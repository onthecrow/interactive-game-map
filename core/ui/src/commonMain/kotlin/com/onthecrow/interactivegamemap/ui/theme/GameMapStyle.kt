package com.onthecrow.interactivegamemap.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
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

    /**
     * The icon of the app as the splash screen shows it: the screens shown while the data loads put it in the same
     * place, so the splash seems to go on.
     */
    @Composable
    fun appIcon(): Painter

    /** Provides what the game's own components read, like its extra colors, around [content]. */
    @Composable
    fun Provide(colors: GameMapColors, content: @Composable () -> Unit)
}
