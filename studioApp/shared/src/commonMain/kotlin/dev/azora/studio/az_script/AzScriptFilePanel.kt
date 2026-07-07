package dev.azora.studio.az_script

import androidx.compose.foundation.background
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import dev.azora.nodes.domain.AzToNodesConverter
import dev.azora.nodes.domain.AzToNodesResult
import dev.azora.nodes.domain.AznFiles
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.FileSystemResult
import dev.azora.sdk.core.theme.palette.AzoraPalette
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.studio.assets.OpenAzScriptFilesManager
import dev.azora.studio.assets.OpenAzoraNodesFilesManager
import dev.azora.studio.editor.EDITOR_AREA_NODE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Code editor for a single `.az` (azora-lang) file with full IntelliSense
 * backed by the Azora Language Server (azls.jar, see [AzoraLanguageIntel]):
 *
 * - live syntax coloring ([AzSyntaxTransformation]),
 * - compiler diagnostics on a debounce — error/warning line tints, a bottom
 *   problems strip, and the shared Problems panel via [DiagnosticsManager],
 * - code completion (auto while typing identifiers or after `.`, or
 *   Ctrl/Cmd+Space) with keyboard navigation,
 * - save with the button or Ctrl/Cmd+S.
 *
 * Without the language-server jar the editor degrades to plain text.
 */
@Composable
fun AzScriptFilePanel(panelId: String, projectPath: String) {
    val manager: OpenAzScriptFilesManager = koinInject()
    val intel: AzoraLanguageIntel = koinInject()
    val diagnosticsManager: DiagnosticsManager = koinInject()
    val fileSystem: FileSystem = koinInject()
    val nodesFilesManager: OpenAzoraNodesFilesManager = koinInject()
    val dockStateManager: DockStateManager = koinInject()
    val console: ConsoleOutputManager = koinInject()
    val openFiles by manager.openFiles.collectAsState()
    val scope = rememberCoroutineScope()
    val state = openFiles[panelId]

    if (state == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(AzoraPalette.Neutral90).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "This file is no longer open.\nReopen it from the Content Browser.",
                color = AzoraPalette.Neutral50,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // ---- editor state -------------------------------------------------
    var fieldValue by remember(panelId) { mutableStateOf(TextFieldValue(state.sourceCode)) }
    // External changes (restore, disk reload) reset the field.
    if (fieldValue.text != state.sourceCode) {
        fieldValue = TextFieldValue(
            text = state.sourceCode,
            selection = fieldValue.selection.coerceIn(state.sourceCode.length)
        )
    }

    var spans by remember(panelId) { mutableStateOf(emptyList<AzHighlightSpan>()) }
    var diagnostics by remember(panelId) { mutableStateOf(emptyList<Diagnostic>()) }
    var completions by remember(panelId) { mutableStateOf(emptyList<AzCompletion>()) }
    var completionsVisible by remember(panelId) { mutableStateOf(false) }
    var selectedCompletion by remember(panelId) { mutableStateOf(0) }
    var completionJob by remember(panelId) { mutableStateOf<Job?>(null) }

    // ---- language server: colors + diagnostics ------------------------
    LaunchedEffect(panelId, fieldValue.text) {
        spans = intel.highlight(fieldValue.text)
    }
    LaunchedEffect(panelId, fieldValue.text) {
        delay(500)
        val result = intel.diagnostics(fieldValue.text, state.filePath, projectPath)
        diagnostics = result
        diagnosticsManager.report(panelId, state.fileName, result)
    }
    DisposableEffect(panelId) {
        onDispose { diagnosticsManager.clear(panelId) }
    }

    fun closeCompletions() {
        completionsVisible = false
        completionJob?.cancel()
    }

    fun requestCompletions(debounceMs: Long) {
        completionJob?.cancel()
        completionJob = scope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val items = intel.complete(
                fieldValue.text, fieldValue.selection.start, state.filePath, projectPath
            )
            completions = items
            selectedCompletion = 0
            completionsVisible = items.isNotEmpty()
        }
    }

    fun acceptCompletion(item: AzCompletion) {
        val text = fieldValue.text
        val cursor = fieldValue.selection.start
        var wordStart = cursor
        while (wordStart > 0 && (text[wordStart - 1].isLetterOrDigit() || text[wordStart - 1] == '_')) wordStart--
        val insert = item.insert.ifEmpty { item.label }
        val newText = text.substring(0, wordStart) + insert + text.substring(cursor)
        fieldValue = TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(wordStart + insert.length)
        )
        manager.updateSource(panelId, newText)
        closeCompletions()
    }

    // ---- layout --------------------------------------------------------
    val errorLines = diagnostics.filter { it.severity == "error" }.map { it.line }.toSet()
    val warningLines = diagnostics.filter { it.severity == "warning" }.map { it.line }.toSet()
    val transformation = remember(spans, errorLines, warningLines) {
        AzSyntaxTransformation(spans, errorLines, warningLines)
    }

    // Caret position in dp for the popup (monospace metrics from one glyph).
    val measurer = rememberTextMeasurer()
    val editorTextStyle = TextStyle(
        color = AzoraPalette.Neutral10,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val glyph = remember(measurer) { measurer.measure("M", editorTextStyle) }

    // Converts the current buffer into a sibling .azn node graph and opens it.
    fun convertToNodes() {
        scope.launch {
            val aznPath = AznFiles.siblingAznPath(state.filePath)
            val aznName = aznPath.substringAfterLast('/')
            if (fileSystem.fileExists(aznPath) is dev.azora.sdk.core.io.ExistsResult.Exists) {
                console.error("$aznName already exists — delete or rename it first.")
                return@launch
            }
            when (val converted = AzToNodesConverter().convert(fieldValue.text, state.fileName)) {
                is AzToNodesResult.Failure -> {
                    console.error("Cannot convert ${state.fileName}.az: ${converted.errors.firstOrNull()}")
                }
                is AzToNodesResult.Success -> {
                    converted.warnings.forEach { console.print("[$aznName] $it") }
                    when (fileSystem.writeToFile(aznPath, AznFiles.encode(converted.graph))) {
                        is FileSystemResult.Success -> {
                            console.info("Converted ${state.fileName}.az → $aznName")
                            val nodesPanelId = nodesFilesManager.openFile(aznPath) ?: return@launch
                            val nodesState = nodesFilesManager.getState(nodesPanelId) ?: return@launch
                            dockStateManager.dispatch(
                                DockAction.AddPanel(
                                    DockPanelDescriptor(id = nodesPanelId, title = nodesState.fileName, closeable = true),
                                    EDITOR_AREA_NODE_ID,
                                    DockZone.CENTER
                                )
                            )
                            dockStateManager.dispatch(DockAction.SelectPanel(nodesPanelId))
                        }
                        is FileSystemResult.Error -> console.error("Failed to write $aznName")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AzoraPalette.Neutral90)) {
        AzScriptHeader(
            fileName = state.fileName,
            isDirty = state.isDirty,
            intelAvailable = intel.available,
            errorCount = errorLines.size,
            warningCount = warningLines.size,
            onConvertToNodes = { convertToNodes() },
            onSave = { scope.launch { manager.saveFile(panelId) } }
        )

        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(AzoraPalette.Neutral90)
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    val textChanged = newValue.text != fieldValue.text
                    val grewByTyping = newValue.text.length == fieldValue.text.length + 1
                    fieldValue = newValue
                    if (textChanged) {
                        manager.updateSource(panelId, newValue.text)
                        val cursor = newValue.selection.start
                        val typed = if (grewByTyping && cursor > 0) newValue.text[cursor - 1] else null
                        when {
                            typed != null && (typed.isLetterOrDigit() || typed == '_' || typed == '.') ->
                                requestCompletions(debounceMs = 120)
                            else -> closeCompletions()
                        }
                    }
                },
                textStyle = editorTextStyle,
                visualTransformation = transformation,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        handleEditorKeys(
                            event = event,
                            completionsVisible = completionsVisible,
                            completions = completions,
                            selectedIndex = selectedCompletion,
                            onSelect = { selectedCompletion = it },
                            onAccept = { acceptCompletion(it) },
                            onClose = { closeCompletions() },
                            onForceComplete = { requestCompletions(debounceMs = 0) },
                            onSave = { scope.launch { manager.saveFile(panelId) } }
                        )
                    }
            )

            if (completionsVisible && completions.isNotEmpty()) {
                val cursor = fieldValue.selection.start.coerceIn(0, fieldValue.text.length)
                val before = fieldValue.text.take(cursor)
                val line = before.count { it == '\n' }
                val column = cursor - (before.lastIndexOf('\n') + 1)
                Box(
                    modifier = Modifier.offset {
                        IntOffset(
                            x = column * glyph.size.width,
                            y = (line + 1) * (glyph.size.height + 5)
                        )
                    }
                ) {
                    AzCompletionPopup(
                        items = completions,
                        selectedIndex = selectedCompletion,
                        onPick = { acceptCompletion(it) }
                    )
                }
            }
        }

        if (diagnostics.isNotEmpty()) {
            AzProblemsStrip(diagnostics)
        }
    }
}

/** Keyboard handling: completion navigation + save shortcut. Returns true when consumed. */
private fun handleEditorKeys(
    event: KeyEvent,
    completionsVisible: Boolean,
    completions: List<AzCompletion>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAccept: (AzCompletion) -> Unit,
    onClose: () -> Unit,
    onForceComplete: () -> Unit,
    onSave: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val meta = event.isCtrlPressed || event.isMetaPressed

    if (meta && event.key == Key.S) {
        onSave()
        return true
    }
    if (meta && event.key == Key.Spacebar) {
        onForceComplete()
        return true
    }
    if (!completionsVisible) return false

    return when (event.key) {
        Key.DirectionDown -> {
            onSelect((selectedIndex + 1).coerceAtMost(completions.lastIndex)); true
        }
        Key.DirectionUp -> {
            onSelect((selectedIndex - 1).coerceAtLeast(0)); true
        }
        Key.Enter, Key.Tab -> {
            completions.getOrNull(selectedIndex)?.let(onAccept); true
        }
        Key.Escape -> {
            onClose(); true
        }
        else -> false
    }
}

@Composable
private fun AzScriptHeader(
    fileName: String,
    isDirty: Boolean,
    intelAvailable: Boolean,
    errorCount: Int,
    warningCount: Int,
    onConvertToNodes: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzoraPalette.Neutral80)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$fileName.az",
            color = AzoraPalette.Neutral20,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (!intelAvailable) {
            Text("azls not installed", color = AzoraPalette.Neutral50, fontSize = 10.sp)
        } else {
            if (errorCount > 0) Text("$errorCount ✕", color = AzoraPalette.AccentRed, fontSize = 10.sp)
            if (warningCount > 0) Text("$warningCount ⚠", color = AzoraPalette.AccentYellow, fontSize = 10.sp)
            if (errorCount == 0 && warningCount == 0) Text("✓", color = AzoraPalette.AccentGreen, fontSize = 10.sp)
        }
        if (isDirty) {
            Text("●", color = AzoraPalette.AccentYellow, fontSize = 10.sp)
            Text("Unsaved", color = AzoraPalette.Neutral40, fontSize = 10.sp)
        } else {
            Text("Saved", color = AzoraPalette.Neutral60, fontSize = 10.sp)
        }
        // Convert this source file to a visual node graph (.azn)
        Text(
            "To Nodes",
            color = AzoraPalette.AccentBlue,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onConvertToNodes() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        SaveButton(enabled = isDirty, onClick = onSave)
    }
}

@Composable
private fun AzProblemsStrip(diagnostics: List<Diagnostic>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzoraPalette.Neutral80)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        for (diagnostic in diagnostics.take(4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (diagnostic.severity == "warning") "⚠" else "✕",
                    color = if (diagnostic.severity == "warning") AzoraPalette.AccentYellow else AzoraPalette.AccentRed,
                    fontSize = 10.sp,
                    modifier = Modifier.width(16.dp)
                )
                Text(
                    text = "line ${diagnostic.line}: ${diagnostic.message}",
                    color = AzoraPalette.Neutral30,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
        if (diagnostics.size > 4) {
            Text(
                text = "… and ${diagnostics.size - 4} more (see Problems panel)",
                color = AzoraPalette.Neutral50,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) AzoraPalette.AccentBlue else AzoraPalette.Neutral70)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            "Save",
            color = if (enabled) AzoraPalette.Neutral90 else AzoraPalette.Neutral50,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Clamps a selection to a (possibly shorter) new text length. */
private fun androidx.compose.ui.text.TextRange.coerceIn(maxLength: Int): androidx.compose.ui.text.TextRange =
    androidx.compose.ui.text.TextRange(start.coerceAtMost(maxLength), end.coerceAtMost(maxLength))
