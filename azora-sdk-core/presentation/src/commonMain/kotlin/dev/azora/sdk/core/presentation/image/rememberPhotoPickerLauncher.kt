package dev.azora.sdk.core.presentation.image

import androidx.compose.runtime.Composable

/**
 * Creates and remembers a [PhotoPickerLauncher] for selecting photos from the device.
 *
 * The launcher is remembered across recompositions to maintain consistent behavior.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun ProfilePhotoSelector() {
 *     var selectedPhoto by remember { mutableStateOf<ByteArray?>(null) }
 *
 *     val photoPickerLauncher = rememberPhotoPickerLauncher { photoBytes ->
 *         selectedPhoto = photoBytes.compressImage(maxWidth = 512, maxHeight = 512)
 *     }
 *
 *     Column {
 *         selectedPhoto?.let { bytes ->
 *             Image(
 *                 bitmap = bytes.toImageBitmap(),
 *                 contentDescription = "Selected photo"
 *             )
 *         }
 *
 *         Button(onClick = { photoPickerLauncher.launch {} }) {
 *             Text("Choose Photo")
 *         }
 *     }
 * }
 * ```
 *
 * @param onPhotoPicked Callback invoked when the user selects a photo.
 *                      Receives the photo data as a [ByteArray].
 *                      Not called if the user cancels the picker.
 * @return A [PhotoPickerLauncher] that can be used to trigger photo selection.
 *
 * @see PhotoPickerLauncher The launcher class for triggering photo selection
 * @see compressImage For reducing photo size before upload
 * @see toImageBitmap For displaying the selected photo
 */
@Composable
expect fun rememberPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher