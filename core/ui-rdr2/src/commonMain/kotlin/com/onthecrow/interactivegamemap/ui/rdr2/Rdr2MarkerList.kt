package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
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
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_expand_more
import org.jetbrains.compose.resources.vectorResource

private const val HIDDEN_CATEGORY_ALPHA = 0.5f

object Rdr2GroupHeader : GroupHeaderComponent {
    @Composable
    override fun Content(title: String, description: String?, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                description?.takeIf { expanded }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Icon(
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
                imageVector = vectorResource(Res.drawable.ic_expand_more),
                contentDescription = null,
            )
        }
    }
}

object Rdr2CategoryItem : CategoryItemComponent {

    override val horizontalPadding: Dp = 8.dp

    override val spacing: Dp = 4.dp

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
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .alpha(if (visible) 1f else HIDDEN_CATEGORY_ALPHA)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (visible) null else TextDecoration.LineThrough,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

object Rdr2VisibilityActions : VisibilityActionsComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

object Rdr2CycleCategory : CycleCategoryComponent {
    @Composable
    override fun Content(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            ),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The number of a cycle, the selected one underlined in the accent color. */
object Rdr2CycleTab : CycleTabComponent {
    @Composable
    override fun Content(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
        Text(
            modifier = modifier
                .size(width = 28.dp, height = 40.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else null,
            textDecoration = if (selected) TextDecoration.Underline else null,
        )
    }
}
