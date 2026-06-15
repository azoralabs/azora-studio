package dev.azora.studio.project_manager.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import dev.azora.studio.project_manager.*
import dev.azora.studio.project_manager.screen.component.*
import dev.azora.studio.project_manager.screen.create_project.CreateProjectDialog
import dev.azora.sdk.core.component.brand.AzoraBrandLogo
import dev.azora.sdk.core.component.indicator.AzoraLoadingIndicator
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.presentation.screen.*
import dev.azora.sdk.core.presentation.util.collectAsSWLC
import dev.azora.sdk.core.theme.LocalAzoraPalette
import org.koin.compose.koinInject

@Composable
fun ProjectManagerView(
    fileSystem: FileSystem = koinInject(),
    screenViewModel: ScreenViewModel = koinInject(),
    state: ProjectManagerState,
    viewModel: ProjectManagerViewModel
) {
    val palette = LocalAzoraPalette.current
    val screenState by screenViewModel.state.collectAsSWLC()

    // Ensure Azora directory exists on app start
    LaunchedEffect(Unit) {
        fileSystem.createDirectory("Azora")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        AzoraBrandLogo()

        Column(Modifier.fillMaxSize()) {
            Header(
                onNewProjectClick = {
                    screenViewModel.onAction(ScreenAction.PushDialog())
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ProjectsLazyGrid(
                    fileSystem = fileSystem,
                    onProjectClick = { projectName ->
                        viewModel.onAction(ProjectManagerAction.OnOpenProject(projectName))
                    },
                    onNewProjectClick = {
                        screenViewModel.onAction(ScreenAction.PushDialog())
                    }
                )
            }

            Footer()
        }

        // Dialog overlay
        if (screenState.onDialog) {
            CreateProjectDialog(viewModel)
        }

        // Loading overlay
        if (state.opening.inProcess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                AzoraLoadingIndicator(large = true)
            }
        }
    }
}