package dev.azora.studio.az_script

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Placeholder for the AzScript editor while the Azora language toolchain is disabled.
 *
 * The full AzScript editing/compilation experience depends on the external azora-lang
 * library, which is intentionally turned off for now. These stubs keep the rest of
 * Studio (editor panels, Problems panel) compiling and running with the feature off.
 */
@Composable
fun AzScriptFilePanel(panelId: String, projectPath: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("AzScript is currently disabled")
    }
}

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
 * Stubbed diagnostics manager — always reports no problems while AzScript is disabled.
 */
class DiagnosticsManager {
    val allDiagnostics: StateFlow<List<FileDiagnostic>> = MutableStateFlow(emptyList())

    fun requestNavigation(panelId: String, line: Int) {
        // no-op while AzScript is disabled
    }
}
