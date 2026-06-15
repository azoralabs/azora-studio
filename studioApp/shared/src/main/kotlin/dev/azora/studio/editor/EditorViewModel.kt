package org.azora.studio.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.azora.studio.assets.OpenAzoraNodesFilesManager
import org.azora.studio.assets.openAzoraNodesFiles
import org.azora.studio.assets.withOpenAzoraNodesFiles
import org.azora.sdk.docking.domain.*
import org.azora.sdk.docking.domain.dockLayout
import org.azora.sdk.docking.domain.withDockLayout
import org.azora.sdk.core.project.domain.AzoraProjectModel
import org.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioViewModel(
    private val project: AzoraProjectModel,
    private val projectPath: String,
    val dockStateManager: DockStateManager,
    private val projectRepository: AzoraProjectRepository,
    private val openFilesManager: OpenAzoraNodesFilesManager
) : ViewModel() {

    private val clazz = this::class.simpleName

    private val eventChannel = Channel<StudioEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(StudioState(project = project, projectPath = projectPath))
    val state = _state.asStateFlow()

    /** All available panel descriptors (never removed, even when panels are closed) */
    val availablePanels: List<DockPanelDescriptor> = listOf(
        DockPanelDescriptor(id = "project", title = "Project", minimumWidth = 200f, minimumHeight = 150f),
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
            loadSavedLayout()
        }

        // Auto-save layout on changes (debounced)
        observeLayoutChanges()

        // Auto-save open files mapping on changes (debounced)
        observeOpenFilesChanges()
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
                dockStateManager.loadLayout(savedLayout)
            } catch (e: Exception) {
                println("Failed to restore saved layout, using default: ${e.message}")
                setupDefaultLayout()
            }
        } else {
            setupDefaultLayout()
        }
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
            if (currentProject is org.azora.sdk.core.domain.util.Res.Success) {
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
            if (currentProject is org.azora.sdk.core.domain.util.Res.Success) {
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
                id = "project",
                title = "Project",
                minimumWidth = 200f,
                minimumHeight = 150f
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
        // Default layout: bottom tabs with Project + Console
        // Plugin panels (Features, Navigations) are added via View > Windows

        val bottomTabs = DockNode.TabGroup(
            id = "node_bottom_tabs",
            panels = listOf("project", "console"),
            activeTabIndex = 0
        )

        val layout = DockLayout(
            rootNode = bottomTabs,
            panelDescriptors = mapOf(
                "project" to DockPanelDescriptor("project", "Project", minimumWidth = 200f, minimumHeight = 150f),
                "console" to DockPanelDescriptor("console", "Console", minimumWidth = 200f, minimumHeight = 100f)
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
