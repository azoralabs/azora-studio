package dev.azora.studio.settings

import androidx.compose.runtime.*
import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.sdk.plugin.core.PluginContext
import dev.azora.sdk.plugin.presentation.PluginManager
import dev.azora.studio.assets.OpenAzsceneFilesManager
import dev.azora.studio.editor.StudioPluginContext
import kotlinx.coroutines.delay
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SettingsScreen(
    projectPath: String,
    pluginManager: PluginManager? = null,
    pluginContext: PluginContext? = null,
    onLaunchPlugin: (String) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel { parametersOf(projectPath) }
) {
    val state by viewModel.state.collectAsState()

    val koin = getKoin()

    // Bypass the parameter chain (which can capture null from a stale remember block) by falling
    // back to the Koin singleton.
    val pm: PluginManager? = pluginManager
        ?: runCatching { koin.getOrNull<PluginManager>() }.getOrNull()

    // Build a PluginContext from Koin deps if the param is null (same stale-capture issue).
    val fileSystem: FileSystem = koinInject()
    val logger: AzoraLogger = koinInject()
    val repository: AzoraProjectRepository = koinInject()
    val openAzsceneFilesManager: OpenAzsceneFilesManager = koinInject()
    val dockStateManager: DockStateManager = koinInject()
    val scope = rememberCoroutineScope()

    var projectModel by remember { mutableStateOf<AzoraProjectModel?>(null) }
    LaunchedEffect(projectPath) {
        projectModel = (repository.getProject() as? Res.Success)?.data
    }

    val ctx: PluginContext? = pluginContext ?: remember(projectModel, pm) {
        if (projectModel != null) StudioPluginContext(
            project = projectModel!!,
            projectPath = projectPath,
            fileSystem = fileSystem,
            logger = logger,
            scope = scope,
            repository = repository,
            openAzsceneFilesManager = openAzsceneFilesManager,
            dockStateManager = dockStateManager
        ) else null
    }

    // Plugin loading is async — poll getSettingsTabs() briefly until the plugin is loaded.
    val installedPlugins = pm?.installedPlugins?.collectAsState()
    var pluginTabs by remember { mutableStateOf<List<PluginSettingsTab>>(emptyList()) }
    LaunchedEffect(pm, installedPlugins?.value) {
        pm?.let { mgr ->
            repeat(10) {
                val tabs = mgr.getSettingsTabs()
                if (tabs.isNotEmpty()) {
                    pluginTabs = tabs.map { (pid, desc) -> PluginSettingsTab(pid, desc.id, desc.label) }
                    return@LaunchedEffect
                }
                delay(500)
            }
        }
    }

    if (!state.isLoading) {
        SettingsPanel(
            state = state,
            onAction = viewModel::onAction,
            onLaunchPlugin = onLaunchPlugin,
            pluginTabs = pluginTabs,
            pluginTabContent = { pid, tid ->
                ctx?.let { context ->
                    pm?.getSettingsTabContent(pid, tid, context)?.invoke()
                }
            }
        )
    }
}
