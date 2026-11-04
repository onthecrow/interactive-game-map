package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.network.MapBounds
import com.onthecrow.interactivegamemap.network.MapFloor
import com.onthecrow.interactivegamemap.ui.AppTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.ic_check
import interactivegamemap.feature.map.ui_common.generated.resources.location_view_exit_dungeon
import interactivegamemap.feature.map.ui_common.generated.resources.location_view_found
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun LocationView(
    title: String = "",
    description: String = "",
    floors: List<MapFloor> = emptyList(),
    selectedFloorId: Int? = null,
    locationFloorId: Int? = null,
    isFound: Boolean = false,
    modifier: Modifier = Modifier,
    onFloorClick: (Int) -> Unit = {},
    onExitClick: () -> Unit = {},
    onFoundClick: () -> Unit = {},
    onLinkClick: (String) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (description.isNotBlank()) {
                // The scroll position is not kept between the descriptions
                key(description) {
                    MarkerDescriptionView(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        description = description,
                        onLinkClick = onLinkClick,
                    )
                }
            }
            if (floors.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The only floor which is already displayed gives nothing to choose from
                    if (floors.size > 1 || selectedFloorId == null) {
                        floors.forEach { floor ->
                            FilterChip(
                                selected = floor.id == selectedFloorId,
                                onClick = { onFloorClick(floor.id) },
                                // The dot marks the floor this location is on
                                label = { Text(if (floor.id == locationFloorId) "• ${floor.name}" else floor.name) },
                            )
                        }
                    }
                    TextButton(onClick = onExitClick) {
                        Text(stringResource(Res.string.location_view_exit_dungeon))
                    }
                }
            }
            FoundButton(
                modifier = Modifier.align(Alignment.End),
                isFound = isFound,
                onClick = onFoundClick,
            )
        }
    }
}

@Composable
private fun FoundButton(
    isFound: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val label = stringResource(Res.string.location_view_found)
    if (isFound) {
        FilledTonalButton(
            modifier = modifier,
            onClick = onClick,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
    } else {
        Button(
            modifier = modifier,
            onClick = onClick,
        ) {
            Text(label)
        }
    }
}

@Preview
@Composable
private fun LocationViewPreview() {
    AppTheme {
        LocationView(
            title = "Location title",
            description = "**Requirements:** Chapter 2\n\n- Find the [cabin](https://rdr2map.com?locationIds=880)\n- Open *the door*",
            floors = listOf(
                MapFloor(1, "B1", "", MapBounds(0.0, 0.0, 0.0, 0.0)),
                MapFloor(2, "B2", "", MapBounds(0.0, 0.0, 0.0, 0.0)),
            ),
            selectedFloorId = 1,
            locationFloorId = 1,
            isFound = true,
        )
    }
}
