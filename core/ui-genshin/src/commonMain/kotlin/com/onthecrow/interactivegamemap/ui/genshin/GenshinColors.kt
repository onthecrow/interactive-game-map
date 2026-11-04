package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.ui.theme.GameMapColors

// The palette of the game UI, as GI / Palette of the Figma file
private val White = Color(0xFFFFFFFF)
private val Paper100 = Color(0xFFF7F4EC)
private val Paper200 = Color(0xFFECE5D8)
private val Paper300 = Color(0xFFE3DBCB)
private val Paper400 = Color(0xFFD8CDB8)
private val Paper500 = Color(0xFFC8B99D)
private val Gold200 = Color(0xFFE7D3A3)
private val Gold300 = Color(0xFFD3BC8E)
private val Gold400 = Color(0xFFBC9E66)
private val Gold500 = Color(0xFF9B7D4A)
private val Gold700 = Color(0xFF5E5139)
private val Gold800 = Color(0xFF4D4436)
private val Amber400 = Color(0xFFF4C443)
private val Slate200 = Color(0xFFA7AEBC)
private val Slate300 = Color(0xFF7E8698)
private val Slate500 = Color(0xFF4A5366)
private val Slate600 = Color(0xFF3B4255)
private val Slate700 = Color(0xFF313747)
private val Slate800 = Color(0xFF262B38)
private val Slate900 = Color(0xFF1C202A)
private val Night = Color(0xFF0E1016)
private val Azure400 = Color(0xFF58B4E8)
private val Red400 = Color(0xFFE5614B)
private val Jade400 = Color(0xFF6FAF8A)

/** The colors of the Genshin Impact components, as GI / Color and GI / Color · Dark of the Figma file. */
@Immutable
data class GenshinColors(
    val isDark: Boolean,
    val bgScreen: Color,
    val bgScrim: Color,
    val surfacePanel: Color,
    val surfaceRaised: Color,
    val surfaceRow: Color,
    val surfaceRowTrail: Color,
    val surfaceRowActive: Color,
    val surfaceMenu: Color,
    val surfaceMenuActive: Color,
    val inputBgActive: Color,
    val inputBgIdle: Color,
    val inputText: Color,
    val inputPlaceholder: Color,
    val inputBgMap: Color,
    val inputBorderMap: Color,
    val inputPlaceholderMap: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textAccent: Color,
    val textLink: Color,
    val textOnDark: Color,
    val textDanger: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconAccent: Color,
    val iconOnDark: Color,
    val borderSubtle: Color,
    val borderOrnament: Color,
    val borderHighlight: Color,
    val ornamentStar: Color,
    val ornamentStarAccent: Color,
    val controlOn: Color,
    val controlOff: Color,
    val controlKnob: Color,
    val controlMarkOn: Color,
    val controlMarkOff: Color,
    val buttonLightBg: Color,
    val buttonLightText: Color,
    val buttonDarkBg: Color,
    val buttonDarkText: Color,
    val buttonGlyphBg: Color,
    val buttonGlyphConfirm: Color,
    val buttonGlyphCancel: Color,
    val mapControlBg: Color,
    val mapControlIcon: Color,
    val mapControlActiveBg: Color,
    val mapControlActiveIcon: Color,
    val statusSuccess: Color,
) {
    companion object {
        val Light = GenshinColors(
            isDark = false,
            bgScreen = Paper200,
            bgScrim = Slate900.copy(alpha = 0.55f),
            surfacePanel = Paper200,
            surfaceRaised = Paper100,
            surfaceRow = Paper300,
            surfaceRowTrail = Paper400,
            surfaceRowActive = Paper100,
            surfaceMenu = Slate600,
            surfaceMenuActive = Slate500,
            inputBgActive = White,
            inputBgIdle = Paper300,
            inputText = Slate500,
            inputPlaceholder = Slate200,
            inputBgMap = Slate900.copy(alpha = 0.45f),
            inputBorderMap = Paper200.copy(alpha = 0.45f),
            inputPlaceholderMap = Paper200,
            textPrimary = Slate500,
            textSecondary = Slate300,
            textAccent = Gold500,
            textLink = Gold500,
            textOnDark = Paper200,
            textDanger = Red400,
            iconPrimary = Slate500,
            iconSecondary = Slate300,
            iconAccent = Gold400,
            iconOnDark = Paper200,
            borderSubtle = Paper500,
            borderOrnament = Gold300,
            borderHighlight = White,
            ornamentStar = Paper400,
            ornamentStarAccent = Gold300,
            controlOn = Gold300,
            controlOff = Slate600,
            controlKnob = Paper100,
            controlMarkOn = Slate500,
            controlMarkOff = Gold400,
            buttonLightBg = Paper200,
            buttonLightText = Slate500,
            buttonDarkBg = Slate500,
            buttonDarkText = Paper200,
            buttonGlyphBg = Slate600,
            buttonGlyphConfirm = Amber400,
            buttonGlyphCancel = Azure400,
            mapControlBg = Paper200,
            mapControlIcon = Slate500,
            mapControlActiveBg = Slate500,
            mapControlActiveIcon = Gold200,
            statusSuccess = Jade400,
        )

        // The night menus of the game: slate panels a step lighter than the screen, cream text, gold rules
        val Dark = GenshinColors(
            isDark = true,
            bgScreen = Slate800,
            bgScrim = Night.copy(alpha = 0.65f),
            surfacePanel = Slate700,
            surfaceRaised = Slate500,
            surfaceRow = Slate600,
            surfaceRowTrail = Slate700,
            surfaceRowActive = Slate500,
            surfaceMenu = Slate900,
            surfaceMenuActive = Slate600,
            inputBgActive = Slate500,
            inputBgIdle = Slate800,
            inputText = Paper200,
            inputPlaceholder = Slate200,
            inputBgMap = Night.copy(alpha = 0.5f),
            inputBorderMap = Paper200.copy(alpha = 0.3f),
            inputPlaceholderMap = Paper200,
            textPrimary = Paper200,
            textSecondary = Slate200,
            textAccent = Gold300,
            textLink = Gold200,
            textOnDark = Paper200,
            textDanger = Red400,
            iconPrimary = Paper200,
            iconSecondary = Slate200,
            iconAccent = Gold300,
            iconOnDark = Paper200,
            borderSubtle = Slate500,
            borderOrnament = Gold400,
            borderHighlight = Gold200,
            ornamentStar = Gold800,
            ornamentStarAccent = Gold700,
            controlOn = Gold300,
            controlOff = Slate900,
            controlKnob = Paper100,
            controlMarkOn = Slate600,
            controlMarkOff = Gold400,
            buttonLightBg = Paper200,
            buttonLightText = Slate600,
            buttonDarkBg = Slate500,
            buttonDarkText = Paper200,
            buttonGlyphBg = Slate700,
            buttonGlyphConfirm = Amber400,
            buttonGlyphCancel = Azure400,
            mapControlBg = Slate600,
            mapControlIcon = Paper200,
            mapControlActiveBg = Gold300,
            mapControlActiveIcon = Slate700,
            statusSuccess = Jade400,
        )
    }
}

internal val LocalGenshinColors = staticCompositionLocalOf { GenshinColors.Light }

/** For the Material components which aren't part of the design system: sheets, ripples, text selection. */
private fun GenshinColors.materialScheme(): ColorScheme {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = textAccent,
        onPrimary = buttonLightBg,
        primaryContainer = controlOn,
        onPrimaryContainer = buttonLightText,
        secondary = buttonDarkBg,
        onSecondary = buttonDarkText,
        secondaryContainer = mapControlBg,
        onSecondaryContainer = mapControlIcon,
        background = bgScreen,
        onBackground = textPrimary,
        surface = surfacePanel,
        onSurface = textPrimary,
        surfaceVariant = surfaceRow,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surfaceRaised,
        surfaceContainerLow = surfacePanel,
        surfaceContainer = surfacePanel,
        surfaceContainerHigh = surfaceRow,
        surfaceContainerHighest = surfaceRowTrail,
        outline = borderSubtle,
        outlineVariant = borderSubtle,
        error = textDanger,
        scrim = bgScrim,
    )
}

internal fun GenshinColors.toGameMapColors(): GameMapColors = GameMapColors(
    isDark = isDark,
    background = bgScreen,
    panel = surfacePanel,
    content = textPrimary,
    contentSecondary = textSecondary,
    accent = textAccent,
    link = textLink,
    outline = borderSubtle,
    scrim = bgScrim,
    mapControl = mapControlBg,
    cluster = buttonGlyphBg,
    onCluster = textOnDark,
    clusterBorder = borderOrnament,
    material = materialScheme(),
)
