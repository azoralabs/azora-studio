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

/** One variable visible while the debugger is paused. */
data class AzDebugLocal(val name: String, val value: String)

/**
 * Snapshot of the (single) azls debug session.
 *
 * @property status `none`, `starting`, `running`, `paused`, `terminated` or `failed`
 * @property line 1-based line the debugger is paused on
 * @property pauseId monotonic counter distinguishing consecutive pauses
 * @property output program output produced since the previous poll
 */
data class AzDebugStatus(
    val status: String = "none",
    val line: Int = 0,
    val pauseId: Int = 0,
    val locals: List<AzDebugLocal> = emptyList(),
    val output: String = "",
    val error: String? = null,
)

/**
 * Debugger controls for `.az` scripts, backed by the language-server jar.
 * Debug builds instrument each statement, so the interpreter can pause at
 * breakpoints, step, and report the variables in scope.
 */
interface AzoraScriptDebugger {
    val available: Boolean
    suspend fun start(source: String, filePath: String, projectPath: String, breakpoints: Set<Int>): AzDebugStatus
    suspend fun status(): AzDebugStatus
    suspend fun resume()
    suspend fun step()
    suspend fun stop()
    suspend fun setBreakpoints(breakpoints: Set<Int>)
}

/** Fallback used on platforms without the language-server jar. */
object NoOpAzoraScriptDebugger : AzoraScriptDebugger {
    override val available: Boolean = false
    override suspend fun start(source: String, filePath: String, projectPath: String, breakpoints: Set<Int>): AzDebugStatus =
        AzDebugStatus(status = "failed", error = "Azora Language Server is not installed.")
    override suspend fun status(): AzDebugStatus = AzDebugStatus()
    override suspend fun resume() {}
    override suspend fun step() {}
    override suspend fun stop() {}
    override suspend fun setBreakpoints(breakpoints: Set<Int>) {}
}
