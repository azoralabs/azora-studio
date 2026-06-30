package dev.azora.studio.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import azora.azora_studio.app.generated.resources.*
import dev.azora.canvas.domain.interpreter.*
import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.container.DockContainer
import dev.azora.sdk.docking.presentation.panel.DockPanelRegistry
import dev.azora.sdk.docking.presentation.theme.DockTheme
import dev.azora.sdk.plugin.core.InstalledPlugin
import dev.azora.sdk.plugin.presentation.PluginManager
import dev.azora.studio.assets.*
import dev.azora.studio.az_script.AzScriptFilePanel
import dev.azora.studio.az_script.DiagnosticsManager
import dev.azora.studio.azora_nodes.AzoraNodesFilePanel
import dev.azora.studio.content_browser.ContentBrowserPanel
import dev.azora.studio.content_browser.ContentBrowserViewModel
import dev.azora.studio.content_browser.OpenTextFilesManager
import dev.azora.studio.content_browser.TextFilePanel
import dev.azora.studio.settings.SettingsScreen
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.koinInject

@Composable
fun StudioView(
    state: StudioState,
    dockState: DockState,
    onAction: (StudioAction) -> Unit,
    screenOffsetX: Float = 0f,
    screenOffsetY: Float = 0f,
    project: AzoraProjectModel? = null,
    pluginManager: PluginManager? = null,
    enabledPlugins: List<InstalledPlugin> = emptyList()
) {
    val palette = LocalAzoraPalette.current
    val projectPath = state.projectPath

    // Inject managers for assets
    val openFilesManager: OpenAzoraNodesFilesManager = koinInject()
    val openFiles by openFilesManager.openFiles.collectAsState()
    val openAzScriptFilesManager: OpenAzScriptFilesManager = koinInject()
    val azScriptOpenFiles by openAzScriptFilesManager.openFiles.collectAsState()
    val fileSystem: dev.azora.sdk.core.io.FileSystem = koinInject()
    val dockStateManager: DockStateManager = koinInject()
    val openTextFilesManager: OpenTextFilesManager = koinInject()
    val openTextFiles by openTextFilesManager.openFiles.collectAsState()
    val openAzsceneFilesManager: OpenAzsceneFilesManager = koinInject()
    val openAzsceneFiles by openAzsceneFilesManager.openFiles.collectAsState()

    // Host context dependencies handed to plugin content
    val pluginLogger: AzoraLogger = koinInject()
    val projectRepository: AzoraProjectRepository = koinInject()
    val undoRedoCoordinator: dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator = koinInject()
    val pluginScope = rememberCoroutineScope()

    // Create ContentBrowserViewModel (the project's single file browser)
    val contentBrowserViewModel = remember(projectPath) {
        ContentBrowserViewModel(
            projectPath = projectPath,
            fileSystem = fileSystem,
            openTextFilesManager = openTextFilesManager,
            openAzsceneFilesManager = openAzsceneFilesManager,
            openAzoraNodesFilesManager = openFilesManager,
            openAzScriptFilesManager = openAzScriptFilesManager,
            dockStateManager = dockStateManager,
            pluginManager = pluginManager
        )
    }

    // Get panel IDs from dock layout that need dynamic registration
    // Combine panels from dock layout AND openFiles to ensure all are registered
    val azoraNodesPanelIds = remember(dockState.layout.panelDescriptors, openFiles) {
        val layoutPanelIds = dockState.layout.panelDescriptors.keys
            .filter { it.startsWith("azn_") }
            .toSet()
        val openFilePanelIds = openFiles.keys
        layoutPanelIds + openFilePanelIds
    }

    val azoraScenePanelIds = remember(dockState.layout.panelDescriptors) {
        dockState.layout.panelDescriptors.keys
            .filter { it.startsWith("azorascene_") }
            .toSet()
    }

    val azscenePanelIds = remember(dockState.layout.panelDescriptors, openAzsceneFiles) {
        dockState.layout.panelDescriptors.keys.filter { it.startsWith("azscene_") }.toSet() + openAzsceneFiles.keys
    }

    val azScriptPanelIds = remember(dockState.layout.panelDescriptors, azScriptOpenFiles) {
        val layoutIds = dockState.layout.panelDescriptors.keys.filter { it.startsWith("azs_") }.toSet()
        val openIds = azScriptOpenFiles.keys
        layoutIds + openIds
    }

    val textPanelIds = remember(dockState.layout.panelDescriptors, openTextFiles) {
        val layoutIds = dockState.layout.panelDescriptors.keys.filter { it.startsWith("txt_") }.toSet()
        val openIds = openTextFiles.keys
        layoutIds + openIds
    }

    // Register panel content synchronously so it's available on first render
    val panelRegistry = remember(projectPath, enabledPlugins, azoraNodesPanelIds, azoraScenePanelIds, azscenePanelIds, azScriptPanelIds, textPanelIds) {
        DockPanelRegistry().apply {
            register("welcome") { WelcomePanel(projectPath = projectPath) }
            register("console") { ConsolePanel() }
            register("problems") { ProblemsPanel() }
            register("settings") {
                SettingsScreen(
                    projectPath = projectPath,
                    pluginManager = pluginManager,
                    pluginContext = if (project != null && pluginManager != null)
                        StudioPluginContext(
                            project = project, projectPath = projectPath, fileSystem = fileSystem,
                            logger = pluginLogger, scope = pluginScope, repository = projectRepository,
                            openAzsceneFilesManager = openAzsceneFilesManager, dockStateManager = dockStateManager,
                            undoRedoCoordinator = undoRedoCoordinator
                        )
                    else null
                )
            }
            register("content_browser") { ContentBrowserPanel(viewModel = contentBrowserViewModel) }

            // Register dynamic panels for .azn files (from layout and openFiles)
            azoraNodesPanelIds.forEach { panelId ->
                register(panelId) {
                    AzoraNodesFilePanel(panelId = panelId, projectPath = projectPath)
                }
            }

            // Register dynamic panels for .az script files
            azScriptPanelIds.forEach { panelId ->
                register(panelId) {
                    AzScriptFilePanel(panelId = panelId, projectPath = projectPath)
                }
            }

            // Register dynamic panels for text files opened in the Content Browser
            textPanelIds.forEach { panelId ->
                register(panelId) {
                    TextFilePanel(panelId = panelId, projectPath = projectPath)
                }
            }

            // Plugin content (Scene Studio panels + enabled plugins) needs a host PluginContext.
            if (project != null && pluginManager != null) {
                val pluginContext = StudioPluginContext(
                    project = project,
                    projectPath = projectPath,
                    fileSystem = fileSystem,
                    logger = pluginLogger,
                    scope = pluginScope,
                    repository = projectRepository,
                    openAzsceneFilesManager = openAzsceneFilesManager,
                    dockStateManager = dockStateManager,
                    undoRedoCoordinator = undoRedoCoordinator
                )

                // Register dynamic panels for .azorascene files (delegated to Scene Studio plugin)
                azoraScenePanelIds.forEach { panelId ->
                    register(panelId) {
                        pluginManager.getPluginPanelContent("dev.azora.scene_studio", panelId)
                            ?.invoke(pluginContext)
                    }
                }

                // Generic .azscene panels: route to the plugin registered for the file's `type`.
                // Restores its state from the panel id so it survives a Studio restart.
                azscenePanelIds.forEach { panelId ->
                    register(panelId) {
                        AzscenePanel(panelId, openAzsceneFilesManager, pluginManager, pluginContext)
                    }
                }

                // Register enabled plugin panels
                enabledPlugins.forEach { plugin ->
                    val panels = pluginManager.getPluginPanels(plugin.id)
                    if (panels.isNotEmpty()) {
                        // Group panels by group name
                        val grouped = panels.filter { it.group != null }.groupBy { it.group!! }
                        val ungrouped = panels.filter { it.group == null }

                        // Grouped panels: single wrapper with nested dock
                        grouped.forEach { (groupName, groupPanels) ->
                            register("plugin_group_${plugin.id}_${groupName}") {
                                PluginGroupPanel(
                                    pluginId = plugin.id,
                                    panels = groupPanels,
                                    pluginManager = pluginManager,
                                    pluginContext = pluginContext
                                )
                            }
                        }

                        // Ungrouped panels: register individually
                        ungrouped.forEach { panel ->
                            val panelContent = pluginManager.getPluginPanelContent(plugin.id, panel.id)
                            if (panelContent != null) {
                                register("plugin_${plugin.id}_${panel.id}") {
                                    panelContent(pluginContext)
                                }
                            }
                        }
                    } else {
                        // Single-panel plugin: register via Content()
                        val content = pluginManager.getPluginContent(plugin.id)
                        if (content != null) {
                            register("plugin_${plugin.id}") {
                                content(pluginContext)
                            }
                        }
                    }
                }
            }
        }
    }

    DockTheme(registry = panelRegistry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            DockContainer(
                layout = dockState.layout,
                dragState = dockState.dragState,
                maximizedPanelId = dockState.maximizedPanelId,
                onAction = { action ->
                    onAction(StudioAction.DockAction(action))
                },
                modifier = Modifier.fillMaxSize(),
                renderFloatingWindows = false, // Native windows rendered separately
                screenOffsetX = screenOffsetX,
                screenOffsetY = screenOffsetY
            )
        }
    }
}

/**
 * Provides dock theming and registry for native floating windows.
 * Should be called at the application level to render floating windows as real OS windows.
 */
@Composable
fun StudioFloatingWindowsProvider(
    projectPath: String,
    dockState: DockState,
    onAction: (StudioAction) -> Unit,
    project: AzoraProjectModel? = null,
    pluginManager: PluginManager? = null,
    enabledPlugins: List<InstalledPlugin> = emptyList(),
    content: @Composable () -> Unit
) {
    val palette = LocalAzoraPalette.current

    // Inject managers for assets
    val openFilesManager: OpenAzoraNodesFilesManager = koinInject()
    val openFiles by openFilesManager.openFiles.collectAsState()
    val openAzScriptFilesManager: OpenAzScriptFilesManager = koinInject()
    val azScriptOpenFiles by openAzScriptFilesManager.openFiles.collectAsState()
    val fileSystem: dev.azora.sdk.core.io.FileSystem = koinInject()
    val dockStateManager: DockStateManager = koinInject()
    val openTextFilesManager: OpenTextFilesManager = koinInject()
    val openTextFiles by openTextFilesManager.openFiles.collectAsState()
    val openAzsceneFilesManager: OpenAzsceneFilesManager = koinInject()
    val openAzsceneFiles by openAzsceneFilesManager.openFiles.collectAsState()

    // Host context dependencies handed to plugin content
    val pluginLogger: AzoraLogger = koinInject()
    val projectRepository: AzoraProjectRepository = koinInject()
    val undoRedoCoordinator: dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator = koinInject()
    val pluginScope = rememberCoroutineScope()

    // Create ContentBrowserViewModel (the project's single file browser)
    val contentBrowserViewModel = remember(projectPath) {
        ContentBrowserViewModel(
            projectPath = projectPath,
            fileSystem = fileSystem,
            openTextFilesManager = openTextFilesManager,
            openAzsceneFilesManager = openAzsceneFilesManager,
            openAzoraNodesFilesManager = openFilesManager,
            openAzScriptFilesManager = openAzScriptFilesManager,
            dockStateManager = dockStateManager,
            pluginManager = pluginManager
        )
    }

    // Get panel IDs from dock layout that need dynamic registration
    // Combine panels from dock layout AND openFiles to ensure all are registered
    val azoraNodesPanelIds = remember(dockState.layout.panelDescriptors, openFiles) {
        val layoutPanelIds = dockState.layout.panelDescriptors.keys
            .filter { it.startsWith("azn_") }
            .toSet()
        val openFilePanelIds = openFiles.keys
        layoutPanelIds + openFilePanelIds
    }

    val azoraScenePanelIds = remember(dockState.layout.panelDescriptors) {
        dockState.layout.panelDescriptors.keys
            .filter { it.startsWith("azorascene_") }
            .toSet()
    }

    val azscenePanelIds = remember(dockState.layout.panelDescriptors, openAzsceneFiles) {
        dockState.layout.panelDescriptors.keys.filter { it.startsWith("azscene_") }.toSet() + openAzsceneFiles.keys
    }

    val azScriptPanelIds = remember(dockState.layout.panelDescriptors, azScriptOpenFiles) {
        val layoutIds = dockState.layout.panelDescriptors.keys.filter { it.startsWith("azs_") }.toSet()
        val openIds = azScriptOpenFiles.keys
        layoutIds + openIds
    }

    val textPanelIds = remember(dockState.layout.panelDescriptors, openTextFiles) {
        val layoutIds = dockState.layout.panelDescriptors.keys.filter { it.startsWith("txt_") }.toSet()
        val openIds = openTextFiles.keys
        layoutIds + openIds
    }

    // Register panel content synchronously so it's available on first render
    val panelRegistry = remember(projectPath, enabledPlugins, azoraNodesPanelIds, azoraScenePanelIds, azscenePanelIds, azScriptPanelIds, textPanelIds) {
        DockPanelRegistry().apply {
            register("welcome") { WelcomePanel(projectPath = projectPath) }
            register("console") { ConsolePanel() }
            register("problems") { ProblemsPanel() }
            register("settings") {
                SettingsScreen(
                    projectPath = projectPath,
                    pluginManager = pluginManager,
                    pluginContext = if (project != null && pluginManager != null)
                        StudioPluginContext(
                            project = project, projectPath = projectPath, fileSystem = fileSystem,
                            logger = pluginLogger, scope = pluginScope, repository = projectRepository,
                            openAzsceneFilesManager = openAzsceneFilesManager, dockStateManager = dockStateManager,
                            undoRedoCoordinator = undoRedoCoordinator
                        )
                    else null
                )
            }
            register("content_browser") { ContentBrowserPanel(viewModel = contentBrowserViewModel) }

            // Register dynamic panels for .azn files (from layout and openFiles)
            azoraNodesPanelIds.forEach { panelId ->
                register(panelId) {
                    AzoraNodesFilePanel(panelId = panelId, projectPath = projectPath)
                }
            }

            // Register dynamic panels for .az script files
            azScriptPanelIds.forEach { panelId ->
                register(panelId) {
                    AzScriptFilePanel(panelId = panelId, projectPath = projectPath)
                }
            }

            // Register dynamic panels for text files opened in the Content Browser
            textPanelIds.forEach { panelId ->
                register(panelId) {
                    TextFilePanel(panelId = panelId, projectPath = projectPath)
                }
            }

            // Plugin content (Scene Studio panels + enabled plugins) needs a host PluginContext.
            if (project != null && pluginManager != null) {
                val pluginContext = StudioPluginContext(
                    project = project,
                    projectPath = projectPath,
                    fileSystem = fileSystem,
                    logger = pluginLogger,
                    scope = pluginScope,
                    repository = projectRepository,
                    openAzsceneFilesManager = openAzsceneFilesManager,
                    dockStateManager = dockStateManager,
                    undoRedoCoordinator = undoRedoCoordinator
                )

                // Register dynamic panels for .azorascene files (delegated to Scene Studio plugin)
                azoraScenePanelIds.forEach { panelId ->
                    register(panelId) {
                        pluginManager.getPluginPanelContent("dev.azora.scene_studio", panelId)
                            ?.invoke(pluginContext)
                    }
                }

                // Generic .azscene panels: route to the plugin registered for the file's `type`.
                // Restores its state from the panel id so it survives a Studio restart.
                azscenePanelIds.forEach { panelId ->
                    register(panelId) {
                        AzscenePanel(panelId, openAzsceneFilesManager, pluginManager, pluginContext)
                    }
                }

                // Register enabled plugin panels
                enabledPlugins.forEach { plugin ->
                    val panels = pluginManager.getPluginPanels(plugin.id)
                    if (panels.isNotEmpty()) {
                        // Group panels by group name
                        val grouped = panels.filter { it.group != null }.groupBy { it.group!! }
                        val ungrouped = panels.filter { it.group == null }

                        // Grouped panels: single wrapper with nested dock
                        grouped.forEach { (groupName, groupPanels) ->
                            register("plugin_group_${plugin.id}_${groupName}") {
                                PluginGroupPanel(
                                    pluginId = plugin.id,
                                    panels = groupPanels,
                                    pluginManager = pluginManager,
                                    pluginContext = pluginContext
                                )
                            }
                        }

                        // Ungrouped panels: register individually
                        ungrouped.forEach { panel ->
                            val panelContent = pluginManager.getPluginPanelContent(plugin.id, panel.id)
                            if (panelContent != null) {
                                register("plugin_${plugin.id}_${panel.id}") {
                                    panelContent(pluginContext)
                                }
                            }
                        }
                    } else {
                        val content = pluginManager.getPluginContent(plugin.id)
                        if (content != null) {
                            register("plugin_${plugin.id}") {
                                content(pluginContext)
                            }
                        }
                    }
                }
            }
        }
    }

    DockTheme(registry = panelRegistry) {
        content()
    }
}

/**
 * Renders a generic `.azscene` panel by delegating to the plugin registered for the file's `type`.
 * Lazily [restores][OpenAzsceneFilesManager.restore] the file from the (path-encoding) panel id so a
 * panel persisted in the dock layout still opens after a Studio restart.
 */
@Composable
private fun AzscenePanel(
    panelId: String,
    manager: OpenAzsceneFilesManager,
    pluginManager: PluginManager,
    context: dev.azora.sdk.plugin.core.PluginContext
) {
    val palette = LocalAzoraPalette.current
    val openFiles by manager.openFiles.collectAsState()
    LaunchedEffect(panelId) { manager.restore(panelId) }

    val st = openFiles[panelId]
    if (st == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…", color = palette.contentMid, fontSize = 12.sp)
        }
        return
    }
    val editor = pluginManager.getAzsceneEditor(st.type, st.filePath)
    if (editor != null) {
        editor(context)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No plugin handles '${st.type}'", color = palette.contentLow, fontSize = 12.sp)
        }
    }
}

/**
 * Console panel - real log output from AzoraNodes and system.
 */
@Composable
internal fun ConsolePanel() {
    val consoleOutputManager: ConsoleOutputManager = koinInject()
    val messages by consoleOutputManager.messages.collectAsState()
    val palette = LocalAzoraPalette.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(palette.surfaceTop)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Console",
                color = palette.contentTop,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Clear",
                color = palette.contentMid,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { consoleOutputManager.clear() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No output",
                    color = palette.contentLow,
                    fontSize = 11.sp
                )
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(messages) { message ->
                        val textColor = when (message.type) {
                            ConsoleMessageType.OUTPUT -> palette.contentTop
                            ConsoleMessageType.ERROR -> Color(0xFFFF6666)
                            ConsoleMessageType.WARNING -> Color(0xFFFFCC00)
                            ConsoleMessageType.INFO -> palette.contentLow
                        }
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}


/**
 * Problems panel - shows all diagnostics across open script files.
 */
@Composable
internal fun ProblemsPanel() {
    val diagnosticsManager: DiagnosticsManager = koinInject()
    val dockStateManager: DockStateManager = koinInject()
    val allDiagnostics by diagnosticsManager.allDiagnostics.collectAsState()
    val palette = LocalAzoraPalette.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with error count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(palette.surfaceTop)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Problems",
                color = palette.contentTop,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (allDiagnostics.isNotEmpty()) {
                val clipboardManager = LocalClipboardManager.current
                Text(
                    text = "${allDiagnostics.size} error${if (allDiagnostics.size != 1) "s" else ""}",
                    color = Color(0xFFFF6666),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Color(0xFFFF6666).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Copy All",
                    color = palette.contentMid,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val text = allDiagnostics.joinToString("\n") { d ->
                                val ln = if (d.diagnostic.line > 0) ":${d.diagnostic.line}" else ""
                                "${d.fileName}$ln  ${d.diagnostic.message}"
                            }
                            clipboardManager.setText(AnnotatedString(text))
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (allDiagnostics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No problems",
                    color = palette.contentLow,
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(allDiagnostics) { fileDiag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .clickable(onClick = {
                                    dockStateManager.dispatch(DockAction.SelectPanel(fileDiag.panelId))
                                    diagnosticsManager.requestNavigation(fileDiag.panelId, fileDiag.diagnostic.line)
                                })
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "\u25CF",
                                color = Color(0xFFFF4444),
                                fontSize = 8.sp
                            )
                            Text(
                                text = fileDiag.fileName,
                                color = palette.contentMid,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (fileDiag.diagnostic.line > 0) {
                                Text(
                                    text = "Ln ${fileDiag.diagnostic.line}",
                                    color = palette.contentLow,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = fileDiag.diagnostic.message,
                                color = Color(0xFFFF6666),
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Welcome tab shown in the editor area of a freshly created/opened project.
 *
 * Purely informational. Once the user closes it, it is gone for good — it is not
 * listed under View > Windows, so there is no way to bring it back.
 */
@Composable
internal fun WelcomePanel(projectPath: String) {
    val palette = LocalAzoraPalette.current
    val projectName = projectPath.substringAfterLast('/').ifBlank { "your project" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Welcome to Azora Studio",
                color = palette.contentTop,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = projectName,
                color = palette.contentMid,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Use the Content Browser below to explore your project files,\n" +
                    "create folders and files, and open them in the editor.",
                color = palette.contentLow,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Generic placeholder for panels.
 */

@Composable
internal fun PanelPlaceholder(
    title: String,
    subtitle: String,
    icon: Painter
) {
    val palette = LocalAzoraPalette.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.contentTop.copy(alpha = 0.2f)),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = palette.contentTop.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = palette.contentTop.copy(alpha = 0.25f),
                fontSize = 11.sp
            )
        }
    }
}