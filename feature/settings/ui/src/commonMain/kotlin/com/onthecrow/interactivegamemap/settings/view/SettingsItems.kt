package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.runtime.Composable
import com.onthecrow.interactivegamemap.ui.components.ChoiceOption
import com.onthecrow.interactivegamemap.ui.components.GameChoiceDialog
import interactivegamemap.feature.settings.ui.generated.resources.Res
import interactivegamemap.feature.settings.ui.generated.resources.settings_cancel
import org.jetbrains.compose.resources.stringResource

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
    GameChoiceDialog(
        title = title,
        options = options.map { ChoiceOption(label = label(it), description = description(it), selected = it == selected) },
        onSelect = { onSelect(options[it]) },
        onDismissRequest = onDismiss,
        dismissText = stringResource(Res.string.settings_cancel),
    )
}
