package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.network.MapFloor
import com.onthecrow.interactivegamemap.ui.components.ButtonKind
import com.onthecrow.interactivegamemap.ui.components.ButtonSize
import com.onthecrow.interactivegamemap.ui.components.GameButton
import com.onthecrow.interactivegamemap.ui.components.GameChip
import com.onthecrow.interactivegamemap.ui.components.GameDivider
import com.onthecrow.interactivegamemap.ui.components.GamePanel
import com.onthecrow.interactivegamemap.ui.components.GameTextButton
import com.onthecrow.interactivegamemap.ui.components.TextButtonGlyph
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.location_view_exit_dungeon
import interactivegamemap.feature.map.ui_common.generated.resources.location_view_found
import org.jetbrains.compose.resources.stringResource

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
    GamePanel(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = GameMapTheme.typography.title,
            color = GameMapTheme.colors.content,
        )
        GameDivider()
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The only floor which is already displayed gives nothing to choose from
                    if (floors.size > 1 || selectedFloorId == null) {
                        floors.forEach { floor ->
                            GameChip(
                                text = floor.name,
                                selected = floor.id == selectedFloorId,
                                // The mark points at the floor this location is on
                                marked = floor.id == locationFloorId,
                                onClick = { onFloorClick(floor.id) },
                            )
                        }
                    }
                }
                GameTextButton(
                    text = stringResource(Res.string.location_view_exit_dungeon),
                    glyph = TextButtonGlyph.Cancel,
                    onClick = onExitClick,
                )
            }
        }
        GameButton(
            modifier = Modifier.align(Alignment.End),
            text = stringResource(Res.string.location_view_found),
            kind = if (isFound) ButtonKind.Done else ButtonKind.Confirm,
            size = ButtonSize.Small,
            onClick = onFoundClick,
        )
    }
}
