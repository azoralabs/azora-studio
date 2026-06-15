package org.azora.studio.azora_nodes.canvas

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.azora.canvas.domain.model.node.AzoraNodeModel
import org.azora.canvas.domain.model.AzoraReroutePointModel
import org.azora.canvas.domain.AzoraPortDefinition
import org.azora.canvas.presentation.canvas.AzoraEditorCanvas
import org.azora.canvas.presentation.data.AzoraDrawableLink
import org.azora.canvas.presentation.node.AzoraInputPortDef
import org.azora.canvas.presentation.node.AzoraNode
import org.azora.canvas.presentation.node.AzoraOutputPortDef
import org.azora.canvas.presentation.state.AzoraCanvasAction
import org.azora.canvas.domain.type.AzoraNodeType
import org.azora.canvas.domain.type.AzoraPortType
import org.azora.studio.azora_nodes.AzoraNodesAction
import org.azora.studio.azora_nodes.AzoraNodesState

/**
 * Key for storing exec input port position (node-relative offset).
 */
private data class InputPortKey(val nodeId: String)

/**
 * Key for storing exec output port position (node-relative offset).
 */
private data class OutputPortKey(val nodeId: String, val portIndex: Int)

/**
 * Key for storing data port position (node-relative offset).
 */
private data class DataPortKey(val nodeId: String, val portName: String)

@Composable
fun AzoraNodesCanvas(
    state: AzoraNodesState,
    onCanvasAction: (AzoraCanvasAction) -> Unit,
    onAction: (AzoraNodesAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val graph = state.graph
    val canvasState = state.canvasState

    // Exec port positions (node-relative offsets, populated by SDK callbacks)
    val inputPortPositions = remember { mutableStateMapOf<InputPortKey, Offset>() }
    val outputPortPositions = remember { mutableStateMapOf<OutputPortKey, Offset>() }

    // Data port positions (node-relative offsets, populated by custom onGloballyPositioned)
    val dataInputPortPositions = remember { mutableStateMapOf<DataPortKey, Offset>() }
    val dataOutputPortPositions = remember { mutableStateMapOf<DataPortKey, Offset>() }

    // Canvas container position in root coordinates
    var canvasPositionInRoot by remember { mutableStateOf(Offset.Zero) }

    // The graph nodes are already AzoraNodeModel
    val azoraNodes = remember(graph.nodes) {
        graph.nodes.values.toList()
    }

    // Build all drawable links (exec + data), all white
    val drawableLinks = remember(
        graph.execLinks, graph.dataLinks, graph.nodes,
        canvasState.panOffset, canvasPositionInRoot,
        inputPortPositions.toMap(), outputPortPositions.toMap(),
        dataInputPortPositions.toMap(), dataOutputPortPositions.toMap()
    ) {
        val execLinks = graph.execLinks.values.mapNotNull { link ->
            val sourceNode = azoraNodes.find { it.id == link.sourceNodeId }
            val targetNode = azoraNodes.find { it.id == link.targetNodeId }
            if (sourceNode == null || targetNode == null) return@mapNotNull null

            val outputOffset = outputPortPositions[OutputPortKey(link.sourceNodeId, link.sourcePortIndex)]
                ?: return@mapNotNull null
            val inputOffset = inputPortPositions[InputPortKey(link.targetNodeId)]
                ?: return@mapNotNull null

            AzoraDrawableLink(
                id = link.id,
                sourceNodeId = link.sourceNodeId,
                targetNodeId = link.targetNodeId,
                startPosition = Offset(
                    sourceNode.positionX + canvasState.panOffset.x + outputOffset.x,
                    sourceNode.positionY + canvasState.panOffset.y + outputOffset.y
                ),
                endPosition = Offset(
                    targetNode.positionX + canvasState.panOffset.x + inputOffset.x,
                    targetNode.positionY + canvasState.panOffset.y + inputOffset.y
                ),
                portType = AzoraPortType.NAV_DIALOG,
                outputPortIndex = link.sourcePortIndex,
                reroutePoints = link.reroutePoints.map { AzoraReroutePointModel(it.id, it.x, it.y) },
                startColor = Color.White,
                endColor = Color.White
            )
        }

        val dataLinks = graph.dataLinks.values.mapNotNull { link ->
            val sourceNode = azoraNodes.find { it.id == link.sourceNodeId }
            val targetNode = azoraNodes.find { it.id == link.targetNodeId }
            if (sourceNode == null || targetNode == null) return@mapNotNull null

            val outputOffset = dataOutputPortPositions[DataPortKey(link.sourceNodeId, link.sourcePortName)]
                ?: return@mapNotNull null
            val inputOffset = dataInputPortPositions[DataPortKey(link.targetNodeId, link.targetPortName)]
                ?: return@mapNotNull null

            AzoraDrawableLink(
                id = link.id,
                sourceNodeId = link.sourceNodeId,
                targetNodeId = link.targetNodeId,
                startPosition = Offset(
                    sourceNode.positionX + canvasState.panOffset.x + outputOffset.x,
                    sourceNode.positionY + canvasState.panOffset.y + outputOffset.y
                ),
                endPosition = Offset(
                    targetNode.positionX + canvasState.panOffset.x + inputOffset.x,
                    targetNode.positionY + canvasState.panOffset.y + inputOffset.y
                ),
                portType = AzoraPortType.NAV_DIALOG,
                outputPortIndex = -1, // Data links don't map to exec output port indices
                reroutePoints = link.reroutePoints.map { AzoraReroutePointModel(it.id, it.x, it.y) },
                startColor = Color.White,
                endColor = Color.White
            )
        }

        execLinks + dataLinks
    }

    // Capture nullable canvas state values for smart cast
    val linkSourceNodeId = canvasState.linkSourceNodeId
    val dataLinkSourcePortName = state.dataLinkSourcePortName

    // Compute link creation start position
    val linkCreationStart = remember(
        canvasState.isCreatingLink, linkSourceNodeId,
        canvasState.linkOutputPortIndex, state.isDataLinkCreation,
        dataLinkSourcePortName, state.dataLinkIsOutput,
        outputPortPositions.toMap(), dataOutputPortPositions.toMap(),
        dataInputPortPositions.toMap(), canvasState.panOffset
    ) {
        if (!canvasState.isCreatingLink || linkSourceNodeId == null) return@remember null

        val sourceNodeModel = azoraNodes.find { it.id == linkSourceNodeId }
            ?: return@remember null

        if (state.isDataLinkCreation && dataLinkSourcePortName != null) {
            // Data link creation
            val offset = if (state.dataLinkIsOutput) {
                dataOutputPortPositions[DataPortKey(linkSourceNodeId, dataLinkSourcePortName)]
            } else {
                dataInputPortPositions[DataPortKey(linkSourceNodeId, dataLinkSourcePortName)]
            }
            offset?.let {
                Offset(
                    sourceNodeModel.positionX + canvasState.panOffset.x + it.x,
                    sourceNodeModel.positionY + canvasState.panOffset.y + it.y
                )
            }
        } else {
            // Exec link creation
            val offset = outputPortPositions[OutputPortKey(linkSourceNodeId, canvasState.linkOutputPortIndex)]
            offset?.let {
                Offset(
                    sourceNodeModel.positionX + canvasState.panOffset.x + it.x,
                    sourceNodeModel.positionY + canvasState.panOffset.y + it.y
                )
            }
        }
    }

    AzoraEditorCanvas(
        canvasState = canvasState,
        onCanvasAction = onCanvasAction,
        links = drawableLinks,
        nodes = azoraNodes,
        linkCreationStart = linkCreationStart,
        linkCreationStartColor = Color.White,
        linkCreationEndColor = Color.White,
        onCanvasPositioned = { position ->
            canvasPositionInRoot = position
        },
        onInputPortPositioned = { nodeId, positionInRoot ->
            val node = azoraNodes.find { it.id == nodeId }
            if (node != null) {
                val posInCanvas = Offset(
                    positionInRoot.x - canvasPositionInRoot.x,
                    positionInRoot.y - canvasPositionInRoot.y
                )
                val nodeCanvasX = node.positionX + canvasState.panOffset.x
                val nodeCanvasY = node.positionY + canvasState.panOffset.y
                inputPortPositions[InputPortKey(nodeId)] = Offset(
                    posInCanvas.x - nodeCanvasX,
                    posInCanvas.y - nodeCanvasY
                )
            }
        },
        onOutputPortPositioned = { nodeId, index, positionInRoot ->
            val node = azoraNodes.find { it.id == nodeId }
            if (node != null) {
                val posInCanvas = Offset(
                    positionInRoot.x - canvasPositionInRoot.x,
                    positionInRoot.y - canvasPositionInRoot.y
                )
                val nodeCanvasX = node.positionX + canvasState.panOffset.x
                val nodeCanvasY = node.positionY + canvasState.panOffset.y
                outputPortPositions[OutputPortKey(nodeId, index)] = Offset(
                    posInCanvas.x - nodeCanvasX,
                    posInCanvas.y - nodeCanvasY
                )
            }
        },
        nodeContent = { node, isSelected, isLinkSource, backLinkTransitionType,
                        linkTransitionType, panOffset, isInputConnected,
                        connectedOutputPortIndices, onSelect, onStartLink,
                        onEndLink, onMove, onEndDrag, onDismissContextMenus,
                        onInputPortPositioned, onOutputPortPositioned ->

            val scriptNode = graph.nodes[node.id] ?: return@AzoraEditorCanvas
            val properties = scriptNode.properties
            // Get variable type for variable nodes to determine color
            val variableType = properties["variableId"]?.let { varId ->
                graph.variables[varId]?.type
            }
            val headerColor = nodeHeaderColorForNode(scriptNode, variableType)
            val hasExecInput = AzoraPortDefinition.execInputCount(scriptNode.type) > 0
            val execOutputLabels = AzoraPortDefinition.execOutputsForNode(scriptNode.type, properties)
            val dataInputs = AzoraPortDefinition.dataInputs(scriptNode.type, properties)
            val dataOutputs = AzoraPortDefinition.dataOutputs(scriptNode.type, properties)

            // Exec port definitions for the SDK
            val inputPortDef = if (hasExecInput) AzoraInputPortDef(type = AzoraPortType.NAV_DIALOG) else null
            val outputPortDefs = execOutputLabels.mapIndexed { index, label ->
                AzoraOutputPortDef(index = index, label = label, type = AzoraPortType.NAV_DIALOG)
            }

            // Connected data ports
            val connectedDataInputs = graph.dataLinks.values
                .filter { it.targetNodeId == node.id }
                .map { it.targetPortName }
                .toSet()
            val connectedDataOutputs = graph.dataLinks.values
                .filter { it.sourceNodeId == node.id }
                .map { it.sourcePortName }
                .toSet()

            AzoraNode(
                node = node,
                isSelected = isSelected,
                isLinkSource = isLinkSource,
                linkTransitionType = linkTransitionType,
                panOffset = panOffset,
                onSelect = onSelect,
                onStartLink = onStartLink,
                onEndLink = onEndLink,
                onMove = onMove,
                onEndDrag = onEndDrag,
                onDismissContextMenus = onDismissContextMenus,
                nodeColor = headerColor,
                borderColor = headerColor.copy(alpha = 0.5f),
                inputPortDef = inputPortDef,
                outputPortDefs = outputPortDefs,
                isInputConnected = isInputConnected,
                connectedOutputPortIndices = connectedOutputPortIndices,
                onInputPortPositioned = onInputPortPositioned,
                onOutputPortPositioned = onOutputPortPositioned,
                headerContent = { inputPorts, outputPorts ->
                    ScriptNodeHeaderContent(
                        scriptNode = scriptNode,
                        headerColor = headerColor,
                        dataInputs = dataInputs,
                        dataOutputs = dataOutputs,
                        connectedDataInputs = connectedDataInputs,
                        connectedDataOutputs = connectedDataOutputs,
                        inputPorts = inputPorts,
                        outputPorts = outputPorts,
                        onDataPortClick = { portName, isOutput, dataType ->
                            if (state.isDataLinkCreation) {
                                onAction(AzoraNodesAction.CompleteDataLink(node.id, portName, isOutput))
                            } else {
                                onAction(AzoraNodesAction.StartDataLink(node.id, portName, isOutput, dataType))
                            }
                        },
                        onDataPortPositioned = { portName, isOutput, posInRoot ->
                            // Convert root position to node-relative offset
                            val posInCanvas = Offset(
                                posInRoot.x - canvasPositionInRoot.x,
                                posInRoot.y - canvasPositionInRoot.y
                            )
                            val nodeCanvasX = node.positionX + canvasState.panOffset.x
                            val nodeCanvasY = node.positionY + canvasState.panOffset.y
                            val relativePos = Offset(
                                posInCanvas.x - nodeCanvasX,
                                posInCanvas.y - nodeCanvasY
                            )
                            if (isOutput) {
                                dataOutputPortPositions[DataPortKey(node.id, portName)] = relativePos
                            } else {
                                dataInputPortPositions[DataPortKey(node.id, portName)] = relativePos
                            }
                        },
                        literalValue = node.properties["literal_Value"] ?: "",
                        onLiteralValueChange = { newValue ->
                            onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("literal_Value" to newValue)))
                        }
                    )
                }
            )
        },
        canvasContextMenuContent = { position, onDismiss ->
            val worldPosition = Offset(
                position.x - canvasState.panOffset.x,
                position.y - canvasState.panOffset.y
            )
            val hasStartNode = graph.nodes.values.any { it.type == AzoraNodeType.START }
            ScriptNodeContextMenu(
                position = position,
                hasStartNode = hasStartNode,
                variables = graph.variables,
                onNodeSelected = { type, properties ->
                    onAction(AzoraNodesAction.AddNode(type, worldPosition, properties))
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        },
        modifier = modifier
    )
}
