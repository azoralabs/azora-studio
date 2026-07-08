package dev.azora.studio.az_script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader

/**
 * Desktop implementation of [AzoraLanguageIntel] backed by the Azora Language
 * Server jar.
 *
 * The jar is produced by the azora-lang repo (`./gradlew :azls:installAzls`)
 * and auto-discovered at `~/.azora/azls/azls.jar`. It is loaded in its own
 * [URLClassLoader] and invoked reflectively with JSON-string payloads, so
 * Studio and the language server stay completely decoupled — updating the
 * language only requires reinstalling the jar.
 *
 * The rest of the compilation unit (other project `.az` sources + installed
 * engine libraries under `~/.azora/libraries`) is assembled into a prelude so
 * completions and diagnostics see cross-file and engine symbols.
 */
class JarAzoraLanguageIntel : AzoraLanguageIntel {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private class Server(
        val instance: Any,
        val highlight: Method,
        val diagnostics: Method,
        val complete: Method,
        val hover: Method,
    )

    private val server: Server? by lazy {
        val jar = File(System.getProperty("user.home"), ".azora/azls/azls.jar")
        if (!jar.isFile) {
            println("[azls] no language server at ${jar.absolutePath} — .az intellisense disabled")
            return@lazy null
        }
        try {
            val loader = URLClassLoader(arrayOf(jar.toURI().toURL()), javaClass.classLoader)
            val cls = Class.forName("org.azora.azls.AzoraLanguageServer", true, loader)
            val instance = cls.getDeclaredConstructor().newInstance()
            val version = cls.getMethod("version").invoke(instance)
            println("[azls] loaded Azora Language Server $version from ${jar.absolutePath}")
            Server(
                instance = instance,
                highlight = cls.getMethod("highlight", String::class.java),
                diagnostics = cls.getMethod("diagnostics", String::class.java, String::class.java),
                complete = cls.getMethod("complete", String::class.java, Int::class.javaPrimitiveType, String::class.java),
                hover = cls.getMethod("hover", String::class.java, Int::class.javaPrimitiveType, String::class.java),
            )
        } catch (e: Exception) {
            println("[azls] failed to load language server: ${e.message}")
            null
        }
    }

    override val available: Boolean get() = server != null

    /** The loaded `AzoraLanguageServer` instance — shared with [JarAzoraScriptDebugger]. */
    val serverInstance: Any? get() = server?.instance

    /** The compilation-unit prelude for [filePath] — shared with the debugger. */
    fun preludeSnapshot(filePath: String, projectPath: String): String = preludeFor(filePath, projectPath)

    override suspend fun highlight(source: String): List<AzHighlightSpan> {
        val srv = server ?: return emptyList()
        return call(emptyList()) {
            val raw = srv.highlight.invoke(srv.instance, source) as String
            json.decodeFromString(ListSerializer(SpanDto.serializer()), raw)
                .map { AzHighlightSpan(it.start, it.end, it.type) }
        }
    }

    override suspend fun diagnostics(source: String, filePath: String, projectPath: String): List<Diagnostic> {
        val srv = server ?: return emptyList()
        val prelude = preludeFor(filePath, projectPath)
        return call(emptyList()) {
            val raw = srv.diagnostics.invoke(srv.instance, source, prelude) as String
            json.decodeFromString(ListSerializer(DiagnosticDto.serializer()), raw)
                .map { Diagnostic(it.line, it.message, it.severity) }
        }
    }

    override suspend fun complete(source: String, offset: Int, filePath: String, projectPath: String): List<AzCompletion> {
        val srv = server ?: return emptyList()
        val prelude = preludeFor(filePath, projectPath)
        return call(emptyList()) {
            val raw = srv.complete.invoke(srv.instance, source, offset, prelude) as String
            json.decodeFromString(ListSerializer(CompletionDto.serializer()), raw)
                .map { AzCompletion(it.label, it.kind, it.detail, it.insert) }
        }
    }

    override suspend fun hover(source: String, offset: Int, filePath: String, projectPath: String): AzHover? {
        val srv = server ?: return null
        val prelude = preludeFor(filePath, projectPath)
        return call(null) {
            val raw = srv.hover.invoke(srv.instance, source, offset, prelude) as String
            if (raw == "null") null
            else json.decodeFromString(HoverDto.serializer(), raw).let { AzHover(it.signature, it.detail) }
        }
    }

    private suspend fun <T> call(default: T, block: () -> T): T =
        withContext(Dispatchers.IO) { mutex.withLock { runCatching(block).getOrDefault(default) } }

    // -----------------------------------------------------------------
    // Prelude assembly (rest of the compilation unit)
    // -----------------------------------------------------------------

    private data class CachedPrelude(val key: String, val builtAt: Long, val text: String)

    @Volatile
    private var cachedPrelude: CachedPrelude? = null

    /**
     * Concatenates installed engine libraries and the project's other `.az`
     * sources (excluding [filePath]) — mirroring how the azora CLI builds its
     * compilation unit. Cached for a few seconds; azls memoizes its parse of
     * the prelude by content, so repeated identical preludes are cheap.
     */
    private fun preludeFor(filePath: String, projectPath: String): String {
        val key = "$projectPath|$filePath"
        val now = System.currentTimeMillis()
        cachedPrelude?.let { if (it.key == key && now - it.builtAt < 5_000) return it.text }

        // (file, module): library sources are tagged with a module derived from
        // their path (`<libId>/<version>/engine/…` → "engine") via an
        // `//@azora-module` marker so azls can gate their symbols behind the
        // document's `use` imports. Project sources (module = null) stay
        // visible unconditionally.
        val sources = mutableListOf<Pair<File, String?>>()
        val libraries = File(System.getProperty("user.home"), ".azora/libraries")
        if (libraries.isDirectory) {
            libraries.walkTopDown()
                // `templates/` holds complete starter projects (each with its own
                // `func main`) — only the library sources belong in the prelude.
                .onEnter { it.name != "templates" }
                .filter { it.isFile && it.extension == "az" }
                .sortedBy { it.absolutePath }
                .forEach { file ->
                    val segments = file.relativeTo(libraries).invariantSeparatorsPath.split("/")
                    // [libId, version, topDir, …] — module is the top-level source
                    // dir; files directly under the version dir fall back to libId.
                    val module = segments.getOrNull(2)?.takeIf { segments.size > 3 } ?: segments.firstOrNull()
                    sources.add(file to module)
                }
        }
        val projectDir = File(projectPath)
        val current = File(filePath).absoluteFile.normalize()
        if (projectDir.isDirectory) {
            projectDir.walkTopDown()
                .onEnter { it.name !in SKIPPED_DIRS }
                .filter { it.isFile && it.extension == "az" && it.absoluteFile.normalize() != current }
                .sortedBy { it.absolutePath }
                .forEach { sources.add(it to null) }
        }

        val text = buildString {
            for ((file, module) in sources) {
                val content = runCatching { stripModuleLines(file.readText()) }.getOrDefault("")
                // A bare marker resets the module so project files never inherit
                // the previous library section's module.
                if (module != null) append("//@azora-module ").append(module).append('\n')
                else append("//@azora-module\n")
                append(content).append("\n\n")
            }
        }
        cachedPrelude = CachedPrelude(key, now, text)
        return text
    }

    /** `package` / `use` lines are CLI metadata, not part of one compilation unit. */
    private fun stripModuleLines(source: String): String =
        source.lines().joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("package ") || trimmed == "package" || trimmed.startsWith("use ")) "" else line
        }

    // ----- JSON mirrors of the azls wire format -----

    @Serializable
    private data class SpanDto(val start: Int, val end: Int, val type: String)

    @Serializable
    private data class DiagnosticDto(val line: Int, val message: String, val severity: String = "error")

    @Serializable
    private data class CompletionDto(val label: String, val kind: String, val detail: String = "", val insert: String = "")

    @Serializable
    private data class HoverDto(val signature: String, val detail: String = "")

    private companion object {
        val SKIPPED_DIRS = setOf(".git", ".gradle", ".idea", ".azora-build", "build", "node_modules")
    }
}
