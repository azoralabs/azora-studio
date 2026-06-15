package dev.azora.studio.azora_nodes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import dev.azora.studio.assets.OpenAzoraNodesFilesManager
import dev.azora.studio.azora_nodes.canvas.AzoraNodesCanvas
import dev.azora.studio.azora_nodes.panel.ScriptNodePropertiesPanel
import dev.azora.studio.azora_nodes.panel.AzoraNodeVarsPanel
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator
import dev.azora.sdk.core.project.domain.globalConstants
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.docking.data.DockStateManagerImpl
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.container.DockContainer
import dev.azora.sdk.docking.presentation.panel.DockPanelRegistry
import dev.azora.sdk.docking.presentation.theme.DockTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Panel for editing a file-backed .azn file.
 */
@Composable
fun AzoraNodesFilePanel(
    panelId: String,
    projectPath: String,
    openFilesManager: OpenAzoraNodesFilesManager = koinInject(),
    consoleOutputManager: ConsoleOutputManager = koinInject(),
    undoRedoCoordinator: GlobalUndoRedoCoordinator = koinInject(),
    projectRepository: AzoraProjectRepository = koinInject(),
    dockStateManager: dev.azora.sdk.docking.domain.DockStateManager = koinInject()
) {
    val openFiles by openFilesManager.openFiles.collectAsState()
    val fileState = openFiles[panelId]
    val palette = LocalAzoraPalette.current

    // Try to recover file if not loaded - look up by panel title in Assets folder
    LaunchedEffect(panelId, projectPath) {
        if (openFilesManager.getState(panelId) == null && projectPath.isNotEmpty()) {
            println("[AzoraNodesFilePanel] File not loaded for panelId=$panelId, attempting recovery...")
            val dockState = dockStateManager.state.value
            val panelDescriptor = dockState.layout.panelDescriptors[panelId]
            println("[AzoraNodesFilePanel] panelDescriptor=$panelDescriptor")
            if (panelDescriptor != null) {
                val title = panelDescriptor.title
                val filePath = "$projectPath/Assets/$title.azn"
                println("[AzoraNodesFilePanel] Attempting to recover file: $filePath")
                val success = openFilesManager.restoreFile(panelId, filePath)
                println("[AzoraNodesFilePanel] Recovery result: $success")
            } else {
                println("[AzoraNodesFilePanel] No panel descriptor found for panelId=$panelId")
            }
        }
    }

    // Show loading state if file not loaded yet
    if (fileState == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(palette.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading...",
                color = palette.contentMid,
                fontSize = 12.sp
            )
        }
        return
    }

    val viewModel = remember(panelId) {
        AzoraNodesFileViewModel(
            panelId = panelId,
            openFilesManager = openFilesManager,
            consoleOutputManager = consoleOutputManager,
            undoRedoCoordinator = undoRedoCoordinator
        )
    }

    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Activate this ViewModel for undo/redo when panel is visible
    LaunchedEffect(Unit) {
        viewModel.setActive()
    }

    // Cleanup on dispose
    DisposableEffect(panelId) {
        onDispose {
            // ViewModel cleanup is handled by onCleared
        }
    }

    // Inner dock state for the 3-panel layout
    val innerStateManager = remember(panelId) {
        val varsId = "inner_${panelId}_vars"
        val canvasId = "inner_${panelId}_canvas"
        val propsId = "inner_${panelId}_props"

        val descriptors = mapOf(
            varsId to DockPanelDescriptor(id = varsId, title = "Variables", minimumWidth = 120f, minimumHeight = 100f, closeable = true),
            canvasId to DockPanelDescriptor(id = canvasId, title = "Node Graph", minimumWidth = 300f, minimumHeight = 200f, closeable = true),
            propsId to DockPanelDescriptor(id = propsId, title = "Properties", minimumWidth = 150f, minimumHeight = 100f, closeable = true),
        )

        val rootNode = DockNode.Split(
            id = "inner_root",
            orientation = DockOrientation.HORIZONTAL,
            first = DockNode.Leaf(id = "inner_left", panelId = varsId),
            second = DockNode.Split(
                id = "inner_right_split",
                orientation = DockOrientation.HORIZONTAL,
                first = DockNode.Leaf(id = "inner_center", panelId = canvasId),
                second = DockNode.Leaf(id = "inner_right", panelId = propsId),
                ratio = 0.75f
            ),
            ratio = 0.2f
        )

        DockStateManagerImpl(initialLayout = DockLayout(rootNode = rootNode, panelDescriptors = descriptors))
    }

    val innerState by innerStateManager.state.collectAsState()

    val varsId = "inner_${panelId}_vars"
    val canvasId = "inner_${panelId}_canvas"
    val propsId = "inner_${panelId}_props"

    val innerRegistry = remember(panelId) {
        DockPanelRegistry().apply {
            register(varsId) {
                AzoraNodeVarsPanel(
                    graph = state.graph,
                    selectedVariableId = state.selectedVariableId,
                    onAction = viewModel::onAction
                )
            }
            register(canvasId) {
                AzoraNodesCanvas(
                    state = state,
                    onCanvasAction = { viewModel.onAction(AzoraNodesAction.CanvasAction(it)) },
                    onAction = viewModel::onAction
                )
            }
            register(propsId) {
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

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        // Toolbar with file name
        AzoraNodesFileToolbar(
            fileName = fileState.fileName,
            isDirty = fileState.isDirty,
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

        // Main content: 3-panel dockable layout
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            DockTheme(registry = innerRegistry) {
                DockContainer(
                    layout = innerState.layout,
                    dragState = innerState.dragState,
                    maximizedPanelId = innerState.maximizedPanelId,
                    onAction = { action -> innerStateManager.dispatch(action) },
                    modifier = Modifier.fillMaxSize(),
                    renderFloatingWindows = false
                )
            }
        }
    }
}

@Composable
private fun AzoraNodesFileToolbar(
    fileName: String,
    isDirty: Boolean,
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
            text = if (isDirty) "$fileName*" else fileName,
            color = palette.contentTop,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.weight(1f))

        // Run / Stop
        if (state.isRunning) {
            FileToolbarButton(
                text = "Stop",
                textColor = palette.error,
                onClick = { onAction(AzoraNodesAction.Stop) }
            )
        } else {
            FileToolbarButton(
                text = "Run",
                textColor = palette.success,
                onClick = onRun
            )
        }

        // Delete selected
        FileToolbarButton(
            text = "Delete",
            enabled = state.canvasState.selectedNodeId != null
                    || state.canvasState.selectedLinkId != null,
            onClick = { onAction(AzoraNodesAction.DeleteSelected) }
        )

        // Save
        FileToolbarButton(
            text = "Save",
            onClick = { onAction(AzoraNodesAction.Save) }
        )
    }
}

@Composable
private fun FileToolbarButton(
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
