package org.azora.studio.az_script

import kotlinx.coroutines.flow.*

data class FileDiagnostic(
    val panelId: String,
    val fileName: String,
    val diagnostic: ScriptDiagnostic
)

data class NavigateRequest(
    val panelId: String,
    val line: Int
)

class DiagnosticsManager {
    private val _diagnostics = MutableStateFlow<Map<String, List<FileDiagnostic>>>(emptyMap())
    val diagnostics: StateFlow<Map<String, List<FileDiagnostic>>> = _diagnostics.asStateFlow()

    val allDiagnostics: StateFlow<List<FileDiagnostic>> = _diagnostics
        .map { map -> map.values.flatten() }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _navigateRequest = MutableSharedFlow<NavigateRequest>(extraBufferCapacity = 1)
    val navigateRequest: SharedFlow<NavigateRequest> = _navigateRequest

    fun updateDiagnostics(panelId: String, fileName: String, diagnostics: List<ScriptDiagnostic>) {
        val fileDiagnostics = diagnostics.map { FileDiagnostic(panelId, fileName, it) }
        _diagnostics.update { current ->
            current + (panelId to fileDiagnostics)
        }
    }

    fun clearDiagnostics(panelId: String) {
        _diagnostics.update { current ->
            current - panelId
        }
    }

    fun requestNavigation(panelId: String, line: Int) {
        _navigateRequest.tryEmit(NavigateRequest(panelId, line))
    }
}
