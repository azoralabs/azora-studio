package dev.azora.sdk.core.presentation.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Android implementation of [compressImage] using [Bitmap] scaling and JPEG compression.
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
        val original = BitmapFactory.decodeByteArray(this, 0, size) ?: return this
        val ratio = min(
            maxWidth.toFloat() / original.width,
            maxHeight.toFloat() / original.height
        )
        val scaled = if (ratio < 1.0f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt().coerceAtLeast(1),
                (original.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            original
        }
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), output)
        output.toByteArray()
    } catch (_: Exception) {
        this
    }
}
