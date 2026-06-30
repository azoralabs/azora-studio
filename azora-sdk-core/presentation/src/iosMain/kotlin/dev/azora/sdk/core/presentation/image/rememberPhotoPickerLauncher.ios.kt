package dev.azora.sdk.core.presentation.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [rememberPhotoPickerLauncher]. The per-launch callback is supplied to
 * [PhotoPickerLauncher.launch].
 */
@Composable
actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher = remember { PhotoPickerLauncher() }
