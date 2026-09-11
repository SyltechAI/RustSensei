package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.Spacing

/**
 * Consent gate for the two features that leave the device.
 *
 * Compiling and running tests post the user's Rust source to the public Rust
 * Playground at play.rust-lang.org. Everything else in RustSensei is local and
 * the app says so loudly, so sending source to a third party needs an explicit,
 * remembered opt-in rather than a footnote under the output.
 *
 * Returns the action to wire to the button, plus a composable that must be
 * placed in the tree to host the dialog.
 */
@Composable
fun rememberNetworkComputeGate(
    hasAccepted: () -> Boolean,
    onAccepted: () -> Unit,
    action: () -> Unit
): Pair<() -> Unit, @Composable () -> Unit> {
    var pending by remember { mutableStateOf(false) }
    val currentAction by rememberUpdatedState(action)
    val currentAccepted by rememberUpdatedState(onAccepted)
    val currentHasAccepted by rememberUpdatedState(hasAccepted)

    val request: () -> Unit = {
        if (currentHasAccepted()) currentAction() else pending = true
    }

    val dialog: @Composable () -> Unit = {
        if (pending) {
            NetworkComputeConsentDialog(
                onAccept = {
                    currentAccepted()
                    pending = false
                    currentAction()
                },
                onDismiss = { pending = false }
            )
        }
    }
    return request to dialog
}

@Composable
fun NetworkComputeConsentDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Dimens.CardRadius),
        title = {
            Text(
                text = stringResource(R.string.remote_compile_consent_title),
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.remote_compile_consent_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Spacing.MD))
                Text(
                    text = stringResource(R.string.remote_compile_consent_scope),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.remote_compile_consent_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.remote_compile_consent_cancel))
            }
        }
    )
}
