package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.onthecrow.interactivegamemap.ui.components.ScreenScaffoldComponent
import com.onthecrow.interactivegamemap.ui.components.SectionHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.SettingAction
import com.onthecrow.interactivegamemap.ui.components.SettingsGroupComponent
import com.onthecrow.interactivegamemap.ui.components.SettingsRowComponent

/** A cream screen: the game's round cross, the title and a gold rule over the content. */
object GenshinScreenScaffold : ScreenScaffoldComponent {
    @Composable
    override fun Content(
        title: String,
        onBackClick: () -> Unit,
        backContentDescription: String,
        modifier: Modifier,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        val colors = Genshin.colors
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bgScreen)
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CloseButton(onClick = onBackClick, contentDescription = backContentDescription)
                Text(
                    text = title,
                    style = Genshin.type.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            OrnamentDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp))
            val navigationBars = WindowInsets.navigationBars.asPaddingValues()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content(PaddingValues(bottom = navigationBars.calculateBottomPadding()))
            }
        }
    }

    /** The game's round cross: a cream disc in a light rim, it stays dark on the disc in both themes. */
    @Composable
    private fun CloseButton(onClick: () -> Unit, contentDescription: String) {
        val colors = Genshin.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(colors.borderHighlight.copy(alpha = 0.55f))
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (pressed) colors.controlOn else colors.buttonLightBg, CircleShape)
                    .border(1.dp, colors.borderOrnament.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GenshinIcons.Close,
                    contentDescription = null,
                    tint = colors.buttonLightText,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** A gold diamond, the gold title and a rule fading to a sparkle. */
object GenshinSectionHeader : SectionHeaderComponent {
    @Composable
    override fun Content(text: String, modifier: Modifier) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
                .semantics { heading() },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = GenshinIcons.DiamondFilled,
                contentDescription = null,
                tint = colors.iconAccent,
                modifier = Modifier.size(10.dp),
            )
            Text(text = text, style = Genshin.type.titleMedium, color = colors.textAccent)
            Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.borderOrnament.copy(alpha = 0.7f)))
            Canvas(modifier = Modifier.size(10.dp)) {
                drawSparkle(center, 10.dp, colors.borderOrnament)
            }
        }
    }
}

/** Rows apart from each other, as in the game settings. */
object GenshinSettingsGroup : SettingsGroupComponent {
    @Composable
    override fun Content(items: List<@Composable () -> Unit>, modifier: Modifier) {
        Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { it() }
        }
    }
}

/**
 * A pill row of the game settings. The texts end before the big star and the switch or the value start
 * after it, the darker end of the row begins at the star's center. The title takes two lines at most, the
 * summary three, the value two. A link has neither the stars nor the darker end, just the chevron, nor has a copy,
 * just its icon.
 */
object GenshinSettingsRow : SettingsRowComponent {
    @Composable
    override fun Content(title: String, summary: String?, action: SettingAction, modifier: Modifier) {
        val colors = Genshin.colors
        val type = Genshin.type
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val click = when (action) {
            is SettingAction.Switch -> Modifier.toggleable(
                value = action.checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = action.onCheckedChange,
            )
            is SettingAction.Choice -> Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = action.onClick,
            )
            is SettingAction.Link -> Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = action.onClick,
            )
            is SettingAction.Copy -> Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = action.onClick,
            )
        }
        val ornamented = action is SettingAction.Switch || action is SettingAction.Choice
        val trail = if (pressed) colors.surfaceRow else colors.surfaceRowTrail
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .height(IntrinsicSize.Min)
                .clip(RowShape)
                .background(if (pressed) colors.surfaceRowActive else colors.surfaceRow)
                .then(if (pressed) Modifier.border(2.dp, colors.borderHighlight, RowShape) else Modifier)
                .then(click),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(start = 24.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = type.bodyLarge,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                summary?.let {
                    Text(
                        text = it,
                        style = type.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (ornamented) {
                // Under the texts, over the darker end
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .zIndex(-1f)
                        .drawBehind {
                            drawRect(trail, topLeft = Offset(size.width / 2, 0f), size = Size(size.width / 2, size.height))
                            drawStarCluster(center, colors)
                        },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .zIndex(-2f)
                    .then(if (ornamented) Modifier.background(trail) else Modifier)
                    .padding(start = 6.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (action) {
                    is SettingAction.Switch -> GenshinSwitch.Content(checked = action.checked, onCheckedChange = null, modifier = Modifier)
                    is SettingAction.Choice -> {
                        Text(
                            text = action.value,
                            style = type.bodyMedium,
                            color = colors.textAccent,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp),
                        )
                        Chevron()
                    }
                    is SettingAction.Link -> Chevron()
                    is SettingAction.Copy -> Icon(
                        imageVector = GenshinIcons.Copy,
                        contentDescription = null,
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun Chevron() {
        Icon(
            imageVector = GenshinIcons.ChevronRight,
            contentDescription = null,
            tint = Genshin.colors.iconSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
