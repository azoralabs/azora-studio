package dev.azora.studio.content_browser

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette
import dev.azora.studio.assets.AssetItem
import azora.azora_studio.app.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Unreal-style Content Browser: a single-folder view with back / forward / up
 * navigation, a clickable breadcrumb path, grid (default) and list views,
 * create / delete / rename, and double-click-to-open text files in an editor tab.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContentBrowserPanel(
    viewModel: ContentBrowserViewModel
) {
    val state by viewModel.state.collectAsState()
    val breadcrumbs = remember(state.currentPath) { viewModel.breadcrumbs() }

    // Rename dialog target (kept local so the VM stays free of dialog state).
    var renameTarget by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AzoraPalette.Neutral80)
    ) {
        Toolbar(
            state = state,
            breadcrumbs = breadcrumbs,
            onBack = viewModel::navigateBack,
            onForward = viewModel::navigateForward,
            onUp = viewModel::navigateUp,
            onBreadcrumbClick = viewModel::navigateTo,
            onToggleView = viewModel::setViewMode,
            onRefresh = viewModel::refresh
        )

        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzoraPalette.AccentRed.copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Text(text = error, color = AzoraPalette.AccentRed, fontSize = 11.sp)
            }
        }

        when {
            state.isLoading -> CenteredHint("Loading...")
            state.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                Text("This folder is empty", color = AzoraPalette.Neutral50, fontSize = 12.sp)
                Text("Right-click to create a folder or file", color = AzoraPalette.Neutral60, fontSize = 11.sp)
            }
            state.viewMode == ViewMode.GRID -> ContentViewGrid(
                items = state.items,
                selectedPath = state.selectedPath,
                onActivate = { handleActivate(it, viewModel) },
                onSelect = viewModel::selectItem,
                onContextMenu = { offset, path -> viewModel.showContextMenu(offset, path) },
                onBackgroundClick = { viewModel.selectItem(null); viewModel.dismissContextMenu() },
                onBackgroundRightClick = { offset -> viewModel.showContextMenu(offset, null) }
            )
            else -> ContentViewList(
                items = state.items,
                selectedPath = state.selectedPath,
                onActivate = { handleActivate(it, viewModel) },
                onSelect = viewModel::selectItem,
                onContextMenu = { offset, path -> viewModel.showContextMenu(offset, path) },
                onBackgroundClick = { viewModel.selectItem(null); viewModel.dismissContextMenu() },
                onBackgroundRightClick = { offset -> viewModel.showContextMenu(offset, null) }
            )
        }
    }

    // Context menu overlay
    state.contextMenuPosition?.let { position ->
        ContentBrowserContextMenu(
            position = position,
            targetPath = state.contextMenuTargetPath,
            onCreateFolder = { name -> viewModel.createFolder(name) },
            onCreateFile = { name -> viewModel.createFile(name) },
            onRenameRequested = { state.contextMenuTargetPath?.let { viewModel.dismissContextMenu(); renameTarget = it } },
            onDelete = { state.contextMenuTargetPath?.let { viewModel.deleteItem(it) } },
            onDismiss = viewModel::dismissContextMenu
        )
    }

    // Rename dialog (driven locally; keeps the VM free of dialog state)
    if (renameTarget != null) {
        val target = renameTarget!!
        val currentName = state.items.find { it.path == target }?.name ?: ""
        NamePromptDialog(
            title = "Rename",
            placeholder = currentName,
            initial = currentName,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != currentName) viewModel.renameItem(target, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

private fun handleActivate(item: AssetItem, viewModel: ContentBrowserViewModel) {
    when (item) {
        is AssetItem.Folder -> viewModel.navigateInto(item.path)
        is AssetItem.File -> if (isProbablyTextFile(item.name)) viewModel.openTextFile(item.path)
    }
}

@Composable
private fun Toolbar(
    state: ContentBrowserState,
    breadcrumbs: List<Breadcrumb>,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onToggleView: (ViewMode) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzoraPalette.Neutral75)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarButton(Res.drawable.ic_arrow_back, enabled = state.backStack.isNotEmpty(), onClick = onBack)
            ToolbarButton(Res.drawable.ic_arrow_forward, enabled = state.forwardStack.isNotEmpty(), onClick = onForward)
            ToolbarButton(Res.drawable.ic_arrow_up, enabled = breadcrumbs.size > 1, onClick = onUp)

            // Breadcrumb path (scrollable)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AzoraPalette.Neutral80)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                breadcrumbs.forEachIndexed { index, crumb ->
                    if (index > 0) {
                        Text("/", color = AzoraPalette.Neutral50, fontSize = 11.sp)
                    }
                    val isLast = index == breadcrumbs.lastIndex
                    Text(
                        text = crumb.name,
                        color = if (isLast) AzoraPalette.Neutral20 else AzoraPalette.Neutral40,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBreadcrumbClick(crumb.path) }
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }

            // View toggle
            ViewToggle(state.viewMode, onToggleView)

            ToolbarButton(Res.drawable.ic_refresh, enabled = true, onClick = onRefresh)
        }
    }
}

@Composable
private fun ToolbarButton(resource: DrawableResource, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (!enabled) Color.Transparent else AzoraPalette.Neutral70.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        VectorIcon(
            resource = resource,
            tint = if (enabled) AzoraPalette.Neutral30 else AzoraPalette.Neutral60,
            size = 16.dp
        )
    }
}

@Composable
private fun ViewToggle(mode: ViewMode, onToggle: (ViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AzoraPalette.Neutral80)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ViewToggleBtn(Res.drawable.ic_grid, selected = mode == ViewMode.GRID) { onToggle(ViewMode.GRID) }
        ViewToggleBtn(Res.drawable.ic_list, selected = mode == ViewMode.LIST) { onToggle(ViewMode.LIST) }
    }
}

@Composable
private fun ViewToggleBtn(resource: DrawableResource, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) AzoraPalette.AccentBlue.copy(alpha = 0.8f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        VectorIcon(
            resource = resource,
            tint = if (selected) AzoraPalette.Neutral90 else AzoraPalette.Neutral40,
            size = 14.dp
        )
    }
}

// ---------- Grid view ----------

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ContentViewGrid(
    items: List<AssetItem>,
    selectedPath: String?,
    onActivate: (AssetItem) -> Unit,
    onSelect: (String) -> Unit,
    onContextMenu: (Offset, String) -> Unit,
    onBackgroundClick: () -> Unit,
    onBackgroundRightClick: (Offset) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(AzoraPalette.Neutral80)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                            onBackgroundRightClick(event.changes.firstOrNull()?.position ?: Offset.Zero)
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onBackgroundClick() },
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items, key = { it.path }) { item ->
            GridTile(
                item = item,
                isSelected = item.path == selectedPath,
                onActivate = { onActivate(item) },
                onSelect = { onSelect(item.path) },
                onContextMenu = { offset -> onContextMenu(offset, item.path) }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GridTile(
    item: AssetItem,
    isSelected: Boolean,
    onActivate: () -> Unit,
    onSelect: () -> Unit,
    onContextMenu: (Offset) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var lastClickTime by remember { mutableStateOf(0L) }

    val background = when {
        isSelected -> AzoraPalette.AccentBlue.copy(alpha = 0.22f)
        isHovered -> AzoraPalette.Neutral70.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .hoverable(interactionSource)
            .pointerInput(item.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                            onSelect()
                            onContextMenu(event.changes.firstOrNull()?.position ?: Offset.Zero)
                        }
                    }
                }
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                val now = System.currentTimeMillis()
                if (now - lastClickTime < 300) onActivate() else onSelect()
                lastClickTime = now
            }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        VectorIcon(resource = iconFor(item), tint = itemIconTint(item), size = 34.dp)
        Text(
            text = item.name,
            color = if (isSelected) AzoraPalette.Neutral10 else AzoraPalette.Neutral20,
            fontSize = 11.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- List view ----------

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ContentViewList(
    items: List<AssetItem>,
    selectedPath: String?,
    onActivate: (AssetItem) -> Unit,
    onSelect: (String) -> Unit,
    onContextMenu: (Offset, String) -> Unit,
    onBackgroundClick: () -> Unit,
    onBackgroundRightClick: (Offset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AzoraPalette.Neutral80)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                            onBackgroundRightClick(event.changes.firstOrNull()?.position ?: Offset.Zero)
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onBackgroundClick() }
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items.forEach { item ->
            ListRow(
                item = item,
                isSelected = item.path == selectedPath,
                onActivate = { onActivate(item) },
                onSelect = { onSelect(item.path) },
                onContextMenu = { offset -> onContextMenu(offset, item.path) }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ListRow(
    item: AssetItem,
    isSelected: Boolean,
    onActivate: () -> Unit,
    onSelect: () -> Unit,
    onContextMenu: (Offset) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var lastClickTime by remember { mutableStateOf(0L) }

    val background = when {
        isSelected -> AzoraPalette.AccentBlue.copy(alpha = 0.22f)
        isHovered -> AzoraPalette.Neutral70.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .hoverable(interactionSource)
            .pointerInput(item.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                            onSelect()
                            onContextMenu(event.changes.firstOrNull()?.position ?: Offset.Zero)
                        }
                    }
                }
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                val now = System.currentTimeMillis()
                if (now - lastClickTime < 300) onActivate() else onSelect()
                lastClickTime = now
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VectorIcon(resource = iconFor(item), tint = itemIconTint(item), size = 16.dp)
        Text(
            text = item.name,
            color = if (isSelected) AzoraPalette.Neutral10 else AzoraPalette.Neutral20,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun iconFor(item: AssetItem): DrawableResource = when (item) {
    is AssetItem.Folder -> Res.drawable.ic_folder
    is AssetItem.File -> Res.drawable.ic_file
}

@Composable
private fun VectorIcon(resource: DrawableResource, tint: Color, size: Dp) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(size)
    )
}

private fun itemIconTint(item: AssetItem): Color =
    if (item is AssetItem.Folder) AzoraPalette.Neutral40 else AzoraPalette.Neutral60

@Composable
private fun CenteredHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = AzoraPalette.Neutral50, fontSize = 12.sp)
    }
}

// ---------- Context menu + dialogs ----------

@Composable
private fun ContentBrowserContextMenu(
    position: Offset,
    targetPath: String?,
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onRenameRequested: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showFolderDialog by remember { mutableStateOf(false) }
    var showFileDialog by remember { mutableStateOf(false) }

    if (showFolderDialog) {
        NamePromptDialog(
            title = "New Folder",
            placeholder = "Folder name",
            confirmLabel = "Create",
            onConfirm = { name -> onCreateFolder(name); showFolderDialog = false },
            onDismiss = { showFolderDialog = false }
        )
        return
    }
    if (showFileDialog) {
        NamePromptDialog(
            title = "New File",
            placeholder = "File name (e.g. notes.txt)",
            confirmLabel = "Create",
            onConfirm = { name -> onCreateFile(name); showFileDialog = false },
            onDismiss = { showFileDialog = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
            .width(180.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(AzoraPalette.Neutral85)
            .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            MenuRow("New Folder") { showFolderDialog = true }
            MenuRow("New File") { showFileDialog = true }
            if (targetPath != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AzoraPalette.Neutral60)
                )
                MenuRow("Rename") { onRenameRequested(); onDismiss() }
                MenuRow("Delete", color = AzoraPalette.AccentRed) { onDelete() }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, color: Color = AzoraPalette.Neutral20, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun NamePromptDialog(
    title: String,
    placeholder: String,
    confirmLabel: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzoraPalette.Neutral90.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(16.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(AzoraPalette.Neutral85)
                .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = AzoraPalette.Neutral10, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(color = AzoraPalette.Neutral10, fontSize = 13.sp),
                cursorBrush = SolidColor(AzoraPalette.AccentBlue),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AzoraPalette.Neutral70)
                    .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(6.dp))
                    .padding(10.dp),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) {
                            Text(placeholder, color = AzoraPalette.Neutral50, fontSize = 13.sp)
                        }
                        inner()
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("Cancel", color = AzoraPalette.Neutral40, fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AzoraPalette.AccentBlue)
                        .clickable(enabled = name.isNotBlank()) { if (name.isNotBlank()) onConfirm(name) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(confirmLabel, color = AzoraPalette.Neutral90, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
