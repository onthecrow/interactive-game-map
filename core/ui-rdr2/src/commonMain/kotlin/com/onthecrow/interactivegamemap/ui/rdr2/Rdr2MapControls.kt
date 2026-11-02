package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.MapButtonComponent
import com.onthecrow.interactivegamemap.ui.components.ZoomControlComponent
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_add
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_remove
import org.jetbrains.compose.resources.vectorResource

/** The size of the buttons over the map, the search field is as tall. */
private val MapControlSize = 48.dp

private const val DISABLED_ALPHA = 0.38f

/** The look of the controls over the map: raised above it in the secondary container color. */
@Composable
internal fun Modifier.mapControl(shape: Shape): Modifier {
    return shadow(elevation = 6.dp, shape = shape)
        .clip(shape)
        .background(MaterialTheme.colorScheme.secondaryContainer)
}

object Rdr2MapButton : MapButtonComponent {
    @Composable
    override fun Content(
        onClick: () -> Unit,
        active: Boolean,
        modifier: Modifier,
        content: @Composable (tint: Color) -> Unit,
    ) {
        val colors = MaterialTheme.colorScheme
        Box(
            modifier = modifier.size(MapControlSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(color = if (active) colors.primary else colors.secondaryContainer)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content(if (active) colors.onPrimary else colors.onSecondaryContainer)
        }
    }
}

/** The zoom buttons in one pill. */
object Rdr2ZoomControl : ZoomControlComponent {
    @Composable
    override fun Content(
        canZoomIn: Boolean,
        canZoomOut: Boolean,
        onZoomInClick: () -> Unit,
        onZoomOutClick: () -> Unit,
        zoomInDescription: String,
        zoomOutDescription: String,
        modifier: Modifier,
    ) {
        Column(
            modifier = modifier
                .width(MapControlSize)
                .mapControl(RoundedCornerShape(MapControlSize / 2)),
        ) {
            ZoomButton(
                imageVector = vectorResource(Res.drawable.ic_add),
                contentDescription = zoomInDescription,
                enabled = canZoomIn,
                onClick = onZoomInClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
            )
            ZoomButton(
                imageVector = vectorResource(Res.drawable.ic_remove),
                contentDescription = zoomOutDescription,
                enabled = canZoomOut,
                onClick = onZoomOutClick,
            )
        }
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
