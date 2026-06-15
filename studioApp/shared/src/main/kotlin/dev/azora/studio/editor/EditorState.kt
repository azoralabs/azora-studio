package org.azora.studio.editor

import org.azora.sdk.core.presentation.util.ProcessState
import org.azora.sdk.core.project.domain.AzoraProjectModel

data class StudioState(
    val project: AzoraProjectModel,
    val projectPath: String,
    val loading: ProcessState = ProcessState()
)