package dev.azora.studio.settings

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen composable that connects the SettingsViewModel to the SettingsPanel.
 *
 * @param projectPath The path of the current project
 * @param onLaunchPlugin Callback when user wants to launch a plugin
 */
@Composable
fun SettingsScreen(
    projectPath: String,
    onLaunchPlugin: (String) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel { parametersOf(projectPath) }
) {
    val state by viewModel.state.collectAsState()

    if (!state.isLoading) {
        SettingsPanel(
            state = state,
            onAction = viewModel::onAction,
            onLaunchPlugin = onLaunchPlugin
        )
    }
}
