package dev.azora.local.database.di

import dev.azora.local.database.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android platform-specific database module.
 *
 * Provides a singleton [DatabaseFactory] instance that uses the Android application
 * [android.content.Context] to create and manage the Room database. The context is automatically
 * resolved from Koin's Android-specific `androidContext()` function.
 *
 * The factory uses the application context to ensure proper lifecycle management
 * and to access Android's internal database directory.
 *
 * **Dependencies:**
 * - Requires Android context to be registered in Koin (via `androidContext()`)
 *
 * **Provides:**
 * - `DatabaseFactory` - Singleton instance for database creation
 *
 * @see DatabaseFactory
 */
actual val platformCoreDatabaseModule = module {
    single { DatabaseFactory(androidContext()) }
}