package dev.azora.sdk.core.presentation.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.azora.sdk.core.presentation.util.findActivity

/**
 * Android implementation of [rememberAppMinimizer]; binds the current activity.
 */
@Composable
actual fun rememberAppMinimizer(): AppMinimizer {
    val context = LocalContext.current
    return remember { AppMinimizer().apply { attach(context.findActivity()) } }
}
