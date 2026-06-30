package dev.azora.sdk.plugin.core

import kotlinx.coroutines.flow.StateFlow

/**
 * A plugin's undo/redo provider — the plugin owns its document snapshot stacks and implements undo /
 * redo / clearHistory. Mirrors `GlobalUndoRedoProvider` from the SDK but lives in plugin-core so
 * plugins can implement it without depending on core-presentation.
 */
interface PluginUndoRedoProvider {
    val providerId: String
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>
    fun undo()
    fun redo()
    fun clearHistory()
}

/**
 * Host-side facade for registering / activating plugin undo/redo providers. Plugins obtain an instance
 * via [PluginContext.undoRedo] and register their [PluginUndoRedoProvider] so the host's toolbar
 * undo/redo buttons drive the plugin's stacks.
 */
interface PluginUndoRedoFacade {
    fun register(provider: PluginUndoRedoProvider)
    fun unregister(providerId: String)
    fun setActive(providerId: String)
}
