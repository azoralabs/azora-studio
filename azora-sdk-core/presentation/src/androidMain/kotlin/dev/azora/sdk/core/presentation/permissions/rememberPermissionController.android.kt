package dev.azora.sdk.core.presentation.permissions

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.azora.sdk.core.presentation.util.findActivity

/**
 * Android implementation of [rememberPermissionController]; binds the current [ComponentActivity].
 */
@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    return remember {
        PermissionController().apply { attach(context.findActivity() as? ComponentActivity) }
    }
}
