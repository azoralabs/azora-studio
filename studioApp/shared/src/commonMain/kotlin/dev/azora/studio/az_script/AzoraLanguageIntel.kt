package dev.azora.studio.az_script

/** A colorized region of `.az` source (character offsets, end exclusive). */
data class AzHighlightSpan(val start: Int, val end: Int, val type: String)

/** A completion proposal from the language server. */
data class AzCompletion(
    val label: String,
    val kind: String,
    val detail: String = "",
    val insert: String = ""
)

/** Hover info for the symbol under the caret. */
data class AzHover(val signature: String, val detail: String = "")

/**
 * Language intelligence for azora-lang (`.az`) sources.
 *
 * The desktop implementation ([available] = true) talks to the Azora Language
 * Server (`azls.jar`, produced by the azora-lang repo and installed at
 * `~/.azora/azls/azls.jar`). Platforms without the jar fall back to
 * [NoOpAzoraLanguageIntel] and the editor degrades to plain text.
 *
 * `filePath`/`projectPath` let the implementation include the rest of the
 * compilation unit (other project sources + installed engine libraries) so
 * cross-file symbols resolve and diagnostics match a real build.
 */
interface AzoraLanguageIntel {

    /** True when a language server is loaded and calls will return results. */
    val available: Boolean

    suspend fun highlight(source: String): List<AzHighlightSpan>

    suspend fun diagnostics(source: String, filePath: String, projectPath: String): List<Diagnostic>

    suspend fun complete(source: String, offset: Int, filePath: String, projectPath: String): List<AzCompletion>

    suspend fun hover(source: String, offset: Int, filePath: String, projectPath: String): AzHover?
}

/** Fallback used on platforms without the language-server jar. */
object NoOpAzoraLanguageIntel : AzoraLanguageIntel {
    override val available: Boolean = false
    override suspend fun highlight(source: String): List<AzHighlightSpan> = emptyList()
    override suspend fun diagnostics(source: String, filePath: String, projectPath: String): List<Diagnostic> = emptyList()
    override suspend fun complete(source: String, offset: Int, filePath: String, projectPath: String): List<AzCompletion> = emptyList()
    override suspend fun hover(source: String, offset: Int, filePath: String, projectPath: String): AzHover? = null
}
