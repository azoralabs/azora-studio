package dev.azora.studio.settings

import androidx.compose.runtime.*
import dev.azora.sdk.plugin.core.PluginContext
import dev.azora.sdk.plugin.presentation.PluginManager
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen composable that connects the SettingsViewModel to the SettingsPanel.
 *
 * @param projectPath The path of the current project
 * @param pluginManager If non-null, plugin-contributed settings tabs are surfaced in the sidebar.
 * @param pluginContext Passed to plugin tab content renderers so they can read/write the project.
 * @param onLaunchPlugin Callback when user wants to launch a plugin
 */
@Composable
fun SettingsScreen(
    projectPath: String,
    pluginManager: PluginManager? = null,
    pluginContext: PluginContext? = null,
    onLaunchPlugin: (String) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel { parametersOf(projectPath) }
) {
    val state by viewModel.state.collectAsState()

    if (!state.isLoading) {
        val pluginTabs = pluginManager?.getSettingsTabs()?.map { (pid, desc) ->
            PluginSettingsTab(pid, desc.id, desc.label)
        } ?: emptyList()
        SettingsPanel(
            state = state,
            onAction = viewModel::onAction,
            onLaunchPlugin = onLaunchPlugin,
            pluginTabs = pluginTabs,
            pluginTabContent = { pid, tid ->
                pluginContext?.let { ctx ->
                    pluginManager?.getSettingsTabContent(pid, tid, ctx)?.invoke()
                }
            }
        )
    }
}
