package dev.azora.sdk.core.presentation.util

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

/**
 * Desktop implementation of event observer.
 * Simplified version without lifecycle dependencies since those are Android-specific.
 * Events are collected while the composable is active.
 */
@Composable
actual fun<T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any?,
    key2: Any?,
    onEvent: (T) -> Unit
) {
    LaunchedEffect(key1, key2) {
        withContext(Dispatchers.Main.immediate) {
            flow.collect(onEvent)
        }
    }
}
