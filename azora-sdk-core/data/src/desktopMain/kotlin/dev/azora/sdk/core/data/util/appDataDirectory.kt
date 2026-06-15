package dev.azora.sdk.core.data.util

import java.io.File

/**
 * Returns the application data directory for the current desktop platform.
 *
 * The directory varies by operating system to follow native conventions:
 * - **Windows:** `%APPDATA%\AVE`
 * - **macOS:** `~/Library/Application Support/AVE`
 * - **Linux:** `~/.local/share/AVE`
 *
 * If the directory does not exist, it will be created during DataStore initialization.
 */
val appDataDirectory: File
    get() {
        val userHome = System.getProperty("user.home")
        return when (DesktopOs.current) {
            DesktopOs.WINDOWS -> File(System.getenv("APPDATA"), "AVE")
            DesktopOs.MACOS -> File(userHome, "Library/Application Support/AVE")
            DesktopOs.LINUX -> File(userHome, ".local/share/AVE")
        }
    }