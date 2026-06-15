package dev.azora.sdk.core.presentation.messaging

import androidx.compose.runtime.*

/**
 * Creates a platform-specific [MessagingController] instance.
 *
 * @return platform-initialized [MessagingController]
 */
@Composable
expect fun createMessagingController(): MessagingController