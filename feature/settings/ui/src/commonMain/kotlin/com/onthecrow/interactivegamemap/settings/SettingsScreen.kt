@file:OptIn(ExperimentalMaterial3Api::class)

package com.onthecrow.interactivegamemap.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.settings.view.AppVersion
import com.onthecrow.interactivegamemap.settings.view.ChoiceDialog
import com.onthecrow.interactivegamemap.settings.view.ClickableItem
import com.onthecrow.interactivegamemap.settings.view.SectionHeader
import com.onthecrow.interactivegamemap.settings.view.SettingsGroup
import com.onthecrow.interactivegamemap.settings.view.SignatureFooter
import com.onthecrow.interactivegamemap.settings.view.SwitchItem
import com.onthecrow.interactivegamemap.ui.AppTheme
import com.onthecrow.interactivegamemap.ui.isDynamicColorSupported
import interactivegamemap.feature.settings.ui.generated.resources.Res
import interactivegamemap.feature.settings.ui.generated.resources.ic_arrow_back
import interactivegamemap.feature.settings.ui.generated.resources.settings_back
import interactivegamemap.feature.settings.ui.generated.resources.settings_cluster_markers
import interactivegamemap.feature.settings.ui.generated.resources.settings_cluster_markers_summary
import interactivegamemap.feature.settings.ui.generated.resources.settings_hide_markers
import interactivegamemap.feature.settings.ui.generated.resources.settings_hide_markers_summary
import interactivegamemap.feature.settings.ui.generated.resources.settings_language
import interactivegamemap.feature.settings.ui.generated.resources.settings_language_system
import interactivegamemap.feature.settings.ui.generated.resources.settings_language_system_summary
import interactivegamemap.feature.settings.ui.generated.resources.settings_privacy_policy
import interactivegamemap.feature.settings.ui.generated.resources.settings_section_appearance
import interactivegamemap.feature.settings.ui.generated.resources.settings_section_language
import interactivegamemap.feature.settings.ui.generated.resources.settings_section_map
import interactivegamemap.feature.settings.ui.generated.resources.settings_section_privacy
import interactivegamemap.feature.settings.ui.generated.resources.settings_system_colors
import interactivegamemap.feature.settings.ui.generated.resources.settings_system_colors_summary
import interactivegamemap.feature.settings.ui.generated.resources.settings_theme
import interactivegamemap.feature.settings.ui.generated.resources.settings_theme_dark
import interactivegamemap.feature.settings.ui.generated.resources.settings_theme_light
import interactivegamemap.feature.settings.ui.generated.resources.settings_theme_system
import interactivegamemap.feature.settings.ui.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

// Each language is named in itself, so it can be found whatever the current one is
private val LanguageNames = mapOf(
    "en" to "English",
    "de" to "Deutsch",
    "es" to "Español",
    "fr" to "Français",
    "ja" to "日本語",
    "ko" to "한국어",
    "pt" to "Português",
    "ru" to "Русский",
    "zh" to "简体中文",
)

private val String.languageName: String
    get() = LanguageNames[this] ?: this

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The color of the screen is the one of the shades under the system bars
    val containerColor = MaterialTheme.colorScheme.background
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = containerColor,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SettingsEvent.OnBackClick) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.settings_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        // At least a screen tall, so the footer sits at the bottom, a longer list scrolls it along
        BoxWithConstraints(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
            ) {
                MapSection(state.settings, onEvent)
                AppearanceSection(state.settings, onEvent)
                LanguageSection(state, onEvent)
                PrivacySection(onEvent)
                Spacer(modifier = Modifier.weight(1f))
                SignatureFooter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                )
                AppVersion(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                )
            }
        }
    }

    when (state.dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(Res.string.settings_theme),
            options = ThemeMode.entries,
            selected = state.settings.theme,
            label = { stringResource(it.labelRes) },
            description = { null },
            onSelect = { onEvent(SettingsEvent.OnThemeSelect(it)) },
            onDismiss = { onEvent(SettingsEvent.OnDialogDismiss) },
        )
        SettingsDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(Res.string.settings_language),
            // Null follows the system
            options = listOf(null) + SUPPORTED_LANGUAGES.sortedBy { it.languageName },
            selected = state.settings.language?.takeIf { it in SUPPORTED_LANGUAGES },
            label = { it?.languageName ?: stringResource(Res.string.settings_language_system) },
            description = { if (it == null) state.systemLanguage.languageName else null },
            onSelect = { onEvent(SettingsEvent.OnLanguageSelect(it)) },
            onDismiss = { onEvent(SettingsEvent.OnDialogDismiss) },
        )
        null -> Unit
    }
}

@Composable
private fun MapSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(text = stringResource(Res.string.settings_section_map))
    SettingsGroup {
        item {
            SwitchItem(
                title = stringResource(Res.string.settings_hide_markers),
                summary = stringResource(Res.string.settings_hide_markers_summary),
                checked = settings.hideMarkersWhileMoving,
                onCheckedChange = { onEvent(SettingsEvent.OnHideMarkersWhileMovingChange(it)) },
            )
        }
        item {
            SwitchItem(
                title = stringResource(Res.string.settings_cluster_markers),
                summary = stringResource(Res.string.settings_cluster_markers_summary),
                checked = settings.clusterMarkers,
                onCheckedChange = { onEvent(SettingsEvent.OnClusterMarkersChange(it)) },
            )
        }
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(text = stringResource(Res.string.settings_section_appearance))
    SettingsGroup {
        item {
            ClickableItem(
                title = stringResource(Res.string.settings_theme),
                summary = stringResource(settings.theme.labelRes),
                onClick = { onEvent(SettingsEvent.OnThemeClick) },
            )
        }
        // The system's colors exist from Android 12: from the wallpaper or picked by the user
        if (isDynamicColorSupported) {
            item {
                SwitchItem(
                    title = stringResource(Res.string.settings_system_colors),
                    summary = stringResource(Res.string.settings_system_colors_summary),
                    checked = settings.dynamicColor,
                    onCheckedChange = { onEvent(SettingsEvent.OnDynamicColorChange(it)) },
                )
            }
        }
    }
}

@Composable
private fun LanguageSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(text = stringResource(Res.string.settings_section_language))
    SettingsGroup {
        item {
            val language = state.settings.language?.takeIf { it in SUPPORTED_LANGUAGES }
            ClickableItem(
                title = stringResource(Res.string.settings_language),
                summary = language?.languageName
                    ?: stringResource(Res.string.settings_language_system_summary, state.systemLanguage.languageName),
                onClick = { onEvent(SettingsEvent.OnLanguageClick) },
            )
        }
    }
}

@Composable
private fun PrivacySection(onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(text = stringResource(Res.string.settings_section_privacy))
    SettingsGroup {
        item {
            ClickableItem(
                title = stringResource(Res.string.settings_privacy_policy),
                onClick = { onEvent(SettingsEvent.OnPrivacyPolicyClick) },
            )
        }
    }
}

private val ThemeMode.labelRes: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.LIGHT -> Res.string.settings_theme_light
        ThemeMode.DARK -> Res.string.settings_theme_dark
    }

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(state = SettingsState(), onEvent = {})
    }
}
