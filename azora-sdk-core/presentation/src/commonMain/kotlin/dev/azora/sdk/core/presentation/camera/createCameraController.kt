package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable

/**
 * Creates and remembers a platform-specific [CameraController] instance.
 *
 * This factory function handles platform-specific initialization.
 *
 * The controller is remembered across recompositions to maintain camera state.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun CameraScreen() {
 *     val cameraController = createCameraController()
 *
 *     DisposableEffect(Unit) {
 *         cameraController.startPreview()
 *         onDispose {
 *             cameraController.stopPreview()
 *         }
 *     }
 *
 *     CameraView(cameraController)
 * }
 * ```
 *
 * @return A remembered [CameraController] instance ready to be used with [CameraView].
 *
 * @see CameraController The controller class for camera operations
 * @see CameraView Composable to display the camera preview
 */
@Composable
expect fun createCameraController(): CameraController