package dev.azora.local.database.di

import dev.azora.local.database.DatabaseFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Desktop platform-specific database module.
 *
 * Provides a singleton [DatabaseFactory] instance for desktop platforms
 * (JVM-based: Windows, macOS, Linux). The factory requires no platform-specific
 * dependencies and manages database files in the application's data directory.
 *
 * Uses Koin's `singleOf` for concise constructor injection without parameters.
 *
 * **Dependencies:**
 * - None (factory has no constructor parameters)
 *
 * **Provides:**
 * - `DatabaseFactory` - Singleton instance for database creation
 *
 * @see DatabaseFactory
 */
actual val platformCoreDatabaseModule = module {
    singleOf(::DatabaseFactory)
}