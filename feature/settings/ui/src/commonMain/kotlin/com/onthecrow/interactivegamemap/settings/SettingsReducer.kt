package com.onthecrow.interactivegamemap.settings

import com.onthecrow.nomadrates.uicore.Reducer

internal class SettingsReducer : Reducer<SettingsState, SettingsEvent> {
    override suspend fun reduce(state: SettingsState, event: SettingsEvent): SettingsState {
        return when (event) {
            // A changed setting comes back from the repository as the new settings
            is SettingsEvent.OnSettingsChanged -> state.copy(settings = event.settings)
            is SettingsEvent.OnSystemLanguageChanged -> state.copy(systemLanguage = event.language)
            SettingsEvent.OnThemeClick -> state.copy(dialog = SettingsDialog.THEME)
            SettingsEvent.OnLanguageClick -> state.copy(dialog = SettingsDialog.LANGUAGE)
            is SettingsEvent.OnThemeSelect,
            is SettingsEvent.OnLanguageSelect,
            SettingsEvent.OnDialogDismiss -> state.copy(dialog = null)
            else -> state
        }
    }
}
