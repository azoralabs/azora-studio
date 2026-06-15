package dev.azora.local.database

import android.content.Context
import androidx.room.*

/**
 * Android implementation of [DatabaseFactory].
 *
 * Creates a Room database instance using the Android application context
 * and stores the database file in the app's standard database directory.
 *
 * @property context Android context used to access the application context
 *                   and database directory
 */
actual class DatabaseFactory(
    private val context: Context
) {

    /**
     * Creates an Android-specific Room database builder.
     *
     * The database file is created in the app's internal storage using
     * [Context.getDatabasePath], ensuring proper isolation and access control.
     *
     * @return A [RoomDatabase.Builder] configured with the Android database path
     */
    actual fun create(): RoomDatabase.Builder<LocalDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(LocalDatabase.DB_NAME)

        return Room.databaseBuilder(
            context.applicationContext,
            dbFile.absolutePath
        )
    }
}