package dev.azora.sdk.core.presentation.undoredo

data class UndoRedoState(
    val canUndo: Boolean,
    val canRedo: Boolean,
    val hasUnsavedChanges: Boolean,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onSave: () -> Unit
)

typealias OnUndoRedoStateChanged = (UndoRedoState) -> Unit