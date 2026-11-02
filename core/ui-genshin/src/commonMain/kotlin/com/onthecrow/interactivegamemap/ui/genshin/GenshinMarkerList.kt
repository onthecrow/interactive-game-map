package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.CategoryItemComponent
import com.onthecrow.interactivegamemap.ui.components.CycleCategoryComponent
import com.onthecrow.interactivegamemap.ui.components.CycleTabComponent
import com.onthecrow.interactivegamemap.ui.components.GroupHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.VisibilityActionsComponent

private const val HIDDEN_CATEGORY_ALPHA = 0.5f
private val ItemShape = RoundedCornerShape(8.dp)

/** A gold diamond, the title and a round chevron which turns over when the group is open. */
object GenshinGroupHeader : GroupHeaderComponent {
    @Composable
    override fun Content(title: String, description: String?, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = GenshinIcons.DiamondFilled,
                contentDescription = null,
                tint = colors.iconAccent,
                modifier = Modifier.size(12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = Genshin.type.titleMedium, color = colors.textPrimary)
                description?.takeIf { expanded }?.let {
                    Text(text = it, style = Genshin.type.bodySmall, color = colors.textSecondary)
                }
            }
            Box(
                modifier = Modifier.size(32.dp).background(colors.surfaceRow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GenshinIcons.ExpandMore,
                    contentDescription = null,
                    tint = colors.iconPrimary,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
        }
    }
}

/**
 * A category on a recessed card, always 48 tall: the name takes two lines at most. A hidden one is
 * faded and struck through.
 */
object GenshinCategoryItem : CategoryItemComponent {

    override val horizontalPadding: Dp = 16.dp

    override val spacing: Dp = 8.dp

    @Composable
    override fun Content(
        name: String,
        count: String,
        visible: Boolean,
        onClick: () -> Unit,
        standalone: Boolean,
        modifier: Modifier,
        icon: @Composable () -> Unit,
    ) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .height(48.dp)
                .clip(ItemShape)
                .then(if (standalone) Modifier.background(colors.surfaceRow) else Modifier)
                .clickable(role = Role.Button, onClick = onClick)
                .alpha(if (visible) 1f else HIDDEN_CATEGORY_ALPHA)
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(28.dp)) {
                icon()
            }
            Text(
                text = name,
                style = Genshin.type.bodyMedium,
                color = colors.textPrimary,
                textDecoration = if (visible) null else TextDecoration.LineThrough,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = count, style = Genshin.type.bodySmall, color = colors.textSecondary)
        }
    }
}

/**
 * The discs of the buttons keep [ActionsGap] from the search field above and from the first group below:
 * [ActionsTopInset] of it is the touch area of a button, and below the touch area, the spacing of the list
 * and the padding of the header already keep [ActionsBottomInset] more, which the row gives back.
 */
object GenshinVisibilityActions : VisibilityActionsComponent {
    private val ActionsGap = 12.dp
    private val ActionsTopInset = 6.dp
    private val ActionsBottomInset = 12.dp

    @Composable
    override fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val bottomInset = ActionsBottomInset.roundToPx()
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, (placeable.height - bottomInset).coerceAtLeast(0)) {
                        placeable.place(0, 0)
                    }
                }
                .padding(top = ActionsGap - ActionsTopInset),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

object GenshinCycleCategory : CycleCategoryComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        val colors = Genshin.colors
        Row(
            modifier = modifier
                .clip(ItemShape)
                .background(colors.surfaceRow)
                .border(1.dp, colors.borderSubtle, ItemShape)
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The number of a cycle, the picked one in gold over a gold bar. */
object GenshinCycleTab : CycleTabComponent {
    @Composable
    override fun Content(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val colors = Genshin.colors
        Column(
            modifier = modifier
                .size(width = 28.dp, height = 40.dp)
                .clickable(role = Role.Tab, onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text,
                style = Genshin.type.bodyMedium,
                color = if (selected) colors.textAccent else colors.textSecondary,
            )
            Spacer(
                modifier = Modifier
                    .size(width = 12.dp, height = 2.dp)
                    .background(if (selected) colors.borderOrnament else colors.borderOrnament.copy(alpha = 0f)),
            )
        }
    }
}
