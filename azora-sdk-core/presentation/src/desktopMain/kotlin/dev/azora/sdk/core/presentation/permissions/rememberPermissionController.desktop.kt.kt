package dev.azora.sdk.core.presentation.permissions

import androidx.compose.runtime.*

/**
 * Desktop implementation of [rememberPermissionController].
 *
 * Creates a simple [PermissionController] instance that always grants permissions.
 * No special setup is required on desktop since there's no runtime permission model.
 *
 * ## Simplicity
 * Unlike mobile platforms that require:
 * - Platform-specific permission controllers
 * - Lifecycle binding for callbacks
 * - Native dialog integration
 *
 * Desktop only needs a simple remembered instance since all permissions
 * are effectively always granted.
 *
 * @return A remembered [PermissionController] that always grants permissions
 */
@Composable
actual fun rememberPermissionController() = remember { PermissionController() }