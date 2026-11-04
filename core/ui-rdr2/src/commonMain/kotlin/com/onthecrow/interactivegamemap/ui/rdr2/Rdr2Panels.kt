package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ChoiceDialogComponent
import com.onthecrow.interactivegamemap.ui.components.ChoiceOption
import com.onthecrow.interactivegamemap.ui.components.DividerComponent
import com.onthecrow.interactivegamemap.ui.components.MenuComponent
import com.onthecrow.interactivegamemap.ui.components.MenuItemComponent
import com.onthecrow.interactivegamemap.ui.components.PanelComponent
import com.onthecrow.interactivegamemap.ui.components.SheetComponent

object Rdr2Menu : MenuComponent {
    @Composable
    override fun Content(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier, content = content)
    }
}

/** A menu item, the current choice in bold. */
object Rdr2MenuItem : MenuItemComponent {
    @Composable
    override fun Content(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        leading: (@Composable (tint: Color) -> Unit)?,
        modifier: Modifier,
    ) {
        val tint = MaterialTheme.colorScheme.onSurfaceVariant
        DropdownMenuItem(
            modifier = modifier,
            leadingIcon = leading?.let { { it(tint) } },
            text = { Text(text = text, fontWeight = if (selected) FontWeight.Bold else null) },
            onClick = onClick,
        )
    }
}

/** A raised surface with rounded corners. */
object Rdr2Panel : PanelComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** The title and the content of a panel are apart enough without a rule. */
object Rdr2Divider : DividerComponent {
    @Composable
    override fun Content(modifier: Modifier) = Unit
}

@OptIn(ExperimentalMaterial3Api::class)
object Rdr2Sheet : SheetComponent {
    @Composable
    override fun Content(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismissRequest = onDismissRequest,
            content = content,
        )
    }
}

/** A choice as the system settings ask it: radio buttons, Cancel keeps the current one. */
object Rdr2ChoiceDialog : ChoiceDialogComponent {
    @Composable
    override fun Content(
        title: String,
        options: List<ChoiceOption>,
        onSelect: (index: Int) -> Unit,
        onDismissRequest: () -> Unit,
        dismissText: String,
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = dismissText)
                }
            },
            title = { Text(text = title) },
            text = {
                // The list of languages may not fit on a small screen
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
                ) {
                    options.forEachIndexed { index, option ->
                        ChoiceRow(option = option, onClick = { onSelect(index) })
                    }
                }
            },
        )
    }
}

@Composable
private fun ChoiceRow(option: ChoiceOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = option.selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = option.selected, onClick = null)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
            option.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
