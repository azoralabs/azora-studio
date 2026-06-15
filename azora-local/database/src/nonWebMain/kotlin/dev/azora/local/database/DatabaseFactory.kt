package dev.azora.local.database

import androidx.room.RoomDatabase

/**
 * Expected database factory for creating a Room database instance.
 *
 * Each platform (Android, iOS, Desktop) provides its own `actual` implementation
 * that configures and returns a Room database builder for [LocalDatabase].
 *
 * The factory ensures proper database initialization across different platforms,
 * handling platform-specific file system locations and context requirements.
 *
 * @see LocalDatabase
 */
expect class DatabaseFactory {

    /**
     * Creates a platform-specific [RoomDatabase.Builder] for [LocalDatabase].
     *
     * @return A configured [RoomDatabase.Builder] instance ready for database creation
     */
    fun create(): RoomDatabase.Builder<LocalDatabase>
}