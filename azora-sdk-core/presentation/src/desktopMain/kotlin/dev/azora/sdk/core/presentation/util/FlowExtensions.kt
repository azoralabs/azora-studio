package dev.azora.sdk.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Desktop implementation using regular collectAsState.
 * No lifecycle awareness needed on desktop.
 */
@Composable
actual fun <T> Flow<T>.collectAsSWLC(
    initialValue: T
): State<T> = collectAsState(initial = initialValue)

/**
 * Desktop implementation using regular collectAsState.
 * No lifecycle awareness needed on desktop.
 */
@Composable
actual fun <T> StateFlow<T>.collectAsSWLC(): State<T> =
    collectAsState()
