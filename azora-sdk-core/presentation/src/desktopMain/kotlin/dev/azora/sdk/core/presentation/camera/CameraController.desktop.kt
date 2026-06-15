package dev.azora.sdk.core.presentation.camera

import androidx.compose.ui.graphics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.bytedeco.javacv.*
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Desktop (JVM) implementation of [CameraController] using JavaCV/OpenCV.
 *
 * This implementation uses the JavaCV library with OpenCV bindings for camera access:
 * - [OpenCVFrameGrabber] for capturing frames from the webcam
 * - [Java2DFrameConverter] for converting frames to BufferedImage
 * - Frame capture runs in a coroutine on [Dispatchers.IO]
 *
 * ## Camera Configuration
 * - Uses device index 0 (primary webcam)
 * - Resolution: 1280x720 pixels
 * - Frame rate: 30 fps
 * - Output format: JPEG for photo capture
 *
 * ## Preview Frames
 * Unlike Android and iOS which use native preview views, the desktop implementation
 * provides preview frames through [previewFrame] StateFlow. These can be rendered
 * in Compose using Image composable.
 *
 * ## Dependencies
 * Requires JavaCV and OpenCV native libraries to be available on the classpath.
 *
 * @see createCameraController Factory function to create instances
 */
actual class CameraController {

    private var grabber: OpenCVFrameGrabber? = null
    private val converter = Java2DFrameConverter()
    private var previewJob: Job? = null

    private val _previewFrame = MutableStateFlow<ImageBitmap?>(null)
    actual val previewFrame: StateFlow<ImageBitmap?> = _previewFrame.asStateFlow()

    private var isRunning = false

    actual fun startPreview() {
        if (isRunning) return

        try {
            grabber = OpenCVFrameGrabber(0).apply {
                imageWidth = 1280
                imageHeight = 720
                frameRate = 30.0
                start()
            }

            isRunning = true

            previewJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive && isRunning) {
                    try {
                        grabber?.grab()?.let { frame ->
                            val image = frame.image
                            if (image != null) {
                                val bufferedImage = converter.convert(frame)
                                if (bufferedImage != null) {
                                    _previewFrame.value = bufferedImage.toComposeImageBitmap()
                                }
                            }
                        }
                        delay(33) // ~30 fps
                    } catch (e: Exception) {
                        println("Error grabbing frame: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to start camera: ${e.message}")
            e.printStackTrace()
            isRunning = false
        }
    }

    actual fun stopPreview() {
        isRunning = false
        previewJob?.cancel()
        previewJob = null

        try {
            grabber?.stop()
            grabber?.release()
        } catch (e: Exception) {
            println("Error stopping grabber: ${e.message}")
        } finally {
            grabber = null
        }
    }

    actual suspend fun takePhoto(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            grabber?.grab()?.let { frame ->
                val image = frame.image
                if (image == null) {
                    return@withContext null
                }

                val bufferedImage = converter.convert(frame)
                if (bufferedImage != null) {
                    val output = ByteArrayOutputStream()
                    ImageIO.write(bufferedImage, "jpg", output)
                    return@withContext output.toByteArray()
                }
            }
            null
        } catch (e: Exception) {
            println("Failed to take photo: ${e.message}")
            null
        }
    }
}