package dev.azora.studio.util

import androidx.compose.ui.input.pointer.PointerEvent

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// Touch input has no secondary button.
actual fun PointerEvent.isSecondaryClick(): Boolean = false
