package dev.azora.local.database

import androidx.room.*
import dev.azora.local.database.dao.project.AzoraProjectDao
import dev.azora.local.database.entity.project.AzoraProjectEntity

@Database(
    entities = [
        AzoraProjectEntity::class
    ],
    version = 1
)
@TypeConverters(DatabaseConverters::class)
@ConstructedBy(LocalDatabaseConstructor::class)
abstract class LocalDatabase : RoomDatabase() {

    abstract val projectDao: AzoraProjectDao

    companion object {
        const val DB_NAME = "azora.db"
    }
}