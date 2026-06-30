package dev.azora.sdk.core.presentation.camera

import androidx.compose.ui.graphics.ImageBitmap
import dev.azora.sdk.core.presentation.util.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.fileDataRepresentation
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import kotlin.coroutines.resume

/**
 * iOS implementation of [CameraController] using AVFoundation.
 *
 * Preview is rendered by [CameraView] through an `AVCaptureVideoPreviewLayer` bound to [session];
 * the [previewFrame] flow is unused on iOS. Capture goes through an [AVCapturePhotoOutput].
 */
@OptIn(ExperimentalForeignApi::class)
actual class CameraController {

    private val _previewFrame = MutableStateFlow<ImageBitmap?>(null)
    actual val previewFrame: StateFlow<ImageBitmap?> = _previewFrame.asStateFlow()

    internal val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()

    // Held strongly while a capture is in flight.
    private var captureDelegate: PhotoCaptureDelegate? = null
    private var configured = false

    private fun configureIfNeeded() {
        if (configured) return
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetPhoto

        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device != null) {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null)
            if (input != null && session.canAddInput(input)) {
                session.addInput(input)
            }
        }
        if (session.canAddOutput(photoOutput)) {
            session.addOutput(photoOutput)
        }
        session.commitConfiguration()
        configured = true
    }

    actual fun startPreview() {
        configureIfNeeded()
        if (!session.running) {
            dispatch_async(dispatch_get_global_queue(0, 0u)) {
                session.startRunning()
            }
        }
    }

    actual fun stopPreview() {
        if (session.running) {
            session.stopRunning()
        }
    }

    actual suspend fun takePhoto(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val delegate = PhotoCaptureDelegate { bytes ->
            captureDelegate = null
            if (continuation.isActive) continuation.resume(bytes)
        }
        captureDelegate = delegate
        photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings(), delegate)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoCaptureDelegate(
    private val onResult: (ByteArray?) -> Unit
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        val bytes = if (error == null) {
            didFinishProcessingPhoto.fileDataRepresentation()?.toByteArray()
        } else {
            null
        }
        onResult(bytes)
    }
}
