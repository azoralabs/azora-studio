package dev.azora.sdk.core.data.util

/**
 * Represents supported desktop operating systems.
 *
 * Used to determine platform-specific directories and behaviors
 * for the desktop environment.
 */
enum class DesktopOs {

    /** Microsoft Windows operating system. */
    WINDOWS,

    /** Apple macOS operating system. */
    MACOS,

    /** Linux-based operating system (Ubuntu, Fedora, etc.). */
    LINUX;

    companion object {
        /**
         * Determines the current desktop operating system based on the JVM `"os.name"` property.
         *
         * The detection uses lowercase string matching and defaults to [DesktopOs.LINUX]
         * if the OS name does not explicitly contain `"win"` or `"mac"`.
         */
        val current: DesktopOs
            get() {
                val osName = System.getProperty("os.name").lowercase()
                return when {
                    osName.contains("win") -> WINDOWS
                    osName.contains("mac") -> MACOS
                    else -> LINUX
                }
            }
    }
}