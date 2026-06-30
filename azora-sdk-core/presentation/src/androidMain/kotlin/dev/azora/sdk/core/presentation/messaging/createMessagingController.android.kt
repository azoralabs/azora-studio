package dev.azora.sdk.core.presentation.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of [createMessagingController]; binds the current [LocalContext].
 */
@Composable
actual fun createMessagingController(): MessagingController {
    val context = LocalContext.current
    return remember { MessagingController().apply { attach(context) } }
}
