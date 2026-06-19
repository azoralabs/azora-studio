package dev.azora.studio.editor

import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.plugin.core.PluginContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Studio's [PluginContext] implementation handed to plugins when their content is rendered.
 *
 * [saveProject] persists a (possibly mutated) project model back into the in-memory store and the
 * `project.azora` file, so plugin edits survive reloads. Persistence runs off the UI thread.
 */
class StudioPluginContext(
    override val project: AzoraProjectModel,
    override val projectPath: String,
    override val fileSystem: FileSystem,
    override val logger: AzoraLogger,
    private val scope: CoroutineScope,
    private val repository: AzoraProjectRepository,
) : PluginContext {

    override fun saveProject(project: AzoraProjectModel) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                repository.updateProject(project)
                repository.saveProject(projectPath)
            }.onFailure { logger.error("PluginContext saveProject failed: ${it.message}", it) }
        }
    }
}
