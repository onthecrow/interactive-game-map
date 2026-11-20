package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.onthecrow.interactivegamemap.ui.genshin.resources.Res
import com.onthecrow.interactivegamemap.ui.genshin.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import com.onthecrow.interactivegamemap.ui.components.GameMapComponents
import com.onthecrow.interactivegamemap.ui.theme.GameMapColors
import com.onthecrow.interactivegamemap.ui.theme.GameMapIcons
import com.onthecrow.interactivegamemap.ui.theme.GameMapStyle
import com.onthecrow.interactivegamemap.ui.theme.GameMapTypography

/**
 * The look of Genshin Impact, as the Genshin Impact page of the Figma file: cream panels, slate text,
 * gold ornaments and the game's own face. The colors are the game's, the system's ones don't apply.
 */
class GenshinStyle(override val components: GameMapComponents) : GameMapStyle {

    override val supportsDynamicColor: Boolean = false

    @Composable
    override fun colors(darkTheme: Boolean, dynamicColor: Boolean): GameMapColors = remember(darkTheme) {
        genshinColors(darkTheme).toGameMapColors()
    }

    @Composable
    override fun typography(colors: GameMapColors): GameMapTypography {
        val type = rememberGenshinType()
        val family = rememberGenshinFontFamily()
        return remember(type, family, colors.accent) {
            GameMapTypography(
                headline = type.titleLarge,
                title = type.titleMedium,
                bodyLarge = type.bodyLarge,
                body = type.bodyMedium,
                caption = type.bodySmall,
                label = type.label,
                // The face has one weight, the words stand out in gold as in the game texts
                emphasis = SpanStyle(color = colors.accent),
                material = materialTypography(family),
            )
        }
    }

    @Composable
    override fun icons(): GameMapIcons = GameMapIcons(
        layers = GenshinIcons.Layers,
        markers = GenshinIcons.List,
        settings = GenshinIcons.Settings,
    )

    @Composable
    override fun appIcon(): Painter = painterResource(Res.drawable.app_icon)

    @Composable
    override fun Provide(colors: GameMapColors, content: @Composable () -> Unit) {
        val genshinColors = genshinColors(colors.isDark)
        CompositionLocalProvider(
            LocalGenshinColors provides genshinColors,
            LocalGenshinType provides rememberGenshinType(),
            LocalContentColor provides genshinColors.textPrimary,
            content = content,
        )
    }
}

private fun genshinColors(darkTheme: Boolean) = if (darkTheme) GenshinColors.Dark else GenshinColors.Light

/** The colors and the type of the Genshin Impact components, inside GameMapTheme with [GenshinStyle]. */
internal object Genshin {
    val colors: GenshinColors
        @Composable @ReadOnlyComposable get() = LocalGenshinColors.current

    val type: GenshinType
        @Composable @ReadOnlyComposable get() = LocalGenshinType.current
}
