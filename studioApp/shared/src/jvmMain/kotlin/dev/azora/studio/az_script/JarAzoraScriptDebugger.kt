package dev.azora.studio.az_script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.lang.reflect.Method

/**
 * Desktop [AzoraScriptDebugger] backed by the same language-server jar as
 * [JarAzoraLanguageIntel]. The jar keeps a single polled debug session; all
 * calls cross the classloader boundary as JSON strings.
 *
 * The compilation-unit prelude (other project sources + engine libraries) is
 * borrowed from [intel] so debugged scripts resolve the same symbols the
 * editor's diagnostics see.
 */
class JarAzoraScriptDebugger(private val intel: JarAzoraLanguageIntel) : AzoraScriptDebugger {

    private val json = Json { ignoreUnknownKeys = true }

    private class Api(
        val instance: Any,
        val start: Method,
        val status: Method,
        val resume: Method,
        val step: Method,
        val stop: Method,
        val setBreakpoints: Method,
    )

    private val api: Api? by lazy {
        val server = intel.serverInstance ?: return@lazy null
        try {
            val cls = server.javaClass
            Api(
                instance = server,
                start = cls.getMethod("debugStart", String::class.java, String::class.java, String::class.java),
                status = cls.getMethod("debugStatus"),
                resume = cls.getMethod("debugResume"),
                step = cls.getMethod("debugStep"),
                stop = cls.getMethod("debugStop"),
                setBreakpoints = cls.getMethod("debugSetBreakpoints", String::class.java),
            )
        } catch (e: Exception) {
            println("[azls] debugger API unavailable: ${e.message} — update azls.jar (:azls:installAzls)")
            null
        }
    }

    override val available: Boolean get() = api != null

    override suspend fun start(source: String, filePath: String, projectPath: String, breakpoints: Set<Int>): AzDebugStatus {
        val a = api ?: return AzDebugStatus(status = "failed", error = "Debugger requires an updated azls.jar.")
        return withContext(Dispatchers.IO) {
            val prelude = intel.preludeSnapshot(filePath, projectPath)
            val raw = a.start.invoke(a.instance, source, prelude, encode(breakpoints)) as String
            if ("\"ok\"" in raw) AzDebugStatus(status = "starting") else decode(raw)
        }
    }

    override suspend fun status(): AzDebugStatus {
        val a = api ?: return AzDebugStatus()
        return withContext(Dispatchers.IO) { decode(a.status.invoke(a.instance) as String) }
    }

    override suspend fun resume() { call { it.resume } }
    override suspend fun step() { call { it.step } }
    override suspend fun stop() { call { it.stop } }

    override suspend fun setBreakpoints(breakpoints: Set<Int>) {
        val a = api ?: return
        withContext(Dispatchers.IO) {
            runCatching { a.setBreakpoints.invoke(a.instance, encode(breakpoints)) }
        }
    }

    private suspend fun call(method: (Api) -> Method) {
        val a = api ?: return
        withContext(Dispatchers.IO) { runCatching { method(a).invoke(a.instance) } }
    }

    private fun encode(breakpoints: Set<Int>): String =
        breakpoints.sorted().joinToString(",", "[", "]")

    private fun decode(raw: String): AzDebugStatus = runCatching {
        val dto = json.decodeFromString(StatusDto.serializer(), raw)
        AzDebugStatus(dto.status, dto.line, dto.pauseId, dto.locals.map { AzDebugLocal(it.name, it.value) }, dto.output, dto.error)
    }.getOrElse { AzDebugStatus(status = "failed", error = "bad debugger response") }

    @Serializable
    private data class LocalDto(val name: String, val value: String)

    @Serializable
    private data class StatusDto(
        val status: String,
        val line: Int = 0,
        val pauseId: Int = 0,
        val locals: List<LocalDto> = emptyList(),
        val output: String = "",
        val error: String? = null,
    )
}
