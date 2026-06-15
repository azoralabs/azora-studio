package dev.azora.local.database.di

import dev.azora.local.database.DatabaseFactory
import org.koin.dsl.module

/**
 * iOS platform-specific database module.
 *
 * Provides a singleton [DatabaseFactory] instance for iOS platforms.
 * The factory requires no platform-specific dependencies and manages database
 * files in the iOS document directory using Foundation framework APIs.
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
    single { DatabaseFactory() }
}