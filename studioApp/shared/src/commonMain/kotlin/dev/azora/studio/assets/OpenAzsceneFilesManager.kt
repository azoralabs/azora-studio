package dev.azora.studio.assets

import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

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
 */
class OpenAzsceneFilesManager(private val fileSystem: FileSystem) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _openFiles = MutableStateFlow<Map<String, OpenAzsceneFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenAzsceneFileState>> = _openFiles.asStateFlow()

    /** Opens [filePath] (or returns the existing panel) and returns its panelId, or null if the file
     *  can't be read or has no `type`. */
    suspend fun openFile(filePath: String): String? {
        _openFiles.value.values.find { it.filePath == filePath }?.let { return it.panelId }

        val content = when (val r = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> r.content
            is FileReadResult.Error -> return null
        }
        val type = runCatching {
            json.parseToJsonElement(content).jsonObject["type"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        val panelId = "azscene_" + Random.nextLong().toString(36)
        _openFiles.value = _openFiles.value + (panelId to OpenAzsceneFileState(
            filePath = filePath,
            fileName = filePath.substringAfterLast('/'),
            panelId = panelId,
            type = type
        ))
        return panelId
    }

    fun getState(panelId: String): OpenAzsceneFileState? = _openFiles.value[panelId]
}
