package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.*

/**
 * Desktop implementation of [RequestCameraPermission].
 *
 * On desktop platforms (Windows, macOS, Linux), runtime camera permissions
 * are not required at the application level. The operating system handles
 * camera access permissions through system-level settings.
 *
 * This implementation immediately calls [onGranted] since there's no
 * permission request flow needed on desktop.
 *
 * ## Platform Behavior
 * - **Windows**: May show a system prompt for camera access in Windows 10/11 privacy settings
 * - **macOS**: May require camera access permission in System Preferences > Security & Privacy
 * - **Linux**: Typically no permission prompt, camera access is managed by device permissions
 *
 * If the camera cannot be accessed due to system-level restrictions, the
 * [CameraController.startPreview] will fail with an appropriate error message.
 *
 * @param onGranted Callback invoked immediately on desktop platforms.
 * @param body Content to display (always rendered on desktop).
 */
@Composable
actual fun RequestCameraPermission(
    onGranted: () -> Unit,
    body: @Composable () -> Unit
) {
    // Desktop (e.g., JVM/Compose for Desktop) doesn't need runtime camera permission
    // We immediately grant access
    LaunchedEffect(Unit) {
        onGranted()
    }

    body()
}