package com.onthecrow.interactivegamemap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import com.onthecrow.interactivegamemap.ui.components.GameMapComponents
import org.koin.compose.koinInject

private val LocalGameMapStyle = staticCompositionLocalOf<GameMapStyle> { error("No GameMapTheme around") }
private val LocalGameMapColors = staticCompositionLocalOf<GameMapColors> { error("No GameMapTheme around") }
private val LocalGameMapTypography = staticCompositionLocalOf<GameMapTypography> { error("No GameMapTheme around") }
private val LocalGameMapIcons = staticCompositionLocalOf<GameMapIcons> { error("No GameMapTheme around") }

/**
 * Applies the style of the game, the one the app registered in Koin, to [content]: the colors, the type,
 * the icons and the components of [GameMapTheme], and the Material theme for the Material components.
 *
 * @param dynamicColor whether to follow the system's colors, where the style and the system support them
 */
@Composable
fun GameMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    GameMapTheme(
        style = koinInject(),
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content,
    )
}

/** Applies [style] to [content], for the previews and the tests which have no Koin. */
@Composable
fun GameMapTheme(
    style: GameMapStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = style.colors(darkTheme = darkTheme, dynamicColor = dynamicColor && style.supportsDynamicColor)
    val typography = style.typography(colors)
    val icons = style.icons()
    CompositionLocalProvider(
        LocalGameMapStyle provides style,
        LocalGameMapColors provides colors,
        LocalGameMapTypography provides typography,
        LocalGameMapIcons provides icons,
    ) {
        MaterialTheme(colorScheme = colors.material, typography = typography.material) {
            style.Provide(colors, content)
        }
    }
}

/** The style of the game inside [GameMapTheme]. */
object GameMapTheme {

    val colors: GameMapColors
        @Composable @ReadOnlyComposable get() = LocalGameMapColors.current

    val typography: GameMapTypography
        @Composable @ReadOnlyComposable get() = LocalGameMapTypography.current

    val icons: GameMapIcons
        @Composable @ReadOnlyComposable get() = LocalGameMapIcons.current

    val components: GameMapComponents
        @Composable @ReadOnlyComposable get() = LocalGameMapStyle.current.components

    /** The icon of the app, see [GameMapStyle.appIcon]. */
    @Composable
    fun appIcon(): Painter = LocalGameMapStyle.current.appIcon()

    /** Whether the style can follow the system's colors at all, the setting is pointless otherwise. */
    val supportsDynamicColor: Boolean
        @Composable @ReadOnlyComposable get() = LocalGameMapStyle.current.supportsDynamicColor
}
