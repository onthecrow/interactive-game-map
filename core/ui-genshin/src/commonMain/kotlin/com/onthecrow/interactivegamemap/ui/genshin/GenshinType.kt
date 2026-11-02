package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.onthecrow.interactivegamemap.ui.genshin.resources.Res
import com.onthecrow.interactivegamemap.ui.genshin.resources.resource_han_rounded
import org.jetbrains.compose.resources.Font

/** The GI text styles of the Figma file: one face for everything, as in the game. */
@Immutable
data class GenshinType(
    val display: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
)

internal val LocalGenshinType = staticCompositionLocalOf<GenshinType> { error("No GenshinStyle around") }

// Only the bold face of Resource Han Rounded is in the app (tools/genshin-font), every weight maps to it so none is synthesized
private val Weights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

@Composable
internal fun rememberGenshinFontFamily(): FontFamily {
    val fonts = Weights.map { Font(Res.font.resource_han_rounded, weight = it) }
    return remember(fonts) { FontFamily(fonts) }
}

@Composable
internal fun rememberGenshinType(): GenshinType {
    val family = rememberGenshinFontFamily()
    return remember(family) {
        fun style(size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            letterSpacing = letterSpacing.em,
        )
        GenshinType(
            display = style(30, 38),
            titleLarge = style(22, 28),
            titleMedium = style(18, 24),
            bodyLarge = style(16, 22),
            bodyMedium = style(14, 20),
            bodySmall = style(12, 16),
            label = style(13, 16, letterSpacing = 0.02),
            button = style(16, 20),
        )
    }
}

/** The Material styles in the face of the game, for the Material components outside the design system. */
internal fun materialTypography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.inFace() = copy(fontFamily = family, fontSynthesis = FontSynthesis.None)
    return base.copy(
        displayLarge = base.displayLarge.inFace(),
        displayMedium = base.displayMedium.inFace(),
        displaySmall = base.displaySmall.inFace(),
        headlineLarge = base.headlineLarge.inFace(),
        headlineMedium = base.headlineMedium.inFace(),
        headlineSmall = base.headlineSmall.inFace(),
        titleLarge = base.titleLarge.inFace(),
        titleMedium = base.titleMedium.inFace(),
        titleSmall = base.titleSmall.inFace(),
        bodyLarge = base.bodyLarge.inFace(),
        bodyMedium = base.bodyMedium.inFace(),
        bodySmall = base.bodySmall.inFace(),
        labelLarge = base.labelLarge.inFace(),
        labelMedium = base.labelMedium.inFace(),
        labelSmall = base.labelSmall.inFace(),
    )
}
