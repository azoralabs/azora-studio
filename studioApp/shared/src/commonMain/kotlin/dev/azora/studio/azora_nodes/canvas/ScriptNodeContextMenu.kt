package dev.azora.studio.azora_nodes.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.canvas.domain.model.node.AzoraNodeVar
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.sdk.core.theme.palette.AzoraPalette

@Composable
fun ScriptNodeContextMenu(
    position: Offset,
    hasStartNode: Boolean,
    variables: Map<String, AzoraNodeVar>,
    onNodeSelected: (AzoraNodeType, Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf<String?>("Flow") }
    val focusRequester = remember { FocusRequester() }

    // Types to exclude from generic listing (we'll show specific variable items instead)
    val excludedTypes = setOf(
        AzoraNodeType.GET_VARIABLE,
        AzoraNodeType.SET_VARIABLE,
        AzoraNodeType.GET_CONSTANT
    )

    // Group node types by category, excluding START if one already exists, and variable-related types
    val categories = remember(hasStartNode) {
        AzoraNodeType.entries
            .filter { type ->
                !(type == AzoraNodeType.START && hasStartNode) && type !in excludedTypes
            }
            .groupBy { it.category }
    }

    // Build variable items: Get X, Set X for each variable (no Set for readonly)
    data class VariableMenuItem(
        val label: String,
        val type: AzoraNodeType,
        val variable: AzoraNodeVar
    )

    val variableItems = remember(variables) {
        variables.values.flatMap { variable ->
            buildList {
                add(VariableMenuItem("Get ${variable.name}", AzoraNodeType.GET_VARIABLE, variable))
                if (!variable.readonly) {
                    add(VariableMenuItem("Set ${variable.name}", AzoraNodeType.SET_VARIABLE, variable))
                }
            }
        }
    }

    // Filter items based on search
    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) categories
        else categories.mapValues { (_, types) ->
            types.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }.filterValues { it.isNotEmpty() }
    }

    // Filter variable items based on search
    val filteredVariableItems = remember(searchQuery, variableItems) {
        if (searchQuery.isBlank()) variableItems
        else variableItems.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    // Request focus on search field when menu opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
            .width(280.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(AzoraPalette.Neutral85)
            .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(12.dp))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzoraPalette.Neutral70)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add Node",
                    color = AzoraPalette.Neutral10,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AzoraPalette.Neutral60)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = AzoraPalette.Neutral30,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AzoraPalette.Neutral70)
                    .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Search",
                        color = AzoraPalette.Neutral50,
                        fontSize = 12.sp
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = AzoraPalette.Neutral10,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(AzoraPalette.AccentBlue),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "print, if, loop...",
                                        color = AzoraPalette.Neutral50,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(AzoraPalette.Neutral60)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                color = AzoraPalette.Neutral40,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Categories and items
            Column(
                modifier = Modifier
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredCategories.forEach { (category, types) ->
                    CategoryHeader(
                        title = category,
                        count = types.size,
                        isExpanded = expandedCategory == category || searchQuery.isNotBlank(),
                        onClick = { expandedCategory = if (expandedCategory == category) null else category }
                    )

                    if (expandedCategory == category || searchQuery.isNotBlank()) {
                        types.forEach { type ->
                            NodeTypeMenuItem(
                                type = type,
                                color = categoryColor(category),
                                onClick = { onNodeSelected(type, emptyMap()) }
                            )
                        }
                    }
                }

                // Variables section (if there are any variables)
                if (filteredVariableItems.isNotEmpty()) {
                    CategoryHeader(
                        title = "Variables",
                        count = filteredVariableItems.size,
                        isExpanded = expandedCategory == "Variables" || searchQuery.isNotBlank(),
                        onClick = { expandedCategory = if (expandedCategory == "Variables") null else "Variables" }
                    )

                    if (expandedCategory == "Variables" || searchQuery.isNotBlank()) {
                        filteredVariableItems.forEach { item ->
                            VariableMenuItem(
                                label = item.label,
                                isGetter = item.type == AzoraNodeType.GET_VARIABLE,
                                color = categoryColor("Variables"),
                                onClick = {
                                    onNodeSelected(
                                        item.type,
                                        mapOf(
                                            "variableId" to item.variable.id,
                                            "variableName" to item.variable.name
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                // Empty state
                if (filteredCategories.isEmpty() && filteredVariableItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found",
                            color = AzoraPalette.Neutral50,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun categoryColor(category: String): Color = when (category) {
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
    else -> Color(0xFF555555)
}

@Composable
private fun CategoryHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isExpanded) AzoraPalette.Neutral70 else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isExpanded) "▼" else "▶",
            color = AzoraPalette.Neutral50,
            fontSize = 10.sp
        )
        Text(
            text = title,
            color = AzoraPalette.Neutral30,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count",
            color = AzoraPalette.Neutral50,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun NodeTypeMenuItem(
    type: AzoraNodeType,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHovered) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isHovered) 1.dp else 0.dp,
                color = if (isHovered) color.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .hoverable(interactionSource)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Color indicator with first letter
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = type.label.first().toString(),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.label,
                color = if (isHovered) AzoraPalette.Neutral10 else AzoraPalette.Neutral20,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = type.category,
                color = AzoraPalette.Neutral50,
                fontSize = 10.sp
            )
        }

        if (isHovered) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = AzoraPalette.Neutral90,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VariableMenuItem(
    label: String,
    isGetter: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHovered) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isHovered) 1.dp else 0.dp,
                color = if (isHovered) color.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .hoverable(interactionSource)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Color indicator with G or S
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isGetter) "G" else "S",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = label,
            color = if (isHovered) AzoraPalette.Neutral10 else AzoraPalette.Neutral20,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (isHovered) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = AzoraPalette.Neutral90,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
