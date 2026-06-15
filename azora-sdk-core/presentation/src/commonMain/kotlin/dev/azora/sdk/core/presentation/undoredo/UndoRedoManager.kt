package dev.azora.sdk.core.presentation.undoredo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UndoRedoManager<T>(
    private val maxHistorySize: Int = 50
) {
    private val undoStack = mutableListOf<T>()
    private val redoStack = mutableListOf<T>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun pushState(state: T) {
        undoStack.add(state)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateFlags()
    }

    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) return null

        val previousState = undoStack.removeLast()
        redoStack.add(currentState)
        updateFlags()
        return previousState
    }

    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) return null

        val nextState = redoStack.removeLast()
        undoStack.add(currentState)
        updateFlags()
        return nextState
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateFlags()
    }

    fun updateFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}