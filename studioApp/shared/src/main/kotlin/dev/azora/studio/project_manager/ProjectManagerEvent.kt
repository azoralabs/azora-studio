package org.azora.studio.project_manager

import org.azora.sdk.core.project.domain.AzoraProjectModel

sealed interface ProjectManagerEvent {

    /**
     * Project was created and saved to disk.
     * Contains the path to load from.
     */
    data class ProjectCreated(
        val projectPath: String,
        val projectName: String
    ) : ProjectManagerEvent

    /**
     * User selected a project to open.
     * Contains the path to load from.
     */
    data class ProjectOpened(
        val projectPath: String,
        val projectName: String
    ) : ProjectManagerEvent

    data class ProjectDeleted(val projectName: String) : ProjectManagerEvent
}