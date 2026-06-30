package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Android implementation of [createCameraController]. The [Context]/lifecycle/preview view are
 * bound later by [CameraView].
 */
@Composable
actual fun createCameraController(): CameraController = remember { CameraController() }
