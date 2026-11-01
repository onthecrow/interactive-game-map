package com.onthecrow.interactivegamemap.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import interactivegamemap.core.ui.generated.resources.Res
import interactivegamemap.core.ui.generated.resources.ic_cancel
import interactivegamemap.core.ui.generated.resources.ic_search
import org.jetbrains.compose.resources.vectorResource

@Composable
fun AppBarSearchView(
    modifier: Modifier = Modifier,
    hint: String,
    value: String,
    showBackButton: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    onBackPress: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onClearClick: () -> Unit = {},
) {
    Row(
        modifier = modifier,
    ) {
        if (leadingContent != null) {
            leadingContent()
        } else if (showBackButton) {
            BackButtonView(onClick = onBackPress)
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
        SearchFieldView(
            modifier = Modifier.weight(1f)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(100.dp),
                ),
            hint = hint,
            value = value,
            onValueChange = onValueChange,
            onClearClick = onClearClick,
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
fun SearchFieldView(
    modifier: Modifier = Modifier,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    onClearClick: () -> Unit = {},
) {
    BasicTextField(
        modifier = modifier
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(100.dp),
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
        }
    )
}

@Preview(
    showBackground = true,
)
@Composable
private fun AppBarSearchViewPreview() {
    MaterialTheme {
        AppBarSearchView(value = "", hint = "")
    }
}
