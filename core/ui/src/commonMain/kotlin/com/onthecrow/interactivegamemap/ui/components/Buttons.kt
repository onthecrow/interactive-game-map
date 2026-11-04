package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

enum class ButtonKind {
    /** The main action, like marking a location as found. */
    Confirm,

    /** Leaves without acting, like Cancel of a dialog. */
    Cancel,

    /** An action already done, like a location already found. */
    Done,
}

enum class ButtonSize { Large, Small }

/** A filled button with a label. */
interface ButtonComponent {
    @Composable
    fun Content(
        text: String,
        onClick: () -> Unit,
        kind: ButtonKind,
        size: ButtonSize,
        enabled: Boolean,
        modifier: Modifier,
    )
}

enum class TextButtonGlyph { None, Confirm, Cancel }

/** A flat action: Exit of a dungeon, Show all and Hide all of the markers. */
interface TextButtonComponent {
    @Composable
    fun Content(text: String, onClick: () -> Unit, glyph: TextButtonGlyph, modifier: Modifier)
}

/** A button with an icon only, like the links under the settings. */
interface IconButtonComponent {
    @Composable
    fun Content(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier)
}

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Confirm,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
) {
    GameMapTheme.components.button.Content(text, onClick, kind, size, enabled, modifier)
}

@Composable
fun GameTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: TextButtonGlyph = TextButtonGlyph.None,
) {
    GameMapTheme.components.textButton.Content(text, onClick, glyph, modifier)
}

@Composable
fun GameIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameMapTheme.components.iconButton.Content(icon, contentDescription, onClick, modifier)
}
