package dev.azora.local.database.dao.project

import androidx.room.*
import dev.azora.local.database.entity.project.AzoraProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AzoraProjectDao {

    @Query("SELECT * FROM azora_projects ORDER BY updatedAt DESC")
    fun getProjects(): Flow<List<AzoraProjectEntity>>

    @Query("SELECT * FROM azora_projects ORDER BY updatedAt DESC")
    suspend fun getProjectsList(): List<AzoraProjectEntity>

    @Query("SELECT * FROM azora_projects WHERE id = :id")
    suspend fun getProjectById(id: String): AzoraProjectEntity?

    @Query("SELECT * FROM azora_projects WHERE name = :name")
    suspend fun getProjectByName(name: String): AzoraProjectEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertProject(project: AzoraProjectEntity)

    @Update
    suspend fun updateProject(project: AzoraProjectEntity)

    @Upsert
    suspend fun upsertProject(project: AzoraProjectEntity)

    @Delete
    suspend fun deleteProject(project: AzoraProjectEntity)

    @Query("DELETE FROM azora_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("DELETE FROM azora_projects")
    suspend fun deleteAllProjects()
}