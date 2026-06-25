package dev.azora.studio.assets

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Assets panel displaying a tree view of files in the Assets folder.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AssetsPanel(
    viewModel: AssetsPanelViewModel
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzoraPalette.Neutral80)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                            val isRightClick = event.button == PointerButton.Secondary
                            if (isRightClick) {
                                viewModel.showContextMenu(position, null)
                            }
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.selectItem(null)
                viewModel.dismissContextMenu()
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzoraPalette.Neutral75)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Project",
                    color = AzoraPalette.Neutral20,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Refresh button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.refresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u21BB", // Refresh symbol
                        color = AzoraPalette.Neutral40,
                        fontSize = 14.sp
                    )
                }
            }

            // Error message
            state.error?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AzoraPalette.AccentRed.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = error,
                        color = AzoraPalette.AccentRed,
                        fontSize = 11.sp
                    )
                }
            }

            // Tree view
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        color = AzoraPalette.Neutral50,
                        fontSize = 12.sp
                    )
                }
            } else if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No files",
                            color = AzoraPalette.Neutral50,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Right-click to create",
                            color = AzoraPalette.Neutral60,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    AssetTreeItems(
                        items = state.items,
                        selectedPath = state.selectedPath,
                        expandedFolders = state.expandedFolders,
                        renamingPath = state.renamingPath,
                        indent = 0,
                        onToggleFolder = viewModel::toggleFolder,
                        onSelectItem = viewModel::selectItem,
                        onDoubleClick = { item ->
                            if (item is AssetItem.File) {
                                when (item.extension) {
                                    "azn" -> viewModel.openAzoraNodesFile(item.path)
                                    "az" -> viewModel.openAzScriptFile(item.path)
                                    "azorascene" -> viewModel.openAzoraSceneFile(item.path)
                                    "azoratilemap" -> viewModel.openAzoraTileMapFile(item.path)
                                    "azscene" -> viewModel.openAzsceneFile(item.path)
                                }
                            }
                        },
                        onContextMenu = { offset, path ->
                            viewModel.showContextMenu(offset, path)
                        },
                        onRenameConfirm = { oldPath, newName ->
                            viewModel.renameItem(oldPath, newName)
                        },
                        onRenameCancel = viewModel::cancelRenaming
                    )
                }
            }
        }

        // Context menu
        state.contextMenuPosition?.let { position ->
            val targetItem = state.contextMenuTargetPath?.let { path ->
                findItemByPath(state.items, path)
            }
            fun resolveParent(): String? = when (targetItem) {
                is AssetItem.Folder -> targetItem.path
                is AssetItem.File -> targetItem.path.substringBeforeLast("/")
                null -> null
            }
            AssetsContextMenu(
                position = position,
                targetPath = state.contextMenuTargetPath,
                isTargetFolder = targetItem is AssetItem.Folder,
                onCreateFolder = { name -> viewModel.createFolder(resolveParent(), name) },
                onCreateGraph = { name -> viewModel.createAzoraNodesFile(resolveParent(), name) },
                onCreateScript = { name, runnable ->
                    viewModel.createAzScriptFile(resolveParent(), name, runnable)
                },
                onCreateFile = { name -> viewModel.createFile(resolveParent(), name) },
                onRename = {
                    state.contextMenuTargetPath?.let { viewModel.startRenaming(it) }
                },
                onDelete = {
                    state.contextMenuTargetPath?.let { viewModel.deleteItem(it) }
                },
                onDismiss = viewModel::dismissContextMenu
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AssetTreeItems(
    items: List<AssetItem>,
    selectedPath: String?,
    expandedFolders: Set<String>,
    renamingPath: String?,
    indent: Int,
    onToggleFolder: (String) -> Unit,
    onSelectItem: (String) -> Unit,
    onDoubleClick: (AssetItem) -> Unit,
    onContextMenu: (Offset, String) -> Unit,
    onRenameConfirm: (String, String) -> Unit,
    onRenameCancel: () -> Unit
) {
    items.forEach { item ->
        AssetTreeItem(
            item = item,
            isSelected = item.path == selectedPath,
            isExpanded = expandedFolders.contains(item.path),
            isRenaming = item.path == renamingPath,
            indent = indent,
            onToggleFolder = onToggleFolder,
            onSelect = { onSelectItem(item.path) },
            onDoubleClick = { onDoubleClick(item) },
            onContextMenu = { offset -> onContextMenu(offset, item.path) },
            onRenameConfirm = { newName -> onRenameConfirm(item.path, newName) },
            onRenameCancel = onRenameCancel
        )

        // Render children if expanded folder
        if (item is AssetItem.Folder && expandedFolders.contains(item.path)) {
            AssetTreeItems(
                items = item.children,
                selectedPath = selectedPath,
                expandedFolders = expandedFolders,
                renamingPath = renamingPath,
                indent = indent + 1,
                onToggleFolder = onToggleFolder,
                onSelectItem = onSelectItem,
                onDoubleClick = onDoubleClick,
                onContextMenu = onContextMenu,
                onRenameConfirm = onRenameConfirm,
                onRenameCancel = onRenameCancel
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AssetTreeItem(
    item: AssetItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    isRenaming: Boolean,
    indent: Int,
    onToggleFolder: (String) -> Unit,
    onSelect: () -> Unit,
    onDoubleClick: () -> Unit,
    onContextMenu: (Offset) -> Unit,
    onRenameConfirm: (String) -> Unit,
    onRenameCancel: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isSelected -> AzoraPalette.AccentBlue.copy(alpha = 0.2f)
        isHovered -> AzoraPalette.Neutral70
        else -> Color.Transparent
    }

    var lastClickTime by remember { mutableStateOf(0L) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 16).dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .pointerInput(item.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                            val isRightClick = event.button == PointerButton.Secondary
                            if (isRightClick) {
                                onSelect()
                                onContextMenu(position)
                            }
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    // Double click
                    onDoubleClick()
                } else {
                    // Single click
                    onSelect()
                    if (item is AssetItem.Folder) {
                        onToggleFolder(item.path)
                    }
                }
                lastClickTime = currentTime
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Icon
        when (item) {
            is AssetItem.Folder -> {
                Text(
                    text = if (isExpanded) "\uD83D\uDCC2" else "\uD83D\uDCC1", // Open/closed folder emoji
                    fontSize = 12.sp
                )
            }
            is AssetItem.File -> {
                val icon = when (item.extension) {
                    "azn" -> "\uD83D\uDCCA" // Chart emoji for nodes
                    "az" -> "\uD83D\uDCDD" // Memo emoji for scripts
                    "azorascene" -> "\uD83C\uDFAE" // Game controller emoji for scenes
                    "azoratilemap" -> "\uD83D\uDDFA" // World map emoji for tilemaps
                    else -> "\uD83D\uDCC4" // Document emoji
                }
                Text(
                    text = icon,
                    fontSize = 12.sp
                )
            }
        }

        // Name (or rename field)
        if (isRenaming) {
            RenameField(
                initialName = item.name,
                onConfirm = onRenameConfirm,
                onCancel = onRenameCancel
            )
        } else {
            Text(
                text = item.name,
                color = if (isSelected) AzoraPalette.Neutral10 else AzoraPalette.Neutral20,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RowScope.RenameField(
    initialName: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = name,
        onValueChange = { name = it },
        textStyle = TextStyle(
            color = AzoraPalette.Neutral10,
            fontSize = 12.sp
        ),
        cursorBrush = SolidColor(AzoraPalette.AccentBlue),
        maxLines = 1,
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(4.dp))
            .background(AzoraPalette.Neutral70)
            .border(1.dp, AzoraPalette.AccentBlue, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Enter -> {
                            if (name.isNotBlank()) onConfirm(name)
                            true
                        }
                        Key.Escape -> {
                            onCancel()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    )
}

private fun findItemByPath(items: List<AssetItem>, path: String): AssetItem? {
    for (item in items) {
        if (item.path == path) return item
        if (item is AssetItem.Folder) {
            findItemByPath(item.children, path)?.let { return it }
        }
    }
    return null
}
