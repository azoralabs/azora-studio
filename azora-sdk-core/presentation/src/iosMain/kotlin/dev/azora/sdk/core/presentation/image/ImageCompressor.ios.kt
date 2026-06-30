package dev.azora.sdk.core.presentation.image

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.math.min

/**
 * iOS implementation of [compressImage] using Skia.
 *
 * Decodes the image, scales it to fit within [maxWidth]/[maxHeight] (preserving aspect ratio),
 * and re-encodes as JPEG at the given [quality]. Returns the original bytes on any failure.
 */
actual fun ByteArray.compressImage(
    maxWidth: Int,
    maxHeight: Int,
    quality: Int
): ByteArray {
    return try {
        val image = Image.makeFromEncoded(this)
        val ratio = min(
            maxWidth.toFloat() / image.width,
            maxHeight.toFloat() / image.height
        )
        val targetWidth = if (ratio < 1.0f) (image.width * ratio).toInt().coerceAtLeast(1) else image.width
        val targetHeight = if (ratio < 1.0f) (image.height * ratio).toInt().coerceAtLeast(1) else image.height

        val scaled = if (ratio < 1.0f) {
            val surface = Surface.makeRasterN32Premul(targetWidth, targetHeight)
            surface.canvas.drawImageRect(
                image,
                Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                Rect.makeWH(targetWidth.toFloat(), targetHeight.toFloat()),
                null
            )
            surface.makeImageSnapshot()
        } else {
            image
        }

        scaled.encodeToData(EncodedImageFormat.JPEG, quality.coerceIn(0, 100))?.bytes ?: this
    } catch (_: Throwable) {
        this
    }
}
