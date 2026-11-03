package com.onthecrow.interactivegamemap.ui.rdr2

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
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.rdr2_body
import com.onthecrow.interactivegamemap.ui.rdr2.resources.rdr2_display
import com.onthecrow.interactivegamemap.ui.rdr2.resources.rdr2_label
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

/**
 * The RDR2 text styles of the Figma file, in the three faces of tools/rdr2-font: the titles of the game's
 * frames, the small capitals of its prompts and subtitles, the soft slab serif of its lists and texts.
 */
@Immutable
data class Rdr2Type(
    /** The title in the frame, in capitals. */
    val display: TextStyle,
    /** The title of a card. */
    val title: TextStyle,
    /** Prompts, subtitles, buttons: the lowercase is drawn as small capitals. */
    val label: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    /** The numbers of the merged markers. */
    val badge: TextStyle,
)

internal val LocalRdr2Type = staticCompositionLocalOf<Rdr2Type> { error("No Rdr2Style around") }

// Every face has one weight in the app, every weight maps to it so none is synthesized
private val Weights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.Black)

@Composable
private fun rememberFamily(resource: FontResource): FontFamily {
    val fonts = Weights.map { Font(resource, weight = it) }
    return remember(fonts) { FontFamily(fonts) }
}

@Composable
internal fun rememberRdr2Type(): Rdr2Type {
    val display = rememberFamily(Res.font.rdr2_display)
    val label = rememberFamily(Res.font.rdr2_label)
    val body = rememberFamily(Res.font.rdr2_body)
    return remember(display, label, body) {
        fun style(family: FontFamily, size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            letterSpacing = letterSpacing.em,
        )
        Rdr2Type(
            display = style(display, 24, 30, letterSpacing = 0.02),
            title = style(display, 22, 28, letterSpacing = 0.01),
            label = style(label, 16, 21, letterSpacing = 0.03),
            bodyLarge = style(body, 16, 22),
            bodyMedium = style(body, 14, 19),
            bodySmall = style(body, 12, 16),
            badge = style(display, 14, 16, letterSpacing = 0.02),
        )
    }
}

/** The Material styles in the body face, for the Material components outside the design system. */
internal fun materialTypography(type: Rdr2Type): Typography {
    val base = Typography()
    val family = type.bodyMedium.fontFamily
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
