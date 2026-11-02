package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import interactivegamemap.feature.settings.ui.generated.resources.Res
import interactivegamemap.feature.settings.ui.generated.resources.ic_github
import interactivegamemap.feature.settings.ui.generated.resources.ic_instagram
import interactivegamemap.feature.settings.ui.generated.resources.ic_telegram
import interactivegamemap.feature.settings.ui.generated.resources.settings_footer_contact
import interactivegamemap.feature.settings.ui.generated.resources.settings_footer_signature
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private const val HANDLE = "@onthecrow"
private const val TELEGRAM_URL = "https://t.me/onthecrow"
private const val GITHUB_URL = "https://github.com/onthecrow"
private const val INSTAGRAM_URL = "https://www.instagram.com/onthecrow"

/** The author's card, as in Caffeine and Wallper: who made the app and where to reach the author. */
@Composable
internal fun SignatureFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val signature = stringResource(Res.string.settings_footer_signature)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Light)) {
                    append(signature)
                    append(' ')
                }
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(HANDLE)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Link(Res.drawable.ic_telegram, "Telegram", TELEGRAM_URL)
            Link(Res.drawable.ic_github, "GitHub", GITHUB_URL)
            Link(Res.drawable.ic_instagram, "Instagram", INSTAGRAM_URL)
        }
        Text(
            text = stringResource(Res.string.settings_footer_contact),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Link(icon: DrawableResource, name: String, url: String) {
    val uriHandler = LocalUriHandler.current
    IconButton(
        // A device with nothing to open the link with keeps the screen
        onClick = { runCatching { uriHandler.openUri(url) } },
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = name,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Preview
@Composable
private fun SignatureFooterPreview() {
    SignatureFooter()
}
