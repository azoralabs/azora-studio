package org.azora.studio.azora_nodes.panel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.azora.studio.azora_nodes.AzoraNodesAction
import org.azora.canvas.domain.model.AzoraGraphModel
import org.azora.canvas.domain.model.node.AzoraNodeDataType
import org.azora.canvas.domain.model.node.AzoraNodeVar
import org.azora.sdk.core.theme.LocalAzoraPalette
import kotlin.random.Random

/**
 * Type colors matching GlobalConstantsPanel for consistency
 */
private object VariableTypeColors {
    val Boolean = Color(0xFFE57373) // AccentRed-like
    val Integer = Color(0xFF64B5F6) // AccentBlue-like
    val Real = Color(0xFF4DB6AC)    // AccentTeal-like
    val Text = Color(0xFF81C784)    // AccentGreen-like
    val Enum = Color(0xFFFFD54F)    // Yellow
    val DataClass = Color(0xFF7986CB) // Indigo
    val Any = Color(0xFF9E9E9E)     // Gray
}

private fun AzoraNodeDataType.toColor(): Color = when (this) {
    AzoraNodeDataType.BOOLEAN -> VariableTypeColors.Boolean
    AzoraNodeDataType.INTEGER -> VariableTypeColors.Integer
    AzoraNodeDataType.REAL -> VariableTypeColors.Real
    AzoraNodeDataType.STRING -> VariableTypeColors.Text
    AzoraNodeDataType.ENUM -> VariableTypeColors.Enum
    AzoraNodeDataType.DATA_CLASS -> VariableTypeColors.DataClass
    AzoraNodeDataType.ANY -> VariableTypeColors.Any
}

@Composable
fun AzoraNodeVarsPanel(
    graph: AzoraGraphModel,
    selectedVariableId: String?,
    onAction: (AzoraNodesAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onAction(AzoraNodesAction.SelectVariable(null)) }
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Variables",
                color = palette.contentTop,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "+",
                color = palette.contentMid,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        val id = Random.nextLong().toString(36)
                        onAction(
                            AzoraNodesAction.AddVariable(
                                AzoraNodeVar(
                                    id = id,
                                    name = "var_${graph.variables.size}",
                                    type = AzoraNodeDataType.INTEGER,
                                    defaultValue = "0"
                                )
                            )
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            graph.variables.values.forEach { variable ->
                VariableRow(
                    variable = variable,
                    isSelected = variable.id == selectedVariableId,
                    onSelect = { onAction(AzoraNodesAction.SelectVariable(variable.id)) },
                    onDelete = { onAction(AzoraNodesAction.DeleteVariable(variable.id)) },
                    onCreateGetter = { onAction(AzoraNodesAction.CreateGetter(variable.id)) },
                    onCreateSetter = { onAction(AzoraNodesAction.CreateSetter(variable.id)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VariableRow(
    variable: AzoraNodeVar,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onCreateGetter: () -> Unit,
    onCreateSetter: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    var showContextMenu by remember { mutableStateOf(false) }
    val typeColor = variable.type.toColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) palette.surfaceTop.copy(alpha = 0.6f)
                else palette.surfaceMid.copy(alpha = 0.5f)
            )
            .then(
                if (isSelected) Modifier.border(1.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = { onSelect() },
                onLongClick = { showContextMenu = true }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type color indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(typeColor)
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = variable.name,
                color = palette.contentTop,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${variable.type.label} = ${variable.defaultValue}",
                color = typeColor.copy(alpha = 0.8f),
                fontSize = 9.sp
            )
        }
        Text(
            text = "x",
            color = palette.contentLow,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .clickable { onDelete() }
                .padding(4.dp)
        )

        // Context menu dropdown
        androidx.compose.material3.DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(palette.surfaceTop)
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Create Getter", color = palette.contentTop, fontSize = 11.sp) },
                onClick = {
                    showContextMenu = false
                    onCreateGetter()
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Create Setter", color = palette.contentTop, fontSize = 11.sp) },
                onClick = {
                    showContextMenu = false
                    onCreateSetter()
                }
            )
        }
    }
}
