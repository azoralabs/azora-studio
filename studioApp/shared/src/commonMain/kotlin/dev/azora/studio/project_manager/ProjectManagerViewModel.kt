package dev.azora.studio.project_manager

import androidx.lifecycle.*
import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.presentation.util.*
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectManagerViewModel(
    private val projectRepository: AzoraProjectRepository,
    private val logger: AzoraLogger
) : ViewModel() {

    private val clazz = this::class.simpleName

    private val eventChannel = Channel<ProjectManagerEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(ProjectManagerState())
    val state = _state.asStateFlow()

    fun onAction(action: ProjectManagerAction) = when (action) {
        is ProjectManagerAction.OnCreateProject -> onCreateProject(action.project)
        is ProjectManagerAction.OnOpenProject -> onOpenProject(action.projectName)
        is ProjectManagerAction.OnDeleteProject -> onDeleteProject(action.projectName)
        is ProjectManagerAction.OnProjectNameChange -> onProjectNameChange(action.name)
        is ProjectManagerAction.OnCompanyNameChange -> onCompanyNameChange(action.name)
        is ProjectManagerAction.OnDomainPathChange -> onDomainPathChange(action.path)
        is ProjectManagerAction.OnTemplateChange -> _state.update { it.copy(template = action.template) }
        is ProjectManagerAction.OnIncludeServerChange -> _state.update { it.copy(includeServer = action.enabled) }
    }

    private fun onProjectNameChange(name: String) {
        _state.update { current ->
            val newPackageName = generatePackageName(current.companyName.field, name)
            current.copy(
                projectName = current.projectName.copy(field = name),
                packageName = current.packageName.copy(field = newPackageName)
            )
        }
    }

    private fun onCompanyNameChange(name: String) {
        _state.update { current ->
            val newPackageName = generatePackageName(name, current.projectName.field)
            current.copy(
                companyName = current.companyName.copy(field = name),
                packageName = current.packageName.copy(field = newPackageName)
            )
        }
    }

    private fun onDomainPathChange(path: String) {
        _state.update { it.copy(packageName = it.packageName.copy(field = path)) }
    }

    private fun generatePackageName(companyName: String, projectName: String): String {
        val sanitizedCompany = companyName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "company" }

        val sanitizedProject = projectName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "project" }

        return "com.$sanitizedCompany.$sanitizedProject"
    }

    private fun onCreateProject(project: AzoraProjectModel) = viewModelScope.launch {
        _state.update { it.copy(creating = ProcessState(inProcess = true)) }
        logger.info("$clazz: Creating project STARTED")

        when (projectRepository.createProject(project)) {
            is Res.Success -> {
                logger.info("$clazz: Project created: ${project.name}")
                val projectPath = projectRepository.getProjectPath(project.name)
                eventChannel.send(ProjectManagerEvent.ProjectCreated(projectPath, project.name))
            }
            is Res.Failure -> {
                logger.error("$clazz: Failed to create project")
                _state.update {
                    it.copy(
                        creating = ProcessState(
                            error = UiText.DynamicString("Failed to create project")
                        )
                    )
                }
            }
        }

        _state.update { it.copy(creating = ProcessState(inProcess = false)) }
        logger.info("$clazz: Creating project FINISHED")
    }

    private fun onOpenProject(projectName: String) = viewModelScope.launch {
        logger.info("$clazz: Project selected: $projectName")
        val projectPath = projectRepository.getProjectPath(projectName)
        // Actual loading happens during the splash screen phase
        eventChannel.send(ProjectManagerEvent.ProjectOpened(projectPath, projectName))
    }

    private fun onDeleteProject(projectName: String) = viewModelScope.launch {
        _state.update { it.copy(deleting = ProcessState(inProcess = true)) }
        logger.info("$clazz: Deleting project STARTED")

        val projectPath = projectRepository.getProjectPath(projectName)
        when (val result = projectRepository.deleteProject(projectPath)) {
            is Res.Success -> {
                logger.info("$clazz: Project deleted: $projectName")
                eventChannel.send(ProjectManagerEvent.ProjectDeleted(projectName))
            }
            is Res.Failure -> {
                logger.error("$clazz: Failed to delete: ${result.error}")
            }
        }

        _state.update { it.copy(deleting = ProcessState(inProcess = false)) }
        logger.info("$clazz: Deleting project FINISHED")
    }
}
