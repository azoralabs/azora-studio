package dev.azora.sdk.core.presentation.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

/**
 * iOS implementation of [toImageBitmap] using Compose Multiplatform's Skia backend.
 */
actual fun ByteArray.toImageBitmap(): ImageBitmap =
    SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
