package dev.azora.studio.assets

import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.studio.data.tilemap.TileMapModel
import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.FileSystemResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * State for an open .azoratilemap file.
 */
data class OpenTileMapFileState(
    val filePath: String,
    val fileName: String,
    val panelId: String,
    val tileMap: TileMapModel,
    val graph: AzoraGraphModel,
    val isDirty: Boolean = false,
    val pendingSave: Boolean = false,
)

/**
 * Manages open .azoratilemap files and their state.
 * Follows the same pattern as OpenAzoraSceneFilesManager.
 */
class OpenAzoraTileMapFilesManager(
    private val fileSystem: FileSystem
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _openFiles = MutableStateFlow<Map<String, OpenTileMapFileState>>(emptyMap())
    val openFiles: StateFlow<Map<String, OpenTileMapFileState>> = _openFiles.asStateFlow()

    /** Incremented when files are created/saved on disk, so the Assets panel can refresh. */
    private val _fileChangeSignal = MutableStateFlow(0)
    val fileChangeSignal: StateFlow<Int> = _fileChangeSignal.asStateFlow()

    /**
     * Opens a .azoratilemap file and returns the panel ID.
     * If the file is already open, returns the existing panel ID.
     */
    suspend fun openFile(filePath: String): String? {
        // Check if file is already open
        _openFiles.value.values.find { it.filePath == filePath }?.let {
            return it.panelId
        }

        // Read tilemap file content
        val content = when (val result = fileSystem.readFromFile(filePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return null
        }

        // Parse tilemap
        val tileMap = try {
            json.decodeFromString<TileMapModel>(content)
        } catch (e: Exception) {
            println("[OpenAzoraTileMapFilesManager] Failed to parse tilemap JSON: ${e.message}")
            return null
        }

        // Try to read companion .azn file
        val nodesFilePath = filePath.substringBeforeLast(".") + ".azn"
        val graph = tryLoadGraph(nodesFilePath)

        val panelId = "azoratilemap_${generateId()}"
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")

        val state = OpenTileMapFileState(
            filePath = filePath,
            fileName = fileName,
            panelId = panelId,
            tileMap = tileMap,
            graph = graph,
            isDirty = false,
        )

        _openFiles.value = _openFiles.value + (panelId to state)
        return panelId
    }

    /**
     * Closes a file by its panel ID.
     */
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

    /**
     * Gets the state for a panel ID.
     */
    fun getState(panelId: String): OpenTileMapFileState? {
        return _openFiles.value[panelId]
    }

    /**
     * Updates the tile map for a panel and marks it as dirty.
     */
    fun updateTileMap(panelId: String, tileMap: TileMapModel) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to current.copy(tileMap = tileMap, isDirty = true))
    }

    /**
     * Updates the graph for a panel and marks it as dirty.
     */
    fun updateGraph(panelId: String, graph: AzoraGraphModel) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value = _openFiles.value + (panelId to current.copy(graph = graph, isDirty = true))
    }

    /**
     * Marks a panel as needing save (called by ViewModel on Save action).
     */
    fun requestSave(panelId: String) {
        val current = _openFiles.value[panelId] ?: return
        _openFiles.value += (panelId to current.copy(pendingSave = true))
    }

    /**
     * Saves the tilemap file and companion .azn file to disk.
     * The graph is only stored in .azn.
     */
    suspend fun saveFile(panelId: String): Boolean {
        val state = _openFiles.value[panelId] ?: return false

        // Save .azoratilemap (tilemap data only, no graph)
        val tileMapContent = json.encodeToString(TileMapModel.serializer(), state.tileMap)
        val tileMapResult = fileSystem.writeToFile(state.filePath, tileMapContent)
        if (tileMapResult is FileSystemResult.Error) return false

        // Save companion .azn (graph lives here)
        val nodesFilePath = state.filePath.substringBeforeLast(".") + ".azn"
        val nodesContent = json.encodeToString(AzoraGraphModel.serializer(), state.graph)
        fileSystem.writeToFile(nodesFilePath, nodesContent)

        _openFiles.value += (panelId to state.copy(isDirty = false, pendingSave = false))
        _fileChangeSignal.value++
        return true
    }

    /**
     * Creates a new .azoratilemap + companion .azn file with defaults.
     */
    suspend fun createNewFile(filePath: String): Boolean {
        val graphId = generateId()
        val startNode = AzoraNodeModel(
            id = generateId(),
            screenId = "",
            title = "Start",
            type = AzoraNodeType.START,
            positionX = 200f,
            positionY = 200f
        )
        val graph = AzoraGraphModel(
            id = graphId,
            name = filePath.substringAfterLast("/").substringBeforeLast("."),
            nodes = mapOf(startNode.id to startNode)
        )
        val tileMap = TileMapModel()

        // Save .azoratilemap
        val tileMapContent = json.encodeToString(TileMapModel.serializer(), tileMap)
        val tileMapResult = fileSystem.writeToFile(filePath, tileMapContent)
        if (tileMapResult is FileSystemResult.Error) return false

        // Save companion .azn
        val nodesFilePath = filePath.substringBeforeLast(".") + ".azn"
        val nodesContent = json.encodeToString(AzoraGraphModel.serializer(), graph)
        val nodesResult = fileSystem.writeToFile(nodesFilePath, nodesContent)
        if (nodesResult is FileSystemResult.Error) return false

        _fileChangeSignal.value++
        return true
    }

    /**
     * Gets the mapping of panelId to filePath for persistence.
     */
    fun getOpenFilesMapping(): Map<String, String> {
        return _openFiles.value.mapValues { it.value.filePath }
    }

    private suspend fun tryLoadGraph(nodesFilePath: String): AzoraGraphModel {
        val nodesContent = when (val result = fileSystem.readFromFile(nodesFilePath)) {
            is FileReadResult.Success -> result.content
            is FileReadResult.Error -> return createDefaultGraph()
        }
        return try {
            json.decodeFromString<AzoraGraphModel>(nodesContent)
        } catch (_: Exception) {
            createDefaultGraph()
        }
    }

    private fun createDefaultGraph(): AzoraGraphModel {
        val startNode = AzoraNodeModel(
            id = generateId(), screenId = "", title = "Start",
            type = AzoraNodeType.START, positionX = 200f, positionY = 200f
        )
        return AzoraGraphModel(
            id = generateId(), name = "Tile Studio",
            nodes = mapOf(startNode.id to startNode)
        )
    }

    private fun generateId(): String = Random.nextLong().toString(36)
}
