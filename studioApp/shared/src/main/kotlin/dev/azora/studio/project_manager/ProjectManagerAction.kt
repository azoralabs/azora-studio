package org.azora.studio.project_manager

import org.azora.sdk.core.project.domain.AzoraProjectModel

sealed interface ProjectManagerAction {

    data class OnCreateProject(val project: AzoraProjectModel) : ProjectManagerAction

    data class OnOpenProject(val projectName: String) : ProjectManagerAction

    data class OnDeleteProject(val projectName: String) : ProjectManagerAction

    data class OnProjectNameChange(val name: String) : ProjectManagerAction

    data class OnCompanyNameChange(val name: String) : ProjectManagerAction

    data class OnDomainPathChange(val path: String) : ProjectManagerAction
}