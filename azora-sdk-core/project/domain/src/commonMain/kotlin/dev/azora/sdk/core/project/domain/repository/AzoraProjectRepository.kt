package dev.azora.sdk.core.project.domain.repository

import dev.azora.sdk.core.domain.util.*
import dev.azora.sdk.core.project.domain.AzoraProjectModel

interface AzoraProjectRepository {

    /**
     * Clears the local database (azora.db).
     * Should be called on app start to ensure clean state.
     */
    suspend fun clearDatabase(): Res<Unit, DataError.Local>

    /**
     * Creates a new project with the given configuration.
     * Creates project directory, project.json, and empty project.db.
     */
    suspend fun createProject(project: AzoraProjectModel): Res<AzoraProjectModel, DataError.Local>

    /**
     * Opens a project by loading its project.db into azora.db.
     * Clears current database state and loads from project.db file.
     */
    suspend fun openProject(projectPath: String): Res<AzoraProjectModel, DataError.Local>

    /**
     * Saves current database state to project.db and updates project.json.
     */
    suspend fun saveProject(projectPath: String): Res<Unit, DataError.Local>

    /**
     * Updates the project model in the database.
     * Call saveProject to persist changes to the project file.
     */
    suspend fun updateProject(project: AzoraProjectModel): Res<Unit, DataError.Local>

    /**
     * Gets the current project from the database.
     */
    suspend fun getProject(): Res<AzoraProjectModel, DataError.Local>

    /**
     * Deletes a project and all its files.
     */
    suspend fun deleteProject(projectPath: String): Res<Unit, DataError.Local>

    /**
     * Lists all projects in the Azora projects directory.
     */
    suspend fun listProjects(): Res<List<String>, DataError.Local>

    /**
     * Gets the base directory for Azora projects.
     */
    fun getProjectsDirectory(): String

    /**
     * Gets the full path for a project.
     */
    fun getProjectPath(projectName: String): String
}