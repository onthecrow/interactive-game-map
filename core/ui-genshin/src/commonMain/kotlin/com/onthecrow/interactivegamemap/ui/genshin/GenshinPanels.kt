package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
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

/** The rows of a choice and of the settings keep this radius however tall they grow. */
internal val RowShape = RoundedCornerShape(32.dp)

/** The dark dropdown of the game, edged in gold. */
object GenshinMenu : MenuComponent {
    @Composable
    override fun Content(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val colors = Genshin.colors
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.padding(horizontal = 6.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = colors.surfaceMenu,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, colors.borderOrnament.copy(alpha = 0.6f)),
            content = content,
        )
    }
}

/** A row of the dark dropdown: the icon on a cream badge, the current choice lighter with a gold check. */
object GenshinMenuItem : MenuItemComponent {
    @Composable
    override fun Content(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        leading: (@Composable (tint: Color) -> Unit)?,
        modifier: Modifier,
    ) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .widthIn(min = 240.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(if (selected) Modifier.background(colors.surfaceMenuActive) else Modifier)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 12.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier.size(32.dp).background(colors.buttonLightBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    leading(colors.buttonLightText)
                }
            }
            Text(
                text = text,
                style = Genshin.type.bodyMedium,
                color = colors.textOnDark,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = GenshinIcons.Check,
                    contentDescription = null,
                    tint = colors.borderOrnament,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** The cream panel of the game with its gold frame and glowing corners. */
object GenshinPanel : PanelComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = modifier
                .genshinPanel(Genshin.colors)
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

object GenshinDivider : DividerComponent {
    @Composable
    override fun Content(modifier: Modifier) {
        OrnamentDivider(modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
object GenshinSheet : SheetComponent {
    @Composable
    override fun Content(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        val colors = Genshin.colors
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = colors.surfacePanel,
            contentColor = colors.textPrimary,
            scrimColor = colors.bgScrim,
            dragHandle = { SheetHandle() },
            content = content,
        )
    }

    /** A short bar between two sparkles. */
    @Composable
    private fun SheetHandle() {
        val colors = Genshin.colors
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val cy = size.height / 2
            val cx = size.width / 2
            val bar = 36.dp.toPx()
            val gap = 6.dp.toPx()
            val sparkle = 9.dp
            drawLine(
                color = colors.borderSubtle,
                start = Offset(cx - bar / 2, cy),
                end = Offset(cx + bar / 2, cy),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawSparkle(Offset(cx - bar / 2 - gap - sparkle.toPx() / 2, cy), sparkle, colors.borderOrnament)
            drawSparkle(Offset(cx + bar / 2 + gap + sparkle.toPx() / 2, cy), sparkle, colors.borderOrnament)
        }
    }
}

/**
 * A choice of the game: the cream panel with the title between gold rules, pill options, and the slate
 * Cancel button. A tap on an option picks it.
 */
object GenshinChoiceDialog : ChoiceDialogComponent {
    @Composable
    override fun Content(
        title: String,
        options: List<ChoiceOption>,
        onSelect: (index: Int) -> Unit,
        onDismissRequest: () -> Unit,
        dismissText: String,
    ) {
        val colors = Genshin.colors
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 345.dp)
                    .genshinPanel(colors)
                    .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    style = Genshin.type.titleLarge,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                OrnamentDivider()
                // The list of languages may not fit on a small screen
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        ChoiceRow(option = option, onClick = { onSelect(index) })
                    }
                }
                OrnamentDivider()
                GenshinButton.Content(
                    text = dismissText,
                    onClick = onDismissRequest,
                    kind = ButtonKind.Cancel,
                    size = ButtonSize.Large,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    @Composable
    private fun ChoiceRow(option: ChoiceOption, onClick: () -> Unit) {
        val colors = Genshin.colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RowShape)
                .background(if (option.selected) colors.surfaceRowActive else colors.surfaceRow)
                .then(if (option.selected) Modifier.border(2.dp, colors.borderHighlight, RowShape) else Modifier)
                .selectable(selected = option.selected, role = Role.RadioButton, onClick = onClick)
                .padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioMark(selected = option.selected)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = option.label, style = Genshin.type.bodyLarge, color = colors.textPrimary)
                option.description?.let {
                    Text(text = it, style = Genshin.type.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }

    @Composable
    private fun RadioMark(selected: Boolean) {
        val colors = Genshin.colors
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (selected) colors.surfaceRaised else colors.surfaceRow, CircleShape)
                .border(
                    width = if (selected) 2.dp else 1.5.dp,
                    color = if (selected) colors.borderOrnament else colors.borderSubtle,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Spacer(modifier = Modifier.size(10.dp).background(colors.iconPrimary, CircleShape))
            }
        }
    }
}
