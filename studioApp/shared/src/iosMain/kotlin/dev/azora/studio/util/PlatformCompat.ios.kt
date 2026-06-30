package dev.azora.studio.util

import androidx.compose.ui.input.pointer.PointerEvent
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

// Touch input has no secondary button.
actual fun PointerEvent.isSecondaryClick(): Boolean = false
