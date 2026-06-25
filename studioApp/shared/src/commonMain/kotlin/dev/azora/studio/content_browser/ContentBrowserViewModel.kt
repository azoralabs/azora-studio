package dev.azora.studio.content_browser

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.FileSystemResult
import dev.azora.sdk.core.io.ListResult
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.studio.assets.AssetItem
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
    private val dockStateManager: DockStateManager,
    private val pluginManager: dev.azora.sdk.plugin.presentation.PluginManager? = null
) : ViewModel() {

    /** Creatable `.azscene` types contributed by plugins, for the "New …" menu. */
    fun azsceneTemplates(): List<dev.azora.sdk.plugin.core.AzsceneTemplate> =
        pluginManager?.azsceneTemplates().orEmpty()

    /** Creates a new `.azscene` file of [type] (content from the owning plugin) and opens it. */
    fun createSceneFile(type: String, name: String) {
        viewModelScope.launch {
            val parent = resolveCreateParent()
            val fileName = if (name.endsWith(".azscene")) name else "$name.azscene"
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
                    null,
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
                    null,
                    DockZone.CENTER
                )
            )
            dockStateManager.dispatch(DockAction.SelectPanel(panelId))
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
