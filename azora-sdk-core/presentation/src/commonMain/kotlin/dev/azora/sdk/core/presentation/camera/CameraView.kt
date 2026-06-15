package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that displays the camera preview.
 *
 * This composable renders a live camera preview using the native camera view for each platform.
 *
 * ## Usage
 * ```kotlin
 * val cameraController = createCameraController()
 *
 * LaunchedEffect(Unit) {
 *     cameraController.startPreview()
 * }
 *
 * CameraView(cameraController)
 * ```
 *
 * @param cameraController The [CameraController] instance managing the camera session.
 *                         Must be created using [createCameraController] and have
 *                         [CameraController.startPreview] called before displaying.
 *
 * @see CameraController The controller managing the camera lifecycle
 * @see createCameraController Factory function to create the camera controller
 * @see RequestCameraPermission Wrapper to handle camera permissions before showing preview
 */
@Composable
expect fun CameraView(cameraController: CameraController)