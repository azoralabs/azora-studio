package org.azora.studio.assets

import org.azora.sdk.core.io.FileReadResult
import org.azora.sdk.core.io.FileSystem
import org.azora.sdk.core.io.FileSystemResult
import org.azora.sdk.core.io.ListResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class OpenAzScriptFileState(
    val filePath: String,
    val fileName: String,
    val panelId: String,
    val sourceCode: String,
    val isDirty: Boolean = false
)

class OpenAzScriptFilesManager(
    private val fileSystem: FileSystem
) {
    private val _openFiles = MutableStateFlow<Map<String, OpenAzScriptFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenAzScriptFileState>> = _openFiles.asStateFlow()

    suspend fun openFile(filePath: String): String? {
        // Check if file is already open
        _openFiles.value.values.find { it.filePath == filePath }?.let {
            return it.panelId
        }

        // Read file content
        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return null
        }

        val panelId = "azs_${generateId()}"
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")

        val state = OpenAzScriptFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            sourceCode = content,
            isDirty = false
        )

        _openFiles.value = _openFiles.value + (panelId to state)
        return panelId
    }

    suspend fun restoreFile(panelId: String, filePath: String): Boolean {
        if (_openFiles.value.containsKey(panelId)) return true

        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return false
        }

        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")

        val state = OpenAzScriptFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            sourceCode = content,
            isDirty = false
        )

        _openFiles.value = _openFiles.value + (panelId to state)
        return true
    }

    fun closeFile(panelId: String) {
        _openFiles.value = _openFiles.value - panelId
    }

    fun getState(panelId: String): OpenAzScriptFileState? {
        return _openFiles.value[panelId]
    }

    fun updateSource(panelId: String, source: String) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to current.copy(sourceCode = source, isDirty = true))
    }

    suspend fun saveFile(panelId: String): Boolean {
        val state = _openFiles.value[panelId] ?: return false

        return when (fileSystem.writeToFile(state.filePath, state.sourceCode)) {
            is FileSystemResult.Success -> {
                _openFiles.value = _openFiles.value + (panelId to state.copy(isDirty = false))
                true
            }
            is FileSystemResult.Error -> false
        }
    }

    suspend fun createNewFile(filePath: String, runnable: Boolean = false): Boolean {
        val defaultContent = if (runnable) {
            """
            |event onStart() {
            |
            |}
            """.trimMargin()
        } else {
            ""
        }

        return when (fileSystem.writeToFile(filePath, defaultContent)) {
            is FileSystemResult.Success -> true
            is FileSystemResult.Error -> false
        }
    }

    /**
     * Searches recursively for a .az script file by name under the given directory.
     * Returns the full path if found, or null.
     */
    suspend fun findScriptFile(dirPath: String, fileName: String): String? {
        when (val result = fileSystem.listDirectory(dirPath)) {
            is ListResult.Success -> {
                for (file in result.files) {
                    if (file.isDirectory) {
                        val found = findScriptFile(file.path, fileName)
                        if (found != null) return found
                    } else if (file.name == fileName) {
                        return file.path
                    }
                }
            }
            is ListResult.Error -> {}
        }
        return null
    }

    /**
     * Reads all .az script files in the given Assets directory.
     * Returns a map of filePath -> sourceCode, excluding the specified file.
     */
    suspend fun readAllScriptSources(assetsPath: String, excludeFilePath: String? = null): Map<String, String> {
        val sources = mutableMapOf<String, String>()
        collectScriptFiles(assetsPath, excludeFilePath, sources)
        return sources
    }

    private suspend fun collectScriptFiles(
        dirPath: String,
        excludeFilePath: String?,
        out: MutableMap<String, String>
    ) {
        when (val result = fileSystem.listDirectory(dirPath)) {
            is ListResult.Success -> {
                for (file in result.files) {
                    if (file.isDirectory) {
                        collectScriptFiles(file.path, excludeFilePath, out)
                    } else if (file.name.endsWith(".az") && file.path != excludeFilePath) {
                        when (val read = fileSystem.readFromFile(file.path)) {
                            is FileReadResult.Success -> out[file.path] = read.content
                            is FileReadResult.Error -> {}
                        }
                    }
                }
            }
            is ListResult.Error -> {}
        }
    }

    fun getOpenFilesMapping(): Map<String, String> {
        return _openFiles.value.mapValues { it.value.filePath }
    }

    private fun generateId(): String = Random.nextLong().toString(36)
}
