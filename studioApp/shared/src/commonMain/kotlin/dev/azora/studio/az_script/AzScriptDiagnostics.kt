package dev.azora.studio.az_script

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A single compiler diagnostic.
 *
 * @property line 1-based line in the file
 * @property message human-readable description
 * @property severity `error` or `warning`
 */
data class Diagnostic(
    val line: Int = 0,
    val message: String = "",
    val severity: String = "error"
)

/** A diagnostic associated with a specific open file/panel. */
data class FileDiagnostic(
    val fileName: String,
    val panelId: String,
    val diagnostic: Diagnostic
)

/**
 * Collects diagnostics from every open `.az` editor (fed by the Azora Language
 * Server via [AzoraLanguageIntel]) and exposes them to the Problems panel.
 */
class DiagnosticsManager {

    private val _allDiagnostics = MutableStateFlow<List<FileDiagnostic>>(emptyList())
    val allDiagnostics: StateFlow<List<FileDiagnostic>> = _allDiagnostics.asStateFlow()

    /** A Problems-panel click asking the editor for [panelId] to reveal a line. */
    private val _navigationRequest = MutableStateFlow<Pair<String, Int>?>(null)
    val navigationRequest: StateFlow<Pair<String, Int>?> = _navigationRequest.asStateFlow()

    /** Replaces the diagnostics of one file (called after each analysis pass). */
    fun report(panelId: String, fileName: String, diagnostics: List<Diagnostic>) {
        _allDiagnostics.value = _allDiagnostics.value.filterNot { it.panelId == panelId } +
            diagnostics.map { FileDiagnostic(fileName, panelId, it) }
    }

    /** Drops all diagnostics of one file (e.g. when its editor closes). */
    fun clear(panelId: String) {
        _allDiagnostics.value = _allDiagnostics.value.filterNot { it.panelId == panelId }
    }

    fun requestNavigation(panelId: String, line: Int) {
        _navigationRequest.value = panelId to line
    }
}
