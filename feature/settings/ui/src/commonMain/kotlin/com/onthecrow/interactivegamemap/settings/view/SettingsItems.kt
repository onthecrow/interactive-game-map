package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import interactivegamemap.feature.settings.ui.generated.resources.Res
import interactivegamemap.feature.settings.ui.generated.resources.settings_cancel
import org.jetbrains.compose.resources.stringResource

// A group is one rounded card, its rows are split by narrow gaps with slightly rounded corners
private val GroupCornerRadius = 24.dp
private val GroupInnerCornerRadius = 4.dp
private val GroupGap = 2.dp

/** A section's title, as in the system settings: small, in the accent color, a heading for screen readers. */
@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

internal class SettingsGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items += content
    }
}

/** The settings of a section as the system settings of Android 16 show them: the rows of one card. */
@Composable
internal fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: SettingsGroupScope.() -> Unit,
) {
    val items = SettingsGroupScope().apply(content).items
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
    ) {
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .clip(groupItemShape(index = index, count = items.size))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                item()
            }
        }
    }
}

private fun groupItemShape(index: Int, count: Int): Shape {
    val top = if (index == 0) GroupCornerRadius else GroupInnerCornerRadius
    val bottom = if (index == count - 1) GroupCornerRadius else GroupInnerCornerRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

private val itemColors
    @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

/** A setting turned on and off. The whole row toggles it, and screen readers read it as one switch. */
@Composable
internal fun SwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = itemColors,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** A setting that opens a choice, another screen or an action; [summary] is its current value. */
@Composable
internal fun ClickableItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = summary?.let { { Text(text = it) } },
        colors = itemColors,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/**
 * One of [options], as the system settings ask it: a tap picks it and closes the dialog, Cancel or back
 * keeps the current one.
 */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    description: @Composable (T) -> String?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.settings_cancel))
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
                options.forEach { option ->
                    ChoiceRow(
                        label = label(option),
                        description = description(option),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                    )
                }
            }
        },
    )
}

@Composable
private fun ChoiceRow(label: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
