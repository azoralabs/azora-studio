package dev.azora.local.database

import androidx.room.*
import java.io.File

/**
 * Desktop implementation of [DatabaseFactory].
 *
 * Creates a Room database instance in the application's data directory,
 * automatically creating the directory structure if it doesn't exist.
 */
actual class DatabaseFactory {

    /**
     * Platform-specific application data directory:
     * - Windows: `%APPDATA%/Azora`
     * - macOS: `~/Library/Application Support/Azora`
     * - Linux: `~/.local/share/Azora`
     */
    private val appDataDirectory: File
        get() {
            val os = System.getProperty("os.name").lowercase()
            val userHome = System.getProperty("user.home")
            return when {
                os.contains("win") -> File(System.getenv("APPDATA") ?: "$userHome/AppData/Roaming", "Azora")
                os.contains("mac") -> File(userHome, "Library/Application Support/Azora")
                else -> File(userHome, ".local/share/Azora")
            }
        }

    /**
     * Creates a desktop-specific Room database builder.
     *
     * The database file is stored in the application's data directory
     * (typically in user's AppData/Application Support folder).
     * Creates the directory structure if it doesn't already exist.
     *
     * @return A [RoomDatabase.Builder] configured with the desktop database path
     */
    actual fun create(): RoomDatabase.Builder<LocalDatabase> {
        val directory = appDataDirectory

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val dbFile = File(directory, LocalDatabase.DB_NAME)
        return Room.databaseBuilder(dbFile.absolutePath)
    }
}