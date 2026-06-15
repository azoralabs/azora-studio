package dev.azora.sdk.core.presentation.image

import androidx.compose.ui.graphics.*
import org.jetbrains.skia.Image as SkiaImage

/**
 * Desktop (JVM) implementation of [toImageBitmap] using Skia.
 *
 * Uses Compose Multiplatform's Skia backend to decode the image data.
 * [SkiaImage.makeFromEncoded] handles the decoding from various formats,
 * and [toComposeImageBitmap] converts the result to a Compose [ImageBitmap].
 *
 * ## Supported Formats
 * Supports all formats that Skia can decode:
 * - JPEG
 * - PNG
 * - GIF
 * - WebP
 * - BMP
 * - ICO
 * - WBMP
 *
 * ## Performance
 * Skia provides hardware-accelerated decoding on supported platforms,
 * making this implementation efficient for most use cases.
 *
 * @return A Compose [ImageBitmap] for use with Image composable
 * @throws IllegalArgumentException if the byte array cannot be decoded
 */
actual fun ByteArray.toImageBitmap() = SkiaImage.makeFromEncoded(this).toComposeImageBitmap()