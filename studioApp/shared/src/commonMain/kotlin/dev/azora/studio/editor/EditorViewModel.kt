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

class StudioViewModel(
    private val project: AzoraProjectModel,
    private val projectPath: String,
    val dockStateManager: DockStateManager,
    private val projectRepository: AzoraProjectRepository,
    private val openFilesManager: OpenAzoraNodesFilesManager,
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
            restoreOpenTextFiles()
            loadSavedLayout()
        }

        // Auto-save layout on changes (debounced)
        observeLayoutChanges()

        // Auto-save open files mapping on changes (debounced)
        observeOpenFilesChanges()

        // Auto-save open text files mapping on changes (debounced)
        observeOpenTextFilesChanges()
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
        if (!layout.panelDescriptors.containsKey("project")) return layout
        return layout.copy(
            rootNode = layout.rootNode?.replacePanelId("project", "welcome"),
            floatingWindows = layout.floatingWindows.map {
                it.copy(content = it.content.replacePanelId("project", "welcome"))
            },
            panelDescriptors = layout.panelDescriptors - "project" +
                ("welcome" to DockPanelDescriptor("welcome", "Welcome", minimumWidth = 200f, minimumHeight = 150f))
        )
    }

    @OptIn(FlowPreview::class)
    private fun observeOpenFilesChanges() {
        viewModelScope.launch {
            println("[StudioViewModel] Starting to observe openFiles changes")
            openFilesManager.openFiles
                .map { files ->
                    println("[StudioViewModel] openFiles changed: ${files.keys}")
                    files.mapValues { entry -> entry.value.filePath }
                }
                .distinctUntilChanged()
                .debounce(500)
                .collect { mapping ->
                    println("[StudioViewModel] After debounce, saving mapping: $mapping")
                    saveOpenFilesToProject(mapping)
                }
        }
    }

    private suspend fun saveOpenFilesToProject(mapping: Map<String, String>) {
        println("[StudioViewModel] saveOpenFilesToProject called with mapping: $mapping")
        try {
            val currentProject = projectRepository.getProject()
            println("[StudioViewModel] currentProject result: $currentProject")
            if (currentProject is dev.azora.sdk.core.domain.util.Res.Success) {
                val updatedProject = currentProject.data.copy(
                    settings = currentProject.data.settings.withOpenAzoraNodesFiles(mapping)
                )
                println("[StudioViewModel] Updating project with openAzoraNodesFiles: ${updatedProject.settings.openAzoraNodesFiles}")
                projectRepository.updateProject(updatedProject)
                projectRepository.saveProject(projectPath)
                println("[StudioViewModel] Project saved successfully")
            }
        } catch (e: Exception) {
            println("[StudioViewModel] Failed to save open files mapping: ${e.message}")
            e.printStackTrace()
        }
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

    @OptIn(FlowPreview::class)
    private fun observeOpenTextFilesChanges() {
        viewModelScope.launch {
            openTextFilesManager.openFiles
                .map { files -> files.mapValues { it.value.filePath } }
                .distinctUntilChanged()
                .debounce(500)
                .collect { mapping -> saveOpenTextFilesToProject(mapping) }
        }
    }

    private suspend fun saveOpenTextFilesToProject(mapping: Map<String, String>) {
        try {
            val currentProject = projectRepository.getProject()
            if (currentProject is dev.azora.sdk.core.domain.util.Res.Success) {
                val updatedProject = currentProject.data.copy(
                    settings = currentProject.data.settings.withOpenTextFiles(mapping)
                )
                projectRepository.updateProject(updatedProject)
                projectRepository.saveProject(projectPath)
            }
        } catch (e: Exception) {
            println("[StudioViewModel] Failed to save open text files mapping: ${e.message}")
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeLayoutChanges() {
        viewModelScope.launch {
            dockStateManager.state
                .map { it.layout }
                .distinctUntilChanged()
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
                dockStateManager.dispatch(action.action)
            }
        }
    }
}
