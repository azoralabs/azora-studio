package dev.azora.studio.content_browser

import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.FileSystemResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * In-memory state for text files opened in the Content Browser's editor tabs.
 * Mirrors `OpenAzScriptFilesManager` but is generic (no script-specific logic).
 *
 * Each open file is identified by a stable `txt_<id>` panel id so it can be
 * rendered as a dock tab and persisted across restarts.
 */
data class OpenTextFileState(
    val filePath: String,
    val fileName: String,
    val panelId: String,
    val content: String,
    val isDirty: Boolean = false
)

class OpenTextFilesManager(
    private val fileSystem: FileSystem
) {
    private val _openFiles = MutableStateFlow<Map<String, OpenTextFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenTextFileState>> = _openFiles.asStateFlow()

    /** Opens a file (or returns the existing panel id if already open). */
    suspend fun openFile(filePath: String): String? {
        _openFiles.value.values.find { it.filePath == filePath }?.let {
            return it.panelId
        }

        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return null
        }

        val panelId = "txt_${generateId()}"
        val fileName = filePath.substringAfterLast("/")

        _openFiles.value = _openFiles.value + (panelId to OpenTextFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            content = content,
            isDirty = false
        ))
        return panelId
    }

    /** Restores a previously-open file under its saved panel id (used on launch). */
    suspend fun restoreFile(panelId: String, filePath: String): Boolean {
        if (_openFiles.value.containsKey(panelId)) return true

        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return false
        }

        val fileName = filePath.substringAfterLast("/")
        _openFiles.value = _openFiles.value + (panelId to OpenTextFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            content = content,
            isDirty = false
        ))
        return true
    }

    fun closeFile(panelId: String) {
        _openFiles.value = _openFiles.value - panelId
    }

    fun getState(panelId: String): OpenTextFileState? = _openFiles.value[panelId]

    fun updateContent(panelId: String, content: String) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to current.copy(content = content, isDirty = true))
    }

    suspend fun saveFile(panelId: String): Boolean {
        val state = _openFiles.value[panelId] ?: return false
        return when (fileSystem.writeToFile(state.filePath, state.content)) {
            is FileSystemResult.Success -> {
                _openFiles.value = _openFiles.value + (panelId to state.copy(isDirty = false))
                true
            }
            is FileSystemResult.Error -> false
        }
    }

    fun getOpenFilesMapping(): Map<String, String> =
        _openFiles.value.mapValues { it.value.filePath }

    private fun generateId(): String = Random.nextLong().toString(36)
}
