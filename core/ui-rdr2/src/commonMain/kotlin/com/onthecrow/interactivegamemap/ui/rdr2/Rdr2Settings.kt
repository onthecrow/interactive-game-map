package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ScreenScaffoldComponent
import com.onthecrow.interactivegamemap.ui.components.SectionHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.SettingAction
import com.onthecrow.interactivegamemap.ui.components.SettingsGroupComponent
import com.onthecrow.interactivegamemap.ui.components.SettingsRowComponent
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_arrow_back
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_copy
import org.jetbrains.compose.resources.vectorResource

// A group is one rounded card, its rows are split by narrow gaps with slightly rounded corners
private val GroupCornerRadius = 24.dp
private val GroupInnerCornerRadius = 4.dp
private val GroupGap = 2.dp

/** A large top app bar which collapses as the content scrolls, in the color of the screen. */
@OptIn(ExperimentalMaterial3Api::class)
object Rdr2ScreenScaffold : ScreenScaffoldComponent {
    @Composable
    override fun Content(
        title: String,
        onBackClick: () -> Unit,
        backContentDescription: String,
        modifier: Modifier,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        // The color of the screen is the one of the shades under the system bars
        val containerColor = MaterialTheme.colorScheme.background
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = containerColor,
            topBar = {
                LargeTopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_arrow_back),
                                contentDescription = backContentDescription,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor,
                        scrolledContainerColor = containerColor,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            content = content,
        )
    }
}

/** A section's title, as in the system settings: small, in the accent color, a heading for screen readers. */
object Rdr2SectionHeader : SectionHeaderComponent {
    @Composable
    override fun Content(text: String, modifier: Modifier) {
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
}

/** The settings of a section as the system settings of Android 16 show them: the rows of one card. */
object Rdr2SettingsGroup : SettingsGroupComponent {
    @Composable
    override fun Content(items: List<@Composable () -> Unit>, modifier: Modifier) {
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
}

/**
 * A list item: a switch toggled by the whole row, read as one switch by screen readers, a row which
 * opens something with the current value under the title, or one which copies its summary.
 */
object Rdr2SettingsRow : SettingsRowComponent {
    @Composable
    override fun Content(title: String, summary: String?, action: SettingAction, modifier: Modifier) {
        val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        when (action) {
            is SettingAction.Switch -> ListItem(
                headlineContent = { Text(text = title) },
                supportingContent = summary?.let { { Text(text = it) } },
                trailingContent = { Switch(checked = action.checked, onCheckedChange = null) },
                colors = colors,
                modifier = modifier.toggleable(
                    value = action.checked,
                    role = Role.Switch,
                    onValueChange = action.onCheckedChange,
                ),
            )
            is SettingAction.Choice -> ListItem(
                headlineContent = { Text(text = title) },
                supportingContent = { Text(text = action.value) },
                colors = colors,
                modifier = modifier.clickable(role = Role.Button, onClick = action.onClick),
            )
            is SettingAction.Link -> ListItem(
                headlineContent = { Text(text = title) },
                supportingContent = summary?.let { { Text(text = it) } },
                colors = colors,
                modifier = modifier.clickable(role = Role.Button, onClick = action.onClick),
            )
            is SettingAction.Copy -> ListItem(
                headlineContent = { Text(text = title) },
                supportingContent = summary?.let { { Text(text = it) } },
                trailingContent = { Icon(imageVector = vectorResource(Res.drawable.ic_copy), contentDescription = null) },
                colors = colors,
                modifier = modifier.clickable(role = Role.Button, onClick = action.onClick),
            )
        }
    }
}
