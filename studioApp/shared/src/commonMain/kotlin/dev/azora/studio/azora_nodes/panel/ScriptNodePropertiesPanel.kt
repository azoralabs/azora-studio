package dev.azora.studio.azora_nodes.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.studio.azora_nodes.AzoraNodesAction
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.project.domain.GlobalConstant
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.domain.model.node.AzoraNodeDataClassField
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.model.node.AzoraNodeVar
import dev.azora.canvas.domain.AzoraPortDefinition
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.sdk.core.project.domain.globalConstants
import dev.azora.sdk.core.theme.LocalAzoraPalette
import org.koin.compose.koinInject

@Composable
fun ScriptNodePropertiesPanel(
    node: AzoraNodeModel?,
    selectedVariable: AzoraNodeVar?,
    graph: AzoraGraphModel,
    onAction: (AzoraNodesAction) -> Unit,
    modifier: Modifier = Modifier,
    projectRepository: AzoraProjectRepository = koinInject()
) {
    // Load global constants for GET_CONSTANT dropdown
    var globalConstants by remember { mutableStateOf<List<GlobalConstant>>(emptyList()) }
    LaunchedEffect(Unit) {
        val result = projectRepository.getProject()
        if (result is Res.Success) {
            globalConstants = result.data.settings.globalConstants
        }
    }
    val palette = LocalAzoraPalette.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Properties",
            color = palette.contentTop,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        // Show variable properties if a variable is selected
        if (selectedVariable != null) {
            VariablePropertiesContent(
                variable = selectedVariable,
                onAction = onAction
            )
            return@Column
        }

        if (node == null) {
            Text(
                text = "Select a node or variable",
                color = palette.contentLow,
                fontSize = 11.sp
            )
            return@Column
        }

        Text(
            text = node.type.label,
            color = palette.contentMid,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(8.dp))

        // Show editable properties based on node type
        when (node.type) {
            AzoraNodeType.GET_VARIABLE -> {
                PropertyDropdown(
                    label = "Variable",
                    currentValue = node.properties["variableName"] ?: "",
                    options = graph.variables.values.map { it.name },
                    onSelect = { name ->
                        val variable = graph.variables.values.find { it.name == name }
                        if (variable != null) {
                            onAction(
                                AzoraNodesAction.UpdateNodeProperties(
                                    node.id,
                                    mapOf("variableId" to variable.id, "variableName" to variable.name)
                                )
                            )
                        }
                    }
                )
            }

            AzoraNodeType.SET_VARIABLE -> {
                // Variable selector
                PropertyDropdown(
                    label = "Variable",
                    currentValue = node.properties["variableName"] ?: "",
                    options = graph.variables.values.map { it.name },
                    onSelect = { name ->
                        val variable = graph.variables.values.find { it.name == name }
                        if (variable != null) {
                            onAction(
                                AzoraNodesAction.UpdateNodeProperties(
                                    node.id,
                                    mapOf("variableId" to variable.id, "variableName" to variable.name)
                                )
                            )
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Check if Value port is connected
                val isValueConnected = graph.dataLinks.values.any {
                    it.targetNodeId == node.id && it.targetPortName == "Value"
                }

                // Value field (only show when not connected)
                if (!isValueConnected) {
                    val variableType = node.properties["variableId"]?.let { graph.variables[it]?.type }
                    when (variableType) {
                        AzoraNodeDataType.BOOLEAN -> {
                            PropertyBooleanToggle(
                                label = "Value",
                                value = node.properties["literal_Value"]?.toBooleanStrictOrNull() ?: false,
                                onValueChange = { newVal ->
                                    onAction(
                                        AzoraNodesAction.UpdateNodeProperties(
                                            node.id,
                                            mapOf("literal_Value" to newVal.toString())
                                        )
                                    )
                                }
                            )
                        }
                        else -> {
                            PropertyTextField(
                                label = "Value",
                                value = node.properties["literal_Value"] ?: "",
                                onValueChange = { newVal ->
                                    onAction(
                                        AzoraNodesAction.UpdateNodeProperties(
                                            node.id,
                                            mapOf("literal_Value" to newVal)
                                        )
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Value: connected",
                        color = palette.contentLow,
                        fontSize = 10.sp
                    )
                }
            }

            AzoraNodeType.GET_CONSTANT -> {
                PropertyDropdown(
                    label = "Constant",
                    currentValue = node.properties["constantName"] ?: "",
                    options = globalConstants.map { it.name },
                    onSelect = { name ->
                        val constant = globalConstants.find { it.name == name }
                        if (constant != null) {
                            onAction(
                                AzoraNodesAction.UpdateNodeProperties(
                                    node.id,
                                    mapOf("constantId" to constant.id, "constantName" to constant.name)
                                )
                            )
                        }
                    }
                )
            }

            AzoraNodeType.PRINT -> {
                PropertyTextField(
                    label = "Prefix",
                    value = node.properties["prefix"] ?: "",
                    onValueChange = { newVal ->
                        onAction(
                            AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("prefix" to newVal))
                        )
                    }
                )
            }

            AzoraNodeType.MATCH -> {
                PropertyTextField(
                    label = "Case Count",
                    value = node.properties["caseCount"] ?: "2",
                    onValueChange = { newVal ->
                        onAction(
                            AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("caseCount" to newVal))
                        )
                    }
                )
                val caseCount = node.properties["caseCount"]?.toIntOrNull() ?: 2
                for (i in 0 until caseCount) {
                    PropertyTextField(
                        label = "Case $i",
                        value = node.properties["case_$i"] ?: "",
                        onValueChange = { newVal ->
                            onAction(
                                AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("case_$i" to newVal))
                            )
                        }
                    )
                }
            }

            AzoraNodeType.CAST -> {
                val castableTypes = listOf("BOOLEAN", "INTEGER", "REAL", "TEXT")
                PropertyDropdown(
                    label = "From Type",
                    currentValue = node.properties["fromType"] ?: "ANY",
                    options = castableTypes + "ANY",
                    onSelect = { type ->
                        onAction(
                            AzoraNodesAction.UpdateNodeProperties(
                                node.id,
                                mapOf("fromType" to type)
                            )
                        )
                    }
                )
                PropertyDropdown(
                    label = "To Type",
                    currentValue = node.properties["toType"] ?: "ANY",
                    options = castableTypes,
                    onSelect = { type ->
                        onAction(
                            AzoraNodesAction.UpdateNodeProperties(
                                node.id,
                                mapOf("toType" to type)
                            )
                        )
                    }
                )
            }

            AzoraNodeType.AZ_CODE, AzoraNodeType.AZ_EXPR -> {
                PropertyTextField(
                    label = if (node.type == AzoraNodeType.AZ_CODE) "Azora code" else "Azora expression",
                    value = node.properties["code"] ?: "",
                    onValueChange = { newVal ->
                        onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("code" to newVal)))
                    }
                )
            }

            AzoraNodeType.PARAM_GET -> {
                PropertyTextField(
                    label = "Parameter name",
                    value = node.properties["name"] ?: "",
                    onValueChange = { newVal ->
                        onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("name" to newVal)))
                    }
                )
            }

            AzoraNodeType.AZ_CALL -> {
                PropertyTextField(
                    label = "Function name",
                    value = node.properties["name"] ?: "",
                    onValueChange = { newVal ->
                        onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("name" to newVal)))
                    }
                )
                PropertyTextField(
                    label = "Argument count",
                    value = node.properties["argCount"] ?: "0",
                    onValueChange = { newVal ->
                        onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("argCount" to newVal)))
                    }
                )
                val argCount = node.properties["argCount"]?.toIntOrNull() ?: 0
                for (i in 0 until argCount) {
                    PropertyTextField(
                        label = "arg_$i",
                        value = node.properties["literal_arg_$i"] ?: "",
                        onValueChange = { newVal ->
                            onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("literal_arg_$i" to newVal)))
                        }
                    )
                }
            }

            AzoraNodeType.FOR_RANGE -> {
                PropertyTextField(
                    label = "Counter name",
                    value = node.properties["counter"] ?: "i",
                    onValueChange = { newVal ->
                        onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("counter" to newVal)))
                    }
                )
                PropertyBooleanToggle(
                    label = "Inclusive end (..)",
                    value = node.properties["inclusive"]?.toBooleanStrictOrNull() ?: true,
                    onValueChange = { newVal ->
                        onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("inclusive" to newVal.toString())))
                    }
                )
                for (portName in listOf("from", "to")) {
                    PropertyTextField(
                        label = portName,
                        value = node.properties["literal_$portName"] ?: "",
                        onValueChange = { newVal ->
                            onAction(AzoraNodesAction.UpdateNodeProperties(node.id, mapOf("literal_$portName" to newVal)))
                        }
                    )
                }
            }

            else -> {
                // Show literal values for unconnected data inputs
                val inputs = AzoraPortDefinition.dataInputs(node.type, node.properties)
                inputs.forEach { (portName, dataType) ->
                    when (dataType) {
                        AzoraNodeDataType.BOOLEAN -> {
                            PropertyBooleanToggle(
                                label = portName,
                                value = node.properties["literal_$portName"]?.toBooleanStrictOrNull() ?: false,
                                onValueChange = { newVal ->
                                    onAction(
                                        AzoraNodesAction.UpdateNodeProperties(
                                            node.id,
                                            mapOf("literal_$portName" to newVal.toString())
                                        )
                                    )
                                }
                            )
                        }
                        else -> {
                            PropertyTextField(
                                label = portName,
                                value = node.properties["literal_$portName"] ?: "",
                                onValueChange = { newVal ->
                                    onAction(
                                        AzoraNodesAction.UpdateNodeProperties(
                                            node.id,
                                            mapOf("literal_$portName" to newVal)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val palette = LocalAzoraPalette.current
    var text by remember(value) { mutableStateOf(value) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            color = palette.contentLow,
            fontSize = 10.sp
        )
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            textStyle = TextStyle(
                color = palette.contentTop,
                fontSize = 11.sp
            ),
            cursorBrush = SolidColor(palette.contentTop),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.surfaceMid)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PropertyDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            color = palette.contentLow,
            fontSize = 10.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.surfaceMid)
                .padding(4.dp)
        ) {
            if (options.isEmpty()) {
                Text(
                    text = "No options",
                    color = palette.contentLow,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(4.dp)
                )
            } else {
                options.forEach { option ->
                    val isSelected = option == currentValue
                    Text(
                        text = option,
                        color = if (isSelected) palette.contentTop else palette.contentMid,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .then(
                                if (isSelected) Modifier.background(palette.surfaceTop.copy(alpha = 0.3f))
                                else Modifier
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyBooleanToggle(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            color = palette.contentLow,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(2.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(false, true).forEach { option ->
                val isSelected = value == option
                Text(
                    text = option.toString(),
                    color = if (isSelected) palette.contentTop else palette.contentLow,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) PropertiesTypeColors.Boolean.copy(alpha = 0.3f) else palette.surfaceMid)
                        .clickable { onValueChange(option) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Type colors matching GlobalConstantsPanel for the properties panel
 */
private object PropertiesTypeColors {
    val Boolean = androidx.compose.ui.graphics.Color(0xFFE57373)
    val Integer = androidx.compose.ui.graphics.Color(0xFF64B5F6)
    val Real = androidx.compose.ui.graphics.Color(0xFF4DB6AC)
    val Text = androidx.compose.ui.graphics.Color(0xFF81C784)
    val Enum = androidx.compose.ui.graphics.Color(0xFFFFD54F)
    val DataClass = androidx.compose.ui.graphics.Color(0xFF7986CB)
    val Any = androidx.compose.ui.graphics.Color(0xFF9E9E9E)
}

private fun AzoraNodeDataType.toPropertiesColor(): androidx.compose.ui.graphics.Color = when (this) {
    AzoraNodeDataType.BOOLEAN -> PropertiesTypeColors.Boolean
    AzoraNodeDataType.INTEGER -> PropertiesTypeColors.Integer
    AzoraNodeDataType.REAL -> PropertiesTypeColors.Real
    AzoraNodeDataType.STRING -> PropertiesTypeColors.Text
    AzoraNodeDataType.ENUM -> PropertiesTypeColors.Enum
    AzoraNodeDataType.DATA_CLASS -> PropertiesTypeColors.DataClass
    AzoraNodeDataType.ANY -> PropertiesTypeColors.Any
}

@Composable
private fun VariablePropertiesContent(
    variable: AzoraNodeVar,
    onAction: (AzoraNodesAction) -> Unit
) {
    val palette = LocalAzoraPalette.current
    val typeColor = variable.type.toPropertiesColor()

    // Use key to preserve state during edits
    key(variable.id) {
        // Variable header with type indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                    .background(typeColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Variable",
                color = palette.contentMid,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(8.dp))

        // Name field
        var name by remember { mutableStateOf(variable.name) }
        LaunchedEffect(variable.name) { name = variable.name }
        Text(text = "Name", color = palette.contentLow, fontSize = 10.sp)
        BasicTextField(
            value = name,
            onValueChange = {
                name = it
                onAction(AzoraNodesAction.UpdateVariable(variable.copy(name = it)))
            },
            textStyle = TextStyle(color = palette.contentTop, fontSize = 11.sp),
            cursorBrush = SolidColor(palette.contentTop),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .background(palette.surfaceMid)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Type selector with colors
        Text(text = "Type", color = palette.contentLow, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .background(palette.surfaceMid)
                .padding(4.dp)
        ) {
            AzoraNodeDataType.entries.filter {
                it != AzoraNodeDataType.ENUM && it != AzoraNodeDataType.DATA_CLASS && it != AzoraNodeDataType.ANY
            }.forEach { type ->
                val isSelected = variable.type == type
                val btnColor = type.toPropertiesColor()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .then(
                            if (isSelected) Modifier.background(btnColor.copy(alpha = 0.2f))
                            else Modifier
                        )
                        .clickable {
                            onAction(AzoraNodesAction.UpdateVariable(variable.copy(type = type)))
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            .background(btnColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = type.label,
                        color = if (isSelected) btnColor else palette.contentMid,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Default value field - different editor based on type
        Text(text = "Default Value", color = palette.contentLow, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        when (variable.type) {
            AzoraNodeDataType.BOOLEAN -> {
                // Boolean toggle (true/false)
                val boolValue = variable.defaultValue.ifEmpty { variable.type.defaultValue }.toBooleanStrictOrNull() ?: false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(false, true).forEach { option ->
                        val isSelected = boolValue == option
                        Text(
                            text = option.toString(),
                            color = if (isSelected) palette.contentTop else palette.contentLow,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) typeColor.copy(alpha = 0.3f) else palette.surfaceMid)
                                .clickable { onAction(AzoraNodesAction.UpdateVariable(variable.copy(defaultValue = option.toString()))) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            else -> {
                // Text field for other types
                var defaultValue by remember { mutableStateOf(variable.defaultValue.ifEmpty { variable.type.defaultValue }) }
                LaunchedEffect(variable.defaultValue) {
                    defaultValue = variable.defaultValue.ifEmpty { variable.type.defaultValue }
                }
                BasicTextField(
                    value = defaultValue,
                    onValueChange = {
                        defaultValue = it
                        onAction(AzoraNodesAction.UpdateVariable(variable.copy(defaultValue = it)))
                    },
                    textStyle = TextStyle(color = palette.contentTop, fontSize = 11.sp),
                    cursorBrush = SolidColor(palette.contentTop),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.surfaceMid)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Nullable checkbox
        PropertyCheckbox(
            label = "Nullable",
            checked = variable.nullable,
            onCheckedChange = { onAction(AzoraNodesAction.UpdateVariable(variable.copy(nullable = it))) }
        )

        Spacer(Modifier.height(4.dp))

        // Readonly checkbox
        PropertyCheckbox(
            label = "Readonly",
            checked = variable.readonly,
            onCheckedChange = { onAction(AzoraNodesAction.UpdateVariable(variable.copy(readonly = it))) }
        )

        Spacer(Modifier.height(12.dp))

        // Create nodes buttons
        Text(text = "Create Node", color = palette.contentLow, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Getter",
                color = palette.contentTop,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(palette.surfaceMid)
                    .clickable { onAction(AzoraNodesAction.CreateGetter(variable.id)) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = "Setter",
                color = palette.contentTop,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(palette.surfaceMid)
                    .clickable { onAction(AzoraNodesAction.CreateSetter(variable.id)) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PropertyCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAzoraPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) palette.primary else palette.surfaceMid)
                .then(
                    if (!checked) Modifier.border(1.dp, palette.contentLow, RoundedCornerShape(3.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    color = palette.contentTop,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = palette.contentMid,
            fontSize = 11.sp
        )
    }
}
