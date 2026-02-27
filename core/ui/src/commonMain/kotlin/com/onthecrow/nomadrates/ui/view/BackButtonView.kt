package com.onthecrow.nomadrates.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import interactivegamemap.core.ui.generated.resources.Res
import interactivegamemap.core.ui.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BackButtonView(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val gradientBaseColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
    val radialBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                gradientBaseColor,
                Color.Transparent,
            ),
        )
    }

    Icon(
        modifier = modifier.size(48.dp)
            .clip(CircleShape)
            .background(brush = radialBrush, shape = CircleShape)
            .clickable(enabled = true, onClick = onClick)
            .padding(12.dp),
        imageVector = vectorResource(Res.drawable.ic_back),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onBackground,
    )
}
