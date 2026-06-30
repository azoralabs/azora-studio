package dev.azora.sdk.core.presentation.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [rememberAppMinimizer].
 */
@Composable
actual fun rememberAppMinimizer(): AppMinimizer = remember { AppMinimizer() }
