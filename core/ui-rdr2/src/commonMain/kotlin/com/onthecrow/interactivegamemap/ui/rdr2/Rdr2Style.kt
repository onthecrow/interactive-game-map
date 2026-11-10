package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.app_icon
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
 * The look of Red Dead Redemption 2, as the RDR2 page of the Figma file: black ink panels and strokes, white
 * type, the red outline of the selection, over the paper of the map or the night. The colors are the game's,
 * the system's ones don't apply.
 */
class Rdr2Style(override val components: GameMapComponents) : GameMapStyle {

    override val supportsDynamicColor: Boolean = false

    @Composable
    override fun colors(darkTheme: Boolean, dynamicColor: Boolean): GameMapColors = remember(darkTheme) {
        rdr2Colors(darkTheme).toGameMapColors()
    }

    @Composable
    override fun typography(colors: GameMapColors): GameMapTypography {
        val type = rememberRdr2Type()
        val rdr2Colors = rdr2Colors(colors.isDark)
        return remember(type, rdr2Colors) {
            GameMapTypography(
                headline = type.display,
                title = type.title,
                bodyLarge = type.bodyLarge,
                body = type.bodyMedium,
                caption = type.bodySmall,
                label = type.badge,
                // Places and names stand out in gold, as in the game's objectives
                emphasis = SpanStyle(color = rdr2Colors.textAccent),
                material = materialTypography(type),
            )
        }
    }

    @Composable
    override fun icons(): GameMapIcons = GameMapIcons(
        layers = Rdr2Icons.Layers,
        markers = Rdr2Icons.Index,
        settings = Rdr2Icons.Settings,
    )

    @Composable
    override fun appIcon(): Painter = painterResource(Res.drawable.app_icon)

    @Composable
    override fun Provide(colors: GameMapColors, content: @Composable () -> Unit) {
        val rdr2Colors = rdr2Colors(colors.isDark)
        CompositionLocalProvider(
            LocalRdr2Colors provides rdr2Colors,
            LocalRdr2Type provides rememberRdr2Type(),
            LocalContentColor provides rdr2Colors.textPrimary,
            content = content,
        )
    }
}

private fun rdr2Colors(darkTheme: Boolean) = if (darkTheme) Rdr2Colors.Dark else Rdr2Colors.Light

/** The colors and the type of the Red Dead Redemption 2 components, inside GameMapTheme with [Rdr2Style]. */
internal object Rdr2 {
    val colors: Rdr2Colors
        @Composable @ReadOnlyComposable get() = LocalRdr2Colors.current

    val type: Rdr2Type
        @Composable @ReadOnlyComposable get() = LocalRdr2Type.current
}
