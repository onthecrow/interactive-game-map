package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

/** A dropdown menu, like the one of the maps. */
interface MenuComponent {
    @Composable
    fun Content(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit,
    )
}

/** An item of [MenuComponent], [selected] is the current choice; [leading] draws its icon in the tint it's given. */
interface MenuItemComponent {
    @Composable
    fun Content(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        leading: (@Composable (tint: Color) -> Unit)?,
        modifier: Modifier,
    )
}

@Composable
fun GameDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GameMapTheme.components.menu.Content(expanded, onDismissRequest, modifier, content)
}

@Composable
fun GameMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leading: (@Composable (tint: Color) -> Unit)? = null,
) {
    GameMapTheme.components.menuItem.Content(text, selected, onClick, leading, modifier)
}
