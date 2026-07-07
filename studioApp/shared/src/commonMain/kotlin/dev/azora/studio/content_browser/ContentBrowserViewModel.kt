package dev.azora.studio.content_browser

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.azora.nodes.domain.AzToNodesConverter
import dev.azora.nodes.domain.AzToNodesResult
import dev.azora.nodes.domain.AznFiles
import dev.azora.nodes.domain.NodesToAzConverter
import dev.azora.sdk.core.io.ExistsResult
import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.FileSystemResult
import dev.azora.sdk.core.io.ListResult
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.studio.assets.AssetItem
import dev.azora.studio.editor.EDITOR_AREA_NODE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Heavy / generated folders hidden so the browser stays usable.
private val HIDDEN_FOLDERS = setOf(".gradle", "build", "node_modules", ".git", ".idea")

enum class ViewMode { GRID, LIST }

data class ContentBrowserState(
    val currentPath: String = "",
    val items: List<AssetItem> = emptyList(),
    val selectedPath: String? = null,
    val backStack: List<String> = emptyList(),
    val forwardStack: List<String> = emptyList(),
    val viewMode: ViewMode = ViewMode.GRID,
    val isLoading: Boolean = false,
    val error: String? = null,
    val contextMenuPosition: Offset? = null,
    val contextMenuTargetPath: String? = null
)

/** A clickable path segment in the breadcrumb bar. */
data class Breadcrumb(val name: String, val path: String)

/**
 * Backs the (Unreal-style) Content Browser: a single-folder view you navigate
 * into, with back / forward / up history and breadcrumbs. Supports create
 * folder/file, delete, rename, and opening text files as editor tabs.
 */
class ContentBrowserViewModel(
    projectPath: String,
    private val fileSystem: FileSystem,
    private val openTextFilesManager: OpenTextFilesManager,
    private val openAzsceneFilesManager: dev.azora.studio.assets.OpenAzsceneFilesManager,
    private val openAzoraNodesFilesManager: dev.azora.studio.assets.OpenAzoraNodesFilesManager,
    private val openAzScriptFilesManager: dev.azora.studio.assets.OpenAzScriptFilesManager,
    private val dockStateManager: DockStateManager,
    private val pluginManager: dev.azora.sdk.plugin.presentation.PluginManager? = null
) : ViewModel() {

    /** Creatable `.azscene` types contributed by plugins, for the "New …" menu. */
    fun azsceneTemplates(): List<dev.azora.sdk.plugin.core.AzsceneTemplate> =
        pluginManager?.azsceneTemplates().orEmpty()

    /** Creates a new `.azn` document of [type] (content from the owning plugin) and opens it. */
    fun createSceneFile(type: String, name: String) {
        viewModelScope.launch {
            val parent = resolveCreateParent()
            val fileName = if (name.endsWith(".azn")) name else "$name.azn"
            val path = "$parent/$fileName"
            val content = pluginManager?.newAzsceneContent(type) ?: "{\n  \"type\": \"$type\"\n}\n"
            when (fileSystem.writeToFile(path, content)) {
                is FileSystemResult.Success -> { refresh(); openAzsceneFile(path) }
                is FileSystemResult.Error -> _state.value = _state.value.copy(error = "Failed to create scene")
            }
            dismissContextMenu()
        }
    }

    private val rootAbs: String = fileSystem.getAbsolutePath(projectPath)
    private val rootName: String = projectPath.substringAfterLast("/")

    private val _state = MutableStateFlow(ContentBrowserState(currentPath = rootAbs))
    val state: StateFlow<ContentBrowserState> = _state.asStateFlow()

    init {
        refresh()
    }

    val canGoUp: Boolean get() = _state.value.currentPath != rootAbs

    fun refresh() {
        val path = _state.value.currentPath
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val items = loadDirectory(path)
                _state.value = _state.value.copy(items = items, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load folder: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadDirectory(path: String): List<AssetItem> {
        return when (val result = fileSystem.listDirectory(path)) {
            is ListResult.Success -> result.files
                .filterNot { it.isDirectory && it.name in HIDDEN_FOLDERS }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { fi ->
                    if (fi.isDirectory) {
                        AssetItem.Folder(path = fi.path, name = fi.name)
                    } else {
                        AssetItem.File(
                            path = fi.path,
                            name = fi.name,
                            extension = fi.name.substringAfterLast(".", "")
                        )
                    }
                }
            is ListResult.Error -> emptyList()
        }
    }

    // ---------- Navigation ----------

    /** Opens a folder (double-click) or jumps via breadcrumb. */
    fun navigateInto(folderPath: String) = pushAndNavigate(folderPath)

    fun navigateTo(absolutePath: String) = pushAndNavigate(absolutePath)

    fun navigateUp() {
        val current = _state.value.currentPath
        if (current != rootAbs) {
            pushAndNavigate(current.substringBeforeLast("/"))
        }
    }

    fun navigateBack() {
        val s = _state.value
        if (s.backStack.isEmpty()) return
        val previous = s.backStack.last()
        _state.value = s.copy(
            currentPath = previous,
            backStack = s.backStack.dropLast(1),
            forwardStack = s.forwardStack + s.currentPath,
            selectedPath = null
        )
        refresh()
    }

    fun navigateForward() {
        val s = _state.value
        if (s.forwardStack.isEmpty()) return
        val next = s.forwardStack.last()
        _state.value = s.copy(
            currentPath = next,
            forwardStack = s.forwardStack.dropLast(1),
            backStack = s.backStack + s.currentPath,
            selectedPath = null
        )
        refresh()
    }

    private fun pushAndNavigate(newPath: String) {
        val s = _state.value
        if (newPath == s.currentPath) return
        _state.value = s.copy(
            currentPath = newPath,
            backStack = s.backStack + s.currentPath,
            forwardStack = emptyList(),
            selectedPath = null
        )
        refresh()
    }

    /** Breadcrumb segments from the project root down to the current folder. */
    fun breadcrumbs(): List<Breadcrumb> {
        val current = _state.value.currentPath
        val result = mutableListOf(Breadcrumb(rootName, rootAbs))
        if (current.length > rootAbs.length && current.startsWith(rootAbs)) {
            val rel = current.substring(rootAbs.length).trimStart('/')
            var acc = rootAbs
            rel.split("/").filter { it.isNotEmpty() }.forEach { segment ->
                acc = "$acc/$segment"
                result.add(Breadcrumb(segment, acc))
            }
        }
        return result
    }

    fun setViewMode(mode: ViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    // ---------- Selection / context menu ----------

    fun selectItem(path: String?) {
        _state.value = _state.value.copy(selectedPath = path)
    }

    fun showContextMenu(position: Offset, targetPath: String?) {
        _state.value = _state.value.copy(contextMenuPosition = position, contextMenuTargetPath = targetPath)
    }

    fun dismissContextMenu() {
        _state.value = _state.value.copy(contextMenuPosition = null, contextMenuTargetPath = null)
    }

    // ---------- File / folder operations ----------

    /**
     * Folder to create inside: the targeted folder, else the current folder.
     */
    private fun resolveCreateParent(): String {
        val target = _state.value.contextMenuTargetPath ?: return _state.value.currentPath
        val item = _state.value.items.find { it.path == target }
        return when (item) {
            is AssetItem.Folder -> item.path
            is AssetItem.File -> _state.value.currentPath
            null -> _state.value.currentPath
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val parent = resolveCreateParent()
            when (fileSystem.createDirectory("$parent/$name")) {
                is FileSystemResult.Success -> refresh()
                is FileSystemResult.Error -> _state.value = _state.value.copy(error = "Failed to create folder")
            }
            dismissContextMenu()
        }
    }

    /** Creates a new azora node graph (`.azn` with a START node) and opens it. */
    fun createNodeGraphFile(name: String) {
        viewModelScope.launch {
            val parent = resolveCreateParent()
            val fileName = if (name.endsWith(".azn")) name else "$name.azn"
            val path = "$parent/$fileName"
            if (openAzoraNodesFilesManager.createNewFile(path)) {
                refresh()
                openAzoraNodesFile(path)
            } else {
                _state.value = _state.value.copy(error = "Failed to create node graph")
            }
            dismissContextMenu()
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val parent = resolveCreateParent()
            when (fileSystem.writeToFile("$parent/$name", "")) {
                is FileSystemResult.Success -> refresh()
                is FileSystemResult.Error -> _state.value = _state.value.copy(error = "Failed to create file")
            }
            dismissContextMenu()
        }
    }

    fun openTextFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openTextFilesManager.openFile(filePath) ?: run {
                _state.value = _state.value.copy(error = "Failed to open file: ${filePath.substringAfterLast('/')}")
                return@launch
            }
            val fileState = openTextFilesManager.getState(panelId) ?: return@launch
            dockStateManager.dispatch(
                DockAction.AddPanel(
                    DockPanelDescriptor(id = panelId, title = fileState.fileName, closeable = true),
                    EDITOR_AREA_NODE_ID,
                    DockZone.CENTER
                )
            )
            dockStateManager.dispatch(DockAction.SelectPanel(panelId))
        }
    }

    /** Opens a generic `.azscene` file in the plugin editor registered for its `type`. */
    fun openAzsceneFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openAzsceneFilesManager.openFile(filePath) ?: run {
                _state.value = _state.value.copy(error = "No editor for: ${filePath.substringAfterLast('/')}")
                return@launch
            }
            val st = openAzsceneFilesManager.getState(panelId) ?: return@launch
            dockStateManager.dispatch(
                DockAction.AddPanel(
                    DockPanelDescriptor(id = panelId, title = st.fileName, closeable = true),
                    EDITOR_AREA_NODE_ID,
                    DockZone.CENTER
                )
            )
            dockStateManager.dispatch(DockAction.SelectPanel(panelId))
        }
    }

    /** Opens a `.azn` file. `.azn` is a generic Azora node document: if its top-level `type` is owned
     *  by a plugin (e.g. a Website page/component), it opens in that plugin's editor; otherwise it
     *  opens in the built-in Azora Nodes editor. */
    fun openAznFile(filePath: String) {
        viewModelScope.launch {
            val type = openAzsceneFilesManager.peekType(filePath)
            if (type != null && pluginManager?.getAzsceneEditor(type, filePath) != null) {
                openAzsceneFile(filePath)
            } else {
                openAzoraNodesFile(filePath)
            }
        }
    }

    /** Opens an `.azn` file in the visual Azora Nodes editor. */
    fun openAzoraNodesFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openAzoraNodesFilesManager.openFile(filePath) ?: run {
                _state.value = _state.value.copy(error = "Failed to open: ${filePath.substringAfterLast('/')}")
                return@launch
            }
            val st = openAzoraNodesFilesManager.getState(panelId) ?: return@launch
            dockStateManager.dispatch(
                DockAction.AddPanel(
                    DockPanelDescriptor(id = panelId, title = st.fileName, closeable = true),
                    EDITOR_AREA_NODE_ID,
                    DockZone.CENTER
                )
            )
            dockStateManager.dispatch(DockAction.SelectPanel(panelId))
        }
    }

    /** Opens an `.az` file in the AzScript editor. */
    fun openAzScriptFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openAzScriptFilesManager.openFile(filePath) ?: run {
                _state.value = _state.value.copy(error = "Failed to open: ${filePath.substringAfterLast('/')}")
                return@launch
            }
            val st = openAzScriptFilesManager.getState(panelId) ?: return@launch
            dockStateManager.dispatch(
                DockAction.AddPanel(
                    DockPanelDescriptor(id = panelId, title = st.fileName, closeable = true),
                    EDITOR_AREA_NODE_ID,
                    DockZone.CENTER
                )
            )
            dockStateManager.dispatch(DockAction.SelectPanel(panelId))
        }
    }

    // ---------- .az ↔ .azn conversion ----------

    /**
     * Converts an `.az` source file into a sibling `.azn` node graph and opens
     * it in the node editor. Refuses to overwrite an existing `.azn`.
     */
    fun convertAzToNodes(azPath: String) {
        viewModelScope.launch {
            dismissContextMenu()
            val content = when (val read = fileSystem.readFromFile(azPath)) {
                is FileReadResult.Success -> read.content
                is FileReadResult.Error -> {
                    _state.value = _state.value.copy(error = "Failed to read: ${azPath.substringAfterLast('/')}")
                    return@launch
                }
            }
            val aznPath = AznFiles.siblingAznPath(azPath)
            if (fileSystem.fileExists(aznPath) is ExistsResult.Exists) {
                _state.value = _state.value.copy(error = "${aznPath.substringAfterLast('/')} already exists — delete or rename it first.")
                return@launch
            }
            val name = azPath.substringAfterLast('/').removeSuffix(".${AznFiles.AZ_EXTENSION}")
            when (val result = AzToNodesConverter().convert(content, name)) {
                is AzToNodesResult.Failure -> {
                    _state.value = _state.value.copy(error = "Cannot convert: ${result.errors.firstOrNull()}")
                }
                is AzToNodesResult.Success -> {
                    result.warnings.forEach { println("[ContentBrowser] convert $name.az → nodes: $it") }
                    when (fileSystem.writeToFile(aznPath, AznFiles.encode(result.graph))) {
                        is FileSystemResult.Success -> {
                            refresh()
                            openAzoraNodesFile(aznPath)
                        }
                        is FileSystemResult.Error -> {
                            _state.value = _state.value.copy(error = "Failed to write ${aznPath.substringAfterLast('/')}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates the sibling `.az` source from an `.azn` node graph and opens
     * it. The output carries the generated-file header, so the run pipeline
     * keeps it in sync with the graph; a hand-written `.az` at the same path
     * is never overwritten.
     */
    fun convertNodesToAz(aznPath: String) {
        viewModelScope.launch {
            dismissContextMenu()
            val content = when (val read = fileSystem.readFromFile(aznPath)) {
                is FileReadResult.Success -> read.content
                is FileReadResult.Error -> {
                    _state.value = _state.value.copy(error = "Failed to read: ${aznPath.substringAfterLast('/')}")
                    return@launch
                }
            }
            val graph = AznFiles.decode(content) ?: run {
                _state.value = _state.value.copy(error = "${aznPath.substringAfterLast('/')} is not an Azora node graph.")
                return@launch
            }
            val azPath = AznFiles.siblingAzPath(aznPath)
            when (val existing = fileSystem.readFromFile(azPath)) {
                is FileReadResult.Success -> if (!AznFiles.isGenerated(existing.content)) {
                    _state.value = _state.value.copy(error = "${azPath.substringAfterLast('/')} is hand-written — refusing to overwrite it.")
                    return@launch
                }
                is FileReadResult.Error -> Unit // no existing file — fine
            }
            val result = NodesToAzConverter().convert(graph)
            result.warnings.forEach { println("[ContentBrowser] generate ${azPath.substringAfterLast('/')}: $it") }
            val output = AznFiles.withGeneratedHeader(result.source, aznPath.substringAfterLast('/'))
            when (fileSystem.writeToFile(azPath, output)) {
                is FileSystemResult.Success -> {
                    refresh()
                    openAzScriptFile(azPath)
                }
                is FileSystemResult.Error -> {
                    _state.value = _state.value.copy(error = "Failed to write ${azPath.substringAfterLast('/')}")
                }
            }
        }
    }

    fun deleteItem(path: String) {
        viewModelScope.launch {
            val isDir = _state.value.items.find { it.path == path } is AssetItem.Folder
            val result = if (isDir) {
                fileSystem.deleteDirectoryRecursively(path)
            } else {
                fileSystem.deleteFile(path)
            }
            when (result) {
                is FileSystemResult.Success -> refresh()
                is FileSystemResult.Error -> _state.value = _state.value.copy(error = "Failed to delete item")
            }
            dismissContextMenu()
        }
    }

    fun renameItem(oldPath: String, newName: String) {
        viewModelScope.launch {
            val parent = oldPath.substringBeforeLast("/")
            val newPath = "$parent/$newName"
            val isDir = _state.value.items.find { it.path == oldPath } is AssetItem.Folder
            val result = if (isDir) {
                fileSystem.renameDirectory(oldPath, newPath)
            } else {
                fileSystem.renameFile(oldPath, newPath)
            }
            when (result) {
                is FileSystemResult.Success -> refresh()
                is FileSystemResult.Error -> _state.value = _state.value.copy(error = "Failed to rename item")
            }
            dismissContextMenu()
        }
    }
}
