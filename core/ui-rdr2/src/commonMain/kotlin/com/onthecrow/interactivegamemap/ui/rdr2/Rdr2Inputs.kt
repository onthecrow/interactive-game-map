package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onthecrow.interactivegamemap.ui.components.ChipComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldPlacement
import com.onthecrow.interactivegamemap.ui.components.SwitchComponent
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_cancel
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_search
import org.jetbrains.compose.resources.vectorResource

private val FieldShape = RoundedCornerShape(100.dp)

/** A pill in the secondary container color, raised over the map. */
object Rdr2SearchField : SearchFieldComponent {
    @Composable
    override fun Content(
        value: String,
        onValueChange: (String) -> Unit,
        hint: String,
        onClearClick: () -> Unit,
        placement: SearchFieldPlacement,
        modifier: Modifier,
    ) {
        val raised = if (placement == SearchFieldPlacement.Map) Modifier.shadow(elevation = 6.dp, shape = FieldShape) else Modifier
        BasicTextField(
            modifier = modifier
                .then(raised)
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = FieldShape,
                ),
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 16.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSecondaryContainer),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(start = 12.dp),
                        imageVector = vectorResource(Res.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        Icon(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = true, onClick = onClearClick)
                                .padding(12.dp),
                            imageVector = vectorResource(Res.drawable.ic_cancel),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            },
        )
    }
}

object Rdr2Switch : SwitchComponent {
    @Composable
    override fun Content(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    }
}

/** A filter chip, the dot marks the floor of the location. */
object Rdr2Chip : ChipComponent {
    @Composable
    override fun Content(text: String, selected: Boolean, marked: Boolean, onClick: () -> Unit, modifier: Modifier) {
        FilterChip(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            label = { Text(if (marked) "• $text" else text) },
        )
    }
}
