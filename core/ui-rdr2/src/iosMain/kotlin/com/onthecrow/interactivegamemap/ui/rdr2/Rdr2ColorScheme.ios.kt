package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun rdr2ColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    return if (darkTheme) DarkColorScheme else LightColorScheme
}
