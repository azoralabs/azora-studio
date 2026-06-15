package dev.azora.sdk.core.presentation.undoredo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class GlobalUndoRedoCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val providers = mutableMapOf<String, GlobalUndoRedoProvider>()
    private val _activeProviderId = MutableStateFlow<String?>(null)

    val canUndo: StateFlow<Boolean> = _activeProviderId
        .flatMapLatest { id ->
            id?.let { providers[it]?.canUndo } ?: flowOf(false)
        }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val canRedo: StateFlow<Boolean> = _activeProviderId
        .flatMapLatest { id ->
            id?.let { providers[it]?.canRedo } ?: flowOf(false)
        }
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun register(provider: GlobalUndoRedoProvider) {
        providers[provider.providerId] = provider
    }

    fun unregister(providerId: String) {
        providers.remove(providerId)
        if (_activeProviderId.value == providerId) {
            _activeProviderId.value = null
        }
    }

    fun setActiveProvider(providerId: String) {
        if (providers.containsKey(providerId)) {
            _activeProviderId.value = providerId
        }
    }

    fun undo() {
        val id = _activeProviderId.value ?: return
        providers[id]?.undo()
    }

    fun redo() {
        val id = _activeProviderId.value ?: return
        providers[id]?.redo()
    }

    fun clearAllHistory() {
        providers.values.forEach { it.clearHistory() }
    }
}
