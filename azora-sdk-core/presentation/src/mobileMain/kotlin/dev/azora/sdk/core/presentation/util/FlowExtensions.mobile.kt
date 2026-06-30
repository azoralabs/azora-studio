package dev.azora.sdk.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mobile (Android/iOS) implementation of [collectAsSWLC].
 *
 * Collects the flow as Compose state. The shared Compose runtime pauses collection when the
 * composition leaves the screen, so a plain [collectAsState] is sufficient here.
 */
@Composable
actual fun <T> Flow<T>.collectAsSWLC(
    initialValue: T
): State<T> = collectAsState(initial = initialValue)

/**
 * Mobile (Android/iOS) implementation of [collectAsSWLC] for [StateFlow].
 */
@Composable
actual fun <T> StateFlow<T>.collectAsSWLC(): State<T> = collectAsState()
