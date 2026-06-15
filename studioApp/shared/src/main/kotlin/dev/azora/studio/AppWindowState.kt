package org.azora.studio

import org.azora.sdk.core.project.domain.AzoraProjectModel

/**
 * Application-level state for window management.
 */
sealed interface AppWindowState {

    data object InitialSplash : AppWindowState

    data object ProjectManager : AppWindowState

    /**
     * Loading a project from disk.
     * The actual project loading happens during this phase.
     */
    data class LoadingProject(
        val projectPath: String,
        val projectName: String
    ) : AppWindowState

    data class Studio(
        val project: AzoraProjectModel,
        val projectPath: String
    ) : AppWindowState
}