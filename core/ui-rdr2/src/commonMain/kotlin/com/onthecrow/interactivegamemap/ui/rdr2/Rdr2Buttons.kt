package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.components.ButtonComponent
import com.onthecrow.interactivegamemap.ui.components.ButtonKind
import com.onthecrow.interactivegamemap.ui.components.ButtonSize
import com.onthecrow.interactivegamemap.ui.components.IconButtonComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonGlyph
import com.onthecrow.interactivegamemap.ui.rdr2.resources.Res
import com.onthecrow.interactivegamemap.ui.rdr2.resources.ic_check
import org.jetbrains.compose.resources.vectorResource

/** A filled button, a tonal one with a check once done. */
object Rdr2Button : ButtonComponent {
    @Composable
    override fun Content(
        text: String,
        onClick: () -> Unit,
        kind: ButtonKind,
        size: ButtonSize,
        enabled: Boolean,
        modifier: Modifier,
    ) {
        when (kind) {
            ButtonKind.Confirm -> Button(modifier = modifier, onClick = onClick, enabled = enabled) {
                Text(text)
            }
            ButtonKind.Done -> FilledTonalButton(modifier = modifier, onClick = onClick, enabled = enabled) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = vectorResource(Res.drawable.ic_check),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text)
            }
            ButtonKind.Cancel -> OutlinedButton(modifier = modifier, onClick = onClick, enabled = enabled) {
                Text(text)
            }
        }
    }
}

object Rdr2TextButton : TextButtonComponent {
    @Composable
    override fun Content(text: String, onClick: () -> Unit, glyph: TextButtonGlyph, modifier: Modifier) {
        TextButton(modifier = modifier, onClick = onClick) {
            Text(text)
        }
    }
}

object Rdr2IconButton : IconButtonComponent {
    @Composable
    override fun Content(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
