package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import interactivegamemap.feature.settings.ui.generated.resources.Res
import interactivegamemap.feature.settings.ui.generated.resources.settings_version
import org.jetbrains.compose.resources.stringResource

/** The version name of the app, null where it can't be read, like in the previews. */
@Composable
internal expect fun rememberAppVersion(): String?

/** The app's version, under the author's card. */
@Composable
internal fun AppVersion(modifier: Modifier = Modifier) {
    val version = rememberAppVersion() ?: return
    Text(
        text = stringResource(Res.string.settings_version, version),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
