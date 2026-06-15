package dev.azora.sdk.core.presentation.permissions

import androidx.compose.runtime.Composable

/**
 * Creates and remembers a platform-specific [PermissionController] instance.
 *
 * This composable handles platform-specific setup for permission management.
 *
 * The controller is remembered across recompositions to maintain consistent
 * permission request behavior.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val permissionController = rememberPermissionController()
 *
 *     Button(onClick = {
 *         scope.launch {
 *             val state = permissionController.requestPermission(Permission.LOCATION)
 *             // Handle permission result
 *         }
 *     }) {
 *         Text("Request Location")
 *     }
 * }
 * ```
 *
 * @return A remembered [PermissionController] ready for permission requests
 *
 * @see PermissionController The controller class for permission operations
 * @see Permission Available permission types
 * @see PermissionState Possible permission states
 */
@Composable
expect fun rememberPermissionController(): PermissionController