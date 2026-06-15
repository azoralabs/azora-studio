package org.azora.studio.project_manager

import org.azora.sdk.core.presentation.util.*

data class ProjectManagerState(
    val projectName: FieldState<String> = FieldState.textField("NewProject"),
    val companyName: FieldState<String> = FieldState.textField("MyCompany"),
    val packageName: FieldState<String> = FieldState.textField("com.mycompany.newproject"),
    val platforms: PlatformSelection = PlatformSelection(),
    val creating: ProcessState = ProcessState(),
    val opening: ProcessState = ProcessState(),
    val deleting: ProcessState = ProcessState()
)

data class PlatformSelection(
    val android: Boolean = true,
    val ios: Boolean = true,
    val desktop: Boolean = false,
    val webWasm: Boolean = false,
    val webReact: Boolean = false,
    val server: Boolean = false
)