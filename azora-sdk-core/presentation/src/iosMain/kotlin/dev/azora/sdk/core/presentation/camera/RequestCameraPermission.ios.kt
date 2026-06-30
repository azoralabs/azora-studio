package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of [RequestCameraPermission].
 *
 * Invokes [onGranted] when camera access is already authorized, otherwise prompts the user and
 * invokes [onGranted] (on the main queue) if they accept. [body] is always rendered.
 */
@Composable
actual fun RequestCameraPermission(
    onGranted: () -> Unit,
    body: @Composable () -> Unit
) {
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> onGranted()
            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    if (granted) dispatch_async(dispatch_get_main_queue()) { onGranted() }
                }
            else -> { /* denied / restricted — leave to body() to show UI */ }
        }
    }
    body()
}
