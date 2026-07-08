package dev.azora.local.database.di

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.azora.local.database.*
import org.koin.core.module.Module
import org.koin.dsl.module

/** v3 adds the per-project code-editor preferences to azora_settings. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "editorFontSize INTEGER NOT NULL DEFAULT 13",
            "editorTabSize INTEGER NOT NULL DEFAULT 4",
            "editorWordWrap INTEGER NOT NULL DEFAULT 0",
            "editorShowLineNumbers INTEGER NOT NULL DEFAULT 1",
            "editorAutoCloseBrackets INTEGER NOT NULL DEFAULT 1",
            "editorSmartIndent INTEGER NOT NULL DEFAULT 1",
            "editorAutoCompletion INTEGER NOT NULL DEFAULT 1",
            "editorHoverDocs INTEGER NOT NULL DEFAULT 1"
        ).forEach { column ->
            connection.execSQL("ALTER TABLE azora_settings ADD COLUMN $column")
        }
    }
}

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
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}