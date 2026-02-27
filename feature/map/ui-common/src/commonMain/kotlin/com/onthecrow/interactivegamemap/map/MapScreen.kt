package com.onthecrow.interactivegamemap.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.view.MapView
import com.onthecrow.nomadrates.ui.AppTheme
import com.onthecrow.nomadrates.ui.view.AppBarSearchView
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.map_screen_search_hint
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapScreen(
    state: MapScreenState,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
) {
    Box(modifier = modifier) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            locations = state.locations,
        )
        AppBarSearchView(
            modifier = Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            value = state.searchValue,
            showBackButton = false,
            hint = stringResource(Res.string.map_screen_search_hint),
            onClearClick = { onEvent(MapEvent.OnClearSearchClick) },
            onValueChange = { onEvent(MapEvent.OnSearchValueChange(it)) },
        )
    }
}

@Preview
@Composable
private fun MapScreenPreview() {
    AppTheme {
        MapScreen(
            modifier = Modifier.fillMaxSize(),
            state = MapScreenState(),
        )
    }
}
