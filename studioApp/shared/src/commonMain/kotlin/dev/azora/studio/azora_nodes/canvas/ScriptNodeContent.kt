package dev.azora.studio.azora_nodes.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.canvas.domain.type.AzoraPortDataType
import dev.azora.canvas.presentation.node.AzoraDataPort
import dev.azora.canvas.presentation.node.InputPortsWrapper
import dev.azora.canvas.presentation.node.OutputPortsWrapper
import dev.azora.sdk.core.theme.palette.AzoraPalette

fun nodeHeaderColor(type: AzoraNodeType): Color = when (type.category) {
    "Flow" -> Color(0xFF4444AA)
    "Variables" -> Color(0xFF2D8B57)
    "Output" -> AzoraPalette.AccentPink
    "Control Flow" -> Color(0xFF888888)
    "Math" -> Color(0xFF338888)
    "Comparison" -> Color(0xFF886633)
    "Logic" -> Color(0xFF883333)
    "Cast" -> Color(0xFF666699)
    "Functions" -> AzoraPalette.AccentPink
    "Enums" -> Color(0xFF998833)
    "Data Classes" -> Color(0xFF336699)
    "Azora Script" -> Color(0xFF7A4FB0)
    else -> Color(0xFF555555)
}

/**
 * Get node header color based on node type and variable/constant data type.
 * For GET_VARIABLE/SET_VARIABLE nodes, returns color based on variable's data type.
 * For GET_CONSTANT nodes, returns color based on constant's data type.
 */
fun nodeHeaderColorForNode(node: AzoraNodeModel, variableType: AzoraNodeDataType?): Color {
    return when (node.type) {
        AzoraNodeType.GET_VARIABLE,
        AzoraNodeType.SET_VARIABLE -> {
            variableType?.toNodeColor() ?: nodeHeaderColor(node.type)
        }
        else -> nodeHeaderColor(node.type)
    }
}

/**
 * Convert AzoraNodeDataType to node header color.
 */
fun AzoraNodeDataType.toNodeColor(): Color = when (this) {
    AzoraNodeDataType.BOOLEAN -> AzoraPalette.AccentRed
    AzoraNodeDataType.INTEGER -> AzoraPalette.AccentBlue
    AzoraNodeDataType.REAL -> AzoraPalette.AccentTeal
    AzoraNodeDataType.STRING -> AzoraPalette.AccentGreen
    AzoraNodeDataType.ENUM -> Color(0xFF998833)
    AzoraNodeDataType.DATA_CLASS -> Color(0xFF336699)
    AzoraNodeDataType.ANY -> Color(0xFF888888)
}

/**
 * Blueprint-style node header title, resolved against the [graph] so nodes
 * always show what they act on ("Set speed", "Call attack", "Make Point",
 * "Color.Red") instead of their generic type label. Falls back to legacy
 * name-properties, then to the type label, so hand-made graphs still render.
 */
fun nodeDisplayTitle(node: AzoraNodeModel, graph: AzoraGraphModel): String {
    val props = node.properties
    fun variableName(): String =
        props["variableId"]?.let { graph.variables[it]?.name }
            ?: props["variableName"] ?: "Variable"

    return when (node.type) {
        AzoraNodeType.SET_VARIABLE -> "Set ${variableName()}"
        AzoraNodeType.GET_VARIABLE -> "Get ${variableName()}"
        AzoraNodeType.GET_CONSTANT -> "Const ${props["constantName"] ?: "Constant"}"
        AzoraNodeType.PARAM_GET -> "Param ${props["name"] ?: "?"}"
        AzoraNodeType.FUNCTION_DEF -> {
            val func = props["functionId"]?.let { graph.functions[it] }
            if (func != null) "Func ${func.name}(${func.parameters.joinToString(", ") { it.name }})"
            else "Func ${props["name"] ?: "Function"}"
        }
        AzoraNodeType.FUNCTION_CALL -> {
            val name = props["functionId"]?.let { graph.functions[it]?.name }
                ?: props["functionName"] ?: "Function"
            "Call $name"
        }
        AzoraNodeType.AZ_CALL -> "Call ${props["name"] ?: "?"}"
        AzoraNodeType.ENUM_DEF -> "Enum ${props["enumId"]?.let { graph.enums[it]?.name } ?: props["name"] ?: "Enum"}"
        AzoraNodeType.ENUM_VALUE -> {
            val enumName = props["enumId"]?.let { graph.enums[it]?.name } ?: props["enumName"] ?: "Enum"
            "$enumName.${props["value"] ?: "?"}"
        }
        AzoraNodeType.DATA_CLASS_DEF -> "Pack ${props["classId"]?.let { graph.dataClasses[it]?.name } ?: props["name"] ?: "Pack"}"
        AzoraNodeType.DATA_CLASS_CREATE -> "Make ${props["classId"]?.let { graph.dataClasses[it]?.name } ?: props["className"] ?: "Pack"}"
        AzoraNodeType.DATA_CLASS_GET_FIELD -> "Get ${props["fieldName"] ?: "field"}"
        AzoraNodeType.DATA_CLASS_SET_FIELD -> "Set ${props["fieldName"] ?: "field"}"
        AzoraNodeType.CAST -> "Cast to ${azTypeLabel(props["toType"])}"
        AzoraNodeType.MATCH -> "Match"
        AzoraNodeType.FOR_RANGE -> {
            val op = if (props["inclusive"]?.toBooleanStrictOrNull() != false) ".." else "..<"
            "For ${props["counter"] ?: "i"} in $op"
        }
        AzoraNodeType.WHILE -> "While"
        AzoraNodeType.AZ_EXPR -> props["code"]?.takeIf { it.isNotBlank() }?.let { shorten(it) } ?: "Azora Expression"
        AzoraNodeType.AZ_CODE -> props["code"]?.lineSequence()?.firstOrNull { it.isNotBlank() }?.let { shorten(it) } ?: "Azora Code"
        else -> node.title.ifBlank { node.type.label }.takeIf { node.type != AzoraNodeType.START } ?: "Start"
    }
}

/** Pin label without the `param_` / `field_` port-key prefixes. */
private fun pinLabel(portName: String): String =
    portName.removePrefix("param_").removePrefix("field_").removePrefix("arg_").let {
        if (portName.startsWith("arg_")) "arg $it" else it
    }

private fun shorten(text: String): String =
    text.trim().let { if (it.length <= 26) it else it.take(24) + "…" }

private fun azTypeLabel(dataTypeName: String?): String = when (dataTypeName) {
    "INTEGER" -> "Int"
    "REAL" -> "Real"
    "STRING" -> "String"
    "BOOLEAN" -> "Bool"
    null -> "?"
    else -> dataTypeName.lowercase().replaceFirstChar { it.uppercaseChar() }
}

fun AzoraNodeDataType.toAzoraPortDataType(): AzoraPortDataType = when (this) {
    AzoraNodeDataType.BOOLEAN -> AzoraPortDataType.BOOL
    AzoraNodeDataType.INTEGER -> AzoraPortDataType.INTEGER
    AzoraNodeDataType.REAL -> AzoraPortDataType.REAL
    AzoraNodeDataType.STRING -> AzoraPortDataType.TEXT
    AzoraNodeDataType.ENUM -> AzoraPortDataType.ENUM
    AzoraNodeDataType.DATA_CLASS -> AzoraPortDataType.DATA_CLASS
    AzoraNodeDataType.ANY -> AzoraPortDataType.ANY
}

/**
 * Header content for script nodes, following the SDK pattern.
 * Uses InputPortsWrapper/OutputPortsWrapper for exec ports (managed by SDK)
 * and AzoraDataPort for data ports (managed by custom code).
 */
@Composable
fun ScriptNodeHeaderContent(
    scriptNode: AzoraNodeModel,
    graph: AzoraGraphModel,
    headerColor: Color,
    dataInputs: List<Pair<String, AzoraNodeDataType>>,
    dataOutputs: List<Pair<String, AzoraNodeDataType>>,
    connectedDataInputs: Set<String>,
    connectedDataOutputs: Set<String>,
    inputPorts: @Composable () -> Unit,
    outputPorts: @Composable () -> Unit,
    onDataPortClick: (portName: String, isOutput: Boolean, dataType: AzoraNodeDataType) -> Unit,
    onDataPortPositioned: (portName: String, isOutput: Boolean, positionInRoot: Offset) -> Unit,
    onLiteralChange: ((portName: String, value: String) -> Unit)? = null,
) {
    val isSetVariable = scriptNode.type == AzoraNodeType.SET_VARIABLE
    val valueInputConnected = "value" in connectedDataInputs || "Value" in connectedDataInputs

    /** Inline default for an unconnected input (Blueprint-style editable pin value). */
    fun literalFor(portName: String): String =
        scriptNode.properties["literal_$portName"]
            ?: scriptNode.properties["literal_${portName.replaceFirstChar { it.uppercaseChar() }}"]
            ?: ""

    Column(modifier = Modifier.widthIn(min = 140.dp, max = 280.dp)) {
        // Title bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = nodeDisplayTitle(scriptNode, graph),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSetVariable) {
            // Special layout for SET_VARIABLE: exec input on top, data input below
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                // Exec ports row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InputPortsWrapper(inputPorts)
                    OutputPortsWrapper(outputPorts)
                }

                Spacer(Modifier.height(4.dp))

                // Data input port with inline textfield
                if (dataInputs.isNotEmpty()) {
                    val (name, type) = dataInputs.first()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                val size = coords.size
                                val center = Offset(
                                    pos.x + size.width / 2f,
                                    pos.y + size.height / 2f
                                )
                                onDataPortPositioned(name, false, center)
                            }
                        ) {
                            AzoraDataPort(
                                dataType = type.toAzoraPortDataType(),
                                isConnected = valueInputConnected,
                                onClick = { onDataPortClick(name, false, type) }
                            )
                        }
                        Spacer(Modifier.width(4.dp))

                        // Show textfield only when not connected
                        if (!valueInputConnected && onLiteralChange != null) {
                            InlineLiteralField(
                                portName = name,
                                value = literalFor(name),
                                onValueChange = { onLiteralChange(name, it) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = name,
                                color = AzoraPalette.Neutral30,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            // Standard layout: exec ports on sides (SDK), data ports in center
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                InputPortsWrapper(inputPorts)

                // Data ports in center area
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) {
                    val maxRows = maxOf(dataInputs.size, dataOutputs.size)
                    for (i in 0 until maxRows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Data input port
                            if (i < dataInputs.size) {
                                val (name, type) = dataInputs[i]
                                val connected = name in connectedDataInputs
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier.onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            val size = coords.size
                                            val center = Offset(
                                                pos.x + size.width / 2f,
                                                pos.y + size.height / 2f
                                            )
                                            onDataPortPositioned(name, false, center)
                                        }
                                    ) {
                                        AzoraDataPort(
                                            dataType = type.toAzoraPortDataType(),
                                            isConnected = connected,
                                            onClick = { onDataPortClick(name, false, type) }
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = pinLabel(name),
                                        color = AzoraPalette.Neutral30,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Unconnected inputs edit their default inline (Blueprint pins).
                                    if (!connected && onLiteralChange != null) {
                                        Spacer(Modifier.width(4.dp))
                                        InlineLiteralField(
                                            portName = name,
                                            value = literalFor(name),
                                            onValueChange = { onLiteralChange(name, it) },
                                            modifier = Modifier.widthIn(min = 36.dp, max = 90.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f, fill = false))
                            }

                            // Data output port
                            if (i < dataOutputs.size) {
                                val (name, type) = dataOutputs[i]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = name,
                                        color = AzoraPalette.Neutral30,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier.onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            val size = coords.size
                                            val center = Offset(
                                                pos.x + size.width / 2f,
                                                pos.y + size.height / 2f
                                            )
                                            onDataPortPositioned(name, true, center)
                                        }
                                    ) {
                                        AzoraDataPort(
                                            dataType = type.toAzoraPortDataType(),
                                            isConnected = name in connectedDataOutputs,
                                            onClick = { onDataPortClick(name, true, type) }
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f, fill = false))
                            }
                        }
                    }
                }

                OutputPortsWrapper(outputPorts)
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Small editable field showing an unconnected input's inline default
 * (stored as the node's `literal_<port>` property) — Unreal-pin style.
 */
@Composable
private fun InlineLiteralField(
    portName: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value) }
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        textStyle = TextStyle(
            color = AzoraPalette.Neutral10,
            fontSize = 10.sp
        ),
        cursorBrush = SolidColor(AzoraPalette.Neutral10),
        singleLine = true,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(AzoraPalette.Neutral70)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        decorationBox = { innerTextField ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = pinLabel(portName),
                        color = AzoraPalette.Neutral50,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}
