package org.azora.studio.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Context menu for the Assets panel.
 */
@Composable
fun AssetsContextMenu(
    position: Offset,
    targetPath: String?,
    isTargetFolder: Boolean,
    onCreateFolder: (name: String) -> Unit,
    onCreateGraph: (name: String) -> Unit,
    onCreateScript: (name: String, runnable: Boolean) -> Unit,
    onCreateFile: (name: String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewGraphDialog by remember { mutableStateOf(false) }
    var showNewScriptDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }

    if (showNewFolderDialog) {
        NewItemDialog(
            title = "New Folder",
            placeholder = "Folder name",
            onConfirm = { name ->
                onCreateFolder(name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
        return
    }

    if (showNewGraphDialog) {
        NewItemDialog(
            title = "New Graph",
            placeholder = "Graph name",
            onConfirm = { name ->
                onCreateGraph(name)
                showNewGraphDialog = false
            },
            onDismiss = { showNewGraphDialog = false }
        )
        return
    }

    if (showNewScriptDialog) {
        NewScriptDialog(
            onConfirm = { name, runnable ->
                onCreateScript(name, runnable)
                showNewScriptDialog = false
            },
            onDismiss = { showNewScriptDialog = false }
        )
        return
    }

    if (showNewFileDialog) {
        NewItemDialog(
            title = "New File",
            placeholder = "File name (e.g. notes.txt)",
            onConfirm = { name ->
                onCreateFile(name)
                showNewFileDialog = false
            },
            onDismiss = { showNewFileDialog = false }
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
            ContextMenuItem(
                text = "New Folder",
                onClick = { showNewFolderDialog = true }
            )
            ContextMenuItem(
                text = "New Graph",
                onClick = { showNewGraphDialog = true }
            )
            ContextMenuItem(
                text = "New Script",
                onClick = { showNewScriptDialog = true }
            )
            ContextMenuItem(
                text = "New File",
                onClick = { showNewFileDialog = true }
            )

            // Show rename/delete only if targeting an item
            if (targetPath != null) {
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(vertical = 4.dp)
                    .background(AzoraPalette.Neutral60)
                )
                ContextMenuItem(
                    text = "Rename",
                    onClick = {
                        onRename()
                        onDismiss()
                    }
                )
                ContextMenuItem(
                    text = "Delete",
                    textColor = AzoraPalette.AccentRed,
                    onClick = {
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    text: String,
    textColor: androidx.compose.ui.graphics.Color = AzoraPalette.Neutral20,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun NewItemDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    DialogShell(onDismiss = onDismiss) {
        Text(
            text = title,
            color = AzoraPalette.Neutral10,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        DialogTextField(value = name, placeholder = placeholder, onValueChange = { name = it })

        DialogButtons(
            confirmEnabled = name.isNotBlank(),
            onCancel = onDismiss,
            onConfirm = { if (name.isNotBlank()) onConfirm(name) }
        )
    }
}

@Composable
private fun NewScriptDialog(
    onConfirm: (name: String, runnable: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var runnable by remember { mutableStateOf(false) }

    DialogShell(onDismiss = onDismiss) {
        Text(
            text = "New Script",
            color = AzoraPalette.Neutral10,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        DialogTextField(value = name, placeholder = "Script name", onValueChange = { name = it })

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (runnable) AzoraPalette.AccentBlue else AzoraPalette.Neutral70)
                    .border(1.dp, if (runnable) AzoraPalette.AccentBlue else AzoraPalette.Neutral50, RoundedCornerShape(4.dp))
                    .clickable { runnable = !runnable },
                contentAlignment = Alignment.Center
            ) {
                if (runnable) {
                    Text(
                        text = "\u2713",
                        color = AzoraPalette.Neutral90,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Runnable",
                color = AzoraPalette.Neutral20,
                fontSize = 12.sp
            )
        }

        DialogButtons(
            confirmEnabled = name.isNotBlank(),
            onCancel = onDismiss,
            onConfirm = { if (name.isNotBlank()) onConfirm(name, runnable) }
        )
    }
}

@Composable
private fun DialogShell(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun DialogTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = AzoraPalette.Neutral10,
            fontSize = 13.sp
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(AzoraPalette.Neutral70)
            .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(6.dp))
            .padding(10.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = AzoraPalette.Neutral50,
                        fontSize = 13.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun DialogButtons(
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onCancel() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Cancel",
                color = AzoraPalette.Neutral40,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AzoraPalette.AccentBlue)
                .clickable(enabled = confirmEnabled) { onConfirm() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Create",
                color = AzoraPalette.Neutral90,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
