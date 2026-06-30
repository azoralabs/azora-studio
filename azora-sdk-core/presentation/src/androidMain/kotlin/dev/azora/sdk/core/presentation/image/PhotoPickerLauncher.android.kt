package dev.azora.sdk.core.presentation.image

/**
 * Android implementation of [PhotoPickerLauncher].
 *
 * Wraps the launch action wired up by [rememberPhotoPickerLauncher] (backed by the Activity
 * Result `PickVisualMedia` contract). The per-launch [onPhotoPicked] callback is routed to the
 * pending result delivered by the system photo picker.
 */
actual class PhotoPickerLauncher(
    private val onLaunch: ((ByteArray) -> Unit) -> Unit
) {

    actual fun launch(onPhotoPicked: (ByteArray) -> Unit) {
        onLaunch(onPhotoPicked)
    }
}
