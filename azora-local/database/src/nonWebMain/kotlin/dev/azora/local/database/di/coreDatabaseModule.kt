package dev.azora.local.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.azora.local.database.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform-specific database module.
 *
 * Each target platform (Android, iOS, Desktop) must provide its own implementation
 * of this module. It typically includes bindings for platform-specific database
 * drivers or utilities required to initialize the shared database layer.
 *
 * This module is included into the shared [coreDatabaseModule] to complete
 * the dependency graph for database creation.
 *
 * @see DatabaseFactory
 * @see coreDatabaseModule
 */
expect val platformCoreDatabaseModule: Module

/**
 * Shared Koin module responsible for providing database-related dependencies.
 *
 * This module:
 *  - Includes the platform-specific database module via [platformCoreDatabaseModule].
 *  - Creates a [LocalDatabase] instance using a [DatabaseFactory].
 *  - Configures the database to use a bundled SQLite driver.
 */
val coreDatabaseModule = module {

    includes(platformCoreDatabaseModule)

    single<LocalDatabase> {
        get<DatabaseFactory>()
            .create()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}