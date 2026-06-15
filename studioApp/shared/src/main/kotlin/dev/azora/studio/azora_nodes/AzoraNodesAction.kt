package org.azora.studio.azora_nodes

import androidx.compose.ui.geometry.Offset
import org.azora.canvas.domain.model.AzoraGraphModel
import org.azora.canvas.presentation.state.AzoraCanvasAction
import org.azora.canvas.domain.model.node.AzoraNodeDataClass
import org.azora.canvas.domain.model.node.AzoraNodeDataType
import org.azora.canvas.domain.model.node.AzoraNodeEnum
import org.azora.canvas.domain.model.node.AzoraNodeFunction
import org.azora.canvas.domain.model.node.AzoraNodeVar
import org.azora.canvas.domain.type.AzoraNodeType

sealed interface AzoraNodesAction {

    // Canvas action forwarding (pan, selection, exec link creation, node move, etc.)
    data class CanvasAction(val action: AzoraCanvasAction) : AzoraNodesAction

    // Graph refresh from project
    data class Refresh(val graphs: List<AzoraGraphModel>) : AzoraNodesAction

    // Node operations
    data class AddNode(val type: AzoraNodeType, val position: Offset, val properties: Map<String, String> = emptyMap()) : AzoraNodesAction
    data class DeleteNodes(val nodeIds: Set<String>) : AzoraNodesAction
    data class UpdateNodeProperties(val nodeId: String, val properties: Map<String, String>) : AzoraNodesAction

    // Data link operations (script-specific, separate from exec links handled by canvas)
    data class StartDataLink(val nodeId: String, val portName: String, val isOutput: Boolean, val dataType: AzoraNodeDataType) : AzoraNodesAction
    data class CompleteDataLink(val targetNodeId: String, val targetPortName: String, val targetIsOutput: Boolean) : AzoraNodesAction

    // Variable operations
    data class AddVariable(val variable: AzoraNodeVar) : AzoraNodesAction
    data class UpdateVariable(val variable: AzoraNodeVar) : AzoraNodesAction
    data class DeleteVariable(val variableId: String) : AzoraNodesAction
    data class SelectVariable(val variableId: String?) : AzoraNodesAction
    data class CreateGetter(val variableId: String) : AzoraNodesAction
    data class CreateSetter(val variableId: String) : AzoraNodesAction

    // Function operations
    data class AddFunction(val function: AzoraNodeFunction) : AzoraNodesAction
    data class UpdateFunction(val function: AzoraNodeFunction) : AzoraNodesAction
    data class DeleteFunction(val functionId: String) : AzoraNodesAction

    // Enum operations
    data class AddEnum(val enumDef: AzoraNodeEnum) : AzoraNodesAction
    data class UpdateEnum(val enumDef: AzoraNodeEnum) : AzoraNodesAction
    data class DeleteEnum(val enumId: String) : AzoraNodesAction

    // Data class operations
    data class AddDataClass(val dataClassDef: AzoraNodeDataClass) : AzoraNodesAction
    data class UpdateDataClass(val dataClassDef: AzoraNodeDataClass) : AzoraNodesAction
    data class DeleteDataClass(val classId: String) : AzoraNodesAction

    // Editing
    data class EditNode(val nodeId: String?) : AzoraNodesAction

    // Execution
    data object Run : AzoraNodesAction
    data object Stop : AzoraNodesAction

    // Delete selected (node or link)
    data object DeleteSelected : AzoraNodesAction

    // Undo/Redo
    data object Undo : AzoraNodesAction
    data object Redo : AzoraNodesAction

    // Save
    data object Save : AzoraNodesAction
}
