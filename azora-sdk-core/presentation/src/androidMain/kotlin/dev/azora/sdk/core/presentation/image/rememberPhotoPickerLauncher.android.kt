package dev.azora.sdk.core.presentation.image

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of [rememberPhotoPickerLauncher] using the system photo picker
 * (`ActivityResultContracts.PickVisualMedia`).
 *
 * The selected image is read from its content URI and delivered to the callback supplied to
 * [PhotoPickerLauncher.launch].
 */
@Composable
actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher {
    val context = LocalContext.current
    // Holds the callback passed to the most recent launch() call.
    val pendingCallback = remember { arrayOfNulls<(ByteArray) -> Unit>(1) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) (pendingCallback[0] ?: onPhotoPicked).invoke(bytes)
        }
    }

    return remember {
        PhotoPickerLauncher { callback ->
            pendingCallback[0] = callback
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}
