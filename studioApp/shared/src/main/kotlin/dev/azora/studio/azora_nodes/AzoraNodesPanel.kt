package org.azora.studio.azora_nodes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.azora.studio.azora_nodes.canvas.AzoraNodesCanvas
import org.azora.studio.azora_nodes.panel.ScriptNodePropertiesPanel
import org.azora.studio.azora_nodes.panel.AzoraNodeVarsPanel
import org.azora.sdk.core.domain.util.Res
import org.azora.sdk.core.project.domain.globalConstants
import org.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import org.azora.sdk.core.theme.LocalAzoraPalette
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AzoraNodesPanel(
    viewModel: AzoraNodesViewModel = koinInject(),
    projectRepository: AzoraProjectRepository = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAzoraPalette.current
    val coroutineScope = rememberCoroutineScope()

    // Activate this ViewModel for undo/redo when panel is visible
    LaunchedEffect(Unit) {
        viewModel.setActive()
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        // Toolbar
        AzoraNodesToolbar(
            state = state,
            onRun = {
                coroutineScope.launch {
                    val result = projectRepository.getProject()
                    val constants = if (result is Res.Success) {
                        result.data.settings.globalConstants
                    } else {
                        emptyList()
                    }
                    viewModel.run(constants)
                }
            },
            onAction = viewModel::onAction
        )

        // Main content: canvas + side panels
        Row(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Left side: variables panel
            Column(
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight()
                    .background(palette.surfaceMid.copy(alpha = 0.3f))
            ) {
                AzoraNodeVarsPanel(
                    graph = state.graph,
                    selectedVariableId = state.selectedVariableId,
                    onAction = viewModel::onAction
                )
            }

            // Center: canvas (context menu handled by AzoraEditorCanvas)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AzoraNodesCanvas(
                    state = state,
                    onCanvasAction = { viewModel.onAction(AzoraNodesAction.CanvasAction(it)) },
                    onAction = viewModel::onAction
                )
            }

            // Right side: properties panel
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(palette.surfaceMid.copy(alpha = 0.3f))
            ) {
                val editingNode = state.editingNodeId?.let { state.graph.nodes[it] }
                val selectedVariable = state.selectedVariableId?.let { state.graph.variables[it] }
                ScriptNodePropertiesPanel(
                    node = editingNode,
                    selectedVariable = selectedVariable,
                    graph = state.graph,
                    onAction = viewModel::onAction
                )
            }
        }
    }
}

@Composable
private fun AzoraNodesToolbar(
    state: AzoraNodesState,
    onRun: () -> Unit,
    onAction: (AzoraNodesAction) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(palette.surfaceTop)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "AzoraNodes",
            color = palette.contentTop,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.weight(1f))

        // Run / Stop
        if (state.isRunning) {
            ToolbarButton(
                text = "Stop",
                textColor = palette.error,
                onClick = { onAction(AzoraNodesAction.Stop) }
            )
        } else {
            ToolbarButton(
                text = "Run",
                textColor = palette.success,
                onClick = onRun
            )
        }

        // Delete selected
        ToolbarButton(
            text = "Delete",
            enabled = state.canvasState.selectedNodeId != null
                    || state.canvasState.selectedLinkId != null,
            onClick = { onAction(AzoraNodesAction.DeleteSelected) }
        )

        // Save
        ToolbarButton(
            text = "Save",
            onClick = { onAction(AzoraNodesAction.Save) }
        )
    }
}

@Composable
private fun ToolbarButton(
    text: String,
    enabled: Boolean = true,
    textColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val color = when {
        !enabled -> palette.contentLow
        textColor != null -> textColor
        else -> palette.contentTop
    }
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
