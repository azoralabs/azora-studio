package dev.azora.sdk.core.presentation.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [createMessagingController]; needs no special initialization.
 */
@Composable
actual fun createMessagingController(): MessagingController = remember { MessagingController() }
