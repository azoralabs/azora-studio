package dev.azora.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.azora.sdk.core.presentation.util.collectAsSWLC
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.docking.domain.DockState
import dev.azora.sdk.plugin.core.InstalledPlugin
import dev.azora.sdk.plugin.presentation.PluginManager
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun StudioScreen(
    project: AzoraProjectModel,
    projectPath: String,
    viewModel: StudioViewModel = koinViewModel { parametersOf(project, projectPath) },
    screenOffsetX: Float = 0f,
    screenOffsetY: Float = 0f,
    pluginManager: PluginManager? = null,
    enabledPlugins: List<InstalledPlugin> = emptyList()
) {
    val state by viewModel.state.collectAsSWLC()
    val dockState by viewModel.dockStateManager.state.collectAsState()

    StudioView(
        state = state,
        dockState = dockState,
        onAction = viewModel::onAction,
        screenOffsetX = screenOffsetX,
        screenOffsetY = screenOffsetY,
        project = project,
        pluginManager = pluginManager,
        enabledPlugins = enabledPlugins
    )
}

/**
 * Provides dock state and action handler for native floating windows.
 * This should be used at the application level to render floating windows.
 */
@Composable
fun rememberStudioDockState(
    project: AzoraProjectModel,
    projectPath: String,
    viewModel: StudioViewModel = koinViewModel { parametersOf(project, projectPath) }
): Pair<DockState, (StudioAction) -> Unit> {
    val dockState by viewModel.dockStateManager.state.collectAsState()
    return Pair(dockState, viewModel::onAction)
}