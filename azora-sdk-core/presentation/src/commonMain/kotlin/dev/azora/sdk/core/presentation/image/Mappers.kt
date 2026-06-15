package dev.azora.sdk.core.presentation.image

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Converts a [ByteArray] containing encoded image data to a Compose [ImageBitmap].
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun DisplayPhoto(photoBytes: ByteArray) {
 *     Image(
 *         bitmap = photoBytes.toImageBitmap(),
 *         contentDescription = "Photo",
 *         modifier = Modifier.size(200.dp)
 *     )
 * }
 * ```
 *
 * ## Supported Formats
 * Supports common image formats including JPEG, PNG, GIF, BMP, and WebP,
 * depending on platform capabilities.
 *
 * @return An [ImageBitmap] ready for use with Compose Image composable.
 * @throws IllegalArgumentException if the byte array cannot be decoded as an image.
 *
 * @see compressImage For compressing images before conversion
 * @see rememberPhotoPickerLauncher For obtaining image bytes from user selection
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap