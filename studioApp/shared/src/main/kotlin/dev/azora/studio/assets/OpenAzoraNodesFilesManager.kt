package org.azora.studio.assets

import org.azora.sdk.core.io.FileReadResult
import org.azora.sdk.core.io.FileSystem
import org.azora.sdk.core.io.FileSystemResult
import org.azora.canvas.domain.model.AzoraGraphModel
import org.azora.canvas.domain.model.node.AzoraNodeModel
import org.azora.canvas.domain.type.AzoraNodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * State for an open .azn file.
 */
data class OpenFileState(
    val filePath: String,
    val fileName: String,
    val panelId: String,
    val graph: AzoraGraphModel,
    val isDirty: Boolean = false
)

/**
 * Manages open .azn files and their state.
 * Provides operations to open, close, update, and save files.
 */
class OpenAzoraNodesFilesManager(
    private val fileSystem: FileSystem
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _openFiles = MutableStateFlow<Map<String, OpenFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenFileState>> = _openFiles.asStateFlow()

    /**
     * Opens a .azn file and returns the panel ID.
     * If the file is already open, returns the existing panel ID.
     */
    suspend fun openFile(filePath: String): String? {
        println("[OpenAzoraNodesFilesManager] openFile called: $filePath")

        // Check if file is already open
        _openFiles.value.values.find { it.filePath == filePath }?.let {
            println("[OpenAzoraNodesFilesManager] File already open with panelId: ${it.panelId}")
            return it.panelId
        }

        // Read file content
        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> {
                println("[OpenAzoraNodesFilesManager] Failed to read file")
                return null
            }
        }

        // Parse graph
        val graph = try {
            json.decodeFromString<AzoraGraphModel>(content)
        } catch (e: Exception) {
            println("[OpenAzoraNodesFilesManager] Failed to parse JSON: ${e.message}")
            return null
        }

        // Generate panel ID and extract file name
        val panelId = "azn_${generateId()}"
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")

        println("[OpenAzoraNodesFilesManager] Opening file: panelId=$panelId, fileName=$fileName")

        val state = OpenFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            graph = graph,
            isDirty = false
        )

        _openFiles.value = _openFiles.value + (panelId to state)
        println("[OpenAzoraNodesFilesManager] After openFile, openFiles keys: ${_openFiles.value.keys}")
        return panelId
    }

    /**
     * Restores a file with a specific panel ID (used when restoring from saved layout).
     * Returns true if successful.
     */
    suspend fun restoreFile(panelId: String, filePath: String): Boolean {
        println("[OpenAzoraNodesFilesManager] restoreFile called: panelId=$panelId, filePath=$filePath")

        // Check if already open with this panelId
        if (_openFiles.value.containsKey(panelId)) {
            println("[OpenAzoraNodesFilesManager] Already open, returning true")
            return true
        }

        // Read file content
        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> {
                println("[OpenAzoraNodesFilesManager] File read success, content length: ${result.content.length}")
                result.content
            }
            is FileReadResult.Error -> {
                println("[OpenAzoraNodesFilesManager] File read error: ${result.message}")
                return false
            }
        }

        // Parse graph
        val graph = try {
            json.decodeFromString<AzoraGraphModel>(content)
        } catch (e: Exception) {
            println("[OpenAzoraNodesFilesManager] JSON parse error: ${e.message}")
            return false
        }

        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
        println("[OpenAzoraNodesFilesManager] Parsed successfully, fileName: $fileName")

        val state = OpenFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            graph = graph,
            isDirty = false
        )

        _openFiles.value = _openFiles.value + (panelId to state)
        println("[OpenAzoraNodesFilesManager] Added to openFiles, current keys: ${_openFiles.value.keys}")
        return true
    }

    /**
     * Closes a file by its panel ID.
     */
    fun closeFile(panelId: String) {
        _openFiles.value = _openFiles.value - panelId
    }

    /**
     * Gets the state for a panel ID.
     */
    fun getState(panelId: String): OpenFileState? {
        return _openFiles.value[panelId]
    }

    /**
     * Updates the graph for a panel and marks it as dirty.
     */
    fun updateGraph(panelId: String, graph: AzoraGraphModel) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to current.copy(graph = graph, isDirty = true))
    }

    /**
     * Saves the file to disk.
     */
    suspend fun saveFile(panelId: String): Boolean {
        val state = _openFiles.value[panelId] ?: return false

        val content = json.encodeToString(AzoraGraphModel.serializer(), state.graph)
        return when (fileSystem.writeToFile(state.filePath, content)) {
            is FileSystemResult.Success -> {
                _openFiles.value = _openFiles.value + (panelId to state.copy(isDirty = false))
                true
            }
            is FileSystemResult.Error -> false
        }
    }

    /**
     * Creates a new .azn file with a default graph.
     */
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
            is FileSystemResult.Success -> true
            is FileSystemResult.Error -> false
        }
    }

    /**
     * Gets the mapping of panelId to filePath for persistence.
     */
    fun getOpenFilesMapping(): Map<String, String> {
        return _openFiles.value.mapValues { it.value.filePath }
    }

    private fun generateId(): String = Random.nextLong().toString(36)
}
