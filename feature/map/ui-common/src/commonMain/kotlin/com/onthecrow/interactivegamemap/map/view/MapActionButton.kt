package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun MapActionButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme

    Icon(
        modifier = modifier.size(48.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(color = if (isActive) colors.primary else colors.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        imageVector = imageVector,
        contentDescription = null,
        tint = if (isActive) colors.onPrimary else colors.onSecondaryContainer,
    )
}
