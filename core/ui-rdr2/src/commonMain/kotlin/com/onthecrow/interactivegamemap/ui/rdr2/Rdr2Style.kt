package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.onthecrow.interactivegamemap.ui.components.GameMapComponents
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_filter_list
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_layers
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_settings
import com.onthecrow.interactivegamemap.ui.theme.GameMapColors
import com.onthecrow.interactivegamemap.ui.theme.GameMapIcons
import com.onthecrow.interactivegamemap.ui.theme.GameMapStyle
import com.onthecrow.interactivegamemap.ui.theme.GameMapTypography
import org.jetbrains.compose.resources.vectorResource

/** The Material colors of the app, the system's ones on Android 12 and later when [dynamicColor]. */
@Composable
internal expect fun rdr2ColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

private const val SCRIM_ALPHA = 0.32f

/** The look of Red Dead Redemption 2: Material components in the Material colors. */
class Rdr2Style(override val components: GameMapComponents) : GameMapStyle {

    override val supportsDynamicColor: Boolean = true

    @Composable
    override fun colors(darkTheme: Boolean, dynamicColor: Boolean): GameMapColors {
        val scheme = rdr2ColorScheme(darkTheme, dynamicColor)
        return remember(scheme, darkTheme) {
            GameMapColors(
                isDark = darkTheme,
                background = scheme.background,
                panel = scheme.surface,
                content = scheme.onSurface,
                contentSecondary = scheme.onSurfaceVariant,
                accent = scheme.primary,
                link = scheme.primary,
                outline = scheme.outlineVariant,
                scrim = scheme.scrim.copy(alpha = SCRIM_ALPHA),
                mapControl = scheme.secondaryContainer,
                cluster = scheme.primary,
                onCluster = scheme.onPrimary,
                clusterBorder = scheme.onPrimary,
                material = scheme,
            )
        }
    }

    @Composable
    override fun typography(colors: GameMapColors): GameMapTypography = remember {
        val material = Typography()
        GameMapTypography(
            headline = material.headlineMedium,
            title = material.titleMedium,
            bodyLarge = material.bodyLarge,
            body = material.bodyMedium,
            caption = material.bodySmall,
            label = material.labelLarge.copy(fontWeight = FontWeight.Bold),
            emphasis = SpanStyle(fontWeight = FontWeight.Bold),
            material = material,
        )
    }

    @Composable
    override fun icons(): GameMapIcons = GameMapIcons(
        layers = vectorResource(Res.drawable.ic_layers),
        markers = vectorResource(Res.drawable.ic_filter_list),
        settings = vectorResource(Res.drawable.ic_settings),
    )

    @Composable
    override fun Provide(colors: GameMapColors, content: @Composable () -> Unit) {
        content()
    }
}
