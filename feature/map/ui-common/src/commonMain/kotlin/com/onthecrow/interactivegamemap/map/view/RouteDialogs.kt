package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onthecrow.interactivegamemap.map.RouteBuildProgress
import com.onthecrow.interactivegamemap.map.model.RouteBuildStage
import com.onthecrow.interactivegamemap.ui.components.ButtonKind
import com.onthecrow.interactivegamemap.ui.components.ButtonSize
import com.onthecrow.interactivegamemap.ui.components.GameButton
import com.onthecrow.interactivegamemap.ui.components.GameDivider
import com.onthecrow.interactivegamemap.ui.components.GamePanel
import com.onthecrow.interactivegamemap.ui.components.GameProgressIndicator
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_cancel
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_count
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_drawing
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_improving
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_ordering
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_preparing
import interactivegamemap.feature.map.ui_common.generated.resources.route_build_title
import interactivegamemap.feature.map.ui_common.generated.resources.route_exit_confirm
import interactivegamemap.feature.map.ui_common.generated.resources.route_exit_dismiss
import interactivegamemap.feature.map.ui_common.generated.resources.route_exit_text
import interactivegamemap.feature.map.ui_common.generated.resources.route_exit_title
import org.jetbrains.compose.resources.stringResource

private val DialogMaxWidth = 360.dp
private val DialogMargin = 24.dp

/** The progress of the route being built; only Cancel closes it, the map stays usable once it's built. */
@Composable
internal fun RouteBuildDialog(progress: RouteBuildProgress, onCancel: () -> Unit) {
    RouteDialog(onDismissRequest = onCancel, dismissible = false) {
        Text(
            text = stringResource(Res.string.route_build_title),
            style = GameMapTheme.typography.title,
            color = GameMapTheme.colors.content,
        )
        GameDivider()
        Text(
            text = stringResource(progress.stage.text()) + " · " + stringResource(Res.string.route_build_count, progress.count),
            style = GameMapTheme.typography.body,
            color = GameMapTheme.colors.content,
        )
        GameProgressIndicator(progress = progress.progress, modifier = Modifier.fillMaxWidth())
        GameButton(
            modifier = Modifier.align(Alignment.End),
            text = stringResource(Res.string.route_build_cancel),
            kind = ButtonKind.Cancel,
            size = ButtonSize.Small,
            onClick = onCancel,
        )
    }
}

/**
 * Whether to end the route: it's dropped, the found marks stay. Keep going goes on with it, a tap outside or Back only
 * closes the dialog.
 */
@Composable
internal fun RouteExitDialog(onConfirm: () -> Unit, onContinue: () -> Unit, onDismiss: () -> Unit) {
    RouteDialog(onDismissRequest = onDismiss, dismissible = true) {
        Text(
            text = stringResource(Res.string.route_exit_title),
            style = GameMapTheme.typography.title,
            color = GameMapTheme.colors.content,
        )
        GameDivider()
        Text(
            text = stringResource(Res.string.route_exit_text),
            style = GameMapTheme.typography.body,
            color = GameMapTheme.colors.content,
        )
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameButton(
                text = stringResource(Res.string.route_exit_dismiss),
                kind = ButtonKind.Cancel,
                size = ButtonSize.Small,
                onClick = onContinue,
            )
            GameButton(
                text = stringResource(Res.string.route_exit_confirm),
                kind = ButtonKind.Confirm,
                size = ButtonSize.Small,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun RouteDialog(
    onDismissRequest: () -> Unit,
    dismissible: Boolean,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        GamePanel(
            modifier = Modifier.padding(DialogMargin).widthIn(max = DialogMaxWidth),
            content = content,
        )
    }
}

private fun RouteBuildStage.text() = when (this) {
    RouteBuildStage.PREPARING -> Res.string.route_build_preparing
    RouteBuildStage.ORDERING -> Res.string.route_build_ordering
    RouteBuildStage.IMPROVING -> Res.string.route_build_improving
    RouteBuildStage.DRAWING -> Res.string.route_build_drawing
}
