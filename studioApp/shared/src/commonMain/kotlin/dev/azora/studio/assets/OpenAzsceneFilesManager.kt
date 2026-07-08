package dev.azora.studio.assets

import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Open-state for one `.azscene` panel. The host only tracks the path and the [type] discriminator;
 *  the plugin that owns [type] does the actual loading/editing. */
data class OpenAzsceneFileState(
    val filePath: String,
    val fileName: String,
    val panelId: String,
    val type: String
)

/**
 * Tracks open `.azscene` files. `.azscene` is a generic Azora document: this manager reads only its
 * top-level `type` field so the host can route the file to the plugin registered for that type
 * (`AzoraPlugin.azsceneEditorTypes()`). The host stays agnostic of what types mean.
 *
 * The panel id encodes the file path, so a panel persisted in the dock layout can be [restore]d
 * after a Studio restart without any extra bookkeeping.
 */
class OpenAzsceneFilesManager(private val fileSystem: FileSystem) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _openFiles = MutableStateFlow<Map<String, OpenAzsceneFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenAzsceneFileState>> = _openFiles.asStateFlow()

    /** Opens [filePath] (or returns the existing panel) and returns its panelId, or null if the file
     *  can't be read or has no `type`. */
    suspend fun openFile(filePath: String): String? {
        val panelId = PREFIX + encode(filePath)
        _openFiles.value[panelId]?.let { return panelId }
        return if (load(panelId, filePath)) panelId else null
    }

    /** Re-populates state for a persisted [panelId] (e.g. after restart) by decoding its file path. */
    suspend fun restore(panelId: String): Boolean {
        if (_openFiles.value.containsKey(panelId)) return true
        if (!panelId.startsWith(PREFIX)) return false
        val filePath = runCatching { decode(panelId.removePrefix(PREFIX)) }.getOrNull() ?: return false
        return load(panelId, filePath)
    }

    fun getState(panelId: String): OpenAzsceneFileState? = _openFiles.value[panelId]

    fun closeFile(panelId: String) {
        _openFiles.value = _openFiles.value - panelId
    }

    /** Reads only the top-level `type` discriminator of [filePath] (without opening a panel), or null
     *  if the file can't be read or has no `type`. Used to decide whether a generic document (e.g. a
     *  `.azn`) belongs to a plugin editor. */
    suspend fun peekType(filePath: String): String? {
        val content = when (val r = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> r.content
            is FileReadResult.Error -> return null
        }
        return runCatching {
            json.parseToJsonElement(content).jsonObject["type"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private suspend fun load(panelId: String, filePath: String): Boolean {
        val content = when (val r = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> r.content
            is FileReadResult.Error -> return false
        }
        val type = runCatching {
            json.parseToJsonElement(content).jsonObject["type"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return false

        _openFiles.value = _openFiles.value + (panelId to OpenAzsceneFileState(
            filePath = filePath,
            fileName = filePath.substringAfterLast('/'),
            panelId = panelId,
            type = type
        ))
        return true
    }

    companion object {
        private const val PREFIX = "azscene_"

        // Hex-encode the path into the panel id (multiplatform-safe, reversible).
        private fun encode(s: String): String =
            s.encodeToByteArray().joinToString("") { b -> (b.toInt() and 0xFF).toString(16).padStart(2, '0') }

        private fun decode(hex: String): String {
            val bytes = ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            return bytes.decodeToString()
        }
    }
}
