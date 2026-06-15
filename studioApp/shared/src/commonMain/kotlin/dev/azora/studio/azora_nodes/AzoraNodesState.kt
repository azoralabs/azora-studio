package dev.azora.studio.azora_nodes

import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.presentation.state.AzoraCanvasState

data class AzoraNodesState(
    val graph: AzoraGraphModel = AzoraGraphModel(id = ""),
    val canvasState: AzoraCanvasState = AzoraCanvasState(),
    // Script-specific link creation extension (data links need port name, not index)
    val isDataLinkCreation: Boolean = false,
    val dataLinkSourcePortName: String? = null,
    val dataLinkIsOutput: Boolean = true,
    val dataLinkSourceDataType: AzoraNodeDataType? = null,
    // Execution
    val isRunning: Boolean = false,
    // Properties panel - can show node or variable
    val editingNodeId: String? = null,
    val selectedVariableId: String? = null,
)
