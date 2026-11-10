package com.onthecrow.interactivegamemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.onthecrow.interactivegamemap.settings.AppLanguageProvider
import com.onthecrow.interactivegamemap.settings.SettingsRepository
import com.onthecrow.interactivegamemap.ui.AppEnvironment
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import com.onthecrow.nomadrates.navigation.NavigationProvider
import com.onthecrow.interactivegamemap.ui.view.ShadePosition
import com.onthecrow.interactivegamemap.ui.view.SystemBarShadeView
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val languageProvider = koinInject<AppLanguageProvider>()
    val settings by settingsRepository.settings.collectAsState()
    val language by languageProvider.language.collectAsState(languageProvider.currentLanguage)

    // Android recreates the activity when the system's languages change
    LaunchedEffect(languageProvider) {
        languageProvider.refreshSystemLanguages()
    }

    AppEnvironment(language = language, isDarkTheme = settings.theme.isDark) {
        GameMapTheme(dynamicColor = settings.dynamicColor) {
            val navigationProvider = getKoin().get<NavigationProvider>()

            Box(
                modifier = Modifier.fillMaxSize()
                    .background(GameMapTheme.colors.background),
            ) {
                // The screens wait for the data of the game, its updates come in a toast over them
                SyncedContent(synchronizer = koinInject()) {
                    navigationProvider.Navigation(modifier = Modifier.fillMaxSize(), contentKey = language)
                }

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
}
