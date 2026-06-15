package org.azora.studio

data class LoadingState(
    val currentTask: String = "Initializing...",
    val progress: Float = 0f,
    val isComplete: Boolean = false
) {

    companion object {

        fun projectLoadingTasks(projectName: String) = listOf(
            "Loading project configuration...",
            "Initializing project resources...",
            "Loading plugins for $projectName...",
            "Preparing editor workspace...",
            "Opening $projectName..."
        )
    }
}