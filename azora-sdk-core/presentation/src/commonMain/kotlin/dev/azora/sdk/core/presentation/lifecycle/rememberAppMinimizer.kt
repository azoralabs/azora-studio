package dev.azora.sdk.core.presentation.lifecycle

import androidx.compose.runtime.Composable

/**
 * Creates and remembers a platform-specific [AppMinimizer] instance.
 *
 * This composable handles platform-specific initialization.
 *
 * The minimizer is remembered across recompositions to maintain a consistent reference.
 *
 * @return A remembered [AppMinimizer] ready for use
 */
@Composable
expect fun rememberAppMinimizer(): AppMinimizer