package dev.azora.sdk.core.presentation.camera

import androidx.compose.runtime.*

/**
 * Desktop implementation of [createCameraController].
 *
 * Creates and remembers a [CameraController] instance that uses JavaCV/OpenCV
 * for camera functionality. The controller manages:
 * - [dev.bytedeco.javacv.OpenCVFrameGrabber] for webcam access
 * - [dev.bytedeco.javacv.Java2DFrameConverter] for frame conversion
 * - Background coroutine for continuous frame capture
 *
 * Desktop doesn't require platform-specific dependencies like Context or
 * LifecycleOwner, so this is a simple factory function.
 *
 * ## Note
 * Ensure JavaCV and OpenCV native libraries are properly configured in
 * the classpath for camera functionality to work.
 *
 * @return A remembered [CameraController] ready for use.
 */
@Composable
actual fun createCameraController() = remember { CameraController() }