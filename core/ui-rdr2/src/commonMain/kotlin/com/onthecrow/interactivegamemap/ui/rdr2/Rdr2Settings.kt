package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ScreenScaffoldComponent
import com.onthecrow.interactivegamemap.ui.components.SectionHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.SettingAction
import com.onthecrow.interactivegamemap.ui.components.SettingsGroupComponent
import com.onthecrow.interactivegamemap.ui.components.SettingsRowComponent

/**
 * A menu of the game over the paper of the map or the night: one tall black panel with the title in the frame
 * and the back disc next to it. The panel runs past the bottom of the screen.
 */
object Rdr2ScreenScaffold : ScreenScaffoldComponent {
    @Composable
    override fun Content(
        title: String,
        onBackClick: () -> Unit,
        backContentDescription: String,
        modifier: Modifier,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        val colors = Rdr2.colors
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(colors.bgScreen)
                    drawScreenGrain(colors.bgScreenGrain)
                }
                .statusBarsPadding()
                .padding(start = 6.dp, end = 6.dp, top = 6.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize().inkPanel(colors, salt = 71, openBottom = true)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = PanelEdge, end = PanelEdge + 4.dp, top = PanelEdge + 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onClick = onBackClick, contentDescription = backContentDescription)
                    TitleFrame(text = title, modifier = Modifier.weight(1f))
                    // As wide as the back button, so the title is in the middle of the screen
                    Box(modifier = Modifier.size(40.dp))
                }
                val navigationBars = WindowInsets.navigationBars.asPaddingValues()
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
                    content(PaddingValues(bottom = navigationBars.calculateBottomPadding()))
                }
            }
        }
    }

    @Composable
    private fun BackButton(onClick: () -> Unit, contentDescription: String) {
        val colors = Rdr2.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            ControlDisc(size = 40.dp, active = pressed) {
                Icon(
                    imageVector = Rdr2Icons.Back,
                    contentDescription = null,
                    tint = colors.iconPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** A subtitle of the game's menus: small capitals in the middle, over a rule with the notch. */
object Rdr2SectionHeader : SectionHeaderComponent {
    @Composable
    override fun Content(text: String, modifier: Modifier) {
        val colors = Rdr2.colors
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = PanelEdge + 10.dp, end = PanelEdge + 10.dp, top = 18.dp, bottom = 8.dp)
                .semantics { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = text, style = Rdr2.type.label, color = colors.textPrimary, textAlign = TextAlign.Center)
            NotchedRule()
        }
    }
}

/** The rows of a section on strokes, a little apart. */
object Rdr2SettingsGroup : SettingsGroupComponent {
    @Composable
    override fun Content(items: List<@Composable () -> Unit>, modifier: Modifier) {
        Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = PanelEdge + 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { it() }
        }
    }
}

/**
 * An entry of the game's menus on a stroke: the title, its summary under it, and the checklist's box, the value
 * with a pointer, a pointer, or the copy glyph at the end. Pressed, it's outlined in red. The title takes two
 * lines at most, the summary three, the value two.
 */
object Rdr2SettingsRow : SettingsRowComponent {
    @Composable
    override fun Content(title: String, summary: String?, action: SettingAction, modifier: Modifier) {
        val colors = Rdr2.colors
        val type = Rdr2.type
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val click = when (action) {
            is SettingAction.Switch -> Modifier.toggleable(
                value = action.checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onValueChange = action.onCheckedChange,
            )
            is SettingAction.Choice -> Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = action.onClick)
            is SettingAction.Link -> Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = action.onClick)
            is SettingAction.Copy -> Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = action.onClick)
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .brushStroke(if (pressed) colors.strokeRowPressed else colors.strokeRow, salt = 73)
                .selectionOutline(pressed, colors.selection, salt = 73)
                .then(click)
                .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = type.bodyLarge, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                summary?.let {
                    Text(text = it, style = type.bodySmall, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            when (action) {
                is SettingAction.Switch -> Checkbox(checked = action.checked)
                is SettingAction.Choice -> Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = action.value,
                        style = type.bodyMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp),
                    )
                    Pointer()
                }
                is SettingAction.Link -> Pointer()
                is SettingAction.Copy -> Icon(
                    imageVector = Rdr2Icons.Copy,
                    contentDescription = null,
                    tint = colors.iconSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    @Composable
    private fun Pointer() {
        Icon(
            imageVector = Rdr2Icons.ArrowRight,
            contentDescription = null,
            tint = Rdr2.colors.iconSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
