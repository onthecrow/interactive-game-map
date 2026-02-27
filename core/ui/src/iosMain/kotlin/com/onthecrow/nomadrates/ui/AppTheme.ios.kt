package com.onthecrow.nomadrates.ui

import androidx.compose.material3.MaterialTheme

@androidx.compose.runtime.Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @androidx.compose.runtime.Composable (() -> Unit)
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
