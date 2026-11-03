package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onthecrow.interactivegamemap.ui.components.ButtonKind
import com.onthecrow.interactivegamemap.ui.components.ButtonSize
import com.onthecrow.interactivegamemap.ui.components.ChoiceDialogComponent
import com.onthecrow.interactivegamemap.ui.components.ChoiceOption
import com.onthecrow.interactivegamemap.ui.components.DividerComponent
import com.onthecrow.interactivegamemap.ui.components.MenuComponent
import com.onthecrow.interactivegamemap.ui.components.MenuItemComponent
import com.onthecrow.interactivegamemap.ui.components.PanelComponent
import com.onthecrow.interactivegamemap.ui.components.SheetComponent

/** The space the ragged edges of a panel take inside its box, the content keeps clear of them. */
internal val PanelEdge = 14.dp

/**
 * A black sheet of the game's menus, its ragged edges inside the box. With [openBottom] the sheet runs on past
 * the bottom of the box, for a panel cut by the edge of the screen. A [raised] one, a dialog over a panel, is
 * solid ink in a grey rim, so it stands apart from the ink under it.
 */
internal fun Modifier.inkPanel(colors: Rdr2Colors, salt: Int = 0, openBottom: Boolean = false, raised: Boolean = false): Modifier =
    brushPanel(
        color = colors.inkPanel,
        rim = if (raised) colors.inkRimRaised else colors.inkRim,
        salt = salt,
        extendBottom = if (openBottom) PanelEdge * 3 else 0.dp,
        underAlpha = if (raised) 0.9f else 0.5f,
        overAlpha = if (raised) 1f else 0.92f,
    )

/** The dropdown of the maps: a panel of black ink. */
object Rdr2Menu : MenuComponent {
    @Composable
    override fun Content(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.inkPanel(Rdr2.colors, salt = 21).padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RectangleShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = content,
        )
    }
}

/** A row of the dropdown on a stroke, the current choice outlined in red. */
object Rdr2MenuItem : MenuItemComponent {
    @Composable
    override fun Content(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        leading: (@Composable (tint: Color) -> Unit)?,
        modifier: Modifier,
    ) {
        val colors = Rdr2.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Row(
            modifier = modifier
                .widthIn(min = 240.dp)
                .height(48.dp)
                .padding(vertical = 2.dp)
                .brushStroke(if (pressed) colors.strokeRowPressed else colors.strokeRow, salt = 23)
                .selectionOutline(selected || pressed, colors.selection, salt = 23)
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                .padding(start = 14.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    leading(colors.iconPrimary)
                }
            }
            Text(
                text = text,
                style = Rdr2.type.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A card of the game, SALOON: a black sheet with ragged edges. */
object Rdr2Panel : PanelComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = modifier
                .inkPanel(Rdr2.colors, salt = 31)
                .padding(start = PanelEdge + 8.dp, end = PanelEdge + 8.dp, top = PanelEdge + 6.dp, bottom = PanelEdge + 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** The thin rule of the game, the notch under its middle. */
object Rdr2Divider : DividerComponent {
    @Composable
    override fun Content(modifier: Modifier) {
        NotchedRule(modifier)
    }
}

/** A tall panel of black ink from the bottom of the screen, a short rule to drag it by. */
@OptIn(ExperimentalMaterial3Api::class)
object Rdr2Sheet : SheetComponent {
    @Composable
    override fun Content(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        val colors = Rdr2.colors
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RectangleShape,
            containerColor = Color.Transparent,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
            scrimColor = colors.bgScrim,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The bottom edge is under the screen's edge, only the top and the sides are ragged
                    .inkPanel(colors, salt = 41, openBottom = true)
                    .padding(top = 6.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 3.dp)
                            .drawBehind { drawRect(colors.rule) },
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 6.dp), content = content)
            }
        }
    }
}

/**
 * A choice of the game: a black panel with the title in the frame, the options on strokes, the current one
 * outlined in red, and the BACK prompt. A tap on an option picks it.
 */
object Rdr2ChoiceDialog : ChoiceDialogComponent {
    @Composable
    override fun Content(
        title: String,
        options: List<ChoiceOption>,
        onSelect: (index: Int) -> Unit,
        onDismissRequest: () -> Unit,
        dismissText: String,
    ) {
        val colors = Rdr2.colors
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 360.dp)
                    .inkPanel(colors, salt = 51, raised = true)
                    .padding(start = PanelEdge + 4.dp, end = PanelEdge + 4.dp, top = PanelEdge + 6.dp, bottom = PanelEdge),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TitleFrame(text = title, modifier = Modifier.fillMaxWidth())
                NotchedRule(notch = false)
                // The list of languages may not fit on a small screen
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        ChoiceRow(option = option, onClick = { onSelect(index) })
                    }
                }
                NotchedRule()
                Rdr2Button.Content(
                    text = dismissText,
                    onClick = onDismissRequest,
                    kind = ButtonKind.Cancel,
                    size = ButtonSize.Small,
                    enabled = true,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }

    @Composable
    private fun ChoiceRow(option: ChoiceOption, onClick: () -> Unit) {
        val colors = Rdr2.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .brushStroke(if (pressed) colors.strokeRowPressed else colors.strokeRow, salt = 53)
                .selectionOutline(option.selected, colors.selection, salt = 53)
                .selectable(
                    selected = option.selected,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Text(text = option.label, style = Rdr2.type.bodyLarge, color = colors.textPrimary)
            option.description?.let {
                Text(text = it, style = Rdr2.type.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
