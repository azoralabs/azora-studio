package dev.azora.sdk.core.presentation.image

/**
 * Compresses an image byte array to reduce file size while maintaining acceptable quality.
 *
 * ## Behavior
 * - Images larger than maxWidth/maxHeight are scaled down while preserving aspect ratio
 * - Images smaller than the limits are not upscaled
 * - Output format is always JPEG for consistent compression
 * - If compression fails, the original byte array is returned unchanged
 *
 * ## Usage
 * ```kotlin
 * val compressedImage = originalImageBytes.compressImage(
 *     maxWidth = 512,
 *     maxHeight = 512,
 *     quality = 70
 * )
 * ```
 *
 * @param maxWidth Maximum width in pixels. Images wider than this will be scaled down.
 *                 Default: 1024
 * @param maxHeight Maximum height in pixels. Images taller than this will be scaled down.
 *                  Default: 1024
 * @param quality JPEG compression quality from 0 (lowest) to 100 (highest).
 *                Lower values produce smaller files but more artifacts. Default: 80
 * @return Compressed image as JPEG-encoded [ByteArray], or the original array if
 *         compression fails.
 *
 * @see toImageBitmap For converting the result to an ImageBitmap for display
 */
expect fun ByteArray.compressImage(
    maxWidth: Int = 512,
    maxHeight: Int = 512,
    quality: Int = 70
): ByteArray