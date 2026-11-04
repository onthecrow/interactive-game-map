package com.onthecrow.interactivegamemap.settings

import com.onthecrow.interactivegamemap.analytics.AnalyticsSetting
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// The language when it follows the system, the only setting that can be null
private const val SYSTEM_LANGUAGE = "system"

/** The settings as the analytics and the crash reports tell them. */
internal val AppSettings.analyticsValues: Map<AnalyticsSetting, Any>
    get() = mapOf(
        AnalyticsSetting.HIDE_MARKERS_WHILE_MOVING to hideMarkersWhileMoving,
        AnalyticsSetting.CLUSTER_MARKERS to clusterMarkers,
        AnalyticsSetting.THEME to theme,
        AnalyticsSetting.DYNAMIC_COLOR to dynamicColor,
        AnalyticsSetting.LANGUAGE to (language ?: SYSTEM_LANGUAGE),
    )

/** Keeps the settings in the keys of the crash reports: setting_theme and so on. */
internal class SettingsCrashKeys(settingsRepository: SettingsRepository, crashReporter: CrashReporter) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        settingsRepository.settings
            .onEach { settings ->
                crashReporter.setKeys(settings.analyticsValues.mapKeys { (setting, _) -> "setting_${setting.name.lowercase()}" })
            }
            .launchIn(scope)
    }
}
