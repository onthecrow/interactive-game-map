package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

/** A card over the map, like the popup of a marker; it spaces its [content] by itself. */
interface PanelComponent {
    @Composable
    fun Content(modifier: Modifier, content: @Composable ColumnScope.() -> Unit)
}

/** A rule between the title of a panel and its content, a style may draw nothing for it. */
interface DividerComponent {
    @Composable
    fun Content(modifier: Modifier)
}

/** A sheet over the screen from its bottom, like the list of the markers. */
interface SheetComponent {
    @Composable
    fun Content(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit)
}

/** One of the options of [ChoiceDialogComponent]. */
data class ChoiceOption(
    val label: String,
    val description: String? = null,
    val selected: Boolean = false,
)

/** A choice of one of [options]: a tap picks it and closes the dialog, [dismissText] keeps the current one. */
interface ChoiceDialogComponent {
    @Composable
    fun Content(
        title: String,
        options: List<ChoiceOption>,
        onSelect: (index: Int) -> Unit,
        onDismissRequest: () -> Unit,
        dismissText: String,
    )
}

@Composable
fun GamePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GameMapTheme.components.panel.Content(modifier, content)
}

@Composable
fun GameDivider(modifier: Modifier = Modifier) {
    GameMapTheme.components.divider.Content(modifier)
}

@Composable
fun GameModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GameMapTheme.components.sheet.Content(onDismissRequest, modifier, content)
}

@Composable
fun GameChoiceDialog(
    title: String,
    options: List<ChoiceOption>,
    onSelect: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
    dismissText: String,
) {
    GameMapTheme.components.choiceDialog.Content(title, options, onSelect, onDismissRequest, dismissText)
}
