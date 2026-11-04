package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

/**
 * A round button over the map. [content] draws the icon in the tint it's given, [active] is the pressed
 * state of a toggle, like the layers of the dungeons shown.
 */
interface MapButtonComponent {
    @Composable
    fun Content(onClick: () -> Unit, active: Boolean, modifier: Modifier, content: @Composable (tint: Color) -> Unit)
}

/** The zoom buttons over the map, a button is dimmed at the end of the zoom range. */
interface ZoomControlComponent {
    @Composable
    fun Content(
        canZoomIn: Boolean,
        canZoomOut: Boolean,
        onZoomInClick: () -> Unit,
        onZoomOutClick: () -> Unit,
        zoomInDescription: String,
        zoomOutDescription: String,
        modifier: Modifier,
    )
}

@Composable
fun GameMapButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    GameMapButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        active = active,
    ) { tint ->
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
fun GameMapButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    content: @Composable (tint: Color) -> Unit,
) {
    GameMapTheme.components.mapButton.Content(onClick, active, modifier, content)
}

@Composable
fun GameZoomControl(
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    zoomInDescription: String,
    zoomOutDescription: String,
    modifier: Modifier = Modifier,
) {
    GameMapTheme.components.zoomControl.Content(
        canZoomIn,
        canZoomOut,
        onZoomInClick,
        onZoomOutClick,
        zoomInDescription,
        zoomOutDescription,
        modifier,
    )
}
