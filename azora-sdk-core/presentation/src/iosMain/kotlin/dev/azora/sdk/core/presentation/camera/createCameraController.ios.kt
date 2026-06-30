package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [createCameraController].
 */
@Composable
actual fun createCameraController(): CameraController = remember { CameraController() }
