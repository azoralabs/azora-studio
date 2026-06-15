package dev.azora.sdk.core.presentation.image

/**
 * Platform-specific launcher for selecting photos from the device's photo library.
 *
 * ## Usage
 * The launcher should be created using [rememberPhotoPickerLauncher] to properly
 * handle the lifecycle and callbacks:
 *
 * ```kotlin
 * @Composable
 * fun PhotoPickerExample() {
 *     val launcher = rememberPhotoPickerLauncher { photoBytes ->
 *         // Handle the selected photo bytes
 *         val compressed = photoBytes.compressImage()
 *     }
 *
 *     Button(onClick = { launcher.launch { /* already handled above */ } }) {
 *         Text("Select Photo")
 *     }
 * }
 * ```
 *
 * @see rememberPhotoPickerLauncher Factory function to create launcher instances
 * @see compressImage For compressing selected photos before upload
 */
expect class PhotoPickerLauncher {

    /**
     * Launches the platform's native photo picker UI.
     *
     * When the user selects a photo, the [onPhotoPicked] callback is invoked with
     * the photo data as a [ByteArray]. If the user cancels the picker, the callback
     * is not invoked.
     *
     * @param onPhotoPicked Callback invoked with the selected photo's raw bytes.
     *                      The format depends on the source image (typically JPEG or PNG).
     */
    fun launch(onPhotoPicked: (ByteArray) -> Unit)
}