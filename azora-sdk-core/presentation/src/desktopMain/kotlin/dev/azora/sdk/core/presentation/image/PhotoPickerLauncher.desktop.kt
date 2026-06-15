package dev.azora.sdk.core.presentation.image

/**
 * Desktop (JVM) implementation of [PhotoPickerLauncher] using AWT FileDialog.
 *
 * This implementation wraps a lambda that opens a native file dialog for
 * selecting image files. The dialog is configured with appropriate file
 * filters in [rememberPhotoPickerLauncher].
 *
 * Unlike mobile platforms where photo selection is asynchronous, desktop
 * file dialogs are typically blocking, so the result is available immediately
 * after the dialog closes.
 *
 * @property openDialog Lambda that shows the file dialog and returns the
 *                      selected file's bytes, or null if cancelled
 *
 * @see rememberPhotoPickerLauncher Factory that configures the file dialog
 */
actual class PhotoPickerLauncher(
    private val openDialog: () -> ByteArray?
) {

    /**
     * Launches the desktop file picker dialog.
     *
     * Opens a native [java.awt.FileDialog] filtered to image files. If the user
     * selects a file, its contents are read and passed to [onPhotoPicked].
     * If the user cancels, the callback is not invoked.
     *
     * @param onPhotoPicked Callback invoked with the file's raw bytes
     *                      when a file is selected
     */
    actual fun launch(onPhotoPicked: (ByteArray) -> Unit) {
        val photoBytes = openDialog()
        photoBytes?.let { onPhotoPicked(it) }
    }
}