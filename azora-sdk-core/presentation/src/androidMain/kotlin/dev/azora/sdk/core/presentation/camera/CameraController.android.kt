package dev.azora.sdk.core.presentation.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [CameraController] using CameraX.
 *
 * Preview is rendered through a native [PreviewView] embedded by [CameraView]; the
 * [previewFrame] flow is unused on Android (kept null) since the native view is more efficient.
 * Photo capture goes through an [ImageCapture] use case bound to the host lifecycle.
 */
actual class CameraController {

    private val _previewFrame = MutableStateFlow<ImageBitmap?>(null)
    actual val previewFrame: StateFlow<ImageBitmap?> = _previewFrame.asStateFlow()

    internal val imageCapture: ImageCapture = ImageCapture.Builder().build()

    internal var context: Context? = null
    internal var lifecycleOwner: LifecycleOwner? = null
    internal var previewView: PreviewView? = null

    actual fun startPreview() {
        val ctx = context ?: return
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return

        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    actual fun stopPreview() {
        val ctx = context ?: return
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            runCatching { providerFuture.get().unbindAll() }
        }, ContextCompat.getMainExecutor(ctx))
    }

    actual suspend fun takePhoto(): ByteArray? {
        val ctx = context ?: return null
        return suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(ctx),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bytes = runCatching {
                            val buffer = image.planes[0].buffer
                            ByteArray(buffer.remaining()).also { buffer.get(it) }
                        }.getOrNull()
                        image.close()
                        if (continuation.isActive) continuation.resume(bytes)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            )
        }
    }
}
