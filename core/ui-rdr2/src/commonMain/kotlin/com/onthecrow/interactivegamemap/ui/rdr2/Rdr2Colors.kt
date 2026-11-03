package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.ui.theme.GameMapColors

// The palette of the game menus and of its map, as RDR2 / Palette of the Figma file
private val Ink950 = Color(0xFF070707)
private val Ink900 = Color(0xFF0C0C0B)
private val Ink800 = Color(0xFF151412)
private val Ink700 = Color(0xFF1F1D1A)
private val Ink600 = Color(0xFF2E2B27)
private val Ink500 = Color(0xFF3D3934)
private val Ash400 = Color(0xFF6F6A62)
private val Ash300 = Color(0xFF9C968C)
private val Ash200 = Color(0xFFC4BFB5)
private val Bone100 = Color(0xFFF1EEE7)
private val Parchment300 = Color(0xFFDEC29C)
private val Sepia800 = Color(0xFF3A2E22)
private val Red600 = Color(0xFFB5161B)
private val Red400 = Color(0xFFD9443F)
private val Gold400 = Color(0xFFD9B15A)

/**
 * The colors of the Red Dead Redemption 2 components, as RDR2 / Color and RDR2 / Color · Dark of the Figma file.
 * The panels and the strokes are the game's black ink in both themes, the themes differ in what lies under
 * them: the paper of the map or the night.
 */
@Immutable
data class Rdr2Colors(
    val isDark: Boolean,
    /** Under the panels of a screen: the paper of the map, or the night. */
    val bgScreen: Color,
    /** The faint grain and the darker edges of [bgScreen]. */
    val bgScreenGrain: Color,
    val bgScrim: Color,
    /** The ink of the panels, the sheets and the dialogs. */
    val inkPanel: Color,
    /** The wider, fainter stroke under a panel: more ink on the paper, a grey brush in the night. */
    val inkRim: Color,
    /** The rim of a dialog, which lies on a panel of the same ink. */
    val inkRimRaised: Color,
    /** The ink of the controls over the map. */
    val inkControl: Color,
    /** The strokes of the rows on a panel. */
    val strokeRow: Color,
    val strokeRowPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    /** The highlighted words of the descriptions, as the game highlights places and names. */
    val textAccent: Color,
    val textLink: Color,
    /** The text on [bgScreen] outside the panels. */
    val textOnScreen: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    /** The rules under the titles and between the sections. */
    val rule: Color,
    /** The frame of the titles. */
    val frame: Color,
    /** The red outline of what's selected, and the checks. */
    val selection: Color,
    val checkboxBorder: Color,
    val checkboxFill: Color,
    val controlRing: Color,
    /** The white discs of the prompts and of the buttons over the map, and their worn glyphs. */
    val promptDisc: Color,
    val promptDiscPressed: Color,
    val promptGlyph: Color,
    /** The stains of the discs. */
    val promptMottle: Color,
    /** The dark shadow around a disc over the map. */
    val promptHalo: Color,
    /** The grey ring of an active disc, as the game rings the prompt being held. */
    val promptRing: Color,
) {
    companion object {
        // The map of the game: black ink on its paper
        val Light = Rdr2Colors(
            isDark = false,
            bgScreen = Parchment300,
            bgScreenGrain = Sepia800,
            bgScrim = Ink950.copy(alpha = 0.55f),
            inkPanel = Ink900,
            inkRim = Ink900,
            inkRimRaised = Ink500,
            inkControl = Ink900,
            strokeRow = Ink600,
            strokeRowPressed = Ink500,
            textPrimary = Bone100,
            textSecondary = Ash300,
            textDisabled = Ash400,
            textAccent = Gold400,
            textLink = Gold400,
            textOnScreen = Sepia800,
            iconPrimary = Bone100,
            iconSecondary = Ash300,
            rule = Ash200.copy(alpha = 0.55f),
            frame = Bone100,
            selection = Red600,
            checkboxBorder = Ash300,
            checkboxFill = Ink700,
            controlRing = Bone100.copy(alpha = 0.35f),
            promptDisc = Bone100,
            promptDiscPressed = Ash200,
            promptGlyph = Ink900,
            promptMottle = Ash300,
            promptHalo = Ink950,
            promptRing = Ash300.copy(alpha = 0.6f),
        )

        // The pause menus of the game: the same ink over the night
        val Dark = Light.copy(
            isDark = true,
            bgScreen = Ink800,
            bgScreenGrain = Ink950,
            bgScrim = Ink950.copy(alpha = 0.7f),
            inkRim = Ink500,
            textOnScreen = Ash300,
        )
    }
}

internal val LocalRdr2Colors = staticCompositionLocalOf { Rdr2Colors.Light }

/** For the Material components which aren't part of the design system: sheets, ripples, text selection. */
private fun Rdr2Colors.materialScheme(): ColorScheme {
    // Everything sits on the black ink, so the Material components are dark in both themes
    return darkColorScheme().copy(
        primary = selection,
        onPrimary = textPrimary,
        primaryContainer = strokeRow,
        onPrimaryContainer = textPrimary,
        secondary = textSecondary,
        onSecondary = inkPanel,
        secondaryContainer = inkControl,
        onSecondaryContainer = iconPrimary,
        background = bgScreen,
        onBackground = if (isDark) textPrimary else textOnScreen,
        surface = inkPanel,
        onSurface = textPrimary,
        surfaceVariant = strokeRow,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = inkPanel,
        surfaceContainerLow = inkPanel,
        surfaceContainer = inkPanel,
        surfaceContainerHigh = strokeRow,
        surfaceContainerHighest = strokeRowPressed,
        outline = textSecondary,
        outlineVariant = textDisabled,
        error = Red400,
        scrim = bgScrim,
    )
}

internal fun Rdr2Colors.toGameMapColors(): GameMapColors = GameMapColors(
    isDark = isDark,
    background = bgScreen,
    panel = inkPanel,
    // The shared screens put their text on the panels only
    content = textPrimary,
    contentSecondary = textSecondary,
    // The pin of the selected location is tinted with it, as the selection everywhere else
    accent = Red400,
    link = textLink,
    outline = rule,
    scrim = bgScrim,
    mapControl = inkControl,
    cluster = inkControl,
    onCluster = textPrimary,
    clusterBorder = Ash200,
    material = materialScheme(),
)
