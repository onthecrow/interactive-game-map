package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.network.MapBounds
import com.onthecrow.interactivegamemap.network.MapFloor
import com.onthecrow.interactivegamemap.ui.AppTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.location_view_exit_dungeon
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LocationView(
    title: String = "",
    description: String = "",
    floors: List<MapFloor> = emptyList(),
    selectedFloorId: Int? = null,
    locationFloorId: Int? = null,
    modifier: Modifier = Modifier,
    onFloorClick: (Int) -> Unit = {},
    onExitClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
        )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
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
    }
}

@Preview
@Composable
private fun LocationViewPreview() {
    AppTheme {
        LocationView(
            title = "Location title",
            description = "Location description",
            floors = listOf(
                MapFloor(1, "B1", "", MapBounds(0.0, 0.0, 0.0, 0.0)),
                MapFloor(2, "B2", "", MapBounds(0.0, 0.0, 0.0, 0.0)),
            ),
            selectedFloorId = 1,
            locationFloorId = 1,
        )
    }
}
