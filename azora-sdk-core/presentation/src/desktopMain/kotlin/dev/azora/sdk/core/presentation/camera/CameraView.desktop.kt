package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable

/**
 * Desktop implementation of [CameraView].
 *
 * Currently a no-op stub as native camera preview embedding is not supported
 * on desktop platforms in the same way as Android and iOS.
 *
 * ## Workaround
 * For desktop camera preview, use the [CameraController.previewFrame] StateFlow
 * to render frames in Compose:
 *
 * ```kotlin
 * @Composable
 * fun DesktopCameraPreview(cameraController: CameraController) {
 *     val frame by cameraController.previewFrame.collectAsState()
 *     frame?.let { bitmap ->
 *         Image(
 *             bitmap = bitmap,
 *             contentDescription = "Camera preview",
 *             modifier = Modifier.fillMaxSize()
 *         )
 *     }
 * }
 * ```
 *
 * @param cameraController The [CameraController] (unused in this stub implementation).
 */
@Composable
actual fun CameraView(cameraController: CameraController) {}