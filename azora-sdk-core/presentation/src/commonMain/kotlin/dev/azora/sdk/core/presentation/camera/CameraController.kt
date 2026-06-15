package dev.azora.sdk.core.presentation.camera

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic camera controller for capturing photos and managing camera preview.
 *
 * ## Usage
 * ```kotlin
 * val cameraController = createCameraController()
 *
 * // Start the camera preview
 * cameraController.startPreview()
 *
 * // Capture a photo
 * val photoBytes = cameraController.takePhoto()
 *
 * // Stop the preview when done
 * cameraController.stopPreview()
 * ```
 *
 * @see createCameraController Factory function to create platform-specific instances
 * @see CameraView Composable to display the camera preview
 * @see RequestCameraPermission Composable to handle camera permission requests
 */
expect class CameraController {

    /**
     * A [StateFlow] emitting the current camera preview frame as an [ImageBitmap].
     *
     * This can be used for custom rendering of the camera preview in Compose.
     * Emits `null` when no preview is available.
     *
     * Note: On Android and iOS, the native preview view is typically used instead
     * of rendering frames from this flow for better performance.
     */
    val previewFrame: StateFlow<ImageBitmap?>

    /**
     * Starts the camera preview.
     *
     * This initializes the camera hardware and begins capturing preview frames.
     *
     * Should be called after the camera controller is created and before
     * attempting to take photos.
     */
    fun startPreview()

    /**
     * Stops the camera preview and releases camera resources.
     *
     * This should be called when the camera is no longer needed to free up
     * system resources. On Android, this unbinds all camera use cases.
     * On iOS, this stops the capture session.
     * On Desktop, this stops and releases the frame grabber.
     */
    fun stopPreview()

    /**
     * Captures a photo from the camera.
     *
     * @return The captured photo as a JPEG-encoded [ByteArray], or `null` if
     *         capture failed or camera is not initialized.
     *
     * The returned byte array contains JPEG image data that can be:
     * - Saved to a file
     * - Uploaded to a server
     * - Converted to a Bitmap/ImageBitmap for display
     *
     * On Android, the image is automatically rotated based on device orientation.
     * On iOS, the image uses the orientation from the photo metadata.
     */
    suspend fun takePhoto(): ByteArray?
}