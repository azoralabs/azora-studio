package dev.azora.studio.azora_nodes

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.azora.canvas.domain.AzoraPortDefinition
import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.AzoraExecLinkModel
import dev.azora.canvas.presentation.state.AzoraCanvasAction
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoProvider
import dev.azora.sdk.core.project.domain.GlobalConstant
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.canvas.domain.model.AzoraDataLinkModel
import dev.azora.canvas.domain.model.AzoraReroutePointModel
import dev.azora.canvas.domain.model.node.AzoraNodeDataClass
import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.domain.model.node.AzoraNodeEnum
import dev.azora.canvas.domain.model.node.AzoraNodeFunction
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.model.node.AzoraNodeVar
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import dev.azora.canvas.domain.interpreter.ScriptInterpreter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class AzoraNodesViewModel(
    private val projectRepository: AzoraProjectRepository,
    val consoleOutputManager: ConsoleOutputManager,
    private val undoRedoCoordinator: GlobalUndoRedoCoordinator,
) : ViewModel(), GlobalUndoRedoProvider {

    override val providerId = "azora_nodes"

    private val _state = MutableStateFlow(AzoraNodesState())
    val state: StateFlow<AzoraNodesState> = _state.asStateFlow()

    // Undo/redo stacks
    private val undoStack = ArrayDeque<AzoraGraphModel>()
    private val redoStack = ArrayDeque<AzoraGraphModel>()
    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)
    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    override val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var interpreterJob: Job? = null
    private var interpreter: ScriptInterpreter? = null

    init {
        undoRedoCoordinator.register(this)
        loadFromProject()
    }

    override fun onCleared() {
        super.onCleared()
        undoRedoCoordinator.unregister(providerId)
        interpreterJob?.cancel()
    }

    fun onAction(action: AzoraNodesAction) {
        when (action) {
            is AzoraNodesAction.CanvasAction -> handleCanvasAction(action.action)
            is AzoraNodesAction.Refresh -> handleRefresh(action.graphs)
            is AzoraNodesAction.AddNode -> handleAddNode(action.type, action.position, action.properties)
            is AzoraNodesAction.DeleteNodes -> handleDeleteNodes(action.nodeIds)
            is AzoraNodesAction.UpdateNodeProperties -> handleUpdateNodeProperties(action.nodeId, action.properties)
            is AzoraNodesAction.StartDataLink -> handleStartDataLink(action.nodeId, action.portName, action.isOutput, action.dataType)
            is AzoraNodesAction.CompleteDataLink -> handleCompleteDataLink(action.targetNodeId, action.targetPortName, action.targetIsOutput)
            is AzoraNodesAction.AddVariable -> handleAddVariable(action.variable)
            is AzoraNodesAction.UpdateVariable -> handleUpdateVariable(action.variable)
            is AzoraNodesAction.DeleteVariable -> handleDeleteVariable(action.variableId)
            is AzoraNodesAction.SelectVariable -> handleSelectVariable(action.variableId)
            is AzoraNodesAction.CreateGetter -> handleCreateGetter(action.variableId)
            is AzoraNodesAction.CreateSetter -> handleCreateSetter(action.variableId)
            is AzoraNodesAction.AddFunction -> handleAddFunction(action.function)
            is AzoraNodesAction.UpdateFunction -> handleUpdateFunction(action.function)
            is AzoraNodesAction.DeleteFunction -> handleDeleteFunction(action.functionId)
            is AzoraNodesAction.AddEnum -> handleAddEnum(action.enumDef)
            is AzoraNodesAction.UpdateEnum -> handleUpdateEnum(action.enumDef)
            is AzoraNodesAction.DeleteEnum -> handleDeleteEnum(action.enumId)
            is AzoraNodesAction.AddDataClass -> handleAddDataClass(action.dataClassDef)
            is AzoraNodesAction.UpdateDataClass -> handleUpdateDataClass(action.dataClassDef)
            is AzoraNodesAction.DeleteDataClass -> handleDeleteDataClass(action.classId)
            is AzoraNodesAction.EditNode -> _state.value = _state.value.copy(editingNodeId = action.nodeId)
            is AzoraNodesAction.Run -> { /* handled externally via run() */ }
            is AzoraNodesAction.Stop -> stop()
            is AzoraNodesAction.DeleteSelected -> handleDeleteSelected()
            is AzoraNodesAction.Undo -> undo()
            is AzoraNodesAction.Redo -> redo()
            is AzoraNodesAction.Save -> save()
        }
    }

    // --- Canvas action handling (bridges AzoraCanvasAction to domain operations) ---

    private fun handleCanvasAction(action: AzoraCanvasAction) {
        val st = _state.value
        when (action) {
            is AzoraCanvasAction.UpdatePan -> {
                _state.value = st.copy(canvasState = st.canvasState.copy(panOffset = action.offset))
            }
            is AzoraCanvasAction.EndPan -> pushUndo()
            is AzoraCanvasAction.SelectNode -> {
                val node = st.graph.nodes[action.nodeId]
                // For variable getter/setter nodes, select the associated variable
                val variableId = if (node?.type == AzoraNodeType.GET_VARIABLE || node?.type == AzoraNodeType.SET_VARIABLE) {
                    node.properties["variableId"]
                } else null
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        selectedNodeId = action.nodeId,
                        selectedLinkId = null
                    ),
                    editingNodeId = action.nodeId,
                    selectedVariableId = variableId
                )
                setActive()
            }
            is AzoraCanvasAction.SelectLink -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        selectedLinkId = action.linkId,
                        selectedNodeId = null
                    ),
                    editingNodeId = null
                )
            }
            is AzoraCanvasAction.ClearSelection -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        selectedNodeId = null,
                        selectedLinkId = null
                    ),
                    editingNodeId = null,
                    // Also cancel data link creation
                    isDataLinkCreation = false,
                    dataLinkSourcePortName = null
                )
            }
            is AzoraCanvasAction.StartCreatingLink -> {
                // Exec link creation via SDK port system
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        isCreatingLink = true,
                        linkSourceNodeId = action.sourceNodeId,
                        linkPortType = action.portType,
                        linkOutputPortIndex = action.outputPortIndex
                    ),
                    isDataLinkCreation = false
                )
            }
            is AzoraCanvasAction.FinishCreatingLink -> {
                if (st.isDataLinkCreation) {
                    // Data link completion - delegate to data link handler
                    // (data links are completed via AzoraNodesAction.CompleteDataLink, not here)
                    handleCancelLink()
                } else {
                    // Exec link completion
                    handleCompleteExecLink(action.targetNodeId)
                }
            }
            is AzoraCanvasAction.CancelCreatingLink -> handleCancelLink()
            is AzoraCanvasAction.UpdateNodePosition -> {
                val node = st.graph.nodes[action.nodeId] ?: return
                val delta = Offset(action.position.x - node.positionX, action.position.y - node.positionY)
                handleMoveNode(action.nodeId, delta)
            }
            is AzoraCanvasAction.EndNodeDrag -> {
                pushUndo()
                autoSave()
            }
            is AzoraCanvasAction.DeleteLink -> {
                if (st.graph.execLinks.containsKey(action.linkId)) {
                    handleDeleteExecLink(action.linkId)
                } else {
                    handleDeleteDataLink(action.linkId)
                }
            }
            is AzoraCanvasAction.ShowCanvasContextMenu -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(canvasContextMenuPosition = action.position)
                )
            }
            is AzoraCanvasAction.DismissCanvasContextMenu -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(canvasContextMenuPosition = null)
                )
            }
            is AzoraCanvasAction.DismissAllContextMenus -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        contextMenuLinkId = null,
                        contextMenuPosition = null,
                        contextMenuReroutePointId = null,
                        contextMenuRerouteLinkId = null,
                        contextMenuReroutePosition = null,
                        canvasContextMenuPosition = null
                    )
                )
            }
            is AzoraCanvasAction.ShowLinkContextMenu -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        contextMenuLinkId = action.linkId,
                        contextMenuPosition = action.position,
                        contextMenuSegmentIndex = action.segmentIndex
                    )
                )
            }
            is AzoraCanvasAction.DismissLinkContextMenu -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        contextMenuLinkId = null,
                        contextMenuPosition = null
                    )
                )
            }
            // Reroute points
            is AzoraCanvasAction.SelectReroutePoint -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        selectedRerouteLinkId = action.linkId,
                        selectedReroutePointId = action.reroutePointId
                    )
                )
            }
            is AzoraCanvasAction.AddReroutePoint -> handleAddReroutePoint(action.linkId, action.position, action.insertIndex)
            is AzoraCanvasAction.UpdateReroutePointPosition -> handleUpdateReroutePointPosition(action.linkId, action.reroutePointId, action.delta)
            is AzoraCanvasAction.EndReroutePointDrag -> {
                pushUndo()
                autoSave()
            }
            is AzoraCanvasAction.ShowReroutePointContextMenu -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        contextMenuRerouteLinkId = action.linkId,
                        contextMenuReroutePointId = action.reroutePointId,
                        contextMenuReroutePosition = action.position
                    )
                )
            }
            is AzoraCanvasAction.DismissReroutePointContextMenu -> {
                _state.value = st.copy(
                    canvasState = st.canvasState.copy(
                        contextMenuRerouteLinkId = null,
                        contextMenuReroutePointId = null,
                        contextMenuReroutePosition = null
                    )
                )
            }
            is AzoraCanvasAction.DeleteReroutePoint -> handleDeleteReroutePoint(action.linkId, action.reroutePointId)
        }
    }

    fun setActive() {
        undoRedoCoordinator.setActiveProvider(providerId)
    }

    fun run(globalConstants: List<GlobalConstant>) {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)
        interpreterJob = viewModelScope.launch {
            consoleOutputManager.clear()
            consoleOutputManager.info("Running AzoraNodes script...")
            try {
                val interp = ScriptInterpreter(
                    _state.value.graph,
                    globalConstants,
                    consoleOutputManager
                )
                interpreter = interp
                interp.execute()
                consoleOutputManager.info("Script finished.")
            } catch (e: Exception) {
                consoleOutputManager.error("Script error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
                interpreter = null
            }
        }
    }

    fun stop() {
        interpreter?.stop()
        interpreterJob?.cancel()
        interpreterJob = null
        interpreter = null
        _state.value = _state.value.copy(isRunning = false)
        consoleOutputManager.info("Script stopped.")
    }

    // --- Undo/Redo ---

    private fun pushUndo() {
        undoStack.addLast(_state.value.graph)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    override fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(_state.value.graph)
        val prev = undoStack.removeLast()
        _state.value = _state.value.copy(graph = prev)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    override fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(_state.value.graph)
        val next = redoStack.removeLast()
        _state.value = _state.value.copy(graph = next)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    override fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    // --- Persistence ---

    private fun loadFromProject() {
        // Create a default graph with a START node
        // Note: Script graphs are now file-backed via OpenAzoraNodesFilesManager
        val startNode = AzoraNodeModel(
            id = generateId(),
            screenId = "",
            title = "Start",
            type = AzoraNodeType.START,
            positionX = 200f,
            positionY = 200f
        )
        val defaultGraph = AzoraGraphModel(
            id = generateId(),
            name = "Main",
            nodes = mapOf(startNode.id to startNode)
        )
        _state.value = _state.value.copy(graph = defaultGraph)
    }

    private fun save() {
        // Note: This ViewModel uses in-memory state only
        // For file persistence, use AzoraNodesFileViewModel with OpenAzoraNodesFilesManager
    }

    private fun handleRefresh(graphs: List<AzoraGraphModel>) {
        if (graphs.isNotEmpty()) {
            _state.value = _state.value.copy(graph = graphs.first())
        }
    }

    // --- Node operations ---

    private fun handleAddNode(type: AzoraNodeType, position: Offset, properties: Map<String, String>) {
        pushUndo()
        val node = AzoraNodeModel(
            id = generateId(),
            screenId = "",
            title = type.label,
            type = type,
            positionX = position.x,
            positionY = position.y,
            properties = properties
        )
        val graph = _state.value.graph
        _state.value = _state.value.copy(
            graph = graph.copy(nodes = graph.nodes + (node.id to node)),
            canvasState = _state.value.canvasState.copy(canvasContextMenuPosition = null)
        )
        autoSave()
    }

    private fun handleDeleteNodes(nodeIds: Set<String>) {
        val graph = _state.value.graph
        // Filter out START nodes - they cannot be deleted
        val deletableNodeIds = nodeIds.filter { nodeId ->
            val node = graph.nodes[nodeId]
            node?.type != AzoraNodeType.START
        }.toSet()
        if (deletableNodeIds.isEmpty()) return

        pushUndo()
        val newNodes = graph.nodes.filterKeys { it !in deletableNodeIds }
        val newExecLinks = graph.execLinks.filterValues {
            it.sourceNodeId !in deletableNodeIds && it.targetNodeId !in deletableNodeIds
        }
        val newDataLinks = graph.dataLinks.filterValues {
            it.sourceNodeId !in deletableNodeIds && it.targetNodeId !in deletableNodeIds
        }
        _state.value = _state.value.copy(
            graph = graph.copy(nodes = newNodes, execLinks = newExecLinks, dataLinks = newDataLinks),
            canvasState = _state.value.canvasState.copy(selectedNodeId = null)
        )
        autoSave()
    }

    private fun handleMoveNode(nodeId: String, delta: Offset) {
        val graph = _state.value.graph
        val node = graph.nodes[nodeId] ?: return
        val updated = node.copy(
            positionX = node.positionX + delta.x,
            positionY = node.positionY + delta.y
        )
        _state.value = _state.value.copy(
            graph = graph.copy(nodes = graph.nodes + (nodeId to updated))
        )
    }

    private fun handleUpdateNodeProperties(nodeId: String, properties: Map<String, String>) {
        pushUndo()
        val graph = _state.value.graph
        val node = graph.nodes[nodeId] ?: return
        val updated = node.copy(properties = node.properties + properties)
        _state.value = _state.value.copy(
            graph = graph.copy(nodes = graph.nodes + (nodeId to updated))
        )
        autoSave()
    }

    // --- Exec link operations ---

    private fun handleCompleteExecLink(targetNodeId: String) {
        val st = _state.value
        val cs = st.canvasState
        val sourceNodeId = cs.linkSourceNodeId ?: return
        if (!cs.isCreatingLink) return
        if (sourceNodeId == targetNodeId) {
            handleCancelLink()
            return
        }

        val targetNode = st.graph.nodes[targetNodeId] ?: return
        if (AzoraPortDefinition.execInputCount(targetNode.type) == 0) {
            handleCancelLink()
            return
        }

        pushUndo()

        val graph = st.graph
        val existing = graph.execLinks.values.find {
            it.sourceNodeId == sourceNodeId && it.sourcePortIndex == cs.linkOutputPortIndex
        }
        var links = if (existing != null) graph.execLinks - existing.id else graph.execLinks

        val link = AzoraExecLinkModel(
            id = generateId(),
            sourceNodeId = sourceNodeId,
            sourcePortIndex = cs.linkOutputPortIndex,
            targetNodeId = targetNodeId
        )
        links = links + (link.id to link)

        _state.value = st.copy(
            graph = graph.copy(execLinks = links),
            canvasState = cs.copy(
                isCreatingLink = false,
                linkSourceNodeId = null,
                linkPortType = null
            )
        )
        autoSave()
    }

    private fun handleCancelLink() {
        val st = _state.value
        _state.value = st.copy(
            canvasState = st.canvasState.copy(
                isCreatingLink = false,
                linkSourceNodeId = null,
                linkPortType = null
            ),
            isDataLinkCreation = false,
            dataLinkSourcePortName = null,
            dataLinkSourceDataType = null
        )
    }

    private fun handleDeleteExecLink(linkId: String) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(
            graph = graph.copy(execLinks = graph.execLinks - linkId),
            canvasState = _state.value.canvasState.copy(selectedLinkId = null)
        )
        autoSave()
    }

    // --- Data link operations ---

    private fun handleStartDataLink(nodeId: String, portName: String, isOutput: Boolean, dataType: AzoraNodeDataType) {
        val st = _state.value
        // Set NAV_DIALOG port type for data links - renders white preview line
        _state.value = st.copy(
            canvasState = st.canvasState.copy(
                isCreatingLink = true,
                linkSourceNodeId = nodeId,
                linkPortType = dev.azora.canvas.domain.type.AzoraPortType.NAV_DIALOG
            ),
            isDataLinkCreation = true,
            dataLinkSourcePortName = portName,
            dataLinkIsOutput = isOutput,
            dataLinkSourceDataType = dataType
        )
    }

    private fun handleCompleteDataLink(targetNodeId: String, targetPortName: String, targetIsOutput: Boolean) {
        val st = _state.value
        val linkSourceNodeId = st.canvasState.linkSourceNodeId ?: return
        val linkSourcePortName = st.dataLinkSourcePortName ?: return
        if (!st.isDataLinkCreation) return
        if (linkSourceNodeId == targetNodeId) {
            handleCancelLink()
            return
        }

        // Validate: must connect output to input (not same type)
        if (st.dataLinkIsOutput == targetIsOutput) {
            // Both are outputs or both are inputs - invalid
            handleCancelLink()
            return
        }

        pushUndo()

        val (sourceNodeId, sourcePortName, destNodeId, destPortName) = if (st.dataLinkIsOutput) {
            // Started from output, target is input
            listOf(linkSourceNodeId, linkSourcePortName, targetNodeId, targetPortName)
        } else {
            // Started from input, target is output
            listOf(targetNodeId, targetPortName, linkSourceNodeId, linkSourcePortName)
        }

        val graph = st.graph
        val existing = graph.dataLinks.values.find {
            it.targetNodeId == destNodeId && it.targetPortName == destPortName
        }
        var links = if (existing != null) graph.dataLinks - existing.id else graph.dataLinks

        val link = AzoraDataLinkModel(
            id = generateId(),
            sourceNodeId = sourceNodeId,
            sourcePortName = sourcePortName,
            targetNodeId = destNodeId,
            targetPortName = destPortName
        )
        links = links + (link.id to link)

        _state.value = st.copy(
            graph = graph.copy(dataLinks = links),
            canvasState = st.canvasState.copy(
                isCreatingLink = false,
                linkSourceNodeId = null
            ),
            isDataLinkCreation = false,
            dataLinkSourcePortName = null
        )
        autoSave()
    }

    private fun handleDeleteDataLink(linkId: String) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(
            graph = graph.copy(dataLinks = graph.dataLinks - linkId),
            canvasState = _state.value.canvasState.copy(selectedLinkId = null)
        )
        autoSave()
    }

    // --- Reroute point operations ---

    private fun handleAddReroutePoint(linkId: String, position: Offset, insertIndex: Int) {
        pushUndo()
        val graph = _state.value.graph
        val newPoint = AzoraReroutePointModel(
            id = generateId(),
            x = position.x,
            y = position.y
        )

        // Check exec links first
        val execLink = graph.execLinks[linkId]
        if (execLink != null) {
            val newPoints = execLink.reroutePoints.toMutableList()
            newPoints.add(insertIndex.coerceIn(0, newPoints.size), newPoint)
            val updatedLink = execLink.copy(reroutePoints = newPoints)
            _state.value = _state.value.copy(
                graph = graph.copy(execLinks = graph.execLinks + (linkId to updatedLink))
            )
            autoSave()
            return
        }

        // Check data links
        val dataLink = graph.dataLinks[linkId]
        if (dataLink != null) {
            val newPoints = dataLink.reroutePoints.toMutableList()
            newPoints.add(insertIndex.coerceIn(0, newPoints.size), newPoint)
            val updatedLink = dataLink.copy(reroutePoints = newPoints)
            _state.value = _state.value.copy(
                graph = graph.copy(dataLinks = graph.dataLinks + (linkId to updatedLink))
            )
            autoSave()
        }
    }

    private fun handleUpdateReroutePointPosition(linkId: String, reroutePointId: String, delta: Offset) {
        val graph = _state.value.graph

        // Check exec links first
        val execLink = graph.execLinks[linkId]
        if (execLink != null) {
            val newPoints = execLink.reroutePoints.map { point ->
                if (point.id == reroutePointId) {
                    point.copy(x = point.x + delta.x, y = point.y + delta.y)
                } else point
            }
            val updatedLink = execLink.copy(reroutePoints = newPoints)
            _state.value = _state.value.copy(
                graph = graph.copy(execLinks = graph.execLinks + (linkId to updatedLink))
            )
            return
        }

        // Check data links
        val dataLink = graph.dataLinks[linkId]
        if (dataLink != null) {
            val newPoints = dataLink.reroutePoints.map { point ->
                if (point.id == reroutePointId) {
                    point.copy(x = point.x + delta.x, y = point.y + delta.y)
                } else point
            }
            val updatedLink = dataLink.copy(reroutePoints = newPoints)
            _state.value = _state.value.copy(
                graph = graph.copy(dataLinks = graph.dataLinks + (linkId to updatedLink))
            )
        }
    }

    private fun handleDeleteReroutePoint(linkId: String, reroutePointId: String) {
        pushUndo()
        val graph = _state.value.graph

        // Check exec links first
        val execLink = graph.execLinks[linkId]
        if (execLink != null) {
            val newPoints = execLink.reroutePoints.filter { it.id != reroutePointId }
            val updatedLink = execLink.copy(reroutePoints = newPoints)
            _state.value = _state.value.copy(
                graph = graph.copy(execLinks = graph.execLinks + (linkId to updatedLink)),
                canvasState = _state.value.canvasState.copy(
                    contextMenuRerouteLinkId = null,
                    contextMenuReroutePointId = null,
                    contextMenuReroutePosition = null
                )
            )
            autoSave()
            return
        }

        // Check data links
        val dataLink = graph.dataLinks[linkId]
        if (dataLink != null) {
            val newPoints = dataLink.reroutePoints.filter { it.id != reroutePointId }
            val updatedLink = dataLink.copy(reroutePoints = newPoints)
            _state.value = _state.value.copy(
                graph = graph.copy(dataLinks = graph.dataLinks + (linkId to updatedLink)),
                canvasState = _state.value.canvasState.copy(
                    contextMenuRerouteLinkId = null,
                    contextMenuReroutePointId = null,
                    contextMenuReroutePosition = null
                )
            )
            autoSave()
        }
    }

    // --- Variable operations ---

    private fun handleAddVariable(variable: AzoraNodeVar) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(variables = graph.variables + (variable.id to variable)))
        autoSave()
    }

    private fun handleUpdateVariable(variable: AzoraNodeVar) {
        pushUndo()
        val graph = _state.value.graph
        // Propagate name change to all GET_VARIABLE / SET_VARIABLE nodes referencing this variable
        val updatedNodes = graph.nodes.mapValues { (_, node) ->
            if ((node.type == AzoraNodeType.GET_VARIABLE || node.type == AzoraNodeType.SET_VARIABLE)
                && node.properties["variableId"] == variable.id
            ) {
                node.copy(
                    title = "${if (node.type == AzoraNodeType.GET_VARIABLE) "Get" else "Set"} ${variable.name}",
                    properties = node.properties + ("variableName" to variable.name)
                )
            } else node
        }
        _state.value = _state.value.copy(
            graph = graph.copy(
                variables = graph.variables + (variable.id to variable),
                nodes = updatedNodes
            )
        )
        autoSave()
    }

    private fun handleDeleteVariable(variableId: String) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(variables = graph.variables - variableId))
        autoSave()
    }

    private fun handleSelectVariable(variableId: String?) {
        _state.value = _state.value.copy(
            selectedVariableId = variableId,
            editingNodeId = null,
            canvasState = _state.value.canvasState.copy(selectedNodeId = null, selectedLinkId = null)
        )
    }

    private fun handleCreateGetter(variableId: String) {
        val variable = _state.value.graph.variables[variableId] ?: return
        pushUndo()
        val panOffset = _state.value.canvasState.panOffset
        val node = AzoraNodeModel(
            id = generateId(),
            screenId = "",
            title = "Get ${variable.name}",
            type = AzoraNodeType.GET_VARIABLE,
            positionX = -panOffset.x + 300f,
            positionY = -panOffset.y + 200f,
            properties = mapOf("variableId" to variableId, "variableName" to variable.name)
        )
        val graph = _state.value.graph
        _state.value = _state.value.copy(
            graph = graph.copy(nodes = graph.nodes + (node.id to node))
        )
        autoSave()
    }

    private fun handleCreateSetter(variableId: String) {
        val variable = _state.value.graph.variables[variableId] ?: return
        pushUndo()
        val panOffset = _state.value.canvasState.panOffset
        val node = AzoraNodeModel(
            id = generateId(),
            screenId = "",
            title = "Set ${variable.name}",
            type = AzoraNodeType.SET_VARIABLE,
            positionX = -panOffset.x + 300f,
            positionY = -panOffset.y + 200f,
            properties = mapOf("variableId" to variableId, "variableName" to variable.name)
        )
        val graph = _state.value.graph
        _state.value = _state.value.copy(
            graph = graph.copy(nodes = graph.nodes + (node.id to node))
        )
        autoSave()
    }

    // --- Function operations ---

    private fun handleAddFunction(function: AzoraNodeFunction) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(functions = graph.functions + (function.id to function)))
        autoSave()
    }

    private fun handleUpdateFunction(function: AzoraNodeFunction) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(functions = graph.functions + (function.id to function)))
        autoSave()
    }

    private fun handleDeleteFunction(functionId: String) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(functions = graph.functions - functionId))
        autoSave()
    }

    // --- Enum operations ---

    private fun handleAddEnum(enumDef: AzoraNodeEnum) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(enums = graph.enums + (enumDef.id to enumDef)))
        autoSave()
    }

    private fun handleUpdateEnum(enumDef: AzoraNodeEnum) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(enums = graph.enums + (enumDef.id to enumDef)))
        autoSave()
    }

    private fun handleDeleteEnum(enumId: String) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(enums = graph.enums - enumId))
        autoSave()
    }

    // --- Data class operations ---

    private fun handleAddDataClass(dataClassDef: AzoraNodeDataClass) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(dataClasses = graph.dataClasses + (dataClassDef.id to dataClassDef)))
        autoSave()
    }

    private fun handleUpdateDataClass(dataClassDef: AzoraNodeDataClass) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(dataClasses = graph.dataClasses + (dataClassDef.id to dataClassDef)))
        autoSave()
    }

    private fun handleDeleteDataClass(classId: String) {
        pushUndo()
        val graph = _state.value.graph
        _state.value = _state.value.copy(graph = graph.copy(dataClasses = graph.dataClasses - classId))
        autoSave()
    }

    private fun handleDeleteSelected() {
        val st = _state.value
        val selectedNodeId = st.canvasState.selectedNodeId
        if (selectedNodeId != null) {
            handleDeleteNodes(setOf(selectedNodeId))
            return
        }
        val selectedLinkId = st.canvasState.selectedLinkId ?: return
        if (st.graph.execLinks.containsKey(selectedLinkId)) {
            handleDeleteExecLink(selectedLinkId)
        } else {
            handleDeleteDataLink(selectedLinkId)
        }
    }

    private fun autoSave() {
        save()
    }

    private fun generateId(): String = Random.nextLong().toString(36)
}
