package dev.azora.sdk.plugin.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/**
 * Generic snapshot-based undo/redo for any document type [T]. Plugins extend this, override
 * [providerId] and [onRestore], then call [pushState] (pre-mutation) and [setCurrent] (post-mutation)
 * around every edit. Owns the ArrayDeque stacks, flag updates, and 50-entry cap.
 *
 * Same pattern as `AzoraNodesViewModel`'s inline stacks — extracted so plugins don't reimplement it.
 */
abstract class AbstractPluginUndoRedo<T> : PluginUndoRedoProvider {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    private var _current: T? = null
    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)
    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    override val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /** Called by undo/redo with the state to restore — the subclass sets its live document to [state]. */
    protected abstract fun onRestore(state: T)

    /** Call BEFORE each mutation — saves the current (pre-mutation) state to the undo stack. */
    fun pushState(preMutation: T) {
        undoStack.addLast(preMutation)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        updateFlags()
    }

    /** Call AFTER each mutation — tracks the post-mutation state so undo knows what to push to redo. */
    fun setCurrent(state: T) { _current = state }

    override fun undo() {
        if (undoStack.isEmpty()) return
        _current?.let { redoStack.addLast(it) }
        val prev = undoStack.removeLast()
        _current = prev
        onRestore(prev)
        updateFlags()
    }

    override fun redo() {
        if (redoStack.isEmpty()) return
        _current?.let { undoStack.addLast(it) }
        val next = redoStack.removeLast()
        _current = next
        onRestore(next)
        updateFlags()
    }

    override fun clearHistory() {
        undoStack.clear(); redoStack.clear(); updateFlags()
    }

    private fun updateFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
