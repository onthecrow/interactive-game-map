package com.onthecrow.nomadrates.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface NavigationProvider {
    /**
     * @param contentKey the screens are recreated when it changes, the back stack stays, e.g. the language
     */
    @Composable
    fun Navigation(modifier: Modifier, contentKey: Any?)
}
