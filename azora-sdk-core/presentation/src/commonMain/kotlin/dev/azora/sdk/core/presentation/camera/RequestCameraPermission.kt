package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that handles camera permission requests.
 *
 * This composable checks for camera permission and requests it if necessary,
 * using platform-native permission APIs.
 *
 * The [body] content is always rendered regardless of permission state, allowing
 * the UI to show appropriate feedback while waiting for permission.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun CameraScreen() {
 *     var hasPermission by remember { mutableStateOf(false) }
 *
 *     RequestCameraPermission(
 *         onGranted = { hasPermission = true }
 *     ) {
 *         if (hasPermission) {
 *             // Show camera preview
 *             val cameraController = createCameraController()
 *             CameraView(cameraController)
 *         } else {
 *             // Show permission request UI or placeholder
 *             Text("Camera permission required")
 *         }
 *     }
 * }
 * ```
 *
 * @param onGranted Callback invoked when camera permission is granted.
 *                  Called immediately if permission was already granted,
 *                  or after user grants permission in the system dialog.
 * @param body The composable content to display. This is rendered regardless
 *             of permission state to allow showing appropriate UI feedback.
 *
 * @see CameraController Controller that requires camera permission to function
 * @see CameraView View that displays camera preview after permission is granted
 */
@Composable
expect fun RequestCameraPermission(
    onGranted: () -> Unit,
    body: @Composable () -> Unit
)