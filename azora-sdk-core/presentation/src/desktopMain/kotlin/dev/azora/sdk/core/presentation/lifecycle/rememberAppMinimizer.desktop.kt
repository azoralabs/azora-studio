package dev.azora.sdk.core.presentation.lifecycle

import androidx.compose.runtime.*
import androidx.compose.ui.window.*

/**
 * Desktop implementation of [rememberAppMinimizer].
 *
 * Creates an [AppMinimizer] and binds it to the current window's [WindowState].
 * Uses [LaunchedEffect] to update the window reference when it changes.
 *
 * ## Note
 * This implementation creates its own [WindowState] via [rememberWindowState].
 * For proper integration, ensure this is called within a [Window] composable
 * that uses the same window state.
 *
 * @return A remembered [AppMinimizer] configured with the current window state
 */
@Composable
actual fun rememberAppMinimizer(): AppMinimizer {
    val windowState = rememberWindowState()
    val minimizer = remember { AppMinimizer() }
    LaunchedEffect(windowState) {
        minimizer.setWindow(windowState)
    }
    return minimizer
}