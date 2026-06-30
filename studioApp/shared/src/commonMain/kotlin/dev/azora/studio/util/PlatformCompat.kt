package dev.azora.studio.util

import androidx.compose.ui.input.pointer.PointerEvent

/**
 * Current wall-clock time in milliseconds since the Unix epoch.
 *
 * Provided per-platform since the JVM's `System.currentTimeMillis()` is not available on Native.
 */
expect fun currentTimeMillis(): Long

/**
 * Whether this pointer event is a secondary (right) click.
 *
 * Only desktop has a secondary mouse button; touch platforms always return false.
 */
expect fun PointerEvent.isSecondaryClick(): Boolean
