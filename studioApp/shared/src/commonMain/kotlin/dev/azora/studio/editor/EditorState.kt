package dev.azora.studio.editor

import dev.azora.sdk.core.presentation.util.ProcessState
import dev.azora.sdk.core.project.domain.AzoraProjectModel

data class StudioState(
    val project: AzoraProjectModel,
    val projectPath: String,
    val loading: ProcessState = ProcessState()
)