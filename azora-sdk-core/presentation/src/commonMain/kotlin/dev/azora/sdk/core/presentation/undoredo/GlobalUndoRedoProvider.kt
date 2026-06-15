package dev.azora.sdk.core.presentation.undoredo

import kotlinx.coroutines.flow.StateFlow

interface GlobalUndoRedoProvider {
    val providerId: String
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>
    fun undo()
    fun redo()
    fun clearHistory()
}
