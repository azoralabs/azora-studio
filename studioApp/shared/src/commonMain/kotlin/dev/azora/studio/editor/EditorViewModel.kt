package dev.azora.studio.editor

import androidx.lifecycle.*
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.docking.domain.*
import dev.azora.studio.assets.*
import dev.azora.studio.content_browser.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * Stable id of the central editor dock node (the "middle-top" area, Unreal-style).
 *
 * Opened files (Azora Nodes, AzScript, text, scenes) are docked here so they sit
 * above the bottom utility tabs (Content Browser / Console / Problems) instead of
 * mixing in with them.
 */
const val EDITOR_AREA_NODE_ID = "node_editor_area"

/** Returns a copy of this node tree with every reference to panel [from] replaced by [to]. */
private fun DockNode.replacePanelId(from: String, to: String): DockNode = when (this) {
    is DockNode.Leaf -> if (panelId == from) copy(panelId = to) else this
    is DockNode.TabGroup -> copy(panels = panels.map { if (it == from) to else it })
    is DockNode.Split -> copy(
        first = first.replacePanelId(from, to),
        second = second.replacePanelId(from, to)
    )
}

/** Removes repeated references to the same panel id from a persisted layout. */
private fun DockNode.withUniquePanels(seen: MutableSet<String>): DockNode? = when (this) {
    is DockNode.Leaf -> takeIf { seen.add(panelId) }
    is DockNode.TabGroup -> copy(panels = panels.filter { seen.add(it) }).let { group ->
        group.takeIf { it.panels.isNotEmpty() }?.copy(
            activeTabIndex = group.activeTabIndex.coerceAtMost(group.panels.lastIndex)
        )
    }
    is DockNode.Split -> {
        val uniqueFirst = first.withUniquePanels(seen)
        val uniqueSecond = second.withUniquePanels(seen)
        when {
            uniqueFirst == null -> uniqueSecond
            uniqueSecond == null -> uniqueFirst
            else -> copy(first = uniqueFirst, second = uniqueSecond)
        }
    }
}

private fun DockLayout.withUniquePanels(): DockLayout {
    val seen = mutableSetOf<String>()
    val uniqueRoot = rootNode?.withUniquePanels(seen)
    val uniqueFloating = floatingWindows.mapNotNull { window ->
        window.content.withUniquePanels(seen)?.let { window.copy(content = it) }
    }
    return copy(
        rootNode = uniqueRoot,
        floatingWindows = uniqueFloating,
        panelDescriptors = panelDescriptors.filterKeys { it in seen }
    )
}

class StudioViewModel(
    private val project: AzoraProjectModel,
    private val projectPath: String,
    val dockStateManager: DockStateManager,
    private val projectRepository: AzoraProjectRepository,
    private val openFilesManager: OpenAzoraNodesFilesManager,
    private val openAzScriptFilesManager: OpenAzScriptFilesManager,
    private val openTextFilesManager: OpenTextFilesManager
) : ViewModel() {

    private val clazz = this::class.simpleName

    private val eventChannel = Channel<StudioEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(StudioState(project = project, projectPath = projectPath))
    val state = _state.asStateFlow()

    /** All available panel descriptors (never removed, even when panels are closed) */
    val availablePanels: List<DockPanelDescriptor> = listOf(
        DockPanelDescriptor(id = "content_browser", title = "Content Browser", minimumWidth = 220f, minimumHeight = 200f),
        DockPanelDescriptor(id = "console", title = "Console", minimumWidth = 200f, minimumHeight = 100f),
        DockPanelDescriptor(id = "problems", title = "Problems", minimumWidth = 200f, minimumHeight = 100f),
        DockPanelDescriptor(id = "settings", title = "Settings", minimumWidth = 400f, minimumHeight = 300f)
    )

    init {
        // Register default panels
        registerDefaultPanels()

        // Restore files and load layout asynchronously to avoid blocking the UI thread.
        // File restoration involves disk I/O and JSON parsing which must not run on EDT.
        viewModelScope.launch {
            restoreOpenFiles()
            restoreOpenAzScriptFiles()
            restoreOpenTextFiles()
            loadSavedLayout()

            // Start persistence only after restoration. Starting collectors
            // against the managers' initial empty maps can erase the saved
            // mappings before disk restoration completes.
            observeLayoutChanges()
            observeOpenFileMappings()
        }
    }

    private suspend fun restoreOpenFiles() {
        val savedMapping = project.settings.openAzoraNodesFiles
        println("[StudioViewModel] restoreOpenFiles called, savedMapping: $savedMapping")
        if (savedMapping.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                savedMapping.forEach { (panelId, filePath) ->
                    println("[StudioViewModel] Restoring file: panelId=$panelId, filePath=$filePath")
                    val success = openFilesManager.restoreFile(panelId, filePath)
                    println("[StudioViewModel] Restore result: $success")
                }
            }
            println("[StudioViewModel] After restore, openFiles: ${openFilesManager.openFiles.value.keys}")
        }
    }

    private fun loadSavedLayout() {
        val savedLayout = project.settings.dockLayout
        if (savedLayout != null) {
            try {
                dockStateManager.loadLayout(migrateLegacyPanels(savedLayout))
            } catch (e: Exception) {
                println("Failed to restore saved layout, using default: ${e.message}")
                setupDefaultLayout()
            }
        } else {
            setupDefaultLayout()
        }
    }

    /**
     * Migrates layouts saved before the "Project" panel was removed: any lingering
     * "project" tab is rewritten to the "Welcome" panel so it renders instead of
     * showing a blank, no-longer-registered tab.
     */
    private fun migrateLegacyPanels(layout: DockLayout): DockLayout {
        val migrated = if (!layout.panelDescriptors.containsKey("project")) {
            layout
        } else {
            layout.copy(
                rootNode = layout.rootNode?.replacePanelId("project", "welcome"),
                floatingWindows = layout.floatingWindows.map {
                    it.copy(content = it.content.replacePanelId("project", "welcome"))
                },
                panelDescriptors = layout.panelDescriptors - "project" +
                    ("welcome" to DockPanelDescriptor("welcome", "Welcome", minimumWidth = 200f, minimumHeight = 150f))
            )
        }
        return migrated.withUniquePanels()
    }

    private suspend fun restoreOpenTextFiles() {
        val savedMapping = project.settings.openTextFiles
        if (savedMapping.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                savedMapping.forEach { (panelId, filePath) ->
                    openTextFilesManager.restoreFile(panelId, filePath)
                }
            }
        }
    }

    private suspend fun restoreOpenAzScriptFiles() {
        val savedMapping = project.settings.openAzScriptFiles
        if (savedMapping.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                savedMapping.forEach { (panelId, filePath) ->
                    openAzScriptFilesManager.restoreFile(panelId, filePath)
                }
            }
        }
    }

    /** Persists all file-tab mappings atomically so concurrent collectors cannot overwrite extras. */
    private fun observeOpenFileMappings() {
        viewModelScope.launch {
            combine(
                openFilesManager.openFiles.map { files -> files.mapValues { it.value.filePath } },
                openAzScriptFilesManager.openFiles.map { files -> files.mapValues { it.value.filePath } },
                openTextFilesManager.openFiles.map { files -> files.mapValues { it.value.filePath } }
            ) { nodes, scripts, text -> Triple(nodes, scripts, text) }
                .distinctUntilChanged()
                .collect { (nodes, scripts, text) ->
                    try {
                        val currentProject = projectRepository.getProject()
                        if (currentProject is dev.azora.sdk.core.domain.util.Res.Success) {
                            val settings = currentProject.data.settings
                                .withOpenAzoraNodesFiles(nodes)
                                .withOpenAzScriptFiles(scripts)
                                .withOpenTextFiles(text)
                            projectRepository.updateProject(currentProject.data.copy(settings = settings))
                            projectRepository.saveProject(projectPath)
                        }
                    } catch (e: Exception) {
                        println("[StudioViewModel] Failed to save open file mappings: ${e.message}")
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeLayoutChanges() {
        viewModelScope.launch {
            dockStateManager.state
                .map { it.layout }
                .distinctUntilChanged()
                .drop(1)
                .debounce(500)
                .collect { layout ->
                    saveLayoutToProject(layout)
                }
        }
    }

    private suspend fun saveLayoutToProject(layout: DockLayout) {
        try {
            // Get fresh project from repository to avoid overwriting other settings
            val currentProject = projectRepository.getProject()
            if (currentProject is dev.azora.sdk.core.domain.util.Res.Success) {
                val updatedProject = currentProject.data.copy(
                    settings = currentProject.data.settings.withDockLayout(layout)
                )
                projectRepository.updateProject(updatedProject)
                projectRepository.saveProject(projectPath)
            }
        } catch (e: Exception) {
            println("Failed to save dock layout: ${e.message}")
        }
    }

    /**
     * Show a panel - if it's in a floating window, dock it; if hidden, add it back to the layout.
     */
    fun showPanel(panelId: String) {
        val currentState = dockStateManager.state.value
        val layout = currentState.layout

        // Check if panel is in a floating window
        val floatingWindow = layout.floatingWindows.find { window ->
            window.content.collectPanelIds().contains(panelId)
        }

        if (floatingWindow != null) {
            // Dock the floating window
            dockStateManager.dispatch(DockAction.DockFloatingWindow(floatingWindow.id, null, DockZone.CENTER))
        } else if (layout.panelDescriptors.containsKey(panelId)) {
            // Panel is already docked, just select it
            dockStateManager.dispatch(DockAction.SelectPanel(panelId))
        } else {
            // Panel is not visible, add it back
            val descriptor = availablePanels.find { it.id == panelId }
            if (descriptor != null) {
                dockStateManager.dispatch(DockAction.AddPanel(descriptor, null, DockZone.CENTER))
            }
        }
    }

    private fun registerDefaultPanels() {
        dockStateManager.registerPanel(
            DockPanelDescriptor(
                id = "content_browser",
                title = "Content Browser",
                minimumWidth = 220f,
                minimumHeight = 200f
            )
        )
        dockStateManager.registerPanel(
            DockPanelDescriptor(
                id = "console",
                title = "Console",
                minimumWidth = 200f,
                minimumHeight = 100f
            )
        )
        dockStateManager.registerPanel(
            DockPanelDescriptor(
                id = "problems",
                title = "Problems",
                minimumWidth = 200f,
                minimumHeight = 100f
            )
        )
        dockStateManager.registerPanel(
            DockPanelDescriptor(
                id = "settings",
                title = "Settings",
                minimumWidth = 400f,
                minimumHeight = 300f
            )
        )
    }

    private fun setupDefaultLayout() {
        // Unreal-style default layout for a freshly created/opened project:
        //
        //   ┌───────────────────────────────────────────────┐
        //   │  Project  (editor tabs open here, middle-top)  │  ← node_editor_area
        //   ├───────────────────────────────────────────────┤
        //   │  Content Browser │ Console │ Problems          │  ← bottom utility tabs
        //   └───────────────────────────────────────────────┘
        //
        // Opened files dock into node_editor_area; utility panels added via
        // View > Windows fall into the bottom group.

        val editorArea = DockNode.TabGroup(
            id = EDITOR_AREA_NODE_ID,
            panels = listOf("welcome"),
            activeTabIndex = 0
        )

        val bottomTabs = DockNode.TabGroup(
            id = "node_bottom_tabs",
            panels = listOf("content_browser", "console", "problems"),
            activeTabIndex = 0
        )

        val rootNode = DockNode.Split(
            id = "node_root_split",
            orientation = DockOrientation.VERTICAL,
            first = editorArea,
            second = bottomTabs,
            ratio = 0.7f
        )

        val layout = DockLayout(
            rootNode = rootNode,
            panelDescriptors = mapOf(
                "welcome" to DockPanelDescriptor("welcome", "Welcome", minimumWidth = 200f, minimumHeight = 150f),
                "content_browser" to DockPanelDescriptor("content_browser", "Content Browser", minimumWidth = 220f, minimumHeight = 200f),
                "console" to DockPanelDescriptor("console", "Console", minimumWidth = 200f, minimumHeight = 100f),
                "problems" to DockPanelDescriptor("problems", "Problems", minimumWidth = 200f, minimumHeight = 100f)
            )
        )

        dockStateManager.loadLayout(layout)
    }

    fun onAction(action: StudioAction) {
        when (action) {
            is StudioAction.DockAction -> {
                val panelIdsToClose = when (val dockAction = action.action) {
                    is DockAction.RemovePanel -> setOf(dockAction.panelId)
                    is DockAction.CloseFloatingWindow -> dockStateManager.state.value.layout
                        .floatingWindows
                        .find { it.id == dockAction.windowId }
                        ?.content
                        ?.collectPanelIds()
                        .orEmpty()
                    else -> emptySet()
                }
                dockStateManager.dispatch(action.action)
                panelIdsToClose.forEach(::closeManagedFile)
            }
        }
    }

    private fun closeManagedFile(panelId: String) {
        when {
            panelId.startsWith("azn_") -> viewModelScope.launch {
                if (openFilesManager.getState(panelId)?.isDirty == true) openFilesManager.saveFile(panelId)
                openFilesManager.closeFile(panelId)
            }
            panelId.startsWith("azs_") -> viewModelScope.launch {
                if (openAzScriptFilesManager.getState(panelId)?.isDirty == true) {
                    openAzScriptFilesManager.saveFile(panelId)
                }
                openAzScriptFilesManager.closeFile(panelId)
            }
            panelId.startsWith("txt_") -> viewModelScope.launch {
                if (openTextFilesManager.getState(panelId)?.isDirty == true) openTextFilesManager.saveFile(panelId)
                openTextFilesManager.closeFile(panelId)
            }
        }
    }
}
