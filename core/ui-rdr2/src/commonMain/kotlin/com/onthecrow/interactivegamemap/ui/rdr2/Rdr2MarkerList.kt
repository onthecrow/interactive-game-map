package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.CategoryItemComponent
import com.onthecrow.interactivegamemap.ui.components.CycleCategoryComponent
import com.onthecrow.interactivegamemap.ui.components.CycleTabComponent
import com.onthecrow.interactivegamemap.ui.components.GroupHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.VisibilityActionsComponent

private const val HIDDEN_CATEGORY_ALPHA = 0.45f

/** A subtitle of the game, OUTFITS: small capitals over a rule, the pointer turns down while the group is open. */
object Rdr2GroupHeader : GroupHeaderComponent {
    @Composable
    override fun Content(title: String, description: String?, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val colors = Rdr2.colors
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = Rdr2.type.label,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Rdr2Icons.ArrowRight,
                    contentDescription = null,
                    tint = colors.iconSecondary,
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 90f else 0f),
                )
            }
            NotchedRule(notch = false)
            description?.takeIf { expanded }?.let {
                Text(text = it, style = Rdr2.type.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

/**
 * A category on a stroke, as the entries of the map's index: its icon, name and count, always 48 tall, the
 * name takes two lines at most. A hidden one is faded and struck through, a pressed one outlined in red.
 */
object Rdr2CategoryItem : CategoryItemComponent {

    override val horizontalPadding: Dp = 16.dp

    override val spacing: Dp = 6.dp

    @Composable
    override fun Content(
        name: String,
        count: String?,
        visible: Boolean,
        onClick: () -> Unit,
        standalone: Boolean,
        modifier: Modifier,
        icon: @Composable () -> Unit,
    ) {
        val colors = Rdr2.colors
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Row(
            modifier = modifier
                .height(48.dp)
                .then(
                    if (standalone) {
                        Modifier.brushStroke(if (pressed) colors.strokeRowPressed else colors.strokeRow, salt = 61)
                    } else {
                        Modifier
                    },
                )
                .selectionOutline(pressed && standalone, colors.selection, salt = 61)
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                .alpha(if (visible) 1f else HIDDEN_CATEGORY_ALPHA)
                .padding(start = 10.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(28.dp)) {
                icon()
            }
            Text(
                text = name,
                style = Rdr2.type.bodyMedium,
                color = colors.textPrimary,
                textDecoration = if (visible) null else TextDecoration.LineThrough,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            count?.let { Text(text = it, style = Rdr2.type.bodySmall, color = colors.textSecondary) }
        }
    }
}

/** The prompts over the groups, in the middle. */
object Rdr2VisibilityActions : VisibilityActionsComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

/** A category with its respawn cycles on one stroke. */
object Rdr2CycleCategory : CycleCategoryComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier
                .brushStroke(Rdr2.colors.strokeRow, salt = 63)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The number of a cycle, the picked one outlined in red. */
object Rdr2CycleTab : CycleTabComponent {
    @Composable
    override fun Content(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val colors = Rdr2.colors
        Box(
            modifier = modifier
                .size(width = 32.dp, height = 40.dp)
                .selectionOutline(selected, colors.selection, salt = 65)
                .clickable(role = Role.Tab, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = Rdr2.type.bodyMedium,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
