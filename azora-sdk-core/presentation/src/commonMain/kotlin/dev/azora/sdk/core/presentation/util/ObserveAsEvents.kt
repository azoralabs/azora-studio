package dev.azora.sdk.core.presentation.util

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific event observer for Flow.
 *
 * @param flow The flow to observe for events
 * @param key1 Optional key to control when the effect is restarted
 * @param key2 Optional second key to control when the effect is restarted
 * @param onEvent Callback invoked for each event
 */
@Composable
expect fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
)