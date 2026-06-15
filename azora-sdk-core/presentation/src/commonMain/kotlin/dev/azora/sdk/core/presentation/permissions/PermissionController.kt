package dev.azora.sdk.core.presentation.permissions

/**
 * Platform-specific controller for requesting and managing runtime permissions.
 *
 * This expect class provides a unified API for permission handling across platforms.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun LocationFeature() {
 *     val permissionController = rememberPermissionController()
 *     val scope = rememberCoroutineScope()
 *
 *     LaunchedEffect(Unit) {
 *         val state = permissionController.requestPermission(Permission.LOCATION)
 *         when (state) {
 *             PermissionState.GRANTED -> { /* Access location */ }
 *             PermissionState.DENIED -> { /* Show rationale */ }
 *             PermissionState.PERMANENTLY_DENIED -> { /* Direct to settings */ }
 *             PermissionState.NOT_DETERMINED -> { /* Waiting for user */ }
 *         }
 *     }
 * }
 * ```
 *
 * @see Permission Available permissions that can be requested
 * @see PermissionState Possible states returned from permission requests
 * @see rememberPermissionController Factory to create controller instances
 */
expect class PermissionController {

    /**
     * Requests a runtime permission from the user.
     *
     * This suspending function shows the platform's native permission dialog
     * and waits for the user's response. The returned [PermissionState] indicates
     * whether the permission was granted, denied, or permanently denied.
     *
     * @param permission The [Permission] to request from the user
     * @return The resulting [PermissionState] after user interaction
     */
    suspend fun requestPermission(permission: Permission): PermissionState
}