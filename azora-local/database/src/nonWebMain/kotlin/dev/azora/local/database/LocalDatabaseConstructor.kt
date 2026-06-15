package dev.azora.local.database

import androidx.room.RoomDatabaseConstructor

/**
 * Expected database constructor for creating the final Room database instance.
 *
 * This hides platform differences so that shared code can simply call
 * `LocalDatabaseConstructor.initialize()` to obtain a fully initialized database.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object LocalDatabaseConstructor : RoomDatabaseConstructor<LocalDatabase> {

    /**
     * Creates and returns a fully-initialized instance of [LocalDatabase].
     */
    override fun initialize(): LocalDatabase
}