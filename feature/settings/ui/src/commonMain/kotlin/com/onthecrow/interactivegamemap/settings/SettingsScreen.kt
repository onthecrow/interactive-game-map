package com.onthecrow.interactivegamemap.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.settings.view.AppVersion
import com.onthecrow.interactivegamemap.settings.view.ChoiceDialog
import com.onthecrow.interactivegamemap.settings.view.SignatureFooter
import com.onthecrow.interactivegamemap.settings.view.plainTextClipEntry
import com.onthecrow.interactivegamemap.ui.components.GameScreenScaffold
import com.onthecrow.interactivegamemap.ui.components.GameSectionHeader
import com.onthecrow.interactivegamemap.ui.components.GameSettingsGroup
import com.onthecrow.interactivegamemap.ui.components.GameSettingsRow
import com.onthecrow.interactivegamemap.ui.components.SettingAction
import com.onthecrow.interactivegamemap.ui.isDynamicColorSupported
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.settings.ui.generated.resources.Res
import interactivegamemap.feature.settings.ui.generated.resources.settings_analytics_id
import interactivegamemap.feature.settings.ui.generated.resources.settings_analytics_id_copied
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

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

// How long a copied id says so
private val CopiedDuration = 2.seconds

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    privacyPolicyUrl: String,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
    legalNotice: List<String> = emptyList(),
) {
    GameScreenScaffold(
        modifier = modifier,
        title = stringResource(Res.string.settings_title),
        onBackClick = { onEvent(SettingsEvent.OnBackClick) },
        backContentDescription = stringResource(Res.string.settings_back),
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
                PrivacySection(state.analyticsId, privacyPolicyUrl)
                Spacer(modifier = Modifier.weight(1f))
                SignatureFooter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                )
                AppVersion(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = if (legalNotice.isEmpty()) 16.dp else 0.dp),
                )
                if (legalNotice.isNotEmpty()) {
                    LegalNoticeText(
                        lines = legalNotice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                    )
                }
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
    GameSectionHeader(text = stringResource(Res.string.settings_section_map))
    GameSettingsGroup {
        item {
            GameSettingsRow(
                title = stringResource(Res.string.settings_hide_markers),
                summary = stringResource(Res.string.settings_hide_markers_summary),
                action = SettingAction.Switch(settings.hideMarkersWhileMoving) {
                    onEvent(SettingsEvent.OnHideMarkersWhileMovingChange(it))
                },
            )
        }
        item {
            GameSettingsRow(
                title = stringResource(Res.string.settings_cluster_markers),
                summary = stringResource(Res.string.settings_cluster_markers_summary),
                action = SettingAction.Switch(settings.clusterMarkers) {
                    onEvent(SettingsEvent.OnClusterMarkersChange(it))
                },
            )
        }
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    GameSectionHeader(text = stringResource(Res.string.settings_section_appearance))
    // The system's colors exist from Android 12: from the wallpaper or picked by the user. A game with
    // colors of its own doesn't follow them
    val hasSystemColors = isDynamicColorSupported && GameMapTheme.supportsDynamicColor
    val themeLabel = stringResource(settings.theme.labelRes)
    val systemColors = stringResource(Res.string.settings_system_colors)
    val systemColorsSummary = stringResource(Res.string.settings_system_colors_summary)
    GameSettingsGroup {
        item {
            GameSettingsRow(
                title = stringResource(Res.string.settings_theme),
                action = SettingAction.Choice(themeLabel) { onEvent(SettingsEvent.OnThemeClick) },
            )
        }
        if (hasSystemColors) {
            item {
                GameSettingsRow(
                    title = systemColors,
                    summary = systemColorsSummary,
                    action = SettingAction.Switch(settings.dynamicColor) {
                        onEvent(SettingsEvent.OnDynamicColorChange(it))
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    GameSectionHeader(text = stringResource(Res.string.settings_section_language))
    val language = state.settings.language?.takeIf { it in SUPPORTED_LANGUAGES }
    val value = language?.languageName
        ?: stringResource(Res.string.settings_language_system_summary, state.systemLanguage.languageName)
    GameSettingsGroup {
        item {
            GameSettingsRow(
                title = stringResource(Res.string.settings_language),
                action = SettingAction.Choice(value) { onEvent(SettingsEvent.OnLanguageClick) },
            )
        }
    }
}

@Composable
private fun PrivacySection(analyticsId: String, privacyPolicyUrl: String) {
    GameSectionHeader(text = stringResource(Res.string.settings_section_privacy))
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(CopiedDuration)
            isCopied = false
        }
    }
    GameSettingsGroup {
        item {
            GameSettingsRow(
                title = stringResource(Res.string.settings_privacy_policy),
                // A device with nothing to open the link with keeps the screen
                action = SettingAction.Link { runCatching { uriHandler.openUri(privacyPolicyUrl) } },
            )
        }
        item {
            GameSettingsRow(
                title = stringResource(Res.string.settings_analytics_id),
                summary = if (isCopied) stringResource(Res.string.settings_analytics_id_copied) else analyticsId,
                action = SettingAction.Copy {
                    scope.launch { clipboard.setClipEntry(plainTextClipEntry(analyticsId)) }
                    isCopied = true
                },
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

@Composable
private fun LegalNoticeText(lines: List<String>, modifier: Modifier = Modifier) {
    Text(
        text = lines.joinToString("\n"),
        style = GameMapTheme.typography.caption,
        color = GameMapTheme.colors.contentSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
