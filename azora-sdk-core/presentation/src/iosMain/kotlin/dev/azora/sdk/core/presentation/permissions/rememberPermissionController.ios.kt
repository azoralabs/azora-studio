package dev.azora.sdk.core.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [rememberPermissionController].
 */
@Composable
actual fun rememberPermissionController(): PermissionController = remember { PermissionController() }
