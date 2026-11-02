package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.ic_add
import interactivegamemap.feature.map.ui_common.generated.resources.ic_remove
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_zoom_in
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_zoom_out
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** The size of the buttons over the map, the search field is as tall. */
internal val MapControlSize = 48.dp

private const val DISABLED_ALPHA = 0.38f

/** The look of the controls over the map: raised above it in the secondary container color. */
@Composable
internal fun Modifier.mapControl(shape: Shape): Modifier {
    return shadow(elevation = 6.dp, shape = shape)
        .clip(shape)
        .background(MaterialTheme.colorScheme.secondaryContainer)
}

@Composable
internal fun MapActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme

    Icon(
        modifier = modifier.size(MapControlSize)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(color = if (isActive) colors.primary else colors.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = if (isActive) colors.onPrimary else colors.onSecondaryContainer,
    )
}

/** The zoom buttons in one pill, a button is dimmed at the end of the zoom range. */
@Composable
internal fun ZoomButtonsView(
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(MapControlSize)
            .mapControl(RoundedCornerShape(MapControlSize / 2)),
    ) {
        ZoomButton(
            imageVector = vectorResource(Res.drawable.ic_add),
            contentDescription = stringResource(Res.string.map_screen_zoom_in),
            enabled = canZoomIn,
            onClick = onZoomInClick,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
        )
        ZoomButton(
            imageVector = vectorResource(Res.drawable.ic_remove),
            contentDescription = stringResource(Res.string.map_screen_zoom_out),
            enabled = canZoomOut,
            onClick = onZoomOutClick,
        )
    }
}

@Composable
private fun ZoomButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSecondaryContainer
    Icon(
        modifier = Modifier.size(MapControlSize)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA),
    )
}
