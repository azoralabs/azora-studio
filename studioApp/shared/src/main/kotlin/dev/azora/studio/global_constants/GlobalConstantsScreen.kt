package org.azora.studio.global_constants

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen composable that connects the GlobalConstantsViewModel to the GlobalConstantsPanel.
 *
 * @param projectPath The path of the current project
 */
@Composable
fun GlobalConstantsScreen(
    projectPath: String,
    viewModel: GlobalConstantsViewModel = koinViewModel { parametersOf(projectPath) }
) {
    val state by viewModel.state.collectAsState()

    if (!state.isLoading) {
        GlobalConstantsPanel(
            state = state,
            onAction = viewModel::onAction
        )
    }
}
