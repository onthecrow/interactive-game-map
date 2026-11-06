package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.MarkerIconLoader
import com.onthecrow.interactivegamemap.map.MarkerImage
import com.onthecrow.interactivegamemap.map.painter
import com.onthecrow.interactivegamemap.network.GameMap
import com.onthecrow.interactivegamemap.ui.components.GameDropdownMenu
import com.onthecrow.interactivegamemap.ui.components.GameMapButton
import com.onthecrow.interactivegamemap.ui.components.GameMenuItem
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_maps
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun MapSelectorView(
    maps: List<GameMap>,
    currentMapId: Int?,
    modifier: Modifier = Modifier,
    onMapSelect: (GameMap) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(Res.string.map_screen_maps)

    Box(modifier = modifier) {
        GameMapButton(
            modifier = Modifier.semantics { contentDescription = description },
            active = expanded,
            onClick = { expanded = true },
        ) { tint ->
            MapIconView(
                modifier = Modifier.size(28.dp),
                iconUrl = maps.firstOrNull { it.id == currentMapId }?.iconUrl,
                tint = tint,
            )
        }
        GameDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            maps.forEach { map ->
                GameMenuItem(
                    text = map.name,
                    selected = map.id == currentMapId,
                    leading = { tint ->
                        MapIconView(
                            modifier = Modifier.size(24.dp),
                            iconUrl = map.iconUrl,
                            tint = tint,
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

/** The icon of a map: a one-color silhouette, drawn in [tint] as the other icons of the controls. */
@Composable
private fun MapIconView(
    iconUrl: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val iconLoader = koinInject<MarkerIconLoader>()
    val image by produceState<MarkerImage?>(null, iconUrl) {
        value = iconUrl?.let { iconLoader.load(it) }
    }

    image?.let {
        Image(
            modifier = modifier,
            painter = it.painter(),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
        )
    } ?: Icon(
        modifier = modifier,
        imageVector = GameMapTheme.icons.layers,
        contentDescription = null,
        tint = tint,
    )
}
