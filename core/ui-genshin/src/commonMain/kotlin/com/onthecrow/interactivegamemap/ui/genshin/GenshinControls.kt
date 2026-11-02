package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ChipComponent
import com.onthecrow.interactivegamemap.ui.components.MapButtonComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldPlacement
import com.onthecrow.interactivegamemap.ui.components.SwitchComponent
import com.onthecrow.interactivegamemap.ui.components.ZoomControlComponent

private const val DISABLED_ALPHA = 0.38f
private val ControlSize = 48.dp

/** A round cream button over the map; the active one is slate with a gold ring. */
object GenshinMapButton : MapButtonComponent {
    @Composable
    override fun Content(
        onClick: () -> Unit,
        active: Boolean,
        modifier: Modifier,
        content: @Composable (tint: Color) -> Unit,
    ) {
        val colors = Genshin.colors
        Box(
            modifier = modifier
                .size(ControlSize)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (active) colors.mapControlActiveBg else colors.mapControlBg)
                .then(if (active) Modifier.border(1.5.dp, colors.borderOrnament, CircleShape) else Modifier)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content(if (active) colors.mapControlActiveIcon else colors.mapControlIcon)
        }
    }
}

/** Plus and minus in one cream pill. */
object GenshinZoomControl : ZoomControlComponent {
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
        val colors = Genshin.colors
        val shape = RoundedCornerShape(ControlSize / 2)
        Column(
            modifier = modifier
                .width(ControlSize)
                .shadow(elevation = 4.dp, shape = shape)
                .clip(shape)
                .background(colors.mapControlBg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ZoomButton(GenshinIcons.Add, zoomInDescription, canZoomIn, onZoomInClick)
            Box(modifier = Modifier.size(width = 24.dp, height = 1.dp).background(colors.borderSubtle))
            ZoomButton(GenshinIcons.Remove, zoomOutDescription, canZoomOut, onZoomOutClick)
        }
    }

    @Composable
    private fun ZoomButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
        val tint = Genshin.colors.mapControlIcon
        Box(
            modifier = Modifier
                .size(ControlSize)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA),
            )
        }
    }
}

private class FieldLook(
    val background: Color,
    val border: Color?,
    val text: Color,
    val hint: Color,
    val icon: Color,
    val raised: Boolean,
)

private enum class FieldState { Idle, Active, Filled }

private fun GenshinColors.fieldLook(onMap: Boolean, state: FieldState): FieldLook = when (state) {
    FieldState.Idle -> if (onMap) {
        FieldLook(inputBgMap, inputBorderMap, inputText, inputPlaceholderMap, iconOnDark, raised = false)
    } else {
        FieldLook(inputBgIdle, borderSubtle, inputText, textSecondary, iconSecondary, raised = false)
    }
    FieldState.Active -> FieldLook(
        background = inputBgActive,
        border = if (onMap) null else borderOrnament,
        text = inputText,
        hint = inputPlaceholder,
        icon = iconSecondary,
        raised = onMap,
    )
    FieldState.Filled -> FieldLook(
        background = if (onMap) mapControlBg else surfaceRaised,
        border = if (onMap) null else borderSubtle,
        text = inputText,
        hint = inputPlaceholder,
        icon = iconPrimary,
        raised = onMap,
    )
}

/**
 * The text field of the game chat: translucent over the map while idle, white while typing, cream with a
 * query; on a panel it's a recessed pill.
 */
object GenshinSearchField : SearchFieldComponent {
    @Composable
    override fun Content(
        value: String,
        onValueChange: (String) -> Unit,
        hint: String,
        onClearClick: () -> Unit,
        placement: SearchFieldPlacement,
        modifier: Modifier,
    ) {
        val colors = Genshin.colors
        val type = Genshin.type
        val interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()
        val state = when {
            focused -> FieldState.Active
            value.isNotEmpty() -> FieldState.Filled
            else -> FieldState.Idle
        }
        val look = colors.fieldLook(onMap = placement == SearchFieldPlacement.Map, state = state)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = type.bodyLarge.copy(color = look.text),
            cursorBrush = SolidColor(colors.borderOrnament),
            modifier = modifier
                .then(if (look.raised) Modifier.shadow(elevation = 4.dp, shape = CircleShape) else Modifier)
                .height(ControlSize)
                .background(look.background, CircleShape)
                .then(look.border?.let { Modifier.border(1.5.dp, it, CircleShape) } ?: Modifier),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = GenshinIcons.Search,
                        contentDescription = null,
                        tint = look.icon,
                        modifier = Modifier.size(22.dp),
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                style = type.bodyLarge,
                                color = look.hint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        ClearButton(onClick = onClearClick, disc = colors.iconSecondary, cut = look.background)
                    }
                }
            },
        )
    }

    /** A disc with the cross cut out of it in the color of the field. */
    @Composable
    private fun ClearButton(onClick: () -> Unit, disc: Color, cut: Color) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val unit = size.minDimension / 24f
                drawCircle(color = disc, radius = 9.5f * unit)
                val stroke = 3f * unit
                drawLine(cut, Offset(9f * unit, 9f * unit), Offset(15f * unit, 15f * unit), stroke, StrokeCap.Round)
                drawLine(cut, Offset(15f * unit, 9f * unit), Offset(9f * unit, 15f * unit), stroke, StrokeCap.Round)
            }
        }
    }
}

/** The switch of the game settings: a gold track with a check when on, a slate one with a gold cross when off. */
object GenshinSwitch : SwitchComponent {
    @Composable
    override fun Content(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier) {
        val colors = Genshin.colors
        val knobX by animateDpAsState(if (checked) 35.dp else 3.dp)
        val shape = RoundedCornerShape(16.dp)
        val toggle = onCheckedChange?.let {
            Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = it)
        } ?: Modifier
        Box(
            modifier = modifier
                .then(toggle)
                .size(width = 64.dp, height = 32.dp)
                .clip(shape)
                .background(if (checked) colors.controlOn else colors.controlOff)
                .border(
                    width = 1.5.dp,
                    color = if (checked) colors.borderOrnament else colors.buttonLightBg.copy(alpha = 0.7f),
                    shape = shape,
                ),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobX, y = 3.dp)
                    .size(26.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(if (checked) colors.controlKnob else colors.buttonLightBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (checked) GenshinIcons.Check else GenshinIcons.Cross,
                    contentDescription = null,
                    tint = if (checked) colors.controlMarkOn else colors.controlMarkOff,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** A floor of a dungeon: slate with a gold edge when picked, the gold diamond marks the floor of the location. */
object GenshinChip : ChipComponent {
    @Composable
    override fun Content(text: String, selected: Boolean, marked: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .height(32.dp)
                .clip(CircleShape)
                .background(if (selected) colors.buttonDarkBg else colors.surfaceRow)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) colors.borderOrnament else colors.borderSubtle,
                    shape = CircleShape,
                )
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (marked) {
                Icon(
                    imageVector = GenshinIcons.DiamondFilled,
                    contentDescription = null,
                    tint = colors.iconAccent,
                    modifier = Modifier.size(9.dp),
                )
            }
            Text(
                text = text,
                style = Genshin.type.bodyMedium,
                color = if (selected) colors.buttonDarkText else colors.textPrimary,
            )
        }
    }
}
