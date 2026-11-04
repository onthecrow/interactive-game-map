package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

/** The header of a collapsible group of categories, the description is shown while it's expanded. */
interface GroupHeaderComponent {
    @Composable
    fun Content(title: String, description: String?, expanded: Boolean, onClick: () -> Unit, modifier: Modifier)
}

/**
 * A category of markers in two columns: its icon, name and count; a hidden one is dimmed. [standalone] is
 * false inside [CycleCategoryComponent], which draws the background then.
 */
interface CategoryItemComponent {
    /** The space from the edges of the list to the columns. */
    val horizontalPadding: Dp

    /** The space between the columns and between the rows. */
    val spacing: Dp

    @Composable
    fun Content(
        name: String,
        count: String,
        visible: Boolean,
        onClick: () -> Unit,
        standalone: Boolean,
        modifier: Modifier,
        icon: @Composable () -> Unit,
    )
}

/** The row of Show all and Hide all above the groups, with the space it keeps around them. */
interface VisibilityActionsComponent {
    @Composable
    fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit)
}

/** The frame of a category with respawn cycles, around the category and its cycle tabs. */
interface CycleCategoryComponent {
    @Composable
    fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit)
}

/** A respawn cycle of a category. */
interface CycleTabComponent {
    @Composable
    fun Content(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier)
}

@Composable
fun GameGroupHeader(
    title: String,
    description: String?,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameMapTheme.components.groupHeader.Content(title, description, expanded, onClick, modifier)
}

@Composable
fun GameCategoryItem(
    name: String,
    count: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    standalone: Boolean = true,
    icon: @Composable () -> Unit,
) {
    GameMapTheme.components.categoryItem.Content(name, count, visible, onClick, standalone, modifier, icon)
}

@Composable
fun GameVisibilityActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    GameMapTheme.components.visibilityActions.Content(modifier, content)
}

@Composable
fun GameCycleCategory(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    GameMapTheme.components.cycleCategory.Content(modifier, content)
}

@Composable
fun GameCycleTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameMapTheme.components.cycleTab.Content(text, selected, onClick, modifier)
}
