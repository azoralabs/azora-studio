package dev.azora.sdk.core.presentation.messaging

import androidx.compose.runtime.*

/**
 * Desktop needs no special initialization.
 */
@Composable
actual fun createMessagingController() = remember { MessagingController() }