package org.azora.studio.az_script

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.azora.lang.analyzer.SemanticAnalyzer
import org.azora.lang.ast.AzDecl
import org.azora.lang.ast.AzProgram
import org.azora.lang.completion.AutoCompleter
import org.azora.lang.completion.Completion
import org.azora.lang.completion.CompletionKind
import org.azora.studio.highlight.SyntaxHighlighter
import org.azora.lang.lexer.Lexer
import org.azora.lang.parser.Parser
import org.azora.lang.preprocessor.AzPreprocessor
import org.azora.lang.stdlib.AzStdlib
import org.azora.lang.token.TokenType
import org.azora.lang.codegen.csharp.CSharpCodeGenerator
import org.azora.lang.codegen.javascript.JavaScriptCodeGenerator
import org.azora.lang.codegen.kotlin.KotlinCodeGenerator
import org.azora.lang.codegen.LlvmIrCodeGenerator
import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import org.azora.studio.assets.OpenAzScriptFilesManager
import kotlinx.coroutines.*
import java.io.BufferedReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScriptDiagnostic(
    val message: String,
    val line: Int = 0,
    val source: String = "preprocessor",
    val quickFix: ScriptQuickFix? = null
)

data class ScriptQuickFix(
    val label: String,
    val insertText: String
)

data class GotoDefinitionInfo(
    val identifier: String,
    val declarationLine: String,
    val sourceName: String?,
    val lineNumber: Int,
    val hoverCategory: String? = null,
    val hoverContent: String? = null
)

enum class ExecutionMode { INTERPRETED, COMPILED_KT, COMPILED_CS, COMPILED_JS, COMPILED_LLVM }

interface KotlinRunner {
    suspend fun execute(kotlinSource: String, console: ConsoleOutputManager)
    fun stop()
}

interface CSharpRunner {
    suspend fun execute(csharpSource: String, console: ConsoleOutputManager)
    fun stop()
}

interface JavaScriptRunner {
    suspend fun execute(jsSource: String, console: ConsoleOutputManager)
    fun stop()
}

interface LlvmRunner {
    suspend fun execute(llvmIrSource: String, console: ConsoleOutputManager)
    fun stop()
}

data class AzScriptState(
    val sourceCode: String = "",
    val isRunning: Boolean = false,
    val completions: List<Completion> = emptyList(),
    val showCompletions: Boolean = false,
    val selectedCompletionIndex: Int = 0,
    val diagnostics: List<ScriptDiagnostic> = emptyList(),
    val generatedSource: String? = null,
    val showGeneratedSource: Boolean = false,
    val hasTests: Boolean = false,
    val testLines: Map<Int, String> = emptyMap(),
    val hookLines: Map<Int, String> = emptyMap(),
    val executionMode: ExecutionMode = ExecutionMode.INTERPRETED,
    val generatedKotlinSource: String? = null,
    val generatedCSharpSource: String? = null,
    val generatedJavaScriptSource: String? = null,
    val generatedLlvmIrSource: String? = null
)

class AzScriptFileViewModel(
    private val panelId: String,
    private val projectPath: String,
    private val openFilesManager: OpenAzScriptFilesManager,
    val consoleOutputManager: ConsoleOutputManager,
    private val diagnosticsManager: DiagnosticsManager
) {
    private val _state = MutableStateFlow(AzScriptState())
    val state: StateFlow<AzScriptState> = _state.asStateFlow()

    var showRuntimeWarnings: Boolean = false

    private var highlighter = SyntaxHighlighter()
    private val autoCompleter = AutoCompleter()
    private val preprocessor = AzPreprocessor()
    private val kotlinCodeGen = KotlinCodeGenerator()
    private val csharpCodeGen = CSharpCodeGenerator()
    private val jsCodeGen = JavaScriptCodeGenerator()
    private val llvmIrCodeGen = LlvmIrCodeGenerator()
    var kotlinRunner: KotlinRunner? = null
    var csharpRunner: CSharpRunner? = null
    var jsRunner: JavaScriptRunner? = null
    var llvmRunner: LlvmRunner? = null

    private val _highlightedSource = mutableStateOf(AnnotatedString(""))
    val highlightedSource: State<AnnotatedString> = _highlightedSource

    private var runProcess: Process? = null
    private var runJob: Job? = null
    private var saveJob: Job? = null
    private var exposedRefreshJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var exposedCompletions: List<Completion> = emptyList()
    private var exposedSiblingSources: String = ""
    private var exposedPrograms: List<AzProgram> = emptyList()
    private var exposedSiblingPrograms: List<AzProgram> = emptyList()
    // Map of identifier -> (declarationLine, sourceFileName) for cross-file goto-definition
    private var exposedDefinitions: Map<String, Pair<String, String>> = emptyMap()
    private var exposedTypeNames: Set<String> = emptySet()

    init {
        val fileState = openFilesManager.getState(panelId)
        if (fileState != null) {
            _state.value = _state.value.copy(sourceCode = fileState.sourceCode)
            _highlightedSource.value = highlighter.highlight(externalTypeNames = exposedTypeNames, source =fileState.sourceCode)
            val ppResult = preprocessor.process(fileState.sourceCode)
            _state.value = _state.value.copy(generatedSource = ppResult.generatedSource)
            validateScript(fileState.sourceCode)
        }
        refreshExposedCompletions()
    }

    fun onSourceChanged(newSource: String) {
        _state.value = _state.value.copy(sourceCode = newSource)
        openFilesManager.updateSource(panelId, newSource)
        _highlightedSource.value = highlighter.highlight(externalTypeNames = exposedTypeNames, source =newSource)

        // Clear console from previous run when user starts editing
        consoleOutputManager.clear()

        // Run preprocessor to keep generated source fresh
        val ppResult = preprocessor.process(newSource, stdlibSourcesFor(newSource))
        _state.value = _state.value.copy(generatedSource = ppResult.generatedSource)

        // Regenerate compiled source when in a compiled mode
        when (_state.value.executionMode) {
            ExecutionMode.COMPILED_KT -> regenerateKotlinSource()
            ExecutionMode.COMPILED_CS -> regenerateCSharpSource()
            ExecutionMode.COMPILED_JS -> regenerateJavaScriptSource()
            ExecutionMode.COMPILED_LLVM -> regenerateLlvmIrSource()
            else -> {}
        }

        validateScript(newSource)
        scheduleAutosave()
        scheduleExposedRefresh()
    }

    fun updateHighlightSettings(
        usePastel: Boolean,
        boldKeywords: Boolean,
        italicPreprocessor: Boolean = true,
        underlineVariables: Boolean = true,
    ) {
        highlighter = SyntaxHighlighter.create(usePastel, boldKeywords, italicPreprocessor, underlineVariables)
        _highlightedSource.value = highlighter.highlight(externalTypeNames = exposedTypeNames, source =_state.value.sourceCode)
    }

    fun toggleShowGeneratedSource() {
        _state.value = _state.value.copy(showGeneratedSource = !_state.value.showGeneratedSource)
    }

    fun toggleExecutionMode() {
        val current = _state.value.executionMode
        val next = when (current) {
            ExecutionMode.INTERPRETED -> ExecutionMode.COMPILED_KT
            ExecutionMode.COMPILED_KT -> ExecutionMode.COMPILED_CS
            ExecutionMode.COMPILED_CS -> ExecutionMode.COMPILED_JS
            ExecutionMode.COMPILED_JS -> ExecutionMode.COMPILED_LLVM
            ExecutionMode.COMPILED_LLVM -> ExecutionMode.INTERPRETED
        }
        _state.value = _state.value.copy(executionMode = next)
        when (next) {
            ExecutionMode.COMPILED_KT -> regenerateKotlinSource()
            ExecutionMode.COMPILED_CS -> regenerateCSharpSource()
            ExecutionMode.COMPILED_JS -> regenerateJavaScriptSource()
            ExecutionMode.COMPILED_LLVM -> regenerateLlvmIrSource()
            else -> {}
        }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        _state.value = _state.value.copy(executionMode = mode)
        when (mode) {
            ExecutionMode.COMPILED_KT -> regenerateKotlinSource()
            ExecutionMode.COMPILED_CS -> regenerateCSharpSource()
            ExecutionMode.COMPILED_JS -> regenerateJavaScriptSource()
            ExecutionMode.COMPILED_LLVM -> regenerateLlvmIrSource()
            else -> {}
        }
    }

    private fun regenerateKotlinSource() {
        try {
            val source = _state.value.sourceCode
            val ppResult = preprocessor.process(source, stdlibSourcesFor(source))
            if (ppResult.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedKotlinSource = null)
                return
            }
            val tokens = Lexer(ppResult.generatedSource).tokenize()
            val parser = Parser(tokens)
            val program = parser.parse()
            if (parser.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedKotlinSource = null)
                return
            }
            val ktSource = kotlinCodeGen.generate(program)
            _state.value = _state.value.copy(generatedKotlinSource = ktSource)
        } catch (_: Exception) {
            _state.value = _state.value.copy(generatedKotlinSource = null)
        }
    }

    private fun regenerateCSharpSource() {
        try {
            val source = _state.value.sourceCode
            val ppResult = preprocessor.process(source, stdlibSourcesFor(source))
            if (ppResult.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedCSharpSource = null)
                return
            }
            val tokens = Lexer(ppResult.generatedSource).tokenize()
            val parser = Parser(tokens)
            val program = parser.parse()
            if (parser.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedCSharpSource = null)
                return
            }
            val csSource = csharpCodeGen.generate(program)
            _state.value = _state.value.copy(generatedCSharpSource = csSource)
        } catch (_: Exception) {
            _state.value = _state.value.copy(generatedCSharpSource = null)
        }
    }

    private fun regenerateJavaScriptSource() {
        try {
            val source = _state.value.sourceCode
            val ppResult = preprocessor.process(source, stdlibSourcesFor(source))
            if (ppResult.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedJavaScriptSource = null)
                return
            }
            val tokens = Lexer(ppResult.generatedSource).tokenize()
            val parser = Parser(tokens)
            val program = parser.parse()
            if (parser.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedJavaScriptSource = null)
                return
            }
            val jsSource = jsCodeGen.generate(program)
            _state.value = _state.value.copy(generatedJavaScriptSource = jsSource)
        } catch (_: Exception) {
            _state.value = _state.value.copy(generatedJavaScriptSource = null)
        }
    }

    private fun regenerateLlvmIrSource() {
        try {
            val source = _state.value.sourceCode
            val ppResult = preprocessor.process(source, stdlibSourcesFor(source))
            if (ppResult.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedLlvmIrSource = null)
                return
            }
            val tokens = Lexer(ppResult.generatedSource).tokenize()
            val parser = Parser(tokens)
            val program = parser.parse()
            if (parser.errors.isNotEmpty()) {
                _state.value = _state.value.copy(generatedLlvmIrSource = null)
                return
            }
            val irSource = llvmIrCodeGen.generate(program)
            _state.value = _state.value.copy(generatedLlvmIrSource = irSource)
        } catch (_: Exception) {
            _state.value = _state.value.copy(generatedLlvmIrSource = null)
        }
    }

    private fun validateScript(source: String) {
        val diagnostics = mutableListOf<ScriptDiagnostic>()

        // Run preprocessor first and report CT errors
        val ppResult = preprocessor.process(source, stdlibSourcesFor(source))
        for (error in ppResult.errors) {
            diagnostics.add(ScriptDiagnostic(
                message = "Preprocessor error (line ${error.line}): ${error.message}",
                line = error.line,
                source = "preprocessor"
            ))
        }
        val processedSource = ppResult.generatedSource
        _state.value = _state.value.copy(generatedSource = processedSource)
        // Map generated-source line numbers back to original-source line numbers.
        // lineMap[i] == 0 means the generated line comes from an injected stdlib source, not the user's file.
        val lineMap = ppResult.lineMap
        fun mapLine(generatedLine: Int): Int {
            val idx = generatedLine - 1
            if (idx !in lineMap.indices) return 0  // out of bounds = injected content, not user source
            val mapped = lineMap[idx]
            return if (mapped > 0) mapped else 0   // 0 = not from user source
        }

        try {
            val tokens = Lexer(processedSource).tokenize()
            val parser = Parser(tokens)
            val program = parser.parse()
            if (parser.errors.isNotEmpty()) {
                for (error in parser.errors) {
                    val originalLine = mapLine(error.token.line)
                    if (originalLine > 0) {
                        diagnostics.add(ScriptDiagnostic(
                            message = error.message ?: "Parse error",
                            line = originalLine,
                            source = "parser"
                        ))
                    }
                }
            } else {
                // Filter exposed programs: only include stdlib modules the user imported + siblings
                val filteredExposed = stdlibProgramsFor(source) + exposedSiblingPrograms

                // Get baseline errors from exposed programs alone so we can
                // filter them out — they belong to sibling files, not this one.
                val exposedOnlyErrors = try {
                    if (filteredExposed.isNotEmpty()) {
                        val baselineResult = SemanticAnalyzer().analyze(*filteredExposed.toTypedArray())
                        baselineResult.errors.map { it.line to it.message }.toSet()
                    } else emptySet()
                } catch (_: Exception) { emptySet() }

                // Always run semantic analysis on the user's program,
                // even if exposed baseline failed
                val allPrograms = filteredExposed + program
                val result = try {
                    SemanticAnalyzer().analyze(*allPrograms.toTypedArray())
                } catch (_: Exception) {
                    // Fall back to analyzing user program alone
                    try { SemanticAnalyzer().analyze(program) } catch (_: Exception) { null }
                }
                if (result != null) {
                    for (error in result.errors) {
                        if ((error.line to error.message) !in exposedOnlyErrors) {
                            val originalLine = mapLine(error.line)
                            if (originalLine > 0) {
                                diagnostics.add(ScriptDiagnostic(
                                    message = "Type error (line ${originalLine}): ${error.message}",
                                    line = originalLine,
                                    source = "semantic"
                                ))
                            }
                        }
                    }
                    for (warning in result.warnings) {
                        val originalLine = mapLine(warning.line)
                        if (originalLine > 0) {
                            diagnostics.add(ScriptDiagnostic(
                                message = "Warning (line ${originalLine}): ${warning.message}",
                                line = originalLine,
                                source = "semantic"
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) { /* ignore lexer/parser crashes on broken source */ }

        // Detect test and event lines from the original source
        val testLineMap = mutableMapOf<Int, String>()
        val hookLineMap = mutableMapOf<Int, String>()
        try {
            val tokens = Lexer(source).tokenize()
            for (i in tokens.indices) {
                if (tokens[i].type == TokenType.TEST && i + 1 < tokens.size && tokens[i + 1].type == TokenType.STRING_LITERAL) {
                    val testName = tokens[i + 1].literal as String
                    testLineMap[tokens[i].line] = testName
                }
                if (tokens[i].type == TokenType.HOOK && i + 1 < tokens.size && tokens[i + 1].type == TokenType.IDENTIFIER) {
                    hookLineMap[tokens[i].line] = tokens[i + 1].lexeme
                }
            }
        } catch (_: Exception) { /* ignore */ }

        _state.value = _state.value.copy(
            diagnostics = diagnostics,
            hasTests = testLineMap.isNotEmpty(),
            testLines = testLineMap,
            hookLines = hookLineMap
        )

        // Update global diagnostics manager
        val fileName = openFilesManager.getState(panelId)?.fileName ?: panelId
        diagnosticsManager.updateDiagnostics(panelId, fileName, diagnostics)
    }

    fun applyQuickFix(quickFix: ScriptQuickFix): TextFieldValue {
        val source = _state.value.sourceCode
        val newText = source.trimEnd() + quickFix.insertText
        onSourceChanged(newText)
        return TextFieldValue(text = newText, selection = TextRange(newText.length))
    }

    private fun scheduleAutosave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(500)
            openFilesManager.saveFile(panelId)
        }
    }

    private fun scheduleExposedRefresh() {
        exposedRefreshJob?.cancel()
        exposedRefreshJob = scope.launch {
            delay(2000)
            refreshExposedCompletions()
        }
    }

    private fun refreshExposedCompletions() {
        scope.launch {
            val currentFilePath = openFilesManager.getState(panelId)?.filePath
            val assetsPath = "$projectPath/Assets"
            val sources = openFilesManager.readAllScriptSources(assetsPath, currentFilePath)
            val completions = mutableListOf<Completion>()
            val definitions = mutableMapOf<String, Pair<String, String>>()
            val parsedPrograms = mutableListOf<AzProgram>()

            // Include stdlib programs and sources for completions
            val stdlibPrograms = AzStdlib.loadPrograms()
            parsedPrograms.addAll(stdlibPrograms)
            val stdlibSources = mapOf(
                "std.math" to AzStdlib.sources[0],
                "std.container" to AzStdlib.sources[1],
                "std.io" to AzStdlib.sources[2]
            )
            for ((filePath, src) in stdlibSources) {
                try {
                    // Scan the RAW source to pick up declarations inside `expose scope` blocks.
                    // The preprocessor removes generic templates from output, so scanning the
                    // preprocessed source misses generic pack/func declarations like Pair<A, B>.
                    val rawTokens = Lexer(src).tokenize()
                    val fileName = filePath
                    val sourceLines = src.lines()
                    for (i in rawTokens.indices) {
                        val t = rawTokens[i]
                        // Match `expose` at top level (both `expose var/fin/func/pack` and `expose scope`)
                        if (t.type == TokenType.EXPOSE) {
                            if (i + 1 < rawTokens.size && (rawTokens[i + 1].type == TokenType.VAR || rawTokens[i + 1].type == TokenType.FIN)) {
                                if (i + 2 < rawTokens.size && rawTokens[i + 2].type == TokenType.IDENTIFIER) {
                                    val name = rawTokens[i + 2].lexeme
                                    completions.add(Completion(name, name, CompletionKind.VARIABLE))
                                    val line = rawTokens[i].line
                                    val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                    definitions[name] = declLine to fileName
                                }
                            } else if (i + 1 < rawTokens.size && rawTokens[i + 1].type == TokenType.FN) {
                                if (i + 2 < rawTokens.size && rawTokens[i + 2].type == TokenType.IDENTIFIER) {
                                    val name = rawTokens[i + 2].lexeme
                                    completions.add(Completion(name, "$name(", CompletionKind.FUNCTION))
                                    val line = rawTokens[i].line
                                    val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                    definitions[name] = declLine to fileName
                                }
                            } else if (i + 1 < rawTokens.size && rawTokens[i + 1].type in setOf(TokenType.ENUM, TokenType.SLOT, TokenType.PACK)) {
                                if (i + 2 < rawTokens.size && rawTokens[i + 2].type == TokenType.IDENTIFIER) {
                                    val name = rawTokens[i + 2].lexeme
                                    completions.add(Completion(name, name, CompletionKind.TYPE))
                                    val line = rawTokens[i].line
                                    val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                    definitions[name] = declLine to fileName
                                }
                            } else if (i + 1 < rawTokens.size && rawTokens[i + 1].type == TokenType.SCOPE) {
                                // Scan inside `expose scope Name { ... }` for declarations
                                var j = i + 2
                                while (j < rawTokens.size && rawTokens[j].type != TokenType.L_BRACE) j++
                                j++ // skip '{'
                                var depth = 1
                                while (j < rawTokens.size && depth > 0) {
                                    when (rawTokens[j].type) {
                                        TokenType.L_BRACE -> depth++
                                        TokenType.R_BRACE -> { depth--; if (depth == 0) break }
                                        else -> {}
                                    }
                                    if (depth == 1) {
                                        if (rawTokens[j].type in setOf(TokenType.PACK, TokenType.ENUM, TokenType.SLOT) &&
                                            j + 1 < rawTokens.size && rawTokens[j + 1].type == TokenType.IDENTIFIER) {
                                            val name = rawTokens[j + 1].lexeme
                                            completions.add(Completion(name, name, CompletionKind.TYPE))
                                            val line = rawTokens[j].line
                                            val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                            definitions[name] = declLine to fileName
                                        } else if (rawTokens[j].type == TokenType.FN &&
                                            j + 1 < rawTokens.size && rawTokens[j + 1].type == TokenType.IDENTIFIER) {
                                            val name = rawTokens[j + 1].lexeme
                                            completions.add(Completion(name, "$name(", CompletionKind.FUNCTION))
                                            val line = rawTokens[j].line
                                            val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                            definitions[name] = declLine to fileName
                                        }
                                    }
                                    j++
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            val siblingParsedPrograms = mutableListOf<AzProgram>()
            for ((filePath, src) in sources) {
                try {
                    val ppResult = preprocessor.process(src)
                    if (ppResult.errors.isNotEmpty()) continue
                    val tokens = Lexer(ppResult.generatedSource).tokenize()
                    val parser = Parser(tokens)
                    val prog = parser.parse()
                    if (parser.errors.isEmpty()) { parsedPrograms.add(prog); siblingParsedPrograms.add(prog) }
                    val fileName = filePath.substringAfterLast('/').removeSuffix(".az")
                    val sourceLines = src.lines()
                    for (i in tokens.indices) {
                        if (tokens[i].type == TokenType.EXPOSE) {
                            if (i + 1 < tokens.size && (tokens[i + 1].type == TokenType.VAR || tokens[i + 1].type == TokenType.FIN)) {
                                if (i + 2 < tokens.size && tokens[i + 2].type == TokenType.IDENTIFIER) {
                                    val name = tokens[i + 2].lexeme
                                    completions.add(Completion(name, name, CompletionKind.VARIABLE))
                                    val line = tokens[i].line
                                    val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                    definitions[name] = declLine to fileName
                                }
                            } else if (i + 1 < tokens.size && tokens[i + 1].type == TokenType.FN) {
                                if (i + 2 < tokens.size && tokens[i + 2].type == TokenType.IDENTIFIER) {
                                    val name = tokens[i + 2].lexeme
                                    completions.add(Completion(name, "$name(", CompletionKind.FUNCTION))
                                    val line = tokens[i].line
                                    val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                    definitions[name] = declLine to fileName
                                }
                            } else if (i + 1 < tokens.size && tokens[i + 1].type in setOf(TokenType.ENUM, TokenType.SLOT, TokenType.PACK)) {
                                if (i + 2 < tokens.size && tokens[i + 2].type == TokenType.IDENTIFIER) {
                                    val name = tokens[i + 2].lexeme
                                    completions.add(Completion(name, name, CompletionKind.TYPE))
                                    val line = tokens[i].line
                                    val declLine = sourceLines.getOrElse(line - 1) { "" }.trim()
                                    definitions[name] = declLine to fileName
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            exposedCompletions = completions.distinctBy { it.label }
            exposedTypeNames = completions
                .filter { it.kind == CompletionKind.TYPE }
                .map { it.label }
                .toSet()
            exposedDefinitions = definitions
            exposedSiblingSources = sources.values.joinToString("\n")
            exposedPrograms = parsedPrograms
            exposedSiblingPrograms = siblingParsedPrograms
            // Re-highlight with updated type names
            _highlightedSource.value = highlighter.highlight(externalTypeNames = exposedTypeNames, source = _state.value.sourceCode)
        }
    }

    fun resolveDefinition(identifier: String, cursorOffset: Int = -1): GotoDefinitionInfo? {
        val source = _state.value.sourceCode
        val hoverInfo = if (cursorOffset >= 0) {
            try { autoCompleter.getHoverInfo(source, cursorOffset) } catch (_: Throwable) { null }
        } else null
        val category = hoverInfo?.category
        val content = hoverInfo?.content

        // Find declaration line number in current file
        val lines = source.lines()
        val pattern = Regex("""(?:expose\s+)?(?:var|fin|func|flow|task|event|for)\s+$identifier\b""")
        var foundLine = 0
        for ((index, line) in lines.withIndex()) {
            if (pattern.containsMatchIn(line)) {
                foundLine = index + 1
                break
            }
        }
        // Also check parameter declarations: func name(IDENT:
        if (foundLine == 0) {
            val paramPattern = Regex("""\b$identifier\s*:""")
            for ((index, line) in lines.withIndex()) {
                if (paramPattern.containsMatchIn(line) && line.contains("(")) {
                    foundLine = index + 1
                    break
                }
            }
        }

        // Check built-in functions first
        builtinSignatures[identifier]?.let { signature ->
            return GotoDefinitionInfo(
                identifier = identifier,
                declarationLine = signature,
                sourceName = "builtin",
                lineNumber = 0,
                hoverCategory = category,
                hoverContent = content
            )
        }

        // Search exposed definitions from sibling files
        val exposed = exposedDefinitions[identifier]
        if (exposed != null) {
            return GotoDefinitionInfo(
                identifier = identifier,
                declarationLine = exposed.first,
                sourceName = exposed.second,
                lineNumber = 0,
                hoverCategory = category,
                hoverContent = content
            )
        }

        // Return hover info with line number
        if (category != null || foundLine > 0) {
            return GotoDefinitionInfo(
                identifier = identifier,
                declarationLine = "",
                sourceName = null,
                lineNumber = foundLine,
                hoverCategory = category,
                hoverContent = content
            )
        }
        return null
    }

    companion object {
        private val builtinSignatures = mapOf(
            "print" to "func print(values...): Unit\nAppends values to the current line (no newline).",
            "println" to "func println(values...): Unit\nPrints values and ends the line.",
            "delay" to "func delay(ms: Int): Unit\nPauses execution for ms milliseconds.",
            "toString" to "func toString(value): String\nConverts any value to its string representation.",
            "toInt" to "func toInt(value): Int\nConverts any value to an integer.",
            "toReal" to "func toReal(value): Real\nConverts any value to a real number.",
            "assert" to "assert condition\nassert condition { \"message\" }\nKeyword. Throws if condition is false.",
            "trace" to "trace { \"message\" }\ntrace .Level { \"message\" }\nKeyword. Structured logging. Levels: .Info .Warning .Error .Todo .Debug",
            "hasDeco" to "func hasDeco(target, decoName): Bool\nCompile-time. Checks if target has the decorator.",
            "getDeco" to "func getDeco(target, decoName, field): Any\nfunc getDeco(target, decoName::field): Any\nCompile-time. Gets a decorator field value.",
            "platform" to "func platform(scope::Member): Bool\nReturns true if the current runtime matches.",
            "infx" to "infx Type.method(param: ParamType): ReturnType { ... }\nKeyword. Infix extension function. Callable as receiver.method(arg) or receiver method arg",
            "task" to "task name(params): ReturnType { ... }\nKeyword. Declares a coroutine-capable function. Allows 'suspend' inside its body.",
            "suspend" to "suspend duration\nKeyword statement. Pauses execution for 'duration' milliseconds.\nOnly valid inside 'task' and 'hook' bodies.",
            "launch" to "launch { body }\nBuilt-in. Runs the lambda concurrently (fire-and-forget).",
            "async" to "async { body }\nBuilt-in. Runs the lambda concurrently and returns a Task.\nUse await() to get the result.",
            "await" to "func await(task): Any\nBuilt-in. Waits for an async Task to complete and returns its result.",
        )
    }

    fun run() {
        if (_state.value.isRunning) return
        when (_state.value.executionMode) {
            ExecutionMode.COMPILED_KT -> runCompiled()
            ExecutionMode.COMPILED_CS -> runCompiledCSharp()
            ExecutionMode.COMPILED_JS -> runCompiledJavaScript()
            ExecutionMode.COMPILED_LLVM -> runCompiledLlvm()
            ExecutionMode.INTERPRETED -> runInterpreted()
        }
    }

    private fun runInterpreted() {
        val filePath = openFilesManager.getState(panelId)?.filePath
        if (filePath == null) {
            consoleOutputManager.error("No file path — save the script first.")
            return
        }
        _state.value = _state.value.copy(isRunning = true)
        consoleOutputManager.clear()

        runJob = scope.launch {
            try {
                // Save before running
                openFilesManager.saveFile(panelId)
                consoleOutputManager.info("Running script...")
                runAzoraCli(listOf("run", "--script", filePath))
            } catch (e: CancellationException) {
                runProcess?.destroyForcibly()
                consoleOutputManager.info("Script stopped.")
                throw e
            } catch (e: Exception) {
                consoleOutputManager.error("Runtime error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
                runProcess = null
            }
        }
    }

    private fun runCompiled() = runWithTarget("Kotlin", "--kt") { workDir ->
        // azora build --kt only generates .kt file, need to compile & run it
        val ktFile = java.io.File(workDir, "build/kotlin-jvm/app.kt")
        if (!ktFile.exists()) {
            consoleOutputManager.error("Kotlin source not generated.")
            return@runWithTarget
        }
        _state.value = _state.value.copy(generatedKotlinSource = ktFile.readText())
        val jarFile = java.io.File(workDir, "build/kotlin-jvm/app.jar")
        consoleOutputManager.info("Compiling Kotlin...")
        runExternalProcess(listOf("kotlinc", ktFile.absolutePath, "-include-runtime", "-d", jarFile.absolutePath), workDir)
        if (jarFile.exists()) {
            consoleOutputManager.info("Running...")
            runExternalProcess(listOf("java", "-jar", jarFile.absolutePath), workDir)
        }
    }
    private fun runCompiledCSharp() = runWithTarget("C#", "--cs")
    private fun runCompiledJavaScript() = runWithTarget("JavaScript", "--js") { workDir ->
        // azora build --js generates .js, run with node
        val jsFile = workDir.resolve("build").listFiles()
            ?.firstOrNull { it.isDirectory }
            ?.listFiles()?.firstOrNull { it.extension == "js" }
        if (jsFile != null && jsFile.exists()) {
            _state.value = _state.value.copy(generatedJavaScriptSource = jsFile.readText())
            consoleOutputManager.info("Running with Node...")
            // Stub browser-only APIs so Node doesn't crash on the React/browser scaffolding
            val shimCode = "globalThis.MutationObserver=class{observe(){}disconnect(){}};" +
                "globalThis.document={getElementById:()=>null,createElement:()=>({}),querySelectorAll:()=>[]};" +
                "globalThis.window=globalThis;\n"
            val shimFile = java.io.File(workDir, "build/web-js/_run.js")
            shimFile.writeText(shimCode + jsFile.readText())
            runExternalProcess(listOf("node", shimFile.absolutePath), workDir)
        }
    }
    private fun runCompiledLlvm() = runWithTarget("LLVM", "--llvm")

    /**
     * Runs the current script via `azora run <target>`.
     * Creates a temporary azora.toml so the CLI can build & run.
     */
    private fun runWithTarget(targetName: String, targetFlag: String, postBuild: (suspend (java.io.File) -> Unit)? = null) {
        val filePath = openFilesManager.getState(panelId)?.filePath
        if (filePath == null) {
            consoleOutputManager.error("No file path — save the script first.")
            return
        }
        _state.value = _state.value.copy(isRunning = true)
        consoleOutputManager.clear()

        runJob = scope.launch {
            try {
                openFilesManager.saveFile(panelId)

                val scriptFile = java.io.File(filePath)
                val scriptDir = scriptFile.parentFile ?: java.io.File(".")

                // Create azora.toml next to the script (or in a temp dir)
                val workDir = java.io.File.createTempFile("azora_run_", "").apply { delete(); mkdirs() }
                workDir.deleteOnExit()
                val srcDir = java.io.File(workDir, "src").apply { mkdirs() }
                // Copy script to src/
                scriptFile.copyTo(java.io.File(srcDir, scriptFile.name), overwrite = true)

                val toml = java.io.File(workDir, "azora.toml")
                toml.writeText("""
[project]
name = "studio_run"
version = "0.1.0"
target = "${targetFlag.removePrefix("--")}"
entry = "${scriptFile.name}"
src = "src"
""".trimIndent())

                if (postBuild != null) {
                    consoleOutputManager.info("Building ($targetName)...")
                    runAzoraCli(listOf("build", targetFlag), workDir = workDir)
                    postBuild(workDir)
                } else {
                    consoleOutputManager.info("Building & running ($targetName)...")
                    runAzoraCli(listOf("run", targetFlag), workDir = workDir)
                }

                // Cleanup
                workDir.deleteRecursively()
            } catch (e: CancellationException) {
                runProcess?.destroyForcibly()
                consoleOutputManager.info("Stopped.")
                throw e
            } catch (e: Exception) {
                consoleOutputManager.error("Error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
                runProcess = null
            }
        }
    }

    private suspend fun loadExposedPrograms(assetsPath: String, excludeFilePath: String?, userSource: String): List<AzProgram> {
        val stdlibPrograms = stdlibProgramsFor(userSource)
        val sources = openFilesManager.readAllScriptSources(assetsPath, excludeFilePath)
        val siblingPrograms = sources.mapNotNull { (_, src) ->
            try {
                val ppResult = preprocessor.process(src)
                if (ppResult.errors.isNotEmpty()) return@mapNotNull null
                val tokens = Lexer(ppResult.generatedSource).tokenize()
                val parser = Parser(tokens)
                val prog = parser.parse()
                if (parser.errors.isEmpty()) prog else null
            } catch (_: Exception) { null }
        }
        return stdlibPrograms + siblingPrograms
    }

    fun stop() {
        runProcess?.destroyForcibly()
        kotlinRunner?.stop()
        csharpRunner?.stop()
        jsRunner?.stop()
        llvmRunner?.stop()
        runJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun runTests() {
        val filePath = openFilesManager.getState(panelId)?.filePath ?: return
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)
        consoleOutputManager.clear()

        runJob = scope.launch {
            try {
                openFilesManager.saveFile(panelId)
                consoleOutputManager.info("Running all tests...")
                runAzoraCli(listOf("test", "--script", filePath))
            } catch (e: CancellationException) {
                runProcess?.destroyForcibly()
                consoleOutputManager.info("Tests stopped.")
                throw e
            } catch (e: Exception) {
                consoleOutputManager.error("Runtime error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
                runProcess = null
            }
        }
    }

    fun runSingleTest(testName: String) {
        val filePath = openFilesManager.getState(panelId)?.filePath ?: return
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)
        consoleOutputManager.clear()

        runJob = scope.launch {
            try {
                openFilesManager.saveFile(panelId)
                consoleOutputManager.info("Running test: $testName")
                runAzoraCli(listOf("test", "--script", "--filter", testName, filePath))
            } catch (e: CancellationException) {
                runProcess?.destroyForcibly()
                consoleOutputManager.info("Test stopped.")
                throw e
            } catch (e: Exception) {
                consoleOutputManager.error("Runtime error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
                runProcess = null
            }
        }
    }

    fun triggerHook(hookName: String) {
        val filePath = openFilesManager.getState(panelId)?.filePath ?: return
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)
        consoleOutputManager.clear()

        runJob = scope.launch {
            try {
                openFilesManager.saveFile(panelId)
                consoleOutputManager.info("Triggering hook: $hookName")
                runAzoraCli(listOf("run", "--script", "--hook:$hookName", filePath))
            } catch (e: CancellationException) {
                runProcess?.destroyForcibly()
                consoleOutputManager.info("Hook stopped.")
                throw e
            } catch (e: Exception) {
                consoleOutputManager.error("Runtime error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isRunning = false)
                runProcess = null
            }
        }
    }

    private suspend fun runAzoraCli(args: List<String>, workDir: java.io.File? = null) = withContext(Dispatchers.IO) {
        val azoraHome = System.getenv("AZORA_HOME") ?: (System.getProperty("user.home") + "/.azoralang")
        val azoraBin = java.io.File(azoraHome, "bin/azora").absolutePath
        val cmd = listOf(azoraBin) + args
        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        if (workDir != null) pb.directory(workDir)
        val process = pb.start()
        runProcess = process

        process.inputStream.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (showRuntimeWarnings || !line.startsWith("WARNING:")) {
                    consoleOutputManager.info(line)
                }
                line = reader.readLine()
            }
        }

        val exitCode = process.waitFor()
        runProcess = null
        if (exitCode == 0) {
            consoleOutputManager.info("Finished (exit code 0)")
        } else {
            consoleOutputManager.error("Process exited with code $exitCode")
        }
    }

    private suspend fun runExternalProcess(cmd: List<String>, workDir: java.io.File) = withContext(Dispatchers.IO) {
        val pb = ProcessBuilder(cmd).redirectErrorStream(true).directory(workDir)
        val process = pb.start()
        runProcess = process
        process.inputStream.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (showRuntimeWarnings || !line.startsWith("WARNING:")) {
                    consoleOutputManager.info(line)
                }
                line = reader.readLine()
            }
        }
        val exitCode = process.waitFor()
        runProcess = null
        if (exitCode != 0) consoleOutputManager.error("Process exited with code $exitCode")
    }

    private fun parseSource(source: String): AzProgram? {
        val ppResult = preprocessor.process(source, stdlibSourcesFor(source))
        for (msg in ppResult.messages) {
            consoleOutputManager.info(msg.message)
        }
        if (ppResult.errors.isNotEmpty()) {
            for (error in ppResult.errors) {
                consoleOutputManager.error("Preprocessor error (line ${error.line}): ${error.message}")
            }
            _state.value = _state.value.copy(isRunning = false)
            return null
        }
        val processedSource = ppResult.generatedSource
        val tokens = Lexer(processedSource).tokenize()
        val parser = Parser(tokens)
        val program = parser.parse()
        if (parser.errors.isNotEmpty()) {
            for (error in parser.errors) {
                consoleOutputManager.error(error.message)
            }
            _state.value = _state.value.copy(isRunning = false)
            return null
        }
        return program
    }

    private fun runSemanticCheck(program: AzProgram, siblingPrograms: List<AzProgram>): Boolean {
        val analyzer = SemanticAnalyzer()
        val allPrograms = siblingPrograms + program
        val result = analyzer.analyze(*allPrograms.toTypedArray())
        for (warning in result.warnings) {
            consoleOutputManager.warn("Warning (line ${warning.line}): ${warning.message}")
        }
        if (result.hasErrors) {
            for (error in result.errors) {
                consoleOutputManager.error("Type error (line ${error.line}): ${error.message}")
            }
            return false
        }
        return true
    }

    fun requestCompletions(cursorOffset: Int) {
        val source = _state.value.sourceCode
        if (cursorOffset <= 0 || cursorOffset > source.length) {
            _state.value = _state.value.copy(showCompletions = false, completions = emptyList())
            return
        }

        // Only show completions if we're currently typing an identifier or dot shorthand
        val charBefore = source[cursorOffset - 1]
        if (!charBefore.isLetterOrDigit() && charBefore != '_' && charBefore != ':' && charBefore != '.') {
            _state.value = _state.value.copy(showCompletions = false, completions = emptyList())
            return
        }

        val filteredStdlib = stdlibSourcesFor(source).joinToString("\n")
        val filteredSources = if (filteredStdlib.isEmpty()) exposedSiblingSources
            else "$filteredStdlib\n$exposedSiblingSources"
        val completions = autoCompleter.complete(source, cursorOffset, exposedCompletions, filteredSources)
        // Don't show if the only match is exactly what's already typed
        val partial = extractPartialAtCursor(source, cursorOffset)
        val filtered = completions.filter { it.label != partial }

        _state.value = _state.value.copy(
            completions = filtered,
            showCompletions = filtered.isNotEmpty(),
            selectedCompletionIndex = 0
        )
    }

    fun dismissCompletions() {
        _state.value = _state.value.copy(showCompletions = false)
    }

    fun selectNextCompletion() {
        val s = _state.value
        if (s.completions.isEmpty()) return
        val maxIndex = s.completions.size - 1
        val next = if (s.selectedCompletionIndex >= maxIndex) 0 else s.selectedCompletionIndex + 1
        _state.value = s.copy(selectedCompletionIndex = next)
    }

    fun selectPreviousCompletion() {
        val s = _state.value
        if (s.completions.isEmpty()) return
        val maxIndex = s.completions.size - 1
        val prev = if (s.selectedCompletionIndex <= 0) maxIndex else s.selectedCompletionIndex - 1
        _state.value = s.copy(selectedCompletionIndex = prev)
    }

    fun getSelectedCompletion(): Completion? {
        val s = _state.value
        return s.completions.getOrNull(s.selectedCompletionIndex)
    }

    fun applyCompletion(completion: Completion, currentValue: TextFieldValue): TextFieldValue? {
        val source = currentValue.text
        val cursor = currentValue.selection.start

        // Find the partial identifier before cursor to replace
        val partial = extractPartialAtCursor(source, cursor)
        val replaceStart = cursor - partial.length

        // For multi-line snippets, match the current line's indentation
        val insertText = if ('\n' in completion.insertText) {
            val lineStart = source.lastIndexOf('\n', replaceStart - 1) + 1
            val indent = source.substring(lineStart, replaceStart).takeWhile { it == ' ' || it == '\t' }
            completion.insertText.replace("\n", "\n$indent")
        } else {
            completion.insertText
        }

        val newText = source.substring(0, replaceStart) +
                insertText +
                source.substring(cursor)
        val newCursor = replaceStart + insertText.length

        _state.value = _state.value.copy(
            sourceCode = newText,
            showCompletions = false
        )
        openFilesManager.updateSource(panelId, newText)
        _highlightedSource.value = highlighter.highlight(externalTypeNames = exposedTypeNames, source =newText)
        consoleOutputManager.clear()
        validateScript(newText)
        scheduleAutosave()

        return TextFieldValue(text = newText, selection = TextRange(newCursor))
    }

    private fun extractPartialAtCursor(source: String, cursor: Int): String {
        var i = cursor - 1
        while (i >= 0 && (source[i].isLetterOrDigit() || source[i] == '_')) i--
        return source.substring(i + 1, cursor)
    }

    /** Returns the subset of AzStdlib.sources whose package matches a `use` declaration in [source]. */
    private fun stdlibSourcesFor(source: String): List<String> {
        val usePackages = source.lines()
            .map { it.trim() }
            .filter { it.startsWith("use ") }
            .map { it.removePrefix("use ").trim() }
            .toSet()
        return AzStdlib.sources.filter { stdlibSrc ->
            val pkgLine = stdlibSrc.lines().firstOrNull { it.startsWith("package ") }
            val pkg = pkgLine?.removePrefix("package ")?.trim()
            pkg != null && pkg in usePackages
        }
    }

    /** Returns the subset of AzStdlib programs whose package matches a `use` declaration in [source]. */
    private fun stdlibProgramsFor(source: String): List<AzProgram> {
        val usePackages = source.lines()
            .map { it.trim() }
            .filter { it.startsWith("use ") && !it.startsWith("use scope ") }
            .map { it.removePrefix("use ").trim() }
            .toSet()
        return AzStdlib.loadPrograms().filter { prog ->
            val pkgDecl = prog.declarations.firstOrNull { it is AzDecl.PackageDecl }
            val pkg = (pkgDecl as? AzDecl.PackageDecl)?.name
            pkg != null && pkg in usePackages
        }
    }
}
