package dev.azora.local.database

import androidx.room.*
import dev.azora.local.database.dao.project.AzoraProjectDao
import dev.azora.local.database.dao.settings.SettingsDao
import dev.azora.local.database.entity.project.AzoraProjectEntity
import dev.azora.local.database.entity.settings.SettingsEntity

@Database(
    entities = [
        AzoraProjectEntity::class,
        SettingsEntity::class
    ],
    version = 3
)
@TypeConverters(DatabaseConverters::class)
@ConstructedBy(LocalDatabaseConstructor::class)
abstract class LocalDatabase : RoomDatabase() {

    abstract val projectDao: AzoraProjectDao
    abstract val settingsDao: SettingsDao

    companion object {
        const val DB_NAME = "azora.db"
    }
}