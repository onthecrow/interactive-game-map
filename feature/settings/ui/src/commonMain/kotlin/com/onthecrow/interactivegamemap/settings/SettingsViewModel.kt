package com.onthecrow.interactivegamemap.settings

import androidx.lifecycle.viewModelScope
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsId
import com.onthecrow.interactivegamemap.analytics.SettingChangeEvent
import com.onthecrow.nomadrates.navigation.Navigator
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Every setting is written to [SettingsRepository] and comes back from it. */
internal class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    languageProvider: AppLanguageProvider,
    private val analytics: Analytics,
    analyticsId: AnalyticsId,
    reducer: SettingsReducer,
) : BaseViewModel<SettingsEvent, SettingsState, SettingsReducer>(reducer) {

    override val initialState: SettingsState get() = SettingsState()

    init {
        eventFlow.onEach { event ->
            when (event) {
                SettingsEvent.OnBackClick -> navigator.navigateBack()
                is SettingsEvent.OnHideMarkersWhileMovingChange -> update { it.copy(hideMarkersWhileMoving = event.enabled) }
                is SettingsEvent.OnClusterMarkersChange -> update { it.copy(clusterMarkers = event.enabled) }
                is SettingsEvent.OnThemeSelect -> update { it.copy(theme = event.theme) }
                is SettingsEvent.OnDynamicColorChange -> update { it.copy(dynamicColor = event.enabled) }
                is SettingsEvent.OnLanguageSelect -> update { it.copy(language = event.language) }
                SettingsEvent.OnPrivacyPolicyClick -> {
                    // TODO: open the privacy policy when it's published
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)

        settingsRepository.settings
            .onEach { onEvent(SettingsEvent.OnSettingsChanged(it)) }
            .launchIn(viewModelScope)
        languageProvider.systemLanguage
            .onEach { onEvent(SettingsEvent.OnSystemLanguageChanged(it)) }
            .launchIn(viewModelScope)
        // The initial state is made before the values of the constructor are set, the id comes as an event too
        viewModelScope.launch { onEvent(SettingsEvent.OnAnalyticsIdLoaded(analyticsId.value)) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val old = settingsRepository.settings.value
        settingsRepository.update(transform)
        logChanges(old, transform(old))
    }

    /** A pick of the value a setting has isn't a change. */
    private fun logChanges(old: AppSettings, new: AppSettings) {
        val oldValues = old.analyticsValues
        new.analyticsValues.forEach { (setting, value) ->
            if (oldValues[setting] != value) analytics.log(SettingChangeEvent(setting, value))
        }
    }
}
