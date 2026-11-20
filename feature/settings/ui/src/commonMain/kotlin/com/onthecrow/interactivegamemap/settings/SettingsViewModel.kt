package com.onthecrow.interactivegamemap.settings

import androidx.lifecycle.viewModelScope
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsId
import com.onthecrow.interactivegamemap.analytics.FoundMarkersTransferEvent
import com.onthecrow.interactivegamemap.analytics.SettingChangeEvent
import com.onthecrow.interactivegamemap.analytics.TransferResult
import com.onthecrow.interactivegamemap.analytics.TransferType
import com.onthecrow.interactivegamemap.map.ExportFoundLocationsUseCase
import com.onthecrow.interactivegamemap.map.FoundLocationsImport
import com.onthecrow.interactivegamemap.map.GetFoundLocationIdsUseCase
import com.onthecrow.interactivegamemap.map.ImportFoundLocationsUseCase
import com.onthecrow.nomadrates.navigation.Navigator
import com.onthecrow.nomadrates.uicore.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// How long the result of an export or an import is shown
private val TransferMessageDuration = 4.seconds

/** Every setting is written to [SettingsRepository] and comes back from it. */
internal class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    languageProvider: AppLanguageProvider,
    private val analytics: Analytics,
    analyticsId: AnalyticsId,
    getFoundLocationIdsUseCase: GetFoundLocationIdsUseCase,
    private val exportFoundLocationsUseCase: ExportFoundLocationsUseCase,
    private val importFoundLocationsUseCase: ImportFoundLocationsUseCase,
    reducer: SettingsReducer,
) : BaseViewModel<SettingsEvent, SettingsState, SettingsReducer>(reducer) {
    private var messageJob: Job? = null

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
                SettingsEvent.OnExportClick -> viewModelScope.launch {
                    onEvent(SettingsEvent.OnExportReady(exportFoundLocationsUseCase()))
                }
                is SettingsEvent.OnExportSaved -> onExportSaved(event.isSaved)
                is SettingsEvent.OnImportFileRead -> viewModelScope.launch { import(event.content) }
                else -> Unit
            }
        }.launchIn(viewModelScope)

        settingsRepository.settings
            .onEach { onEvent(SettingsEvent.OnSettingsChanged(it)) }
            .launchIn(viewModelScope)
        languageProvider.systemLanguage
            .onEach { onEvent(SettingsEvent.OnSystemLanguageChanged(it)) }
            .launchIn(viewModelScope)
        getFoundLocationIdsUseCase()
            .onEach { onEvent(SettingsEvent.OnFoundCountChanged(it.size)) }
            .launchIn(viewModelScope)
        // The initial state is made before the values of the constructor are set, the id comes as an event too
        viewModelScope.launch { onEvent(SettingsEvent.OnAnalyticsIdLoaded(analyticsId.value)) }
    }

    private fun onExportSaved(isSaved: Boolean) {
        val count = state.value.foundCount
        show(if (isSaved) TransferMessage.Exported(count) else TransferMessage.ExportFailed)
        analytics.log(
            FoundMarkersTransferEvent(
                type = TransferType.EXPORT,
                result = if (isSaved) TransferResult.DONE else TransferResult.FAILED,
                markersCount = count,
            )
        )
    }

    private suspend fun import(content: String) {
        val result = importFoundLocationsUseCase(content)
        show(
            when (result) {
                is FoundLocationsImport.Done -> TransferMessage.Imported(result.added, result.total)
                FoundLocationsImport.NotAFile -> TransferMessage.NotAFile
                is FoundLocationsImport.OtherGame -> TransferMessage.OtherGame
            }
        )
        analytics.log(
            FoundMarkersTransferEvent(
                type = TransferType.IMPORT,
                result = when (result) {
                    is FoundLocationsImport.Done -> TransferResult.DONE
                    FoundLocationsImport.NotAFile -> TransferResult.NOT_A_FILE
                    is FoundLocationsImport.OtherGame -> TransferResult.OTHER_GAME
                },
                markersCount = (result as? FoundLocationsImport.Done)?.added ?: 0,
            )
        )
    }

    private fun show(message: TransferMessage) {
        messageJob?.cancel()
        onEvent(SettingsEvent.OnTransferMessage(message))
        messageJob = viewModelScope.launch {
            delay(TransferMessageDuration)
            onEvent(SettingsEvent.OnTransferMessage(null))
        }
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
