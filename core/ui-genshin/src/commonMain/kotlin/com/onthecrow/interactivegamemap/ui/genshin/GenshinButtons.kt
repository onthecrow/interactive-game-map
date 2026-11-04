package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ButtonComponent
import com.onthecrow.interactivegamemap.ui.components.ButtonKind
import com.onthecrow.interactivegamemap.ui.components.ButtonSize
import com.onthecrow.interactivegamemap.ui.components.IconButtonComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonGlyph

private const val DISABLED_ALPHA = 0.45f

/** The round mark at the start of a button: the yellow ring, the blue cross or the check. */
@Composable
private fun ButtonGlyph(kind: ButtonKind, size: Dp) {
    val colors = Genshin.colors
    Box(
        modifier = Modifier.size(size).background(colors.buttonGlyphBg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            ButtonKind.Confirm -> Canvas(modifier = Modifier.size(size)) {
                drawCircle(
                    color = colors.buttonGlyphConfirm,
                    radius = this.size.minDimension * 0.265f,
                    style = Stroke(width = this.size.minDimension * 0.09f),
                )
            }
            ButtonKind.Cancel -> Icon(
                imageVector = GenshinIcons.Cross,
                contentDescription = null,
                tint = colors.buttonGlyphCancel,
                modifier = Modifier.size(size * 0.61f),
            )
            ButtonKind.Done -> Icon(
                imageVector = GenshinIcons.Check,
                contentDescription = null,
                tint = colors.statusSuccess,
                modifier = Modifier.size(size * 0.66f),
            )
        }
    }
}

/**
 * The pill button of the game: cream with the yellow ring to confirm, slate with the blue cross to cancel,
 * slate with a check once done. A large one keeps its label in the middle.
 */
object GenshinButton : ButtonComponent {
    @Composable
    override fun Content(
        text: String,
        onClick: () -> Unit,
        kind: ButtonKind,
        size: ButtonSize,
        enabled: Boolean,
        modifier: Modifier,
    ) {
        val colors = Genshin.colors
        val type = Genshin.type
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val light = kind == ButtonKind.Confirm
        val background = when {
            light && pressed -> colors.surfaceRowTrail
            light -> colors.buttonLightBg
            pressed -> colors.buttonGlyphBg
            else -> colors.buttonDarkBg
        }
        val large = size == ButtonSize.Large
        val height = if (large) 48.dp else 36.dp
        val glyph = if (large) 36.dp else 28.dp
        val inset = (height - glyph) / 2
        Row(
            modifier = modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .then(if (enabled) Modifier.shadow(elevation = 4.dp, shape = CircleShape) else Modifier)
                .height(height)
                .then(if (large) Modifier.widthIn(min = 160.dp) else Modifier)
                .clip(CircleShape)
                .background(background)
                .then(if (pressed) Modifier.border(2.dp, colors.borderOrnament, CircleShape) else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(start = inset, end = if (large) inset else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ButtonGlyph(kind, glyph)
            Text(
                text = text,
                style = if (large) type.button else type.bodyMedium,
                color = if (light) colors.buttonLightText else colors.buttonDarkText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (large) Modifier.weight(1f).padding(horizontal = 16.dp) else Modifier.padding(start = 10.dp),
            )
            if (large) Spacer(modifier = Modifier.size(glyph))
        }
    }
}

/** A flat action of the game dialogs, "⊗ Disable  ◯ Close": a small white disc with the mark, and the label. */
object GenshinTextButton : TextButtonComponent {
    @Composable
    override fun Content(text: String, onClick: () -> Unit, glyph: TextButtonGlyph, modifier: Modifier) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = if (glyph == TextButtonGlyph.None) 12.dp else 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (glyph != TextButtonGlyph.None) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(colors.surfaceRaised, CircleShape)
                        .border(1.5.dp, colors.borderSubtle, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (glyph == TextButtonGlyph.Confirm) {
                        Canvas(modifier = Modifier.size(13.dp)) {
                            drawCircle(
                                color = colors.iconSecondary,
                                radius = size.minDimension / 2 - 1.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = GenshinIcons.Cross,
                            contentDescription = null,
                            tint = colors.iconSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(text = text, style = Genshin.type.bodyLarge, color = colors.textPrimary)
        }
    }
}

/** A small cream disc with the icon, like the links under the settings, in a touch target of 48. */
object GenshinIconButton : IconButtonComponent {
    @Composable
    override fun Content(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier) {
        val colors = Genshin.colors
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(colors.mapControlBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = colors.mapControlIcon,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
