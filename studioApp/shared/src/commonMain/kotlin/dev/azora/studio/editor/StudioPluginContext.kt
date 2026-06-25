package dev.azora.studio.editor

import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.plugin.core.PluginContext
import dev.azora.studio.assets.OpenAzsceneFilesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
) : PluginContext {

    override fun saveProject(project: AzoraProjectModel) {
        scope.launch(Dispatchers.IO) {
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
