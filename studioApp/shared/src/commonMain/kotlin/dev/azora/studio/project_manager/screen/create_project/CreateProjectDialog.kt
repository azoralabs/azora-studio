package dev.azora.studio.project_manager.screen.create_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azora.studio.project_manager.ProjectManagerViewModel
import dev.azora.sdk.core.presentation.screen.*
import dev.azora.sdk.core.presentation.util.collectAsSWLC
import dev.azora.sdk.core.theme.LocalAzoraPalette
import org.koin.compose.koinInject

@Composable
fun CreateProjectDialog(
    viewModel: ProjectManagerViewModel,
    screenViewModel: ScreenViewModel = koinInject()
) {
    val palette = LocalAzoraPalette.current
    val state by viewModel.state.collectAsSWLC()
    val onPop = { screenViewModel.onAction(ScreenAction.PopDialog) }

    LaunchedEffect(Unit) {
        screenViewModel.onAction(ScreenAction.PushDialog())
    }

    NewProjectWindow(onCloseRequest = onPop) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CreateProjectView(state) { action ->
                viewModel.onAction(action)
            }

            // Project location footer
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Project Location",
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Light,
                    color = palette.contentLow
                )
                Text(
                    text = "~/Documents/Azora/${state.projectName.field.ifBlank { "MyProject" }}",
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = palette.contentMid
                )
            }
        }
    }
}
