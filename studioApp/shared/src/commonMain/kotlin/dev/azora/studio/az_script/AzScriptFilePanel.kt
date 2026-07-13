package dev.azora.studio.az_script

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.project.domain.SettingsModel
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.core.project.domain.repository.SettingsRepository
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
 * - hover docs, Go To Definition (Ctrl/Cmd+B, F12, or Ctrl/Cmd+click) with
 *   cross-file jumps, and Go To Symbol (Ctrl/Cmd+Shift+O),
 * - auto-saves shortly after you stop typing (Ctrl/Cmd+S forces it),
 * - zoom with Ctrl/Cmd+scroll (trackpad pinch arrives the same way) or
 *   Ctrl/Cmd +/−/0.
 *
 * Without the language-server jar the editor degrades to plain text.
 */
@Composable
fun AzScriptFilePanel(panelId: String, projectPath: String) {
    val manager: OpenAzScriptFilesManager = koinInject()
    val intel: AzoraLanguageIntel = koinInject()
    val debugger: AzoraScriptDebugger = koinInject()
    val diagnosticsManager: DiagnosticsManager = koinInject()
    val fileSystem: FileSystem = koinInject()
    val nodesFilesManager: OpenAzoraNodesFilesManager = koinInject()
    val dockStateManager: DockStateManager = koinInject()
    val console: ConsoleOutputManager = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val projectRepository: AzoraProjectRepository = koinInject()
    val openFiles by manager.openFiles.collectAsState()

    // Live editor preferences (Settings ▸ Editor) — open editors react immediately.
    var prefs by remember { mutableStateOf(SettingsModel.default("")) }
    LaunchedEffect(Unit) {
        val project = projectRepository.getProject()
        if (project is Res.Success) {
            settingsRepository.observeSettings(project.data.id).collect { prefs = it }
        }
    }
    val indentUnit = " ".repeat(prefs.editorTabSize)
    val scope = rememberCoroutineScope()
    val state = openFiles[panelId]

    // Builds before `.az` session persistence stored the dock tab but not its
    // file path. Recover those legacy tabs once by their descriptor title.
    LaunchedEffect(panelId, projectPath) {
        if (manager.getState(panelId) == null && projectPath.isNotEmpty()) {
            val title = dockStateManager.state.value.layout.panelDescriptors[panelId]?.title
            if (!title.isNullOrBlank()) {
                val fileName = if (title.endsWith(".az")) title else "$title.az"
                manager.findScriptFile(projectPath, fileName)?.let { filePath ->
                    manager.restoreFile(panelId, filePath)
                }
            }
        }
    }

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

    // ---- undo / redo ---------------------------------------------------
    val undoStack = remember(panelId) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(panelId) { mutableStateListOf<TextFieldValue>() }
    var lastUndoPush by remember(panelId) { mutableStateOf<kotlin.time.TimeMark?>(null) }

    /** Central edit application: smart-typing pipeline + undo history + persistence. */
    fun applyEdit(proposed: TextFieldValue, viaPipeline: Boolean = true) {
        val processed = if (viaPipeline) {
            AzEditorText.processEdit(
                fieldValue, proposed,
                indent = indentUnit,
                autoCloseBrackets = prefs.editorAutoCloseBrackets,
                smartIndent = prefs.editorSmartIndent,
            )
        } else proposed
        if (processed.text != fieldValue.text) {
            val sinceLastPush = lastUndoPush?.elapsedNow()?.inWholeMilliseconds ?: Long.MAX_VALUE
            if (sinceLastPush > 400 || undoStack.isEmpty()) {
                undoStack.add(fieldValue)
                if (undoStack.size > 200) undoStack.removeAt(0)
                lastUndoPush = kotlin.time.TimeSource.Monotonic.markNow()
            }
            redoStack.clear()
            manager.updateSource(panelId, processed.text)
        }
        fieldValue = processed
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.add(fieldValue)
        fieldValue = previous
        manager.updateSource(panelId, previous.text)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.add(fieldValue)
        fieldValue = next
        manager.updateSource(panelId, next.text)
    }

    // ---- find / replace --------------------------------------------------
    var findVisible by remember(panelId) { mutableStateOf(false) }
    var findQuery by remember(panelId) { mutableStateOf("") }
    var replaceQuery by remember(panelId) { mutableStateOf("") }
    var activeMatch by remember(panelId) { mutableStateOf(0) }
    val searchMatches = remember(findQuery, fieldValue.text) {
        if (findQuery.isBlank()) emptyList()
        else buildList {
            var index = fieldValue.text.indexOf(findQuery, ignoreCase = true)
            while (index >= 0 && size < 2000) {
                add(index to index + findQuery.length)
                index = fieldValue.text.indexOf(findQuery, index + 1, ignoreCase = true)
            }
        }
    }

    fun gotoMatch(index: Int) {
        if (searchMatches.isEmpty()) return
        val wrapped = ((index % searchMatches.size) + searchMatches.size) % searchMatches.size
        activeMatch = wrapped
        val (start, end) = searchMatches[wrapped]
        fieldValue = fieldValue.copy(selection = androidx.compose.ui.text.TextRange(start, end))
    }

    // ---- go to symbol (Cmd/Ctrl+Shift+O) ---------------------------------
    var symbolPickerVisible by remember(panelId) { mutableStateOf(false) }
    var fileSymbols by remember(panelId) { mutableStateOf(emptyList<AzSymbol>()) }

    fun openSymbolPicker() {
        scope.launch {
            fileSymbols = intel.symbols(fieldValue.text)
            symbolPickerVisible = fileSymbols.isNotEmpty()
        }
    }

    var fontSize by remember { mutableStateOf(13) }
    LaunchedEffect(prefs.editorFontSize) { fontSize = prefs.editorFontSize }

    // ---- auto-save: persist shortly after typing pauses ------------------
    LaunchedEffect(panelId, fieldValue.text, state.isDirty) {
        if (!state.isDirty) return@LaunchedEffect
        delay(600)
        manager.saveFile(panelId)
    }

    // ---- hover docs ------------------------------------------------------
    var textLayout by remember(panelId) { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    var hoverInfo by remember(panelId) { mutableStateOf<AzHover?>(null) }
    var hoverPosition by remember(panelId) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var hoverJob by remember(panelId) { mutableStateOf<Job?>(null) }

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

    // ---- debugger ------------------------------------------------------
    var breakpoints by remember(panelId) { mutableStateOf(setOf<Int>()) }
    var debugActive by remember(panelId) { mutableStateOf(false) }
    var debugState by remember(panelId) { mutableStateOf(AzDebugStatus()) }

    fun toggleBreakpoint(line: Int) {
        breakpoints = if (line in breakpoints) breakpoints - line else breakpoints + line
        if (debugActive) scope.launch { debugger.setBreakpoints(breakpoints) }
    }

    fun startDebug() {
        scope.launch {
            console.info("Debug ▸ ${state.fileName}.az")
            val started = debugger.start(fieldValue.text, state.filePath, projectPath, breakpoints)
            if (started.status == "failed") {
                console.error("Debug failed: ${started.error}")
            } else {
                debugState = AzDebugStatus(status = "running")
                debugActive = true
            }
        }
    }

    // Poll the session while it is active; stream output to the console.
    LaunchedEffect(debugActive) {
        while (debugActive) {
            val polled = debugger.status()
            if (polled.output.isNotEmpty()) {
                polled.output.trimEnd('\n').lines().forEach { console.println(it) }
            }
            debugState = polled
            if (polled.status == "terminated" || polled.status == "failed" || polled.status == "none") {
                polled.error?.let { console.error("Debug: $it") }
                console.info("Debug ▸ finished")
                debugActive = false
            }
            delay(120)
        }
    }
    DisposableEffect(panelId) {
        onDispose { if (debugActive) scope.launch { debugger.stop() } }
    }

    // ---- layout --------------------------------------------------------
    val errorLines = diagnostics.filter { it.severity == "error" }.map { it.line }.toSet()
    val warningLines = diagnostics.filter { it.severity == "warning" }.map { it.line }.toSet()
    val debugLine = if (debugActive && debugState.status == "paused") debugState.line else null
    val cursorLine = AzEditorText.lineColumn(fieldValue.text, fieldValue.selection.start).first
    val bracketPair = if (fieldValue.selection.collapsed)
        AzEditorText.matchingBracket(fieldValue.text, fieldValue.selection.start) else null
    val transformation = remember(spans, errorLines, warningLines, debugLine, cursorLine, bracketPair, searchMatches, activeMatch) {
        AzSyntaxTransformation(
            spans, errorLines, warningLines, debugLine,
            cursorLine = cursorLine,
            bracketPair = bracketPair,
            searchMatches = if (findVisible) searchMatches else emptyList(),
            activeMatch = activeMatch,
        )
    }

    // Caret position in dp for the popup (monospace metrics from one glyph).
    val measurer = rememberTextMeasurer()
    val editorTextStyle = TextStyle(
        color = AzoraPalette.Neutral10,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 5).sp
    )
    val glyph = remember(measurer, fontSize) { measurer.measure("M", editorTextStyle) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val lineHeightDp = with(density) { (fontSize + 5).sp.toDp() }
    val lineHeightPx = with(density) { (fontSize + 5).sp.toPx() }

    // Shared vertical scroll so Go-To-Definition can bring a line into view.
    val scrollState = rememberScrollState()

    /** Moves the caret to (and scrolls to) the start of [line] (1-based). */
    fun gotoLineInEditor(line: Int) {
        val text = fieldValue.text
        val lineStarts = buildList {
            add(0)
            text.forEachIndexed { i, c -> if (c == '\n') add(i + 1) }
        }
        val target = (line - 1).coerceIn(0, lineStarts.lastIndex)
        val offset = lineStarts[target]
        fieldValue = fieldValue.copy(selection = androidx.compose.ui.text.TextRange(offset))
        scope.launch {
            // Leave a few lines of context above the target.
            val y = ((target - 3) * lineHeightPx).toInt().coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(y)
        }
    }

    /** Resolves and jumps to the declaration of the symbol at the caret. */
    fun goToDefinition() {
        scope.launch {
            val def = intel.definition(fieldValue.text, fieldValue.selection.start, state.filePath, projectPath)
            if (def == null) {
                console.print("No definition found")
                return@launch
            }
            if (def.filePath == null || def.filePath == state.filePath) {
                gotoLineInEditor(def.line)
            } else {
                // Open (or focus) the target file, then ask it to scroll to the line.
                val targetPanelId = manager.openFile(def.filePath) ?: run {
                    console.error("Cannot open ${def.filePath.substringAfterLast('/')}")
                    return@launch
                }
                val targetState = manager.getState(targetPanelId)
                if (targetState != null) {
                    dockStateManager.dispatch(
                        DockAction.AddPanel(
                            DockPanelDescriptor(id = targetPanelId, title = targetState.fileName, closeable = true),
                            EDITOR_AREA_NODE_ID,
                            DockZone.CENTER
                        )
                    )
                    dockStateManager.dispatch(DockAction.SelectPanel(targetPanelId))
                }
                manager.requestGoto(targetPanelId, def.line)
            }
        }
    }

    // Apply a pending goto handed to this editor (e.g. a cross-file jump that
    // just opened this file), then clear it.
    LaunchedEffect(state.gotoLine) {
        val line = state.gotoLine ?: return@LaunchedEffect
        gotoLineInEditor(line)
        manager.consumeGoto(panelId)
    }

    // Converts the current buffer into a sibling .azn node graph and opens it.
    fun convertToNodes() {
        scope.launch {
            val aznPath = AznFiles.siblingAznPath(state.filePath)
            val aznName = aznPath.substringAfterLast('/')
            when (val converted = AzToNodesConverter().convert(fieldValue.text, state.fileName)) {
                is AzToNodesResult.Failure -> {
                    console.error("Cannot convert ${state.fileName}.az: ${converted.errors.firstOrNull()}")
                }
                is AzToNodesResult.Success -> {
                    converted.warnings.forEach { console.print("[$aznName] $it") }
                    when (fileSystem.writeToFile(aznPath, AznFiles.encode(converted.graph))) {
                        is FileSystemResult.Success -> {
                            console.info("Converted ${state.fileName}.az → $aznName")
                            val nodesPanelId = nodesFilesManager.reloadFile(aznPath) ?: return@launch
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

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(AzoraPalette.Neutral90)) {
        AzScriptHeader(
            fileName = state.fileName,
            isDirty = state.isDirty,
            intelAvailable = intel.available,
            errorCount = errorLines.size,
            warningCount = warningLines.size,
            debugActive = debugActive,
            debugStatus = debugState.status,
            debugLine = debugState.line,
            onDebug = { startDebug() },
            onContinue = { scope.launch { debugger.resume() } },
            onStep = { scope.launch { debugger.step() } },
            onStopDebug = { scope.launch { debugger.stop() } },
            onConvertToNodes = { convertToNodes() }
        )

        if (findVisible) {
            AzFindBar(
                query = findQuery,
                replace = replaceQuery,
                matchCount = searchMatches.size,
                activeMatch = if (searchMatches.isEmpty()) 0 else activeMatch + 1,
                onQueryChange = { findQuery = it; activeMatch = 0 },
                onReplaceChange = { replaceQuery = it },
                onNext = { gotoMatch(activeMatch + 1) },
                onPrevious = { gotoMatch(activeMatch - 1) },
                onReplaceOne = {
                    if (searchMatches.isNotEmpty()) {
                        val (start, end) = searchMatches[activeMatch.coerceIn(searchMatches.indices)]
                        val text = fieldValue.text.substring(0, start) + replaceQuery + fieldValue.text.substring(end)
                        applyEdit(TextFieldValue(text, androidx.compose.ui.text.TextRange(start + replaceQuery.length)), viaPipeline = false)
                    }
                },
                onReplaceAll = {
                    if (findQuery.isNotBlank()) {
                        val text = fieldValue.text.replace(findQuery, replaceQuery, ignoreCase = true)
                        applyEdit(TextFieldValue(text, fieldValue.selection.coerceIn(text.length)), viaPipeline = false)
                    }
                },
                onClose = { findVisible = false }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(AzoraPalette.Neutral90)
                // Ctrl/Cmd + scroll zooms the editor. macOS trackpad pinch is
                // delivered as exactly this (scroll + ctrl), matching browsers.
                // Consumed in the Initial pass so verticalScroll never sees it.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type != PointerEventType.Scroll) continue
                            val mods = event.keyboardModifiers
                            if (!mods.isCtrlPressed && !mods.isMetaPressed) continue
                            val dy = event.changes.fold(0f) { acc, c -> acc + c.scrollDelta.y }
                            if (dy != 0f) {
                                fontSize = (fontSize + if (dy < 0f) 1 else -1).coerceIn(9, 28)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .verticalScroll(scrollState)
        ) {
            if (prefs.editorShowLineNumbers) BreakpointGutter(
                lineCount = fieldValue.text.count { it == '\n' } + 1,
                breakpoints = breakpoints,
                debugLine = debugLine,
                rowHeight = lineHeightDp,
                fontSize = (fontSize - 3).coerceAtLeast(8),
                onToggle = { toggleBreakpoint(it) }
            )
            // Without word wrap, long lines scroll horizontally: the field is laid
            // out at max(viewport, widest line) width — exact for monospace text.
            val hScroll = rememberScrollState()
            val widestLineChars = remember(fieldValue.text) {
                fieldValue.text.lineSequence().maxOfOrNull { it.length } ?: 0
            }
            BoxWithConstraints(modifier = Modifier.weight(1f).padding(top = 12.dp, end = 12.dp, bottom = 12.dp)) {
            val viewportWidth = maxWidth
            val contentWidth = if (prefs.editorWordWrap) viewportWidth else {
                val textWidth = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (widestLineChars * glyph.size.width).toDp() + 24.dp
                }
                maxOf(viewportWidth, textWidth)
            }
            Box(
                modifier = if (prefs.editorWordWrap) Modifier else Modifier.horizontalScroll(hScroll)
            ) {
            Box(modifier = Modifier.width(contentWidth)) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    val textChanged = newValue.text != fieldValue.text
                    val selectionMoved = newValue.selection != fieldValue.selection
                    val grewByTyping = newValue.text.length == fieldValue.text.length + 1
                    applyEdit(newValue)
                    if (textChanged) {
                        val cursor = newValue.selection.start
                        val typed = if (grewByTyping && cursor > 0) newValue.text[cursor - 1] else null
                        when {
                            prefs.editorAutoCompletion && typed != null &&
                                (typed.isLetterOrDigit() || typed == '_' || typed == '.') ->
                                requestCompletions(debounceMs = 120)
                            else -> closeCompletions()
                        }
                    } else if (selectionMoved) {
                        // Caret moved without typing (mouse click, arrow keys):
                        // the popup no longer matches the cursor position.
                        closeCompletions()
                    }
                },
                textStyle = editorTextStyle,
                visualTransformation = transformation,
                onTextLayout = { textLayout = it },
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(panelId) {
                        // Hover docs: after the pointer rests ~400ms over a symbol,
                        // ask the language server for its signature + doc comment.
                        // Ctrl/Cmd+click jumps to the symbol's definition.
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val mods = event.keyboardModifiers
                                if (event.type == PointerEventType.Press && (mods.isCtrlPressed || mods.isMetaPressed)) {
                                    val position = event.changes.firstOrNull()?.position
                                    val layout = textLayout
                                    if (position != null && layout != null) {
                                        val offset = layout.getOffsetForPosition(position)
                                        fieldValue = fieldValue.copy(selection = androidx.compose.ui.text.TextRange(offset))
                                        hoverInfo = null
                                        hoverJob?.cancel()
                                        goToDefinition()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                                if (event.type == PointerEventType.Move) {
                                    val position = event.changes.firstOrNull()?.position ?: continue
                                    hoverInfo = null
                                    hoverJob?.cancel()
                                    hoverJob = scope.launch {
                                        if (!prefs.editorHoverDocs) return@launch
                                        delay(400)
                                        val layout = textLayout ?: return@launch
                                        val offset = layout.getOffsetForPosition(position)
                                        val hover = intel.hover(fieldValue.text, offset, state.filePath, projectPath)
                                        if (hover != null) {
                                            hoverPosition = position
                                            hoverInfo = hover
                                        }
                                    }
                                }
                                if (event.type == PointerEventType.Exit) {
                                    hoverJob?.cancel()
                                    hoverInfo = null
                                }
                            }
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        // Completion popup + save/complete shortcuts take priority…
                        val handled = handleEditorKeys(
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
                        if (handled) return@onPreviewKeyEvent true
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val meta = event.isCtrlPressed || event.isMetaPressed
                        when {
                            // …then the professional-editor shortcuts.
                            event.key == Key.Tab && event.isShiftPressed ->
                                { applyEdit(AzEditorText.outdentSelection(fieldValue, indentUnit), viaPipeline = false); true }
                            event.key == Key.Tab ->
                                { applyEdit(AzEditorText.indentSelection(fieldValue, indentUnit), viaPipeline = false); true }
                            meta && event.key == Key.Slash ->
                                { applyEdit(AzEditorText.toggleLineComment(fieldValue), viaPipeline = false); true }
                            meta && event.isShiftPressed && event.key == Key.K ->
                                { applyEdit(AzEditorText.deleteLine(fieldValue), viaPipeline = false); true }
                            meta && event.key == Key.D ->
                                { applyEdit(AzEditorText.duplicateLine(fieldValue), viaPipeline = false); true }
                            event.isAltPressed && event.key == Key.DirectionUp ->
                                { applyEdit(AzEditorText.moveLine(fieldValue, up = true), viaPipeline = false); true }
                            event.isAltPressed && event.key == Key.DirectionDown ->
                                { applyEdit(AzEditorText.moveLine(fieldValue, up = false), viaPipeline = false); true }
                            meta && event.isShiftPressed && event.key == Key.Z -> { redo(); true }
                            meta && event.key == Key.Z -> { undo(); true }
                            meta && event.key == Key.Y -> { redo(); true }
                            meta && event.key == Key.F -> {
                                val sel = fieldValue.text.substring(fieldValue.selection.min, fieldValue.selection.max)
                                if (sel.isNotBlank() && '\n' !in sel) findQuery = sel
                                findVisible = true
                                true
                            }
                            // Go To Definition (IntelliJ Cmd/Ctrl+B, plus F12).
                            (meta && event.key == Key.B) || event.key == Key.F12 -> {
                                goToDefinition(); true
                            }
                            // Go To Symbol in file (VS Code / IntelliJ Cmd/Ctrl+Shift+O).
                            meta && event.isShiftPressed && event.key == Key.O -> {
                                openSymbolPicker(); true
                            }
                            meta && (event.key == Key.Equals || event.key == Key.Plus) ->
                                { fontSize = (fontSize + 1).coerceAtMost(28); true }
                            meta && event.key == Key.Minus ->
                                { fontSize = (fontSize - 1).coerceAtLeast(9); true }
                            meta && event.key == Key.Zero -> { fontSize = prefs.editorFontSize; true }
                            event.key == Key.Escape && findVisible -> { findVisible = false; true }
                            else -> false
                        }
                    }
            )

            if (completionsVisible && completions.isNotEmpty()) {
                // (popup remains anchored inside the editor box)
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
            hoverInfo?.let { hover ->
                AzHoverCard(
                    hover = hover,
                    modifier = Modifier.offset {
                        IntOffset(hoverPosition.x.toInt(), (hoverPosition.y - 34).toInt().coerceAtLeast(0))
                    }
                )
            }
            } // content-width box
            } // h-scroll box
            } // editor box
        }

        if (debugActive && debugState.status == "paused" && debugState.locals.isNotEmpty()) {
            AzDebugLocalsBar(debugState.locals)
        }
        if (diagnostics.isNotEmpty()) {
            AzProblemsStrip(diagnostics)
        }

        AzStatusBar(
            line = cursorLine,
            column = AzEditorText.lineColumn(fieldValue.text, fieldValue.selection.start).second,
            selectionLength = fieldValue.selection.max - fieldValue.selection.min,
            intelAvailable = intel.available,
            fontSize = fontSize
        )
    }

    if (symbolPickerVisible) {
        AzSymbolPicker(
            symbols = fileSymbols,
            onJump = { symbolPickerVisible = false; gotoLineInEditor(it.line) },
            onClose = { symbolPickerVisible = false },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
        )
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
    debugActive: Boolean,
    debugStatus: String,
    debugLine: Int,
    onDebug: () -> Unit,
    onContinue: () -> Unit,
    onStep: () -> Unit,
    onStopDebug: () -> Unit,
    onConvertToNodes: () -> Unit,
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
            Text("Saving…", color = AzoraPalette.Neutral40, fontSize = 10.sp)
        } else {
            Text("Saved", color = AzoraPalette.Neutral60, fontSize = 10.sp)
        }
        // Debugger controls (needs the language-server jar)
        if (!debugActive) {
            if (intelAvailable) {
                HeaderButton("⏵ Debug", AzoraPalette.AccentGreen, onDebug)
            }
        } else {
            Text(
                text = if (debugStatus == "paused") "paused at line $debugLine" else "running…",
                color = if (debugStatus == "paused") AzoraPalette.AccentYellow else AzoraPalette.AccentGreen,
                fontSize = 10.sp
            )
            if (debugStatus == "paused") {
                HeaderButton("⏵ Continue", AzoraPalette.AccentGreen, onContinue)
                HeaderButton("⤵ Step", AzoraPalette.AccentCyan, onStep)
            }
            HeaderButton("⏹ Stop", AzoraPalette.AccentRed, onStopDebug)
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
    }
}

/** Floating docs card shown on hover: signature (monospace) + doc comment. */
@Composable
private fun AzHoverCard(hover: AzHover, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AzoraPalette.Neutral85)
            .border(1.dp, AzoraPalette.Neutral70, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = hover.signature,
            color = AzoraPalette.AccentBlue,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        if (hover.detail.isNotBlank()) {
            Text(text = hover.detail, color = AzoraPalette.Neutral40, fontSize = 11.sp)
        }
        if (hover.doc.isNotBlank()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AzoraPalette.Neutral70)
            )
            Text(text = hover.doc, color = AzoraPalette.Neutral20, fontSize = 11.sp, lineHeight = 15.sp)
        }
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

/** Clamps a selection to a (possibly shorter) new text length. */
private fun androidx.compose.ui.text.TextRange.coerceIn(maxLength: Int): androidx.compose.ui.text.TextRange =
    androidx.compose.ui.text.TextRange(start.coerceAtMost(maxLength), end.coerceAtMost(maxLength))

@Composable
private fun HeaderButton(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Line-number gutter with clickable breakpoint dots. Row heights match the
 * editor's monospace line height so numbers align with source lines; the
 * paused line is marked with an arrow.
 */
@Composable
private fun BreakpointGutter(
    lineCount: Int,
    breakpoints: Set<Int>,
    debugLine: Int?,
    rowHeight: androidx.compose.ui.unit.Dp,
    fontSize: Int,
    onToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .background(AzoraPalette.Neutral80.copy(alpha = 0.6f))
            .padding(top = 12.dp, bottom = 12.dp)
            .width(46.dp)
    ) {
        for (line in 1..lineCount) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(rowHeight)
                    .fillMaxWidth()
                    .clickable { onToggle(line) }
                    .padding(horizontal = 4.dp)
            ) {
                Box(modifier = Modifier.width(10.dp), contentAlignment = Alignment.Center) {
                    when {
                        line in breakpoints -> Text("●", color = AzoraPalette.AccentRed, fontSize = 9.sp)
                        line == debugLine -> Text("▶", color = AzoraPalette.AccentYellow, fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    text = line.toString(),
                    color = if (line == debugLine) AzoraPalette.AccentYellow else AzoraPalette.Neutral50,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Variables in scope while the debugger is paused. */
@Composable
private fun AzDebugLocalsBar(locals: List<AzDebugLocal>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzoraPalette.Neutral80)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text("Locals", color = AzoraPalette.Neutral40, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        for (row in locals.chunked(4)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (local in row) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(local.name, color = AzoraPalette.AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(" = ${local.value}", color = AzoraPalette.Neutral20, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Go-To-Symbol palette (Cmd/Ctrl+Shift+O): a filterable list of the file's
 * top-level declarations. Type to fuzzy-filter by name, ↑/↓ to move, Enter to
 * jump, Esc to dismiss. Grabs focus on open.
 */
@Composable
private fun AzSymbolPicker(
    symbols: List<AzSymbol>,
    onJump: (AzSymbol) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val filtered = remember(symbols, query) {
        if (query.isBlank()) symbols
        else symbols.filter { it.name.contains(query, ignoreCase = true) }
    }
    // Keep the selection valid as the filter narrows the list.
    if (selected > filtered.lastIndex) selected = filtered.lastIndex.coerceAtLeast(0)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        if (selected in filtered.indices) listState.animateScrollToItem(selected)
    }

    Column(
        modifier = modifier
            .width(460.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AzoraPalette.Neutral85)
            .border(1.dp, AzoraPalette.Neutral70, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = { query = it; selected = 0 },
            singleLine = true,
            textStyle = TextStyle(color = AzoraPalette.Neutral10, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(4.dp))
                .background(AzoraPalette.Neutral70)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { selected = (selected + 1).coerceAtMost(filtered.lastIndex.coerceAtLeast(0)); true }
                        Key.DirectionUp -> { selected = (selected - 1).coerceAtLeast(0); true }
                        Key.Enter -> { filtered.getOrNull(selected)?.let(onJump); true }
                        Key.Escape -> { onClose(); true }
                        else -> false
                    }
                },
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text(
                        "Go to symbol…",
                        color = AzoraPalette.Neutral50,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    inner()
                }
            }
        )
        if (filtered.isEmpty()) {
            Text("No matching symbols", color = AzoraPalette.Neutral50, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
        } else {
            LazyColumn(state = listState, modifier = Modifier.heightIn(max = 320.dp)) {
                itemsIndexed(filtered) { index, sym ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == selected) AzoraPalette.Neutral70 else Color.Transparent)
                            .clickable { onJump(sym) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(symbolGlyph(sym.kind), color = symbolColor(sym.kind), fontSize = 11.sp, modifier = Modifier.width(16.dp))
                        Text(sym.name, color = AzoraPalette.Neutral10, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = sym.detail.ifBlank { sym.kind },
                            color = AzoraPalette.Neutral50,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${sym.line}", color = AzoraPalette.Neutral60, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/** Single-glyph badge for a symbol kind in the Go-To-Symbol list. */
private fun symbolGlyph(kind: String): String = when (kind) {
    "function" -> "ƒ"
    "pack" -> "▢"
    "enum" -> "≡"
    "solo" -> "◈"
    "node" -> "◇"
    "test" -> "✓"
    "variable" -> "="
    "impl" -> "⊕"
    "bridge" -> "⇄"
    else -> "•"
}

private fun symbolColor(kind: String): Color = when (kind) {
    "function" -> AzoraPalette.AccentBlue
    "pack", "solo" -> AzoraPalette.AccentCyan
    "enum" -> AzoraPalette.AccentYellow
    "variable" -> AzoraPalette.AccentGreen
    "test" -> AzoraPalette.AccentGreen
    else -> AzoraPalette.Neutral40
}

/** VS-Code-style find & replace bar (Cmd/Ctrl+F, Esc closes). */
@Composable
private fun AzFindBar(
    query: String,
    replace: String,
    matchCount: Int,
    activeMatch: Int,
    onQueryChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzoraPalette.Neutral80)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FindField(query, "Find", onQueryChange, onEnter = onNext, modifier = Modifier.weight(1f))
        Text(
            text = if (matchCount == 0) "No results" else "$activeMatch/$matchCount",
            color = if (matchCount == 0 && query.isNotBlank()) AzoraPalette.AccentRed else AzoraPalette.Neutral40,
            fontSize = 10.sp
        )
        HeaderButton("↑", AzoraPalette.Neutral30, onPrevious)
        HeaderButton("↓", AzoraPalette.Neutral30, onNext)
        FindField(replace, "Replace", onReplaceChange, onEnter = onReplaceOne, modifier = Modifier.weight(1f))
        HeaderButton("Replace", AzoraPalette.Neutral30, onReplaceOne)
        HeaderButton("All", AzoraPalette.Neutral30, onReplaceAll)
        HeaderButton("✕", AzoraPalette.Neutral40, onClose)
    }
}

@Composable
private fun FindField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = AzoraPalette.Neutral10, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AzoraPalette.Neutral70)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                    onEnter(); true
                } else false
            },
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = AzoraPalette.Neutral50, fontSize = 11.sp)
                inner()
            }
        }
    )
}

/** Bottom status bar: caret position, selection size, language-server state. */
@Composable
private fun AzStatusBar(
    line: Int,
    column: Int,
    selectionLength: Int,
    intelAvailable: Boolean,
    fontSize: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzoraPalette.Neutral80)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Ln $line, Col $column" + if (selectionLength > 0) "  ($selectionLength selected)" else "",
            color = AzoraPalette.Neutral40,
            fontSize = 9.sp
        )
        Spacer(Modifier.weight(1f))
        Text("Spaces: 4", color = AzoraPalette.Neutral50, fontSize = 9.sp)
        Text("${fontSize}px", color = AzoraPalette.Neutral50, fontSize = 9.sp)
        Text("Azora", color = AzoraPalette.Neutral50, fontSize = 9.sp)
        Text(
            text = if (intelAvailable) "azls ✓" else "azls ✕",
            color = if (intelAvailable) AzoraPalette.AccentGreen else AzoraPalette.AccentRed,
            fontSize = 9.sp
        )
    }
}
