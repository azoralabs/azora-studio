@file:OptIn(ExperimentalForeignApi::class)

package dev.azora.local.database

import androidx.room.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

/**
 * iOS implementation of [DatabaseFactory].
 *
 * Creates a Room database instance in the app's document directory,
 * following iOS file system conventions for persistent app data.
 */
actual class DatabaseFactory {

    /**
     * Creates an iOS-specific Room database builder.
     *
     * The database file is stored in the application's document directory,
     * which is the standard location for user-generated content and
     * persistent data on iOS.
     *
     * @return A [RoomDatabase.Builder] configured with the iOS database path
     */
    actual fun create(): RoomDatabase.Builder<LocalDatabase> {
        val dbFile = documentDirectory() + "/${LocalDatabase.DB_NAME}"

        return Room.databaseBuilder(dbFile)
    }

    /**
     * Retrieves the path to the iOS document directory.
     *
     * Uses [NSFileManager] to locate the standard document directory
     * in the user domain, which is backed up by iTunes/iCloud and
     * persists across app updates.
     *
     * @return The absolute path to the document directory
     * @throws IllegalArgumentException if the document directory cannot be located
     */
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )

        return requireNotNull(documentDirectory?.path)
    }
}