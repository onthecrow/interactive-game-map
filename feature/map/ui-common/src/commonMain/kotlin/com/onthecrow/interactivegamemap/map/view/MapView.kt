package com.onthecrow.interactivegamemap.map.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.onthecrow.interactivegamemap.map.model.MapLocation

@Composable
internal expect fun MapView(
    locations: List<MapLocation>,
    modifier: Modifier = Modifier,
)
