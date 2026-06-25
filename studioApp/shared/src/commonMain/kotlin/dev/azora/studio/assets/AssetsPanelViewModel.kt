package dev.azora.studio.assets

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.*
import dev.azora.sdk.core.io.*
import dev.azora.sdk.docking.domain.*
import dev.azora.studio.editor.EDITOR_AREA_NODE_ID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Assets panel, handling file browser logic.
 */
class AssetsPanelViewModel(
    private val projectPath: String,
    private val fileSystem: FileSystem,
    private val openFilesManager: OpenAzoraNodesFilesManager,
    private val openSceneFilesManager: OpenAzoraSceneFilesManager,
    private val openTileMapFilesManager: OpenAzoraTileMapFilesManager,
    private val openAzScriptFilesManager: OpenAzScriptFilesManager,
    private val openAzsceneFilesManager: OpenAzsceneFilesManager,
    private val dockStateManager: DockStateManager
) : ViewModel() {

    private val assetsPath get() = "$projectPath/Assets"

    private val _state = MutableStateFlow(AssetsPanelState())
    val state: StateFlow<AssetsPanelState> = _state.asStateFlow()

    init {
        refresh()
        // Auto-refresh when scene files are created/saved on disk
        viewModelScope.launch {
            openSceneFilesManager.fileChangeSignal.drop(1).collect { refresh() }
        }
        // Auto-refresh when tilemap files are created/saved on disk
        viewModelScope.launch {
            openTileMapFilesManager.fileChangeSignal.drop(1).collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val items = loadDirectory(assetsPath)
                _state.value = _state.value.copy(items = items, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load assets: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadDirectory(path: String): List<AssetItem> {
        return when (val result = fileSystem.listDirectory(path)) {
            is ListResult.Success -> {
                result.files
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    .map { fileInfo ->
                        if (fileInfo.isDirectory) {
                            AssetItem.Folder(
                                path = fileInfo.path,
                                name = fileInfo.name,
                                children = if (_state.value.expandedFolders.contains(fileInfo.path)) {
                                    loadDirectory(fileInfo.path)
                                } else {
                                    emptyList()
                                }
                            )
                        } else {
                            AssetItem.File(
                                path = fileInfo.path,
                                name = fileInfo.name,
                                extension = fileInfo.name.substringAfterLast(".", "")
                            )
                        }
                    }
            }
            is ListResult.Error -> emptyList()
        }
    }

    fun toggleFolder(path: String) {
        val expanded = _state.value.expandedFolders
        _state.value = _state.value.copy(
            expandedFolders = if (expanded.contains(path)) {
                expanded - path
            } else {
                expanded + path
            }
        )
        refresh()
    }

    fun selectItem(path: String?) {
        _state.value = _state.value.copy(selectedPath = path)
    }

    fun showContextMenu(position: Offset, targetPath: String?) {
        _state.value = _state.value.copy(
            contextMenuPosition = position,
            contextMenuTargetPath = targetPath
        )
    }

    fun dismissContextMenu() {
        _state.value = _state.value.copy(
            contextMenuPosition = null,
            contextMenuTargetPath = null
        )
    }

    fun createFolder(parentPath: String?, name: String) {
        viewModelScope.launch {
            val basePath = parentPath ?: assetsPath
            val folderPath = "$basePath/$name"
            when (fileSystem.createDirectory(folderPath)) {
                is FileSystemResult.Success -> {
                    // Auto-expand parent folder
                    if (parentPath != null) {
                        _state.value = _state.value.copy(
                            expandedFolders = _state.value.expandedFolders + parentPath
                        )
                    }
                    refresh()
                }
                is FileSystemResult.Error -> {
                    _state.value = _state.value.copy(error = "Failed to create folder")
                }
            }
            dismissContextMenu()
        }
    }

    fun createAzoraNodesFile(parentPath: String?, name: String) {
        viewModelScope.launch {
            val basePath = parentPath ?: assetsPath
            val fileName = if (name.endsWith(".azn")) name else "$name.azn"
            val filePath = "$basePath/$fileName"

            if (openFilesManager.createNewFile(filePath)) {
                // Auto-expand parent folder
                if (parentPath != null) {
                    _state.value = _state.value.copy(
                        expandedFolders = _state.value.expandedFolders + parentPath
                    )
                }
                refresh()
            } else {
                _state.value = _state.value.copy(error = "Failed to create file")
            }
            dismissContextMenu()
        }
    }

    fun createMapFile(parentPath: String?, name: String) {
        viewModelScope.launch {
            val basePath = parentPath ?: assetsPath
            val fileName = if (name.endsWith(".azorascene")) name else "$name.azorascene"
            val filePath = "$basePath/$fileName"

            if (openSceneFilesManager.createNewFile(filePath)) {
                if (parentPath != null) {
                    _state.value = _state.value.copy(
                        expandedFolders = _state.value.expandedFolders + parentPath
                    )
                }
                refresh()
            } else {
                _state.value = _state.value.copy(error = "Failed to create map")
            }
            dismissContextMenu()
        }
    }

    fun openAzoraSceneFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openSceneFilesManager.openFile(filePath)
            if (panelId != null) {
                val state = openSceneFilesManager.getState(panelId) ?: return@launch
                dockStateManager.dispatch(
                    DockAction.AddPanel(
                        descriptor = DockPanelDescriptor(
                            id = panelId,
                            title = state.fileName,
                            closeable = true
                        ),
                        targetNodeId = EDITOR_AREA_NODE_ID
                    )
                )
            }
        }
    }

    /** Opens a generic `.azscene` file; the registered plugin (by its `type`) renders the editor. */
    fun openAzsceneFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openAzsceneFilesManager.openFile(filePath) ?: return@launch
            val state = openAzsceneFilesManager.getState(panelId) ?: return@launch
            dockStateManager.dispatch(
                DockAction.AddPanel(
                    descriptor = DockPanelDescriptor(id = panelId, title = state.fileName, closeable = true),
                    targetNodeId = EDITOR_AREA_NODE_ID
                )
            )
        }
    }

    fun createTileMapFile(parentPath: String?, name: String) {
        viewModelScope.launch {
            val basePath = parentPath ?: assetsPath
            val fileName = if (name.endsWith(".azoratilemap")) name else "$name.azoratilemap"
            val filePath = "$basePath/$fileName"

            if (openTileMapFilesManager.createNewFile(filePath)) {
                if (parentPath != null) {
                    _state.value = _state.value.copy(
                        expandedFolders = _state.value.expandedFolders + parentPath
                    )
                }
                refresh()
            } else {
                _state.value = _state.value.copy(error = "Failed to create tilemap")
            }
            dismissContextMenu()
        }
    }

    fun openAzoraTileMapFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openTileMapFilesManager.openFile(filePath)
            if (panelId != null) {
                val state = openTileMapFilesManager.getState(panelId) ?: return@launch
                dockStateManager.dispatch(
                    DockAction.AddPanel(
                        descriptor = DockPanelDescriptor(
                            id = panelId,
                            title = state.fileName,
                            closeable = true
                        ),
                        targetNodeId = EDITOR_AREA_NODE_ID
                    )
                )
            }
        }
    }

    fun createFile(parentPath: String?, name: String) {
        viewModelScope.launch {
            val basePath = parentPath ?: assetsPath
            val filePath = "$basePath/$name"

            when (fileSystem.writeToFile(filePath, "")) {
                is FileSystemResult.Success -> {
                    if (parentPath != null) {
                        _state.value = _state.value.copy(
                            expandedFolders = _state.value.expandedFolders + parentPath
                        )
                    }
                    refresh()
                }
                is FileSystemResult.Error -> {
                    _state.value = _state.value.copy(error = "Failed to create file")
                }
            }
            dismissContextMenu()
        }
    }

    fun createAzScriptFile(parentPath: String?, name: String, runnable: Boolean = false) {
        viewModelScope.launch {
            val basePath = parentPath ?: assetsPath
            val fileName = if (name.endsWith(".az")) name else "$name.az"
            val filePath = "$basePath/$fileName"

            if (openAzScriptFilesManager.createNewFile(filePath, runnable)) {
                if (parentPath != null) {
                    _state.value = _state.value.copy(
                        expandedFolders = _state.value.expandedFolders + parentPath
                    )
                }
                refresh()
            } else {
                _state.value = _state.value.copy(error = "Failed to create script")
            }
            dismissContextMenu()
        }
    }

    fun openAzoraNodesFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openFilesManager.openFile(filePath)
            if (panelId != null) {
                val state = openFilesManager.getState(panelId) ?: return@launch

                // Register the panel descriptor and add it to the dock
                dockStateManager.dispatch(
                    DockAction.AddPanel(
                        descriptor = DockPanelDescriptor(
                            id = panelId,
                            title = state.fileName,
                            closeable = true
                        ),
                        targetNodeId = EDITOR_AREA_NODE_ID
                    )
                )
            }
        }
    }

    fun openAzScriptFile(filePath: String) {
        viewModelScope.launch {
            val panelId = openAzScriptFilesManager.openFile(filePath)
            if (panelId != null) {
                val state = openAzScriptFilesManager.getState(panelId) ?: return@launch
                dockStateManager.dispatch(
                    DockAction.AddPanel(
                        descriptor = DockPanelDescriptor(
                            id = panelId,
                            title = state.fileName,
                            closeable = true
                        ),
                        targetNodeId = EDITOR_AREA_NODE_ID
                    )
                )
            }
        }
    }

    fun deleteItem(path: String) {
        viewModelScope.launch {
            // Determine if it's a file or directory
            val isDir = _state.value.items.findItemByPath(path) is AssetItem.Folder

            val result = if (isDir) {
                fileSystem.deleteDirectoryRecursively(path)
            } else {
                fileSystem.deleteFile(path)
            }

            when (result) {
                is FileSystemResult.Success -> {
                    refresh()
                }
                is FileSystemResult.Error -> {
                    _state.value = _state.value.copy(error = "Failed to delete item")
                }
            }
            dismissContextMenu()
        }
    }

    fun startRenaming(path: String) {
        _state.value = _state.value.copy(renamingPath = path)
        dismissContextMenu()
    }

    fun cancelRenaming() {
        _state.value = _state.value.copy(renamingPath = null)
    }

    fun renameItem(oldPath: String, newName: String) {
        viewModelScope.launch {
            val parentPath = oldPath.substringBeforeLast("/")
            val newPath = "$parentPath/$newName"

            val isDir = _state.value.items.findItemByPath(oldPath) is AssetItem.Folder

            val result = if (isDir) {
                fileSystem.renameDirectory(oldPath, newPath)
            } else {
                fileSystem.renameFile(oldPath, newPath)
            }

            when (result) {
                is FileSystemResult.Success -> {
                    _state.value = _state.value.copy(renamingPath = null)
                    refresh()
                }
                is FileSystemResult.Error -> {
                    _state.value = _state.value.copy(
                        error = "Failed to rename item",
                        renamingPath = null
                    )
                }
            }
        }
    }

    private fun List<AssetItem>.findItemByPath(path: String): AssetItem? {
        for (item in this) {
            if (item.path == path) return item
            if (item is AssetItem.Folder) {
                item.children.findItemByPath(path)?.let { return it }
            }
        }
        return null
    }
}
