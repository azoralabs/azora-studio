package dev.azora.sdk.plugin.core

import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel

/**
 * Host-provided context handed to a plugin whenever its [AzoraPlugin.Content] (or a panel) is
 * rendered for the currently open project.
 *
 * This is the plugin's only sanctioned bridge to host capabilities: it exposes the live project,
 * its on-disk path, a [FileSystem] for reading/writing files, a [logger], and a [saveProject]
 * hook to persist mutations back into the project file (`project.azora`) and the in-memory store.
 *
 * The host constructs a fresh [PluginContext] per render, so plugins should always read the current
 * project from [project] rather than caching it.
 */
interface PluginContext {

    /** The currently open project (a fresh snapshot each render). */
    val project: AzoraProjectModel

    /** Absolute path of the open project's root directory. */
    val projectPath: String

    /** Host file system for reading/writing files inside the project. */
    val fileSystem: FileSystem

    /** Host logger. */
    val logger: AzoraLogger

    /**
     * Persist [project] (typically a copy mutated by the plugin) to `project.azora` and update the
     * in-memory project store. Safe to call from any thread.
     */
    fun saveProject(project: AzoraProjectModel)

    /**
     * Open an Azora document (e.g. a `.azscene` file at [filePath]) in a Studio editor tab, routed to
     * the plugin registered for the document's `type` — the same as double-clicking the file in the
     * content browser. Used by a plugin to jump to another scene (e.g. a component's implementation).
     * Default no-op so hosts that don't support it (or other [PluginContext] impls) still compile.
     */
    fun openScene(filePath: String) {}

    /** Host undo/redo facade — register a [PluginUndoRedoProvider] so the toolbar buttons drive the
     *  plugin's undo/redo stacks. Null when the host doesn't support it. */
    val undoRedo: PluginUndoRedoFacade? get() = null
}
