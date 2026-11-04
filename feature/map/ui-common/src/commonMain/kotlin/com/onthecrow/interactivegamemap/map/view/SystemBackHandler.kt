package com.onthecrow.interactivegamemap.map.view

import androidx.compose.runtime.Composable

/** Handles the system back on the platforms which have it. */
@Composable
internal expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
