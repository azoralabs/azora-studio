package dev.azora.studio.editor

import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoProvider
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.plugin.core.PluginContext
import dev.azora.sdk.plugin.core.PluginUndoRedoFacade
import dev.azora.sdk.plugin.core.PluginUndoRedoProvider
import dev.azora.studio.assets.OpenAzsceneFilesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Studio's [PluginContext] implementation handed to plugins when their content is rendered.
 *
 * [saveProject] persists a (possibly mutated) project model back into the in-memory store and the
 * `project.azora` file, so plugin edits survive reloads. Persistence runs off the UI thread.
 *
 * [openScene] opens an `.azscene` file in an editor tab; it needs [openAzsceneFilesManager] and
 * [dockStateManager] (both optional — when absent, [openScene] is a no-op, e.g. for the game context).
 */
class StudioPluginContext(
    override val project: AzoraProjectModel,
    override val projectPath: String,
    override val fileSystem: FileSystem,
    override val logger: AzoraLogger,
    private val scope: CoroutineScope,
    private val repository: AzoraProjectRepository,
    private val openAzsceneFilesManager: OpenAzsceneFilesManager? = null,
    private val dockStateManager: DockStateManager? = null,
    private val undoRedoCoordinator: GlobalUndoRedoCoordinator? = null,
) : PluginContext {

    /** Adapts [GlobalUndoRedoCoordinator] → [PluginUndoRedoFacade] so plugins can register their
     *  own undo/redo providers and have the toolbar buttons drive them. */
    override val undoRedo: PluginUndoRedoFacade? = undoRedoCoordinator?.let { coord ->
        object : PluginUndoRedoFacade {
            override fun register(provider: PluginUndoRedoProvider) {
                coord.register(PluginProviderAdapter(provider))
            }
            override fun unregister(providerId: String) = coord.unregister(providerId)
            override fun setActive(providerId: String) = coord.setActiveProvider(providerId)
        }
    }

    override fun saveProject(project: AzoraProjectModel) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                repository.updateProject(project)
                repository.saveProject(projectPath)
            }.onFailure { logger.error("PluginContext saveProject failed: ${it.message}", it) }
        }
    }

    /** Opens [filePath] as an `.azscene` editor tab (or focuses it if already open), mirroring the
     *  content browser's open flow. No-op if the host didn't supply the managers. */
    override fun openScene(filePath: String) {
        val manager = openAzsceneFilesManager ?: return
        val dock = dockStateManager ?: return
        scope.launch {
            runCatching {
                val panelId = manager.openFile(filePath) ?: run {
                    logger.error("openScene: no editor for ${filePath.substringAfterLast('/')}", null)
                    return@launch
                }
                val state = manager.getState(panelId) ?: return@launch
                dock.dispatch(
                    DockAction.AddPanel(
                        DockPanelDescriptor(id = panelId, title = state.fileName, closeable = true),
                        EDITOR_AREA_NODE_ID,
                        DockZone.CENTER
                    )
                )
                dock.dispatch(DockAction.SelectPanel(panelId))
            }.onFailure { logger.error("openScene failed: ${it.message}", it) }
        }
    }
}

/** Adapts a plugin-side [PluginUndoRedoProvider] to the host's [GlobalUndoRedoProvider] interface. */
private class PluginProviderAdapter(val plugin: PluginUndoRedoProvider) : GlobalUndoRedoProvider {
    override val providerId: String get() = plugin.providerId
    override val canUndo: StateFlow<Boolean> get() = plugin.canUndo
    override val canRedo: StateFlow<Boolean> get() = plugin.canRedo
    override fun undo() = plugin.undo()
    override fun redo() = plugin.redo()
    override fun clearHistory() = plugin.clearHistory()
}
