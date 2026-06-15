package dev.azora.sdk.core.data.util

/**
 * Platform-specific utilities for runtime information and diagnostics.
 *
 * Each platform must provide an `actual` implementation.
 */
expect object PlatformUtils {

    /**
     * Returns a human-readable name of the operating system
     * (e.g., "Android", "iOS", "Windows", "macOS", "Web").
     */
    fun getOSName(): String
}