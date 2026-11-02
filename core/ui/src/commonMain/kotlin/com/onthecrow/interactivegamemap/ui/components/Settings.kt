package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

/** A screen with a title and a way back, like the settings. [content] gets the space the header takes. */
interface ScreenScaffoldComponent {
    @Composable
    fun Content(
        title: String,
        onBackClick: () -> Unit,
        backContentDescription: String,
        modifier: Modifier,
        content: @Composable (PaddingValues) -> Unit,
    )
}

/** The title of a group of settings. */
interface SectionHeaderComponent {
    @Composable
    fun Content(text: String, modifier: Modifier)
}

/** The settings of a section together. */
interface SettingsGroupComponent {
    @Composable
    fun Content(items: List<@Composable () -> Unit>, modifier: Modifier)
}

/** What a setting does when it's clicked, and what it shows for it. */
sealed interface SettingAction {
    /** Turns the setting on and off. */
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingAction

    /** Opens a choice; [value] is the current one. */
    data class Choice(val value: String, val onClick: () -> Unit) : SettingAction

    /** Opens something else, like a page. */
    data class Link(val onClick: () -> Unit) : SettingAction
}

/** A setting: its [title], what it does in [summary], and its [action]. */
interface SettingsRowComponent {
    @Composable
    fun Content(title: String, summary: String?, action: SettingAction, modifier: Modifier)
}

@Composable
fun GameScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    backContentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    GameMapTheme.components.screenScaffold.Content(title, onBackClick, backContentDescription, modifier, content)
}

@Composable
fun GameSectionHeader(text: String, modifier: Modifier = Modifier) {
    GameMapTheme.components.sectionHeader.Content(text, modifier)
}

class SettingsGroupScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items += content
    }
}

@Composable
fun GameSettingsGroup(
    modifier: Modifier = Modifier,
    content: SettingsGroupScope.() -> Unit,
) {
    val items = SettingsGroupScope().apply(content).items
    GameMapTheme.components.settingsGroup.Content(items, modifier)
}

@Composable
fun GameSettingsRow(
    title: String,
    action: SettingAction,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    GameMapTheme.components.settingsRow.Content(title, summary, action, modifier)
}
