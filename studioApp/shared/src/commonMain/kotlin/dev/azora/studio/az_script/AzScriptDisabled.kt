package dev.azora.studio.az_script

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stubs for the parts of the AzScript toolchain that are still disabled
 * (diagnostics/compilation depend on the external azora-lang library).
 * Editing `.az` files works — see [AzScriptFilePanel].
 */

/** A single diagnostic (line + message). */
data class Diagnostic(
    val line: Int = 0,
    val message: String = ""
)

/** A diagnostic associated with a specific open file/panel. */
data class FileDiagnostic(
    val fileName: String,
    val panelId: String,
    val diagnostic: Diagnostic
)

/**
 * Stubbed diagnostics manager - always reports no problems while AzScript is disabled.
 */
class DiagnosticsManager {
    val allDiagnostics: StateFlow<List<FileDiagnostic>> = MutableStateFlow(emptyList())

    fun requestNavigation(panelId: String, line: Int) {
        // no-op while AzScript is disabled
    }
}
