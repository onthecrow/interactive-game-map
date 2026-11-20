package com.onthecrow.interactivegamemap.settings

import com.onthecrow.nomadrates.uicore.Reducer

internal class SettingsReducer : Reducer<SettingsState, SettingsEvent> {
    override suspend fun reduce(state: SettingsState, event: SettingsEvent): SettingsState {
        return when (event) {
            // A changed setting comes back from the repository as the new settings
            is SettingsEvent.OnSettingsChanged -> state.copy(settings = event.settings)
            is SettingsEvent.OnSystemLanguageChanged -> state.copy(systemLanguage = event.language)
            is SettingsEvent.OnAnalyticsIdLoaded -> state.copy(analyticsId = event.id)
            is SettingsEvent.OnFoundCountChanged -> state.copy(foundCount = event.count)
            is SettingsEvent.OnExportReady -> state.copy(pendingExport = event.export)
            SettingsEvent.OnExportHandedOver -> state.copy(pendingExport = null)
            is SettingsEvent.OnTransferMessage -> state.copy(transferMessage = event.message)
            SettingsEvent.OnThemeClick -> state.copy(dialog = SettingsDialog.THEME)
            SettingsEvent.OnMapStyleClick -> state.copy(dialog = SettingsDialog.MAP_STYLE)
            SettingsEvent.OnLanguageClick -> state.copy(dialog = SettingsDialog.LANGUAGE)
            is SettingsEvent.OnThemeSelect,
            is SettingsEvent.OnMapStyleSelect,
            is SettingsEvent.OnLanguageSelect,
            SettingsEvent.OnDialogDismiss -> state.copy(dialog = null)
            else -> state
        }
    }
}
