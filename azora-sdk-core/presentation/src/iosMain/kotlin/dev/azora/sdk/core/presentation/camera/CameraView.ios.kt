package dev.azora.sdk.core.presentation.camera

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.UIKit.UIView

/**
 * iOS implementation of [CameraView] embedding a [UIView] whose layer hosts an
 * `AVCaptureVideoPreviewLayer` bound to the controller's capture session.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraView(cameraController: CameraController) {
    val previewLayer = remember {
        AVCaptureVideoPreviewLayer(session = cameraController.session).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }

    UIKitView(
        factory = {
            UIView().apply { layer.addSublayer(previewLayer) }
        },
        update = { view ->
            previewLayer.setFrame(view.bounds)
        },
        modifier = Modifier.fillMaxSize()
    )
}
