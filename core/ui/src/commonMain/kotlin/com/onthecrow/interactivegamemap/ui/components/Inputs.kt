package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

enum class SearchFieldPlacement {
    /** Over the map, as tall as the buttons around it. */
    Map,

    /** On a panel or a sheet. */
    Panel,
}

/** A one-line search field with a clear button once something is typed. */
interface SearchFieldComponent {
    @Composable
    fun Content(
        value: String,
        onValueChange: (String) -> Unit,
        hint: String,
        onClearClick: () -> Unit,
        placement: SearchFieldPlacement,
        modifier: Modifier,
    )
}

/** An on/off switch; without [onCheckedChange] the row around it handles the clicks. */
interface SwitchComponent {
    @Composable
    fun Content(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier)
}

/** A chip to pick one of a few, the floors of a dungeon. [marked] points at the floor of the location. */
interface ChipComponent {
    @Composable
    fun Content(text: String, selected: Boolean, marked: Boolean, onClick: () -> Unit, modifier: Modifier)
}

@Composable
fun GameSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onClearClick: () -> Unit = { onValueChange("") },
    placement: SearchFieldPlacement = SearchFieldPlacement.Map,
) {
    GameMapTheme.components.searchField.Content(value, onValueChange, hint, onClearClick, placement, modifier)
}

@Composable
fun GameSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    GameMapTheme.components.switch.Content(checked, onCheckedChange, modifier)
}

@Composable
fun GameChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    marked: Boolean = false,
) {
    GameMapTheme.components.chip.Content(text, selected, marked, onClick, modifier)
}
