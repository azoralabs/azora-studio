package dev.azora.sdk.core.presentation.image

import androidx.compose.runtime.*
import java.awt.*
import java.io.File

/**
 * Desktop implementation of [rememberPhotoPickerLauncher] using AWT FileDialog.
 *
 * Creates a [PhotoPickerLauncher] that uses Java AWT's native [FileDialog]
 * for file selection. The dialog is configured with a filename filter that
 * accepts common image formats.
 *
 * ## Supported Formats
 * The file filter accepts:
 * - JPEG (.jpg, .jpeg)
 * - PNG (.png)
 * - GIF (.gif)
 * - BMP (.bmp)
 * - WebP (.webp)
 *
 * ## Behavior
 * - Dialog title: "Select Image"
 * - Mode: Load (FileDialog.LOAD)
 * - Uses native OS file dialog for familiar UX
 * - Blocking call - UI thread waits for selection
 *
 * ## Error Handling
 * If file reading fails, the exception is printed and null is returned,
 * preventing the callback from being invoked.
 *
 * @param onPhotoPicked Callback for when photos are selected (passed to launcher)
 * @return A remembered [PhotoPickerLauncher] configured with the file dialog
 */
@Composable
actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher {
    return remember {
        PhotoPickerLauncher(
            openDialog = {
                val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
                fileDialog.setFilenameFilter { _, name ->
                    name.endsWith(".jpg", ignoreCase = true) ||
                            name.endsWith(".jpeg", ignoreCase = true) ||
                            name.endsWith(".png", ignoreCase = true) ||
                            name.endsWith(".gif", ignoreCase = true) ||
                            name.endsWith(".bmp", ignoreCase = true) ||
                            name.endsWith(".webp", ignoreCase = true)
                }
                fileDialog.isVisible = true

                val directory = fileDialog.directory
                val file = fileDialog.file

                if (directory != null && file != null) {
                    try {
                        File(directory, file).readBytes()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }
            }
        )
    }
}