package org.azora.studio.project_manager.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import org.azora.studio.project_manager.*
import org.azora.sdk.core.presentation.screen.*
import org.azora.sdk.core.presentation.util.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProjectManagerScreen(
    viewModel: ProjectManagerViewModel = koinViewModel(),
    screenViewModel: ScreenViewModel = koinInject(),
    onProjectSelected: (projectPath: String, projectName: String) -> Unit
) {
    val state by viewModel.state.collectAsSWLC()
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ProjectManagerEvent.ProjectCreated -> {
                screenViewModel.onAction(ScreenAction.PopDialog)
                scope.launch {
                    hostState.showSnackbar("Project ${event.projectName} created")
                }
                onProjectSelected(event.projectPath, event.projectName)
            }
            is ProjectManagerEvent.ProjectOpened -> {
                onProjectSelected(event.projectPath, event.projectName)
            }
            is ProjectManagerEvent.ProjectDeleted -> {
                scope.launch {
                    hostState.showSnackbar("Project ${event.projectName} deleted")
                }
            }
        }
    }

    ProjectManagerView(
        state = state,
        viewModel = viewModel
    )
}