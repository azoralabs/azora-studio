package dev.azora.sdk.core.project.data.repository

import dev.azora.local.database.LocalDatabase
import dev.azora.sdk.core.domain.util.*
import dev.azora.sdk.core.io.*
import dev.azora.sdk.core.project.data.generator.WebsiteTemplateGenerator
import dev.azora.sdk.core.project.data.mapper.*
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectTemplate
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import kotlinx.serialization.json.Json

actual class LocalAzoraProjectRepository(
    private val fileSystem: FileSystem,
    private val db: LocalDatabase
) : AzoraProjectRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val PROJECTS_DIR_NAME = "Azora"
        private const val PROJECT_DB_FILE_NAME = "project.azora"
        private const val CONFIG_DIR_NAME = "Azora"
    }

    override suspend fun clearDatabase(): Res<Unit, DataError.Local> {
        return try {
            db.projectDao.deleteAllProjects()
            Res.Success(Unit)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun createProject(project: AzoraProjectModel): Res<AzoraProjectModel, DataError.Local> {
        return try {
            val projectPath = getProjectPath(project.name)

            // Check if project already exists
            when (fileSystem.directoryExists(projectPath)) {
                is ExistsResult.Exists -> {
                    return Res.Failure(DataError.Local.UNKNOWN)
                }
                else -> { /* Continue */ }
            }

            // Create project directory
            when (fileSystem.createDirectory(projectPath)) {
                is FileSystemResult.Error -> {
                    return Res.Failure(DataError.Local.UNKNOWN)
                }
                else -> { /* Continue */ }
            }

            // Create config directory
            fileSystem.createDirectory("$projectPath/$CONFIG_DIR_NAME")

            // Clear any existing project from database before inserting new one
            clearDatabase()

            // Insert project into database
            db.projectDao.insertProject(project.toEntity())

            // Save database state to project.azora
            saveProjectDatabaseToFile(projectPath)

            // Scaffold template-specific source (e.g. a Kobweb site for the Website template).
            generateTemplate(project, projectPath)

            Res.Success(project)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun openProject(projectPath: String): Res<AzoraProjectModel, DataError.Local> {
        return try {
            // Check if project exists
            when (fileSystem.directoryExists(projectPath)) {
                is ExistsResult.NotExists -> {
                    return Res.Failure(DataError.Local.NOT_FOUND)
                }
                is ExistsResult.Error -> {
                    return Res.Failure(DataError.Local.UNKNOWN)
                }
                else -> { /* Continue */ }
            }

            // Check if project.azora exists
            when (fileSystem.fileExists("$projectPath/$PROJECT_DB_FILE_NAME")) {
                is ExistsResult.NotExists -> {
                    return Res.Failure(DataError.Local.NOT_FOUND)
                }
                is ExistsResult.Error -> {
                    return Res.Failure(DataError.Local.UNKNOWN)
                }
                else -> { /* Continue */ }
            }

            // Clear current database
            clearDatabase()

            // Load local azora.db into memory azora.db
            loadProjectDatabaseFromFile(projectPath)

            // Build and return AzoraProjectModel from database
            val projectModel = buildAzoraProjectModelFromDatabase()
                ?: return Res.Failure(DataError.Local.NOT_FOUND)

            // Ensure Assets folder exists
            fileSystem.createDirectory("$projectPath/Assets")

            Res.Success(projectModel)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    private suspend fun loadProjectDatabaseFromFile(projectPath: String) {
        val dbJson = when (val result = fileSystem.readFromFile("$projectPath/$PROJECT_DB_FILE_NAME")) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return
        }

        val project = json.decodeFromString<AzoraProjectModel>(dbJson)
        db.projectDao.insertProject(project.toEntity())
    }

    override suspend fun saveProject(projectPath: String): Res<Unit, DataError.Local> {
        return try {
            // Save database state to project.azora
            saveProjectDatabaseToFile(projectPath)
            Res.Success(Unit)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun updateProject(project: AzoraProjectModel): Res<Unit, DataError.Local> {
        return try {
            db.projectDao.insertProject(project.toEntity())
            Res.Success(Unit)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getProject(): Res<AzoraProjectModel, DataError.Local> {
        return try {
            val projectModel = buildAzoraProjectModelFromDatabase()
                ?: return Res.Failure(DataError.Local.NOT_FOUND)
            Res.Success(projectModel)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    private suspend fun generateTemplate(project: AzoraProjectModel, projectPath: String) {
        when (project.template) {
            ProjectTemplate.WEBSITE -> WebsiteTemplateGenerator(fileSystem).generate(project, projectPath)
            else -> { /* No scaffolding for other templates yet */ }
        }
    }

    private suspend fun saveProjectDatabaseToFile(projectPath: String) {
        val projectModel = buildAzoraProjectModelFromDatabase() ?: return
        val dbJson = json.encodeToString(projectModel)
        fileSystem.writeToFile("$projectPath/$PROJECT_DB_FILE_NAME", dbJson)
    }

    private suspend fun buildAzoraProjectModelFromDatabase(): AzoraProjectModel? {
        val projects = db.projectDao.getProjectsList()
        val projectEntity = projects.firstOrNull() ?: return null

        return projectEntity.toModel()
    }

    override suspend fun deleteProject(projectPath: String): Res<Unit, DataError.Local> {
        return try {
            when (fileSystem.deleteDirectoryRecursively(projectPath)) {
                is FileSystemResult.Success -> Res.Success(Unit)
                is FileSystemResult.Error -> Res.Failure(DataError.Local.UNKNOWN)
            }
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun listProjects(): Res<List<String>, DataError.Local> {
        return try {
            val projectsDir = getProjectsDirectory()

            // Ensure directory exists
            fileSystem.createDirectory(projectsDir)

            when (val result = fileSystem.listDirectory(projectsDir)) {
                is ListResult.Success -> {
                    val projectPaths = result.files
                        .filter { it.isDirectory }
                        .map { "$projectsDir/${it.name}" }
                        .filter { path ->
                            // Only include directories that have a project.azora
                            fileSystem.fileExists("$path/$PROJECT_DB_FILE_NAME") is ExistsResult.Exists
                        }
                    Res.Success(projectPaths)
                }
                is ListResult.Error -> {
                    Res.Failure(DataError.Local.UNKNOWN)
                }
            }
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override fun getProjectsDirectory() = PROJECTS_DIR_NAME

    override fun getProjectPath(projectName: String) = "$PROJECTS_DIR_NAME/$projectName"
}