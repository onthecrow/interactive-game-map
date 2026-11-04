package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.MarkerIconLoader
import com.onthecrow.interactivegamemap.network.GameMap
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.ic_layers
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_maps
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject

private val InvertColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

@Composable
internal fun MapSelectorView(
    maps: List<GameMap>,
    currentMapId: Int?,
    modifier: Modifier = Modifier,
    onMapSelect: (GameMap) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        MapIconView(
            modifier = Modifier.size(MapControlSize)
                .mapControl(CircleShape)
                .clickable(role = Role.Button, onClick = { expanded = true })
                .padding(10.dp),
            iconUrl = maps.firstOrNull { it.id == currentMapId }?.iconUrl,
            contentDescription = stringResource(Res.string.map_screen_maps),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            maps.forEach { map ->
                DropdownMenuItem(
                    leadingIcon = {
                        MapIconView(
                            modifier = Modifier.size(24.dp),
                            iconUrl = map.iconUrl,
                        )
                    },
                    text = {
                        Text(
                            text = map.name,
                            fontWeight = if (map.id == currentMapId) FontWeight.Bold else null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onMapSelect(map)
                    },
                )
            }
        }
    }
}

@Composable
private fun MapIconView(
    iconUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val iconLoader = koinInject<MarkerIconLoader>()
    val image by produceState<ImageBitmap?>(null, iconUrl) {
        value = iconUrl?.let { iconLoader.load(it) }
    }

    image?.let { bitmap ->
        Image(
            modifier = modifier,
            bitmap = bitmap,
            contentDescription = contentDescription,
            // The icons are dark, the site inverts them on a dark background too
            colorFilter = if (MaterialTheme.colorScheme.secondaryContainer.luminance() < 0.5f) InvertColorFilter else null,
        )
    } ?: Icon(
        modifier = modifier,
        imageVector = vectorResource(Res.drawable.ic_layers),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
