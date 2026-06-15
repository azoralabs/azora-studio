package dev.azora.local.database.dao.settings

import androidx.room.*
import dev.azora.local.database.entity.settings.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM azora_settings WHERE projectId = :projectId")
    fun getSettingsFlow(projectId: String): Flow<SettingsEntity?>

    @Query("SELECT * FROM azora_settings WHERE projectId = :projectId")
    suspend fun getSettings(projectId: String): SettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)

    @Query("DELETE FROM azora_settings WHERE projectId = :projectId")
    suspend fun deleteSettings(projectId: String)
}
