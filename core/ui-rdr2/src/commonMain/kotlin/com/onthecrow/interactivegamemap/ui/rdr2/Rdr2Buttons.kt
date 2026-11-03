package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ButtonComponent
import com.onthecrow.interactivegamemap.ui.components.ButtonKind
import com.onthecrow.interactivegamemap.ui.components.ButtonSize
import com.onthecrow.interactivegamemap.ui.components.IconButtonComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonGlyph

private const val DISABLED_ALPHA = 0.45f

/**
 * A prompt of the game on a stroke of ink: the label in small capitals and the round glyph after it, the red
 * outline while pressed. Once done, the glyph is the checklist's box with its red tick.
 */
object Rdr2Button : ButtonComponent {
    @Composable
    override fun Content(
        text: String,
        onClick: () -> Unit,
        kind: ButtonKind,
        size: ButtonSize,
        enabled: Boolean,
        modifier: Modifier,
    ) {
        val colors = Rdr2.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val large = size == ButtonSize.Large
        Row(
            modifier = modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .heightIn(min = if (large) 52.dp else 44.dp)
                .then(if (large) Modifier.widthIn(min = 160.dp) else Modifier)
                .brushStroke(if (pressed) colors.strokeRowPressed else colors.strokeRow, salt = 3)
                .selectionOutline(pressed, colors.selection, salt = 3)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(start = if (large) 20.dp else 18.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = Rdr2.type.label,
                color = colors.textPrimary,
                textAlign = if (large) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (large) Modifier.weight(1f) else Modifier,
            )
            when (kind) {
                ButtonKind.Confirm -> PromptGlyphView(PromptGlyph.Confirm)
                ButtonKind.Cancel -> PromptGlyphView(PromptGlyph.Back)
                ButtonKind.Done -> Checkbox(checked = true)
            }
        }
    }
}

/** A bare prompt of the game, SHOW ALL ⓥ: the label and the round glyph, the red outline while pressed. */
object Rdr2TextButton : TextButtonComponent {
    @Composable
    override fun Content(text: String, onClick: () -> Unit, glyph: TextButtonGlyph, modifier: Modifier) {
        val colors = Rdr2.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Row(
            modifier = modifier
                .heightIn(min = 44.dp)
                .selectionOutline(pressed, colors.selection, salt = 5)
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, style = Rdr2.type.label, color = colors.textPrimary)
            when (glyph) {
                TextButtonGlyph.None -> Unit
                TextButtonGlyph.Confirm -> PromptGlyphView(PromptGlyph.Confirm)
                TextButtonGlyph.Cancel -> PromptGlyphView(PromptGlyph.Cancel)
            }
        }
    }
}

/** A black disc with a white glyph, as the cores of the game's HUD, in a touch target of 48. */
object Rdr2IconButton : IconButtonComponent {
    @Composable
    override fun Content(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier) {
        val colors = Rdr2.colors
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            ControlDisc(size = 40.dp, active = false) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = colors.iconPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
