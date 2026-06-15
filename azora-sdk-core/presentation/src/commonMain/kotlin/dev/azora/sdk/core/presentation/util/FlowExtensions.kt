package dev.azora.sdk.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific collection of Flow as Compose State.
 *
 * - On mobile platforms (Android/iOS): Uses lifecycle-aware collection
 * - On desktop: Uses regular collectAsState
 *
 * This ensures the flow collection respects the component lifecycle on mobile
 * while providing a simpler implementation for desktop where lifecycle is not a concern.
 */
@Composable
expect fun <T> Flow<T>.collectAsSWLC(
    initialValue: T
): State<T>

/**
 * Platform-specific collection of StateFlow as Compose State.
 *
 * - On mobile platforms (Android/iOS): Uses lifecycle-aware collection
 * - On desktop: Uses regular collectAsState
 */
@Composable
expect fun <T> StateFlow<T>.collectAsSWLC(): State<T>