package com.onthecrow.interactivegamemap.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.view.LocationView
import com.onthecrow.interactivegamemap.map.view.MapView
import com.onthecrow.interactivegamemap.ui.AppTheme
import com.onthecrow.interactivegamemap.ui.view.AppBarSearchView
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
            onEvent = onEvent,
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

        var displayedLocation by remember { mutableStateOf(MapLocation.EMPTY) }

        LaunchedEffect(state.selectedLocation) {
            if (state.selectedLocation != null) {
                displayedLocation = state.selectedLocation
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp),
            visible = state.selectedLocation != null,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 300)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(durationMillis = 300)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 220)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(durationMillis = 220)
            )
        ) {
            LocationView(
                title = displayedLocation.name,
                description = displayedLocation.description,
            )
        }
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
