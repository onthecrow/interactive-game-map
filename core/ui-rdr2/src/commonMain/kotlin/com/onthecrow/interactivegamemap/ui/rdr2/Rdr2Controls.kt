package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ChipComponent
import com.onthecrow.interactivegamemap.ui.components.MapButtonComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldPlacement
import com.onthecrow.interactivegamemap.ui.components.SwitchComponent
import com.onthecrow.interactivegamemap.ui.components.ZoomControlComponent

private const val DISABLED_ALPHA = 0.45f
private const val CONTROL_ALPHA = 0.9f
private val ControlSize = 48.dp

/**
 * A disc of black ink in a faint rim, as the cores of the game's HUD; the active one is ringed with the red of
 * the selection.
 */
@Composable
internal fun ControlDisc(size: Dp, active: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = Rdr2.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.inkControl.copy(alpha = CONTROL_ALPHA), CircleShape)
            .border(1.5.dp, colors.controlRing, CircleShape)
            .drawWithCache {
                val ring = if (active) brushRing(this.size.width / density.toDouble(), seed = 41, scale = density) else null
                onDrawWithContent {
                    drawContent()
                    ring?.let { drawPath(it, colors.selection) }
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** The diameter of the discs over the map, inside the touch target of [ControlSize]. */
private val MapDiscSize = 40.dp

/**
 * A button over the map as the game's prompts: a worn white disc with a dark glyph and a dark shadow around it,
 * the grey ring while active, a greyer disc while pressed.
 */
object Rdr2MapButton : MapButtonComponent {
    @Composable
    override fun Content(
        onClick: () -> Unit,
        active: Boolean,
        modifier: Modifier,
        content: @Composable (tint: Color) -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        // Each button on the screen wears its own way
        val salt = currentCompositeKeyHash
        Box(
            modifier = modifier
                .size(ControlSize)
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            PromptDisc(size = MapDiscSize, ring = active, pressed = pressed, salt = salt) {
                content(Rdr2.colors.promptGlyph)
            }
        }
    }
}

/** Plus and minus on two discs, one over the other; the one at the end of the zoom range fades. */
object Rdr2ZoomControl : ZoomControlComponent {
    @Composable
    override fun Content(
        canZoomIn: Boolean,
        canZoomOut: Boolean,
        onZoomInClick: () -> Unit,
        onZoomOutClick: () -> Unit,
        zoomInDescription: String,
        zoomOutDescription: String,
        modifier: Modifier,
    ) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ZoomButton(Rdr2Icons.Add, zoomInDescription, canZoomIn, onZoomInClick, salt = 1)
            ZoomButton(Rdr2Icons.Remove, zoomOutDescription, canZoomOut, onZoomOutClick, salt = 2)
        }
    }

    @Composable
    private fun ZoomButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit, salt: Int) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(ControlSize)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            PromptDisc(size = MapDiscSize, pressed = pressed, salt = salt) {
                Icon(imageVector = icon, contentDescription = null, tint = Rdr2.colors.promptGlyph, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * A stroke of black ink with the text on it, the red outline while typing. Over the map it's the panel's ink,
 * on a panel it's a row's lighter stroke.
 */
object Rdr2SearchField : SearchFieldComponent {
    @Composable
    override fun Content(
        value: String,
        onValueChange: (String) -> Unit,
        hint: String,
        onClearClick: () -> Unit,
        placement: SearchFieldPlacement,
        modifier: Modifier,
    ) {
        val colors = Rdr2.colors
        val type = Rdr2.type
        val interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()
        val onMap = placement == SearchFieldPlacement.Map
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = type.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.selection),
            modifier = modifier
                .height(ControlSize)
                .then(
                    if (onMap) {
                        Modifier.brushStroke(colors.inkControl, salt = 11, underAlpha = 0.6f, overAlpha = 0.86f)
                    } else {
                        Modifier.brushStroke(colors.strokeRow, salt = 11)
                    },
                )
                .selectionOutline(focused, colors.selection, salt = 11),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Rdr2Icons.Search,
                        contentDescription = null,
                        tint = if (value.isEmpty()) colors.iconSecondary else colors.iconPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                style = type.bodyLarge,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClearClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            PromptGlyphView(PromptGlyph.Cancel)
                        }
                    }
                }
            },
        )
    }
}

/** The box of the game's checklist: the painted red tick when on. */
object Rdr2Switch : SwitchComponent {
    @Composable
    override fun Content(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier) {
        val toggle = onCheckedChange?.let {
            Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = it)
        } ?: Modifier
        Checkbox(checked = checked, modifier = modifier.then(toggle))
    }
}

/** A floor of a dungeon on a short stroke, the picked one outlined in red; the diamond marks the location's floor. */
object Rdr2Chip : ChipComponent {
    @Composable
    override fun Content(text: String, selected: Boolean, marked: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val colors = Rdr2.colors
        Row(
            modifier = modifier
                .heightIn(min = 36.dp)
                .brushStroke(colors.strokeRow, salt = 13)
                .selectionOutline(selected, colors.selection, salt = 13)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (marked) {
                Icon(
                    imageVector = Rdr2Icons.Diamond,
                    contentDescription = null,
                    tint = colors.textAccent,
                    modifier = Modifier.size(9.dp),
                )
            }
            Text(
                text = text,
                style = Rdr2.type.bodyMedium,
                color = if (selected) colors.textPrimary else colors.textSecondary,
            )
        }
    }
}
