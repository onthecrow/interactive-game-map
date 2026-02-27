package com.onthecrow.interactivegamemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.onthecrow.nomadrates.navigation.NavigationProvider
import com.onthecrow.interactivegamemap.ui.AppTheme
import com.onthecrow.interactivegamemap.ui.view.ShadePosition
import com.onthecrow.interactivegamemap.ui.view.SystemBarShadeView
import org.koin.compose.getKoin

@Composable
@Preview
fun App() {
    AppTheme {
        val navigationProvider = getKoin().get<NavigationProvider>()

        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            navigationProvider.Navigation(modifier = Modifier.fillMaxSize())

            SystemBarShadeView(
                modifier = Modifier.fillMaxWidth(),
                position = ShadePosition.BOTTOM,
            )

            SystemBarShadeView(
                modifier = Modifier.fillMaxWidth(),
                position = ShadePosition.TOP,
            )
        }
    }
}