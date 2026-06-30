package dev.azora.sdk.core.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android implementation of [RequestCameraPermission].
 *
 * Invokes [onGranted] immediately when the camera permission is already held, otherwise launches
 * the system permission dialog and invokes [onGranted] if the user accepts. [body] is always
 * rendered so the caller can show appropriate UI for each state.
 */
@Composable
actual fun RequestCameraPermission(
    onGranted: () -> Unit,
    body: @Composable () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted()
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) onGranted() else launcher.launch(Manifest.permission.CAMERA)
    }

    body()
}
