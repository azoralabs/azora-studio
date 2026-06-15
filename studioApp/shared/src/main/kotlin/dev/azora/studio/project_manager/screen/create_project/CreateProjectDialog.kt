package org.azora.studio.project_manager.screen.create_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.azora.studio.project_manager.ProjectManagerViewModel
import org.azora.sdk.core.component.dialog.AzoraDialog
import org.azora.sdk.core.presentation.screen.*
import org.azora.sdk.core.presentation.util.collectAsSWLC
import org.azora.sdk.core.theme.LocalAzoraPalette
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

    AzoraDialog(
        onDismissRequest = onPop,
        contentAlignment = Alignment.Start,
        bottom = {
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
    ) {
        CreateProjectView(state) { action ->
            viewModel.onAction(action)
        }
    }
}