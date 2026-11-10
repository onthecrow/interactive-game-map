package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import com.onthecrow.interactivegamemap.ui.resources.Res
import com.onthecrow.interactivegamemap.ui.resources.content_loading
import com.onthecrow.interactivegamemap.ui.resources.content_loading_failed
import com.onthecrow.interactivegamemap.ui.resources.content_retry
import com.onthecrow.interactivegamemap.ui.resources.content_updated
import com.onthecrow.interactivegamemap.ui.resources.content_updating
import org.jetbrains.compose.resources.stringResource

// As the splash screen of Android 12 shows an icon without a background, centered
private val SplashIconSize = 288.dp

// Under the visible disc of the icon, which is 160 dp of the 288
private val UnderIconOffset = 104.dp

/**
 * The screen while the app has no data to show yet: the icon of the splash screen in its place, and under it the
 * progress of the first load, or why it failed with a retry.
 *
 * @param progress from 0 to 1, null while unknown
 */
@Composable
fun ContentPlaceholder(
    failed: Boolean,
    progress: Float?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GameMapTheme.colors.background),
    ) {
        Image(
            painter = GameMapTheme.appIcon(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(SplashIconSize),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight / 2 + UnderIconOffset)
                .widthIn(max = 320.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (failed) Res.string.content_loading_failed else Res.string.content_loading),
                style = GameMapTheme.typography.body,
                color = GameMapTheme.colors.content,
                textAlign = TextAlign.Center,
            )
            if (failed) {
                GameButton(
                    text = stringResource(Res.string.content_retry),
                    onClick = onRetry,
                    kind = ButtonKind.Confirm,
                    size = ButtonSize.Small,
                )
            } else {
                GameProgressIndicator(progress = progress, modifier = Modifier.width(200.dp))
            }
        }
    }
}

/**
 * A panel at the top over every screen while the data is updated; [done] says it's finished. A tap hides it, the
 * update goes on.
 *
 * @param progress from 0 to 1, null while unknown
 */
@Composable
fun UpdateToast(
    visible: Boolean,
    progress: Float?,
    done: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            GamePanel(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            ) {
                // The panel of the game pads and spaces its content
                Text(
                    text = stringResource(if (done) Res.string.content_updated else Res.string.content_updating),
                    style = GameMapTheme.typography.title,
                    color = GameMapTheme.colors.content,
                )
                GameProgressIndicator(
                    progress = if (done) 1f else progress,
                    modifier = Modifier.width(240.dp),
                )
            }
        }
    }
}
