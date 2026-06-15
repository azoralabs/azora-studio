package org.azora.studio.az_script

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.azora.lang.completion.Completion
import org.azora.lang.completion.CompletionKind
import org.azora.studio.highlight.SyntaxHighlighter
import org.azora.studio.assets.OpenAzScriptFilesManager
import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import org.azora.sdk.core.domain.util.Res
import org.azora.sdk.core.project.domain.SettingsModel
import org.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import org.azora.sdk.core.project.domain.repository.SettingsRepository
import org.azora.sdk.core.theme.LocalAzoraPalette
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.azora.sdk.docking.domain.DockAction
import org.azora.sdk.docking.domain.DockStateManager
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import azora.azora_studio.app.generated.resources.Res as AppRes
import azora.azora_studio.app.generated.resources.ic_run_test
import azora.azora_studio.app.generated.resources.ic_trigger_event

private fun Char.isIdChar() = isLetterOrDigit() || this == '_'

private val hoverKeywords = setOf(
    "var", "fin", "func", "flow", "task", "if", "else", "for", "loop", "while",
    "in", "as", "is", "when", "return", "break", "continue", "null",
    "expose", "confine", "assert", "trace", "launch", "async", "await",
    "enum", "slot", "pack", "impl", "infx", "scope", "package", "use",
    "flip", "flop", "by", "typealias", "with", "spec", "where", "each",
    "type", "let", "suspend", "yield", "fail", "try", "catch", "defer",
    "rescue", "guard", "dyn", "node", "protect", "base", "leaf", "this",
    "throw", "prop", "self", "it", "ref", "mut", "alloc", "drop", "unsafe",
    "inject", "wrap", "bind", "lazy", "view", "effect", "out", "bridge",
    "hook", "test", "inline", "platform", "deco", "oper", "ctor", "dtor",
    "true", "false"
)

private fun extractWordAt(source: String, offset: Int): Pair<String, Int>? {
    val pos = when {
        offset < source.length && source[offset].isIdChar() -> offset
        offset > 0 && source[offset - 1].isIdChar() -> offset - 1
        else -> return null
    }
    var start = pos
    while (start > 0 && source[start - 1].isIdChar()) start--
    var end = pos + 1
    while (end < source.length && source[end].isIdChar()) end++
    val word = source.substring(start, end)
    if (word.isEmpty() || !word.first().isLetter()) return null
    if (word in hoverKeywords) return null
    return word to start
}

@Composable
fun AzScriptFilePanel(
    panelId: String,
    projectPath: String,
    openFilesManager: OpenAzScriptFilesManager = koinInject(),
    consoleOutputManager: ConsoleOutputManager = koinInject(),
    dockStateManager: DockStateManager = koinInject(),
    projectRepository: AzoraProjectRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    diagnosticsManager: DiagnosticsManager = koinInject()
) {
    val openFiles by openFilesManager.openFiles.collectAsState()
    val fileState = openFiles[panelId]
    val palette = LocalAzoraPalette.current

    // Load project ID for settings observation
    var projectId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val result = projectRepository.getProject()
        if (result is Res.Success) {
            projectId = result.data.id
        }
    }

    // Observe script highlight settings reactively
    val settingsModel by remember(projectId) {
        if (projectId != null) {
            settingsRepository.observeSettings(projectId!!)
        } else {
            kotlinx.coroutines.flow.flowOf(SettingsModel.default(""))
        }
    }.collectAsState(SettingsModel.default(""))

    val usePastel = settingsModel.azScriptUsePastel
    val boldKeywords = settingsModel.azScriptBoldKeywords
    val italicPreprocessor = settingsModel.azScriptItalicPreprocessor
    val underlineVariables = settingsModel.azScriptUnderlineVariables

    // Try to recover file if not loaded
    LaunchedEffect(panelId, projectPath) {
        if (openFilesManager.getState(panelId) == null && projectPath.isNotEmpty()) {
            val dockState = dockStateManager.state.value
            val panelDescriptor = dockState.layout.panelDescriptors[panelId]
            if (panelDescriptor != null) {
                val title = panelDescriptor.title
                val assetsPath = "$projectPath/Assets"
                // Search recursively for the file under Assets/
                val filePath = openFilesManager.findScriptFile(assetsPath, "$title.az")
                    ?: "$assetsPath/$title.az" // fallback to flat path
                openFilesManager.restoreFile(panelId, filePath)
            }
        }
    }

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

    // Runners are only available on desktop, null on other platforms
    val kotlinRunner: KotlinRunner? = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<KotlinRunner>()
        } catch (_: Exception) { null }
    }
    val csharpRunner: CSharpRunner? = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<CSharpRunner>()
        } catch (_: Exception) { null }
    }
    val jsRunner: JavaScriptRunner? = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<JavaScriptRunner>()
        } catch (_: Exception) { null }
    }
    val llvmRunner: LlvmRunner? = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<LlvmRunner>()
        } catch (_: Exception) { null }
    }

    val viewModel = remember(panelId) {
        AzScriptFileViewModel(
            panelId = panelId,
            projectPath = projectPath,
            openFilesManager = openFilesManager,
            consoleOutputManager = consoleOutputManager,
            diagnosticsManager = diagnosticsManager
        ).also {
            it.kotlinRunner = kotlinRunner
            it.csharpRunner = csharpRunner
            it.jsRunner = jsRunner
            it.llvmRunner = llvmRunner
        }
    }

    // Update highlighting and runtime settings when settings change
    LaunchedEffect(usePastel, boldKeywords, italicPreprocessor, underlineVariables) {
        viewModel.updateHighlightSettings(usePastel, boldKeywords, italicPreprocessor, underlineVariables)
    }
    LaunchedEffect(settingsModel.showRuntimeWarnings) {
        viewModel.showRuntimeWarnings = settingsModel.showRuntimeWarnings
    }

    val state by viewModel.state.collectAsState()

    // Script is runnable if it has an onStart hook
    val isRunnable = remember(state.sourceCode) {
        state.sourceCode.isNotBlank()
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        // Toolbar
        AzScriptFileToolbar(
            fileName = fileState.fileName,
            isRunning = state.isRunning,
            isRunnable = isRunnable,
            showGeneratedSource = state.showGeneratedSource,
            hasErrors = state.diagnostics.isNotEmpty(),
            hasTests = state.hasTests,
            executionMode = state.executionMode,
            onRun = { viewModel.run() },
            onStop = { viewModel.stop() },
            onToggleGenerated = { viewModel.toggleShowGeneratedSource() },
            onRunTests = { viewModel.runTests() },
            onSetExecutionMode = { mode -> viewModel.setExecutionMode(mode) }
        )

        val showGenerated = state.showGeneratedSource && state.diagnostics.isEmpty() && (
            (state.executionMode == ExecutionMode.INTERPRETED && state.generatedSource != null) ||
            (state.executionMode == ExecutionMode.COMPILED_KT && state.generatedKotlinSource != null) ||
            (state.executionMode == ExecutionMode.COMPILED_CS && state.generatedCSharpSource != null) ||
            (state.executionMode == ExecutionMode.COMPILED_JS && state.generatedJavaScriptSource != null) ||
            (state.executionMode == ExecutionMode.COMPILED_LLVM && state.generatedLlvmIrSource != null)
        )
        var splitRatio by remember { mutableStateOf(0.5f) }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidthPx = constraints.maxWidth.toFloat()

            Row(modifier = Modifier.fillMaxSize()) {
                // Code editor - always visible on the left
                Box(
                    modifier = Modifier
                        .weight(if (showGenerated) splitRatio else 1f)
                        .fillMaxHeight()
                        .background(Color(0xFF1E1E1E))
                ) {
                    CodeStudio(
                        sourceCode = state.sourceCode,
                        highlightedSource = viewModel.highlightedSource.value,
                        completions = state.completions,
                        showCompletions = state.showCompletions,
                        selectedCompletionIndex = state.selectedCompletionIndex,
                        diagnostics = state.diagnostics,
                        testLines = state.testLines,
                        hookLines = state.hookLines,
                        panelId = panelId,
                        diagnosticsManager = diagnosticsManager,
                        dockStateManager = dockStateManager,
                        onSourceChanged = { newSource, newCursor ->
                            viewModel.onSourceChanged(newSource)
                            viewModel.requestCompletions(newCursor)
                        },
                        onInsertCompletion = { completion, textFieldValue ->
                            viewModel.applyCompletion(completion, textFieldValue)
                        },
                        onDismissCompletions = { viewModel.dismissCompletions() },
                        onNavigateUp = { viewModel.selectPreviousCompletion() },
                        onNavigateDown = { viewModel.selectNextCompletion() },
                        onAcceptCompletion = { viewModel.getSelectedCompletion() },
                        onResolveDefinition = { identifier, offset -> viewModel.resolveDefinition(identifier, offset) },
                        onRunSingleTest = { testName -> viewModel.runSingleTest(testName) },
                        onTriggerHook = { hookName -> viewModel.triggerHook(hookName) }
                    )
                }

                if (showGenerated) {
                    // Draggable vertical divider
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF333333))
                            .pointerHoverIcon(PointerIcon.Hand)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    splitRatio = (splitRatio + delta / totalWidthPx).coerceIn(0.25f, 0.75f)
                                }
                            )
                    )

                    // Generated/Kotlin source view on the right
                    Box(
                        modifier = Modifier
                            .weight(1f - splitRatio)
                            .fillMaxHeight()
                    ) {
                        if (state.executionMode == ExecutionMode.COMPILED_KT && state.generatedKotlinSource != null) {
                            KotlinSourceView(
                                kotlinSource = state.generatedKotlinSource!!
                            )
                        } else if (state.executionMode == ExecutionMode.COMPILED_CS && state.generatedCSharpSource != null) {
                            KotlinSourceView(
                                kotlinSource = state.generatedCSharpSource!!
                            )
                        } else if (state.executionMode == ExecutionMode.COMPILED_JS && state.generatedJavaScriptSource != null) {
                            KotlinSourceView(
                                kotlinSource = state.generatedJavaScriptSource!!
                            )
                        } else if (state.executionMode == ExecutionMode.COMPILED_LLVM && state.generatedLlvmIrSource != null) {
                            KotlinSourceView(
                                kotlinSource = state.generatedLlvmIrSource!!
                            )
                        } else if (state.generatedSource != null) {
                            GeneratedSourceView(
                                generatedSource = state.generatedSource!!,
                                highlighter = remember(usePastel, boldKeywords, italicPreprocessor, underlineVariables) {
                                    SyntaxHighlighter.create(usePastel, boldKeywords, italicPreprocessor, underlineVariables)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CodeStudio(
    sourceCode: String,
    highlightedSource: AnnotatedString,
    completions: List<Completion>,
    showCompletions: Boolean,
    selectedCompletionIndex: Int,
    diagnostics: List<ScriptDiagnostic> = emptyList(),
    testLines: Map<Int, String> = emptyMap(),
    hookLines: Map<Int, String> = emptyMap(),
    panelId: String = "",
    diagnosticsManager: DiagnosticsManager? = null,
    dockStateManager: DockStateManager? = null,
    onSourceChanged: (String, Int) -> Unit,
    onInsertCompletion: (Completion, TextFieldValue) -> TextFieldValue?,
    onDismissCompletions: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onAcceptCompletion: () -> Completion?,
    onResolveDefinition: (String, Int) -> GotoDefinitionInfo? = { _, _ -> null },
    onRunSingleTest: (String) -> Unit = {},
    onTriggerHook: (String) -> Unit = {}
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = sourceCode))
    }

    // Sync external source changes (e.g. from completion insertion)
    LaunchedEffect(sourceCode) {
        if (textFieldValue.text != sourceCode) {
            textFieldValue = textFieldValue.copy(text = sourceCode)
        }
    }

    // Syntax highlighting via VisualTransformation
    val visualTransformation = remember(highlightedSource) {
        SyntaxHighlightTransformation(highlightedSource)
    }

    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val editorCoroutineScope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Error lines for wavy underlines
    val errorLines = remember(diagnostics) {
        diagnostics.filter { it.line > 0 }.map { it.line }.toSet()
    }

    // Error messages by line for tooltips
    val errorMessagesByLine = remember(diagnostics) {
        diagnostics.filter { it.line > 0 }.groupBy { it.line }
            .mapValues { (_, diags) -> diags.joinToString("\n") { it.message } }
    }

    // Goto-definition state
    var hoverTooltip by remember { mutableStateOf<GotoDefinitionInfo?>(null) }
    var tooltipPosition by remember { mutableStateOf(Offset.Zero) }

    // Error tooltip state
    var errorTooltipMessage by remember { mutableStateOf<String?>(null) }
    var errorTooltipPosition by remember { mutableStateOf(Offset.Zero) }

    // Navigate to line on request from DiagnosticsManager
    val density = LocalDensity.current
    if (diagnosticsManager != null && panelId.isNotEmpty()) {
        LaunchedEffect(panelId) {
            diagnosticsManager.navigateRequest.collect { request ->
                if (request.panelId == panelId) {
                    val lineHeightPx = with(density) { 20.sp.toPx() }
                    val targetScroll = ((request.line - 1) * lineHeightPx).toInt().coerceAtLeast(0)
                    scrollState.animateScrollTo(targetScroll)
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = constraints.maxHeight
        val textFieldMinWidth = this.maxWidth - 48.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        ) {
            // Line numbers gutter with test/hook icons
            val lineCount = sourceCode.count { it == '\n' } + 1
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF252526))
                    .verticalScroll(scrollState)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    val testName = testLines[i]
                    val hookName = hookLines[i]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (testName != null) {
                            Icon(
                                painter = painterResource(AppRes.drawable.ic_run_test),
                                contentDescription = "Run test",
                                tint = Color(0xFF73C991),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 2.dp)
                                    .size(14.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { onRunSingleTest(testName) }
                            )
                        } else if (hookName != null) {
                            Icon(
                                painter = painterResource(AppRes.drawable.ic_trigger_event),
                                contentDescription = "Trigger hook",
                                tint = Color(0xFFE5C07B),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 2.dp)
                                    .size(14.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { onTriggerHook(hookName) }
                            )
                        }
                        Text(
                            text = "$i",
                            color = Color(0xFF858585),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            // Code text field
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text

                    if (newValue.text != oldText) {
                        textFieldValue = newValue
                        onSourceChanged(newValue.text, newValue.selection.start)
                    } else {
                        // Only dismiss completions if the cursor actually moved (e.g. user clicked
                        // elsewhere). Ignore spurious onValueChange callbacks from recomposition
                        // (same text AND same selection) which can happen when VisualTransformation changes.
                        val cursorChanged = textFieldValue.selection != newValue.selection
                        val savedScroll = scrollState.value
                        textFieldValue = newValue
                        if (cursorChanged) {
                            onDismissCompletions()
                        }
                        editorCoroutineScope.launch {
                            withTimeoutOrNull(300) {
                                snapshotFlow { scrollState.value }
                                    .collect { current ->
                                        if (current != savedScroll) {
                                            scrollState.scrollTo(savedScroll)
                                        }
                                    }
                            }
                        }
                    }
                },
                onTextLayout = { textLayoutResult = it },
                visualTransformation = visualTransformation,
                textStyle = TextStyle(
                    color = Color(0xFFD4D4D4),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(Color(0xFFAEAFAD)),
                modifier = Modifier
                    .defaultMinSize(minWidth = textFieldMinWidth)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
                    .drawWithContent {
                        drawContent()
                        // Draw wavy red underlines on error lines
                        val layout = textLayoutResult ?: return@drawWithContent
                        if (errorLines.isEmpty()) return@drawWithContent
                        val waveHeight = 2.dp.toPx()
                        val waveLength = 4.dp.toPx()
                        val errorColor = Color(0xFFFF4444)
                        for (errorLine in errorLines) {
                            val lineIndex = errorLine - 1 // 0-based
                            if (lineIndex < 0 || lineIndex >= layout.lineCount) continue
                            val lineTop = layout.getLineTop(lineIndex)
                            val lineBottom = layout.getLineBottom(lineIndex)
                            val lineLeft = layout.getLineLeft(lineIndex)
                            val lineRight = layout.getLineRight(lineIndex)
                            val baselineY = lineBottom - 1.dp.toPx()
                            val startX = lineLeft
                            val endX = lineRight.coerceAtLeast(startX + 20.dp.toPx())

                            val path = Path()
                            path.moveTo(startX, baselineY)
                            var x = startX
                            while (x < endX) {
                                path.quadraticTo(
                                    x + waveLength / 4, baselineY - waveHeight,
                                    x + waveLength / 2, baselineY
                                )
                                path.quadraticTo(
                                    x + 3 * waveLength / 4, baselineY + waveHeight,
                                    x + waveLength, baselineY
                                )
                                x += waveLength
                            }
                            drawPath(path, color = errorColor, style = Stroke(width = 1.5f))
                        }
                    }
                    .onPointerEvent(PointerEventType.Move) { event ->
                        val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                        val layout = textLayoutResult ?: return@onPointerEvent
                        try {
                            val offset = layout.getOffsetForPosition(position)
                            if (offset < 0 || offset >= sourceCode.length) {
                                hoverTooltip = null
                                errorTooltipMessage = null
                                return@onPointerEvent
                            }

                            // Check if mouse is past the end of the line's actual text
                            val line = layout.getLineForOffset(offset)
                            val lineEnd = layout.getLineEnd(line)
                            val lineRight = if (lineEnd > 0) layout.getHorizontalPosition(lineEnd - 1, false) + 10f else 0f
                            if (position.x > lineRight) {
                                hoverTooltip = null
                                errorTooltipMessage = null
                                return@onPointerEvent
                            }

                            // Check if hovering over an error line
                            val hoveredLine = line + 1 // 1-based
                            val errorMsg = errorMessagesByLine[hoveredLine]
                            if (errorMsg != null) {
                                errorTooltipMessage = errorMsg
                                errorTooltipPosition = position
                                hoverTooltip = null
                            } else {
                                errorTooltipMessage = null
                                // Find identifier at cursor position
                                val word = extractWordAt(sourceCode, offset)
                                if (word != null) {
                                    val info = onResolveDefinition(word.first, word.second)
                                    hoverTooltip = info
                                    tooltipPosition = position
                                } else {
                                    hoverTooltip = null
                                }
                            }
                        } catch (_: Exception) {
                            hoverTooltip = null
                            errorTooltipMessage = null
                        }
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        hoverTooltip = null
                        errorTooltipMessage = null
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        // When completions popup is showing, intercept navigation keys
                        if (showCompletions) {
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    onNavigateDown()
                                    return@onPreviewKeyEvent true
                                }
                                Key.DirectionUp -> {
                                    onNavigateUp()
                                    return@onPreviewKeyEvent true
                                }
                                Key.Enter -> {
                                    val completion = onAcceptCompletion()
                                    if (completion != null) {
                                        val result = onInsertCompletion(completion, textFieldValue)
                                        if (result != null) {
                                            textFieldValue = result
                                        }
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                Key.Tab -> {
                                    val completion = onAcceptCompletion()
                                    if (completion != null) {
                                        val result = onInsertCompletion(completion, textFieldValue)
                                        if (result != null) {
                                            textFieldValue = result
                                        }
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                Key.Escape -> {
                                    onDismissCompletions()
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }

                        when (keyEvent.key) {
                            Key.Tab -> {
                                val cursor = textFieldValue.selection.start
                                val newText = textFieldValue.text.substring(0, cursor) +
                                        "    " +
                                        textFieldValue.text.substring(cursor)
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(cursor + 4)
                                )
                                onSourceChanged(newText, cursor + 4)
                                true
                            }
                            Key.Enter -> {
                                val cursor = textFieldValue.selection.start
                                val text = textFieldValue.text

                                // Find the current line start
                                val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
                                val currentLine = text.substring(lineStart, cursor)

                                // Extract leading whitespace from current line
                                val indent = currentLine.takeWhile { it == ' ' || it == '\t' }

                                // Check if line before cursor ends with '{' (ignoring whitespace)
                                val trimmedBeforeCursor = text.substring(lineStart, cursor).trimEnd()
                                val extraIndent = if (trimmedBeforeCursor.endsWith("{")) "    " else ""

                                val insertion = "\n$indent$extraIndent"
                                val newText = text.substring(0, cursor) + insertion + text.substring(cursor)
                                val newCursor = cursor + insertion.length
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursor)
                                )
                                onSourceChanged(newText, newCursor)
                                true
                            }
                            Key.Escape -> {
                                if (showCompletions) {
                                    onDismissCompletions()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    },
                decorationBox = { innerTextField ->
                    Box {
                        if (sourceCode.isEmpty()) {
                            Text(
                                text = "// Write your Azora script here...",
                                color = Color(0xFF5A5A5A),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Completions popup
        if (showCompletions && completions.isNotEmpty()) {
            CompletionsPopup(
                completions = completions,
                selectedIndex = selectedCompletionIndex,
                cursorOffset = textFieldValue.selection.start,
                textLayoutResult = textLayoutResult,
                scrollOffset = scrollState.value,
                availableHeight = availableHeight,
                onSelect = { completion ->
                    val result = onInsertCompletion(completion, textFieldValue)
                    if (result != null) {
                        textFieldValue = result
                    }
                },
                onDismiss = onDismissCompletions
            )
        }

        // Error tooltip
        if (errorTooltipMessage != null) {
            val errMsg = errorTooltipMessage!!
            val errTooltipX = with(density) { (errorTooltipPosition.x + 56.dp.toPx()).toDp() }
            val errTooltipY = with(density) { (errorTooltipPosition.y - scrollState.value + 8.dp.toPx()).toDp() }
            Box(
                modifier = Modifier
                    .padding(start = errTooltipX, top = errTooltipY)
                    .background(Color(0xFF2D2020), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFFF4444).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = errMsg,
                    color = Color(0xFFFF6666),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Goto-definition / hover tooltip
        if (hoverTooltip != null) {
            val info = hoverTooltip!!
            val tooltipX = with(density) { (tooltipPosition.x + 56.dp.toPx()).toDp() }
            val tooltipY = with(density) { (tooltipPosition.y - scrollState.value + 8.dp.toPx()).toDp() }
            Box(modifier = Modifier.padding(start = tooltipX, top = tooltipY)) {
                HoverTooltipContent(info)
            }
        }

        // Scrollbars
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        )
    }
}

@Composable
private fun GeneratedSourceView(
    generatedSource: String,
    highlighter: org.azora.studio.highlight.SyntaxHighlighter
) {
    val highlighted = remember(generatedSource) { highlighter.highlight(generatedSource) }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineCount = generatedSource.count { it == '\n' } + 1

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A2A1A))
                .horizontalScroll(horizontalScrollState)
        ) {
            // Line numbers gutter
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1E2B1E))
                    .verticalScroll(verticalScrollState)
                    .padding(end = 8.dp, top = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = Color(0xFF658565),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
            }

            // Generated source text (read-only, selectable)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = highlighted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                        .padding(8.dp)
                )
            }
        }

        // Scrollbars
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalScrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        )
    }
}

@Composable
private fun KotlinSourceView(kotlinSource: String) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineCount = kotlinSource.count { it == '\n' } + 1

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2A))
                .horizontalScroll(horizontalScrollState)
        ) {
            // Line numbers gutter
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1E1E2B))
                    .verticalScroll(verticalScrollState)
                    .padding(end = 8.dp, top = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = Color(0xFF656585),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
            }

            // Kotlin source text (read-only, selectable)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = kotlinSource,
                    color = Color(0xFFD4D4D4),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                        .padding(8.dp)
                )
            }
        }

        // Scrollbars
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalScrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        )
    }
}

@Composable
private fun CompletionsPopup(
    completions: List<Completion>,
    selectedIndex: Int,
    cursorOffset: Int,
    textLayoutResult: TextLayoutResult?,
    scrollOffset: Int,
    availableHeight: Int,
    onSelect: (Completion) -> Unit,
    onDismiss: () -> Unit
) {
    val popupScrollState = rememberScrollState()
    val maxVisibleItems = 8
    val itemHeightDp = 24.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // Auto-scroll to keep selected item visible
    LaunchedEffect(selectedIndex) {
        val targetScroll = (selectedIndex * itemHeightPx).toInt()
        popupScrollState.animateScrollTo(targetScroll.coerceAtLeast(0))
    }

    // Calculate position from cursor
    val gutterWidthDp = 56.dp // 48dp gutter + 8dp padding
    val gutterWidthPx = with(density) { gutterWidthDp.toPx() }
    val textPaddingPx = with(density) { 8.dp.toPx() }

    val layout = textLayoutResult
    if (layout != null && cursorOffset <= layout.layoutInput.text.length) {
        val cursorRect = layout.getCursorRect(cursorOffset.coerceAtMost(layout.layoutInput.text.length))
        val popupX = cursorRect.left + gutterWidthPx + textPaddingPx
        val cursorBottom = cursorRect.bottom - scrollOffset + textPaddingPx
        val cursorTop = cursorRect.top - scrollOffset + textPaddingPx
        val displayedItems = completions.size.coerceAtMost(maxVisibleItems)
        val popupHeightPx = itemHeightPx * displayedItems + with(density) { 2.dp.toPx() } // border

        // Decide: below cursor, or flip above if not enough space
        val popupY = if (cursorBottom + popupHeightPx <= availableHeight) {
            cursorBottom
        } else {
            (cursorTop - popupHeightPx).coerceAtLeast(0f)
        }

        val popupXDp = with(density) { popupX.toDp() }
        val popupYDp = with(density) { popupY.toDp() }

        Box(
            modifier = Modifier.padding(start = popupXDp, top = popupYDp)
        ) {
            CompletionsPopupContent(completions, selectedIndex, popupScrollState, onSelect)
        }
    } else {
        // Fallback: fixed position
        Box(
            modifier = Modifier.padding(start = 56.dp, top = 32.dp)
        ) {
            CompletionsPopupContent(completions, selectedIndex, popupScrollState, onSelect)
        }
    }
}

@Composable
private fun CompletionsPopupContent(
    completions: List<Completion>,
    selectedIndex: Int,
    scrollState: ScrollState,
    onSelect: (Completion) -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .heightIn(max = (24 * 8 + 2).dp) // max 8 visible items + border
            .background(Color(0xFF252526), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF454545), RoundedCornerShape(4.dp))
            .verticalScroll(scrollState)
    ) {
        completions.forEachIndexed { index, completion ->
            val kindColor = when (completion.kind) {
                CompletionKind.KEYWORD -> Color(0xFFD16B8E)
                CompletionKind.TYPE -> Color(0xFF5FA89F)
                CompletionKind.FUNCTION -> Color(0xFFD4A574)
                CompletionKind.VARIABLE -> Color(0xFFD9D9D9)
                CompletionKind.SNIPPET -> Color(0xFFCE9178)
                CompletionKind.PROPERTY -> Color(0xFFB06FA8)
                CompletionKind.MODULE -> Color(0xFFE6C96B)
                CompletionKind.ENUM_MEMBER -> Color(0xFF5FA89F)
            }
            val kindLabel = when (completion.kind) {
                CompletionKind.KEYWORD -> "kw"
                CompletionKind.TYPE -> "T"
                CompletionKind.FUNCTION -> "fn"
                CompletionKind.VARIABLE -> "v"
                CompletionKind.SNIPPET -> "sn"
                CompletionKind.PROPERTY -> "p"
                CompletionKind.MODULE -> "mod"
                CompletionKind.ENUM_MEMBER -> "em"
            }

            val isSelected = index == selectedIndex
            val rowBackground = if (isSelected) Color(0xFF094771) else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBackground)
                    .clickable { onSelect(completion) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = kindLabel,
                    color = kindColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = completion.label,
                    color = Color(0xFFD4D4D4),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

private class SyntaxHighlightTransformation(
    private val highlighted: AnnotatedString
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Only apply highlighting if the text matches (same source)
        val result = if (highlighted.text == text.text) highlighted else text
        return TransformedText(result, OffsetMapping.Identity)
    }
}

@Composable
private fun AzScriptFileToolbar(
    fileName: String,
    isRunning: Boolean,
    isRunnable: Boolean,
    showGeneratedSource: Boolean,
    hasErrors: Boolean,
    hasTests: Boolean,
    executionMode: ExecutionMode,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onToggleGenerated: () -> Unit,
    onRunTests: () -> Unit,
    onSetExecutionMode: (ExecutionMode) -> Unit
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
            text = fileName,
            color = palette.contentTop,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Execution mode toggle pill
        ExecutionModeToggle(
            executionMode = executionMode,
            onSelectMode = onSetExecutionMode
        )

        if (!hasErrors) {
            val toggleLabel = when (executionMode) {
                ExecutionMode.COMPILED_KT -> if (showGeneratedSource) "Hide Kotlin" else "Show Kotlin"
                ExecutionMode.COMPILED_CS -> if (showGeneratedSource) "Hide C#" else "Show C#"
                ExecutionMode.COMPILED_JS -> if (showGeneratedSource) "Hide JS" else "Show JS"
                ExecutionMode.COMPILED_LLVM -> if (showGeneratedSource) "Hide LLVM" else "Show LLVM"
                else -> if (showGeneratedSource) "Hide Generated" else "Show Generated"
            }
            val toggleColor = when (executionMode) {
                ExecutionMode.COMPILED_KT -> Color(0xFF7F52FF) // Kotlin purple
                ExecutionMode.COMPILED_CS -> Color(0xFF68217A) // C# purple
                ExecutionMode.COMPILED_JS -> Color(0xFFF7DF1E) // JS yellow
                ExecutionMode.COMPILED_LLVM -> Color(0xFF4A90D9) // LLVM blue
                else -> Color(0xFFD16FD1) // magenta
            }
            ScriptToolbarButton(
                text = toggleLabel,
                textColor = toggleColor,
                onClick = onToggleGenerated
            )
        }

        Spacer(Modifier.weight(1f))

        if (hasTests) {
            if (isRunning) {
                ScriptToolbarButton(
                    text = "Stop",
                    textColor = palette.error,
                    onClick = onStop
                )
            } else {
                ScriptToolbarButton(
                    text = "Run Tests",
                    textColor = Color(0xFF73C991),
                    onClick = onRunTests
                )
            }
        }

        if (isRunnable) {
            if (isRunning) {
                ScriptToolbarButton(
                    text = "Stop",
                    textColor = palette.error,
                    onClick = onStop
                )
            } else {
                ScriptToolbarButton(
                    text = "Run",
                    textColor = palette.success,
                    onClick = onRun
                )
            }
        }
    }
}

@Composable
private fun ExecutionModeToggle(
    executionMode: ExecutionMode,
    onSelectMode: (ExecutionMode) -> Unit
) {
    data class ModeSegment(val mode: ExecutionMode, val label: String)
    val segments = listOf(
        ModeSegment(ExecutionMode.INTERPRETED, "Interpreted"),
        ModeSegment(ExecutionMode.COMPILED_KT, "Kotlin"),
        ModeSegment(ExecutionMode.COMPILED_JS, "JS"),
        ModeSegment(ExecutionMode.COMPILED_LLVM, "LLVM"),
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2D2D2D))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (segment in segments) {
            val isSelected = executionMode == segment.mode
            Text(
                text = segment.label,
                color = if (isSelected) Color(0xFFD4D4D4) else Color(0xFF858585),
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isSelected) Color(0xFF094771) else Color.Transparent)
                    .clickable { if (!isSelected) onSelectMode(segment.mode) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ScriptToolbarButton(
    text: String,
    enabled: Boolean = true,
    textColor: Color? = null,
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

// ── Hover Tooltip ────────────────────────────────────────────────────

@Composable
internal fun HoverTooltipContent(info: GotoDefinitionInfo) {
    Box(
        modifier = Modifier
            .background(Color(0xFF252526), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF454545), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            // Line 1: Category (e.g. "Parameter", "Variable", "Loop variable")
            if (info.hoverCategory != null) {
                Text(
                    text = info.hoverCategory,
                    color = Color(0xFF858585),
                    fontSize = 12.sp
                )
            }
            // Line 2: Content (e.g. "n: Int", "var sum: Int", "flow range(n: Int): Int")
            if (info.hoverContent != null) {
                Text(
                    text = info.hoverContent,
                    color = Color(0xFFD9D9D9),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (info.declarationLine.isNotEmpty()) {
                Text(
                    text = info.declarationLine,
                    color = Color(0xFFD4D4D4),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Line 3: Location
            if (info.sourceName != null) {
                Text(text = "from ${info.sourceName}", color = Color(0xFF676767), fontSize = 10.sp)
            } else if (info.lineNumber > 0) {
                Text(text = "Line ${info.lineNumber}", color = Color(0xFF676767), fontSize = 10.sp)
            }
        }
    }
}

