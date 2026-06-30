package dev.azora.sdk.core.presentation.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Android implementation of [CameraView] embedding a CameraX [PreviewView] and binding the
 * given [cameraController] to the current lifecycle.
 */
@Composable
actual fun CameraView(cameraController: CameraController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier,
        factory = { ctx ->
            PreviewView(ctx).also { view ->
                cameraController.context = context
                cameraController.lifecycleOwner = lifecycleOwner
                cameraController.previewView = view
                cameraController.startPreview()
            }
        }
    )
}
