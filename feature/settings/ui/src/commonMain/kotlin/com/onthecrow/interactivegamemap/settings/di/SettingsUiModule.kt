package com.onthecrow.interactivegamemap.settings.di

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onthecrow.interactivegamemap.settings.LegalNotice
import com.onthecrow.interactivegamemap.settings.PrivacyPolicyUrl
import com.onthecrow.interactivegamemap.settings.SettingsCrashKeys
import com.onthecrow.interactivegamemap.settings.SettingsDestination
import com.onthecrow.interactivegamemap.settings.SettingsReducer
import com.onthecrow.interactivegamemap.settings.SettingsScreen
import com.onthecrow.interactivegamemap.settings.SettingsViewModel
import com.onthecrow.nomadrates.navigation.registerScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsUiModule = module {
    single { SettingsReducer() }
    // At the launch, so the crash reports have the settings from the start
    single(createdAtStart = true) { SettingsCrashKeys(get(), get()) }
    viewModelOf(::SettingsViewModel)

    registerScreen<SettingsDestination> { _, modifier ->
        val viewModel: SettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        SettingsScreen(
            modifier = modifier,
            state = state,
            // Given by the app: every app has its own policy
            privacyPolicyUrl = koinInject<PrivacyPolicyUrl>().value,
            onEvent = viewModel::onEvent,
            legalNotice = koinInject<LegalNotice>().lines,
        )
    }
}
