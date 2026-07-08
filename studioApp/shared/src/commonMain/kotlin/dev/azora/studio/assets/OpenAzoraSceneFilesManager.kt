package dev.azora.studio.assets

import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.FileSystemResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * State for an open .azorascene file.
 */
data class OpenSceneFileState(
    val filePath: String,
    val fileName: String,
    val panelId: String,
    val graph: AzoraGraphModel,
    val isDirty: Boolean = false
)

/**
 * Manages open .azorascene files and their state.
 */
class OpenAzoraSceneFilesManager(
    private val fileSystem: FileSystem
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _openFiles = MutableStateFlow<Map<String, OpenSceneFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenSceneFileState>> = _openFiles.asStateFlow()

    private val _fileChangeSignal = MutableStateFlow(0)
    val fileChangeSignal: StateFlow<Int> = _fileChangeSignal.asStateFlow()

    suspend fun openFile(filePath: String): String? {
        _openFiles.value.values.find { it.filePath == filePath }?.let {
            return it.panelId
        }

        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return null
        }

        val graph = try {
            json.decodeFromString<AzoraGraphModel>(content)
        } catch (e: Exception) {
            println("[OpenAzoraSceneFilesManager] Failed to parse JSON: ${e.message}")
            return null
        }

        val panelId = "azorascene_${generateId()}"
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")

        val state = OpenSceneFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            graph = graph,
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

        val graph = try {
            json.decodeFromString<AzoraGraphModel>(content)
        } catch (_: Exception) {
            return false
        }

        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
        val state = OpenSceneFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            graph = graph,
            isDirty = false
        )

        _openFiles.value = _openFiles.value + (panelId to state)
        return true
    }

    fun closeFile(panelId: String) {
        _openFiles.value = _openFiles.value - panelId
    }

    /** Points an open tab at a new path after the file was renamed/moved on disk. */
    fun relocate(panelId: String, newPath: String) {
        val state = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to state.copy(
            filePath = newPath,
            fileName = newPath.substringAfterLast("/").substringBeforeLast(".")
        ))
    }

    fun getState(panelId: String): OpenSceneFileState? {
        return _openFiles.value[panelId]
    }

    fun updateGraph(panelId: String, graph: AzoraGraphModel) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to current.copy(graph = graph, isDirty = true))
    }

    suspend fun saveFile(panelId: String): Boolean {
        val state = _openFiles.value[panelId] ?: return false

        val content = json.encodeToString(AzoraGraphModel.serializer(), state.graph)
        return when (fileSystem.writeToFile(state.filePath, content)) {
            is FileSystemResult.Success -> {
                _openFiles.value = _openFiles.value + (panelId to state.copy(isDirty = false))
                _fileChangeSignal.value++
                true
            }
            is FileSystemResult.Error -> false
        }
    }

    suspend fun createNewFile(filePath: String): Boolean {
        val startNode = AzoraNodeModel(
            id = generateId(),
            screenId = "",
            title = "Start",
            type = AzoraNodeType.START,
            positionX = 200f,
            positionY = 200f
        )
        val graph = AzoraGraphModel(
            id = generateId(),
            name = filePath.substringAfterLast("/").substringBeforeLast("."),
            nodes = mapOf(startNode.id to startNode)
        )

        val content = json.encodeToString(AzoraGraphModel.serializer(), graph)
        return when (fileSystem.writeToFile(filePath, content)) {
            is FileSystemResult.Success -> {
                _fileChangeSignal.value++
                true
            }
            is FileSystemResult.Error -> false
        }
    }

    fun getOpenFilesMapping(): Map<String, String> {
        return _openFiles.value.mapValues { it.value.filePath }
    }

    private fun generateId(): String = Random.nextLong().toString(36)
}
