package dev.azora.nodes.domain

import dev.azora.canvas.domain.model.*
import dev.azora.canvas.domain.model.node.*
import dev.azora.canvas.domain.type.AzoraNodeType
import org.azora.lang.frontend.*

/** Result of an azora-source → node-graph conversion. */
sealed class AzToNodesResult {
    /**
     * @property graph The converted node graph.
     * @property warnings Constructs that were preserved as raw-source nodes
     *   (`AZ_CODE` / `AZ_EXPR`) or verbatim preamble instead of structured nodes.
     */
    data class Success(val graph: AzoraGraphModel, val warnings: List<String>) : AzToNodesResult()

    /** The source failed to parse — nothing was converted. */
    data class Failure(val errors: List<String>) : AzToNodesResult()
}

/**
 * Converts azora-lang source code into an [AzoraGraphModel] node graph (.azn).
 *
 * The conversion is **total and lossless in semantics**: everything the node
 * vocabulary can express becomes structured nodes (variables, `if`, `while`,
 * `for` over ranges, `when` over literals, calls, packs, enums, arithmetic,
 * comparisons, …); anything else is preserved verbatim —
 * - unsupported *top-level* declarations (solo/node/impl/bridge/annotated
 *   functions, …) are carried as raw source in `meta["preamble"]`,
 * - unsupported *statements/expressions* inside a function become `AZ_CODE` /
 *   `AZ_EXPR` nodes holding printed source.
 *
 * Generating .az back from the result ([NodesToAzConverter]) yields a program
 * with the same behavior. `func main` becomes the `START` chain; other plain
 * functions become `FUNCTION_DEF` chains.
 */
class AzToNodesConverter {

    fun convert(source: String, graphName: String): AzToNodesResult {
        val program = try {
            Parser(Lexer(source).tokenize()).parse()
        } catch (e: Exception) {
            return AzToNodesResult.Failure(listOf(e.message ?: "Failed to parse azora source."))
        }
        return Builder(source, graphName).build(program)
    }

    // =====================================================================

    private class Builder(private val source: String, private val graphName: String) {

        private val warnings = mutableListOf<String>()
        private val sourceLines = source.lines()

        private val nodes = linkedMapOf<String, AzoraNodeModel>()
        private val execLinks = linkedMapOf<String, AzoraExecLinkModel>()
        private val dataLinks = linkedMapOf<String, AzoraDataLinkModel>()
        private val variables = linkedMapOf<String, AzoraNodeVar>()
        private val functions = linkedMapOf<String, AzoraNodeFunction>()
        private val enums = linkedMapOf<String, AzoraNodeEnum>()
        private val dataClasses = linkedMapOf<String, AzoraNodeDataClass>()

        private var nodeCounter = 0
        private var linkCounter = 0
        private var varCounter = 0

        /** pack name → classId, enum name → enumId, func name → functionId. */
        private val packIds = mutableMapOf<String, String>()
        private val enumIds = mutableMapOf<String, String>()
        private val funcIds = mutableMapOf<String, String>()
        private val funcDecls = mutableMapOf<String, FuncDecl>()

        // -------------------------------------------------------------

        fun build(program: Program): AzToNodesResult {
            val preamble = mutableListOf<String>()
            val imports = mutableListOf<String>()
            val structuredFuncs = mutableListOf<FuncDecl>()

            val items = program.items
            for ((index, item) in items.withIndex()) {
                val slice = { sliceItem(items, index) }
                when (item) {
                    is TopLevel.UseImport -> imports.add(slice().trim())
                    is TopLevel.Enum -> registerEnum(item)
                    is TopLevel.Pack -> {
                        if (isSimplePack(item)) registerPack(item)
                        else {
                            warnings.add("pack ${item.name} has non-primitive fields or defaults — kept as raw source.")
                            preamble.add(slice())
                        }
                    }
                    is TopLevel.Func -> {
                        if (isStructuredFunc(item.decl)) structuredFuncs.add(item.decl)
                        else {
                            warnings.add("func ${item.decl.name} uses features without a node representation — kept as raw source.")
                            preamble.add(slice())
                        }
                    }
                    is TopLevel.VarDecl -> registerTopLevelVar(item.name, item.type, item.initializer, readonly = false) { preamble.add(slice()) }
                    is TopLevel.LetDecl -> registerTopLevelVar(item.name, item.type, item.initializer, readonly = true) { preamble.add(slice()) }
                    is TopLevel.FinDecl -> registerTopLevelVar(item.name, item.type, item.initializer, readonly = true) { preamble.add(slice()) }
                    else -> {
                        warnings.add("Top-level ${item::class.simpleName} declaration kept as raw source.")
                        preamble.add(slice())
                    }
                }
            }

            // Register structured function signatures before converting bodies
            // (so calls resolve regardless of declaration order).
            for (decl in structuredFuncs) {
                if (decl.name == "main") continue
                val id = "f${funcIds.size}"
                funcIds[decl.name] = id
                funcDecls[decl.name] = decl
                functions[id] = AzoraNodeFunction(
                    id = id,
                    name = decl.name,
                    parameters = decl.params.map { param ->
                        AzoraNodeFunctionParam(
                            id = "p_${id}_${param.name}",
                            name = param.name,
                            type = dataTypeOf(param.type) ?: AzoraNodeDataType.ANY
                        )
                    },
                    returnType = (decl.returnType as? TypeAnnotation.Explicit)?.ref
                        ?.let { dataTypeOf(it) }
                )
            }

            // Definition nodes (left column) for enums and packs.
            var defY = 60f
            for (enum in enums.values) {
                addNode(AzoraNodeType.ENUM_DEF, "Enum ${enum.name}", 40f, defY, mapOf(
                    "enumId" to enum.id, "name" to enum.name, "values" to enum.values.joinToString(",")
                ))
                defY += 150f
            }
            for (dataClass in dataClasses.values) {
                addNode(AzoraNodeType.DATA_CLASS_DEF, "Pack ${dataClass.name}", 40f, defY, mapOf(
                    "classId" to dataClass.id, "name" to dataClass.name
                ))
                defY += 150f
            }

            // Function chains — main (START) first, then the rest.
            var rowY = 120f
            val ordered = structuredFuncs.sortedBy { if (it.name == "main") 0 else 1 }
            for (decl in ordered) {
                val converter = FunctionConverter(decl, rowY)
                converter.convert()
                rowY = converter.nextRowY()
            }

            val meta = buildMap {
                if (imports.isNotEmpty()) put("imports", imports.joinToString("\n"))
                if (preamble.isNotEmpty()) put("preamble", preamble.joinToString("\n\n") { it.trimEnd() })
            }

            val graph = AzoraGraphModel(
                id = graphName,
                name = graphName,
                nodes = nodes,
                execLinks = execLinks,
                dataLinks = dataLinks,
                variables = variables,
                functions = functions,
                enums = enums,
                dataClasses = dataClasses,
                meta = meta,
            )
            return AzToNodesResult.Success(graph, warnings)
        }

        // -------------------------------------------------------------
        // Top-level registration
        // -------------------------------------------------------------

        private fun registerEnum(item: TopLevel.Enum) {
            val id = "enum${enums.size}"
            enumIds[item.name] = id
            enums[id] = AzoraNodeEnum(id = id, name = item.name, values = item.variants)
        }

        private fun isSimplePack(item: TopLevel.Pack): Boolean =
            item.typeParams.isEmpty() && item.annotations.isEmpty() && item.fields.all { field ->
                field.default == null && dataTypeOf(field.type) != null
            }

        private fun registerPack(item: TopLevel.Pack) {
            val id = "pack${dataClasses.size}"
            packIds[item.name] = id
            dataClasses[id] = AzoraNodeDataClass(
                id = id,
                name = item.name,
                fields = item.fields.map { field ->
                    AzoraNodeDataClassField(field.name, dataTypeOf(field.type) ?: AzoraNodeDataType.ANY)
                }
            )
        }

        /** A function is structured when its whole signature maps onto [AzoraNodeFunction]. */
        private fun isStructuredFunc(decl: FuncDecl): Boolean {
            if (decl.annotations.isNotEmpty() || decl.isInline || decl.isFlow || decl.isOverride || decl.isVirtual) return false
            if (decl.typeParams.isNotEmpty()) return false
            if (decl.name == "main" && decl.params.isNotEmpty()) return false
            val paramsOk = decl.params.all {
                it.modifier.isEmpty() && !it.variadic && it.defaultValue == null && dataTypeOf(it.type) != null
            }
            if (!paramsOk) return false
            val ret = decl.returnType
            return when (ret) {
                is TypeAnnotation.Explicit -> {
                    val name = (ret.ref as? TypeRef.Named)?.name
                    name == "Unit" || dataTypeOf(ret.ref) != null
                }
                TypeAnnotation.Inferred -> true
            }
        }

        private fun registerTopLevelVar(
            name: String,
            type: TypeRef?,
            initializer: Expr,
            readonly: Boolean,
            fallback: () -> Unit
        ) {
            val literal = literalText(initializer)
            val dataType = type?.let { dataTypeOf(it) } ?: literalDataType(initializer)
            if (literal == null || dataType == null) {
                warnings.add("Top-level binding \"$name\" has a non-literal initializer or unsupported type — kept as raw source.")
                fallback()
                return
            }
            val id = "v${varCounter++}_$name"
            variables[id] = AzoraNodeVar(id = id, name = name, type = dataType, defaultValue = literal, readonly = readonly)
        }

        /** Original source lines of top-level item [index] (declaration granularity). */
        private fun sliceItem(items: List<TopLevel>, index: Int): String {
            val start = itemStartLine(items[index])
            val end = items.getOrNull(index + 1)?.let { itemStartLine(it) - 1 } ?: sourceLines.size
            if (start <= 0 || start > sourceLines.size) return ""
            return sourceLines.subList(start - 1, end.coerceIn(start - 1, sourceLines.size))
                .joinToString("\n")
                .trimEnd()
        }

        private fun itemStartLine(item: TopLevel): Int = when (item) {
            is TopLevel.Func -> minOf(item.decl.line, item.decl.annotations.minOfOrNull { it.line } ?: item.decl.line)
            is TopLevel.Pack -> minOf(item.line, item.annotations.minOfOrNull { it.line } ?: item.line)
            is TopLevel.VarDecl -> minOf(item.line, item.annotations.minOfOrNull { it.line } ?: item.line)
            is TopLevel.FinDecl -> minOf(item.line, item.annotations.minOfOrNull { it.line } ?: item.line)
            is TopLevel.LetDecl -> minOf(item.line, item.annotations.minOfOrNull { it.line } ?: item.line)
            is TopLevel.Enum -> item.line
            is TopLevel.Test -> item.line
            is TopLevel.UseImport -> item.line
            is TopLevel.InlineVar -> item.line
            is TopLevel.InlineFin -> item.line
            is TopLevel.InlineLet -> item.line
            is TopLevel.InlineAssignment -> item.line
            is TopLevel.InlineIf -> item.line
            is TopLevel.InlineBlock -> item.line
            is TopLevel.DeepInlineBlock -> item.line
            is TopLevel.DeepInlineIf -> item.line
            is TopLevel.Deco -> item.line
            is TopLevel.Bridge -> item.line
            is TopLevel.Solo -> item.line
            is TopLevel.Node -> item.line
            is TopLevel.Wrap -> item.line
            is TopLevel.View -> item.line
            is TopLevel.Hook -> item.line
            is TopLevel.Fail -> item.line
            is TopLevel.Impl -> item.line
            is TopLevel.InlineAssert -> item.line
            is TopLevel.InlineTrace -> item.line
            is TopLevel.Slot -> item.line
            is TopLevel.Spec -> item.line
            is TopLevel.TypeAlias -> item.line
        }

        // -------------------------------------------------------------
        // Graph primitives
        // -------------------------------------------------------------

        private fun addNode(
            type: AzoraNodeType,
            title: String,
            x: Float,
            y: Float,
            properties: Map<String, String> = emptyMap()
        ): String {
            val id = "n${nodeCounter++}"
            nodes[id] = AzoraNodeModel(
                id = id,
                title = title,
                type = type,
                positionX = x,
                positionY = y,
                width = AzoraNodeModel.calculateWidth(title),
                properties = properties
            )
            return id
        }

        private fun setNodeProperty(nodeId: String, key: String, value: String) {
            val node = nodes[nodeId] ?: return
            nodes[nodeId] = node.copy(properties = node.properties + (key to value))
        }

        private fun addExecLink(sourceNodeId: String, sourcePortIndex: Int, targetNodeId: String) {
            val id = "e${linkCounter++}"
            execLinks[id] = AzoraExecLinkModel(
                id = id,
                sourceNodeId = sourceNodeId,
                sourcePortIndex = sourcePortIndex,
                targetNodeId = targetNodeId
            )
        }

        private fun addDataLink(sourceNodeId: String, sourcePortName: String, targetNodeId: String, targetPortName: String) {
            val id = "d${linkCounter++}"
            dataLinks[id] = AzoraDataLinkModel(
                id = id,
                sourceNodeId = sourceNodeId,
                sourcePortName = sourcePortName,
                targetNodeId = targetNodeId,
                targetPortName = targetPortName
            )
        }

        // -------------------------------------------------------------
        // Function bodies
        // -------------------------------------------------------------

        /** A pending exec output waiting to be linked to the next statement node. */
        private data class ExecSrc(val nodeId: String, val portIndex: Int)

        /** What a name in scope resolves to when read. */
        private sealed class Binding {
            data class GraphVar(val varId: String, val packName: String?) : Binding()
            data class Counter(val nodeId: String) : Binding()
            data class Param(val name: String, val packName: String?) : Binding()
            /** Declared inside an AZ_CODE fallback — only referencable by name. */
            data class Raw(val name: String) : Binding()
        }

        /** How a data input gets its value: an inline literal or a producing port. */
        private sealed class InputValue {
            data class Lit(val text: String) : InputValue()
            data class Port(val nodeId: String, val portName: String) : InputValue()
        }

        private inner class FunctionConverter(private val decl: FuncDecl, private val rowY: Float) {

            private val scope = mutableMapOf<String, Binding>()
            private var cursorX = 240f
            private var maxDepth = 0
            private var pureSlot = 0

            fun nextRowY(): Float = rowY + 260f + maxDepth * 170f

            fun convert() {
                val isMain = decl.name == "main"
                val rootId = if (isMain) {
                    addNode(AzoraNodeType.START, "Start", 60f, rowY)
                } else {
                    val functionId = funcIds[decl.name] ?: return
                    for (param in decl.params) {
                        scope[param.name] = Binding.Param(param.name, packNameOf(param.type))
                    }
                    addNode(
                        AzoraNodeType.FUNCTION_DEF, "Func ${decl.name}", 60f, rowY,
                        mapOf("functionId" to functionId, "name" to decl.name)
                    )
                }
                val open = convertBlock(decl.body, mutableListOf(ExecSrc(rootId, 0)), depth = 0)
                // Open ends simply stop executing — same as azora falling off the end.
                open.clear()
            }

            /**
             * Converts [stmts] into exec nodes, linking the first one from every
             * source in [frontier]. Returns the open exec ends after the last
             * statement (empty when the block ends in `return`).
             */
            private fun convertBlock(
                stmts: List<Stmt>,
                frontier: MutableList<ExecSrc>,
                depth: Int
            ): MutableList<ExecSrc> {
                if (depth > maxDepth) maxDepth = depth
                var open = frontier
                for (stmt in stmts) {
                    if (open.isEmpty()) {
                        warnings.add("Unreachable code after return in ${decl.name} was dropped.")
                        break
                    }
                    open = convertStmt(stmt, open, depth)
                }
                return open
            }

            private fun convertStmt(stmt: Stmt, frontier: MutableList<ExecSrc>, depth: Int): MutableList<ExecSrc> {
                return when (stmt) {
                    is Stmt.VarDecl -> convertDecl(stmt.name, stmt.type, stmt.initializer, readonly = false, frontier, depth)
                    is Stmt.LetDecl -> convertDecl(stmt.name, stmt.type, stmt.initializer, readonly = true, frontier, depth)
                    is Stmt.FinDecl -> convertDecl(stmt.name, stmt.type, stmt.initializer, readonly = true, frontier, depth)

                    is Stmt.Assignment -> {
                        val binding = scope[stmt.name]
                        if (binding is Binding.GraphVar) {
                            val value = convertExpr(stmt.value, frontier, depth)
                            val nodeId = execNode(AzoraNodeType.SET_VARIABLE, "Set ${stmt.name}", frontier, depth,
                                mapOf("variableId" to binding.varId))
                            wireInput(nodeId, "value", value)
                            mutableListOf(ExecSrc(nodeId, 0))
                        } else {
                            fallbackStmt(stmt, frontier, depth)
                        }
                    }

                    is Stmt.MemberAssign -> convertMemberAssign(stmt, frontier, depth)

                    is Stmt.ExprStmt -> convertExprStmt(stmt, frontier, depth)

                    is Stmt.Return -> {
                        val value = stmt.value?.let { convertExpr(it, frontier, depth) }
                        val nodeId = execNode(AzoraNodeType.FUNCTION_RETURN, "Return", frontier, depth)
                        value?.let { wireInput(nodeId, "value", it) }
                        mutableListOf() // chain closed
                    }

                    is Stmt.If -> {
                        val condition = convertExpr(stmt.condition, frontier, depth)
                        val nodeId = execNode(AzoraNodeType.IF, "If", frontier, depth)
                        wireInput(nodeId, "condition", condition)
                        val thenOpen = convertBlock(stmt.thenBranch, mutableListOf(ExecSrc(nodeId, 0)), depth + 1)
                        val elseOpen = if (stmt.elseBranch != null) {
                            convertBlock(stmt.elseBranch!!, mutableListOf(ExecSrc(nodeId, 1)), depth + 1)
                        } else {
                            mutableListOf(ExecSrc(nodeId, 1))
                        }
                        (thenOpen + elseOpen).toMutableList()
                    }

                    is Stmt.While -> {
                        // Loop conditions re-evaluate every iteration, so a condition
                        // with side effects (calls) cannot be represented by a data port.
                        if (stmt.label != null || containsCall(stmt.condition)) {
                            fallbackStmt(stmt, frontier, depth)
                        } else {
                            val condition = convertExpr(stmt.condition, frontier, depth)
                            val nodeId = execNode(AzoraNodeType.WHILE, "While", frontier, depth)
                            wireInput(nodeId, "condition", condition)
                            convertBlock(stmt.body, mutableListOf(ExecSrc(nodeId, 0)), depth + 1).clear()
                            mutableListOf(ExecSrc(nodeId, 1))
                        }
                    }

                    is Stmt.For -> convertFor(stmt, frontier, depth)

                    is Stmt.When -> convertWhen(stmt, frontier, depth)

                    else -> fallbackStmt(stmt, frontier, depth)
                }
            }

            private fun convertDecl(
                name: String,
                type: TypeAnnotation,
                initializer: Expr,
                readonly: Boolean,
                frontier: MutableList<ExecSrc>,
                depth: Int
            ): MutableList<ExecSrc> {
                val explicitType = (type as? TypeAnnotation.Explicit)?.ref
                val dataType = explicitType?.let { dataTypeOf(it) }
                    ?: literalDataType(initializer)
                    ?: inferDataType(initializer)
                val packName = explicitType?.let { packNameOf(it) } ?: ctorPackName(initializer)

                val uniqueName = if (scope.containsKey(name)) {
                    val alt = "${name}_${varCounter}"
                    warnings.add("Variable \"$name\" shadows an earlier one — renamed to \"$alt\" in the graph.")
                    alt
                } else name

                val varId = "v${varCounter++}_$uniqueName"
                val literal = literalText(initializer)
                if (literal != null && dataType != null && packName == null) {
                    // Literal initializer folds into the variable's default value.
                    variables[varId] = AzoraNodeVar(varId, uniqueName, dataType, defaultValue = literal, readonly = readonly)
                    scope[name] = Binding.GraphVar(varId, null)
                    return frontier
                }

                variables[varId] = AzoraNodeVar(
                    varId, uniqueName,
                    dataType ?: AzoraNodeDataType.ANY,
                    defaultValue = "",
                    readonly = readonly
                )
                scope[name] = Binding.GraphVar(varId, packName)
                val value = convertExpr(initializer, frontier, depth)
                val nodeId = execNode(AzoraNodeType.SET_VARIABLE, "Set $uniqueName", frontier, depth,
                    mapOf("variableId" to varId))
                wireInput(nodeId, "value", value)
                return mutableListOf(ExecSrc(nodeId, 0))
            }

            private fun convertMemberAssign(stmt: Stmt.MemberAssign, frontier: MutableList<ExecSrc>, depth: Int): MutableList<ExecSrc> {
                val target = stmt.target
                val packName = (target as? Expr.Identifier)?.let { packNameOfBinding(scope[it.name]) }
                val classId = packName?.let { packIds[it] }
                val fieldExists = classId != null && dataClasses[classId]?.fields?.any { it.name == stmt.name } == true
                if (classId == null || !fieldExists) return fallbackStmt(stmt, frontier, depth)

                val instance = convertExpr(target, frontier, depth)
                val value = convertExpr(stmt.value, frontier, depth)
                val nodeId = execNode(AzoraNodeType.DATA_CLASS_SET_FIELD, "Set .${stmt.name}", frontier, depth,
                    mapOf("classId" to classId, "fieldName" to stmt.name))
                wireInput(nodeId, "instance", instance)
                wireInput(nodeId, "value", value)
                return mutableListOf(ExecSrc(nodeId, 0))
            }

            private fun convertExprStmt(stmt: Stmt.ExprStmt, frontier: MutableList<ExecSrc>, depth: Int): MutableList<ExecSrc> {
                val expr = stmt.expr
                if (expr is Expr.Call) {
                    // println / print → PRINT node
                    if ((expr.callee == "println" || expr.callee == "print") && expr.args.size == 1 &&
                        expr.args[0] !is Expr.NamedArg
                    ) {
                        val value = convertExpr(expr.args[0], frontier, depth)
                        val nodeId = execNode(AzoraNodeType.PRINT, "Print", frontier, depth)
                        wireInput(nodeId, "value", value)
                        return mutableListOf(ExecSrc(nodeId, 0))
                    }
                    // Pack constructor as a bare statement has no effect — still convert.
                    val callNode = hoistCall(expr, frontier, depth)
                    if (callNode != null) return mutableListOf(ExecSrc(callNode, 0))
                }
                return fallbackStmt(stmt, frontier, depth)
            }

            private fun convertFor(stmt: Stmt.For, frontier: MutableList<ExecSrc>, depth: Int): MutableList<ExecSrc> {
                val range = stmt.iterable as? Expr.Range
                if (range == null || stmt.step != null || stmt.reverse || stmt.label != null) {
                    return fallbackStmt(stmt, frontier, depth)
                }
                val from = convertExpr(range.from, frontier, depth)
                val to = convertExpr(range.to, frontier, depth)
                val nodeId = execNode(
                    AzoraNodeType.FOR_RANGE, "For ${stmt.name}", frontier, depth,
                    mapOf("counter" to stmt.name, "inclusive" to range.inclusive.toString())
                )
                wireInput(nodeId, "from", from)
                wireInput(nodeId, "to", to)

                val outer = scope[stmt.name]
                scope[stmt.name] = Binding.Counter(nodeId)
                convertBlock(stmt.body, mutableListOf(ExecSrc(nodeId, 0)), depth + 1).clear()
                if (outer != null) scope[stmt.name] = outer else scope.remove(stmt.name)

                return mutableListOf(ExecSrc(nodeId, 1))
            }

            private fun convertWhen(stmt: Stmt.When, frontier: MutableList<ExecSrc>, depth: Int): MutableList<ExecSrc> {
                // Every pattern must be a literal the MATCH node can compare by display string.
                val branchPatterns = stmt.branches.map { branch ->
                    branch.patterns.map { patternText(it) ?: return fallbackStmt(stmt, frontier, depth) }
                }

                val scrutinee = convertExpr(stmt.scrutinee, frontier, depth)
                val caseCount = branchPatterns.sumOf { it.size }
                val properties = mutableMapOf("caseCount" to caseCount.toString())
                var caseIndex = 0
                for (patterns in branchPatterns) {
                    for (pattern in patterns) properties["case_${caseIndex++}"] = pattern
                }
                val nodeId = execNode(AzoraNodeType.MATCH, "Match", frontier, depth, properties)
                wireInput(nodeId, "value", scrutinee)

                val open = mutableListOf<ExecSrc>()
                caseIndex = 0
                for ((i, branch) in stmt.branches.withIndex()) {
                    val ports = branchPatterns[i].indices.map { caseIndex++ }
                    val heads = ports.map { ExecSrc(nodeId, it) }.toMutableList()
                    open += convertBlock(branch.body, heads, depth + 1)
                }
                if (stmt.elseBranch != null) {
                    open += convertBlock(stmt.elseBranch!!, mutableListOf(ExecSrc(nodeId, caseCount)), depth + 1)
                } else {
                    open += ExecSrc(nodeId, caseCount)
                }
                return open
            }

            /** Whole statement preserved verbatim as an AZ_CODE node. */
            private fun fallbackStmt(stmt: Stmt, frontier: MutableList<ExecSrc>, depth: Int): MutableList<ExecSrc> {
                registerRawDeclarations(stmt)
                val code = AzSourcePrinter.printStmt(stmt)
                val nodeId = execNode(AzoraNodeType.AZ_CODE, "Azora Code", frontier, depth, mapOf("code" to code))
                return mutableListOf(ExecSrc(nodeId, 0))
            }

            /** Names declared inside a raw block stay referencable by later expressions. */
            private fun registerRawDeclarations(stmt: Stmt) {
                when (stmt) {
                    is Stmt.VarDecl -> scope[stmt.name] = Binding.Raw(stmt.name)
                    is Stmt.LetDecl -> scope[stmt.name] = Binding.Raw(stmt.name)
                    is Stmt.FinDecl -> scope[stmt.name] = Binding.Raw(stmt.name)
                    else -> {}
                }
            }

            // ---------------------------------------------------------
            // Expressions
            // ---------------------------------------------------------

            private fun convertExpr(expr: Expr, frontier: MutableList<ExecSrc>, depth: Int): InputValue {
                literalText(expr)?.let { return InputValue.Lit(it) }

                return when (expr) {
                    is Expr.Grouping -> convertExpr(expr.expr, frontier, depth)

                    is Expr.Identifier -> when (val binding = scope[expr.name]) {
                        is Binding.GraphVar -> {
                            val variable = variables[binding.varId]
                            val nodeId = pureNode(AzoraNodeType.GET_VARIABLE, "Get ${variable?.name ?: expr.name}", depth,
                                mapOf("variableId" to binding.varId))
                            InputValue.Port(nodeId, "value")
                        }
                        is Binding.Counter -> InputValue.Port(binding.nodeId, "index")
                        is Binding.Param -> {
                            val nodeId = pureNode(AzoraNodeType.PARAM_GET, expr.name, depth, mapOf("name" to expr.name))
                            InputValue.Port(nodeId, "value")
                        }
                        is Binding.Raw, null -> rawExpr(expr, depth)
                    }

                    is Expr.Binary -> {
                        val type = binaryNodeType(expr.op) ?: return rawExpr(expr, frontier, depth)
                        val a = convertExpr(expr.left, frontier, depth)
                        val b = convertExpr(expr.right, frontier, depth)
                        val nodeId = pureNode(type, type.label, depth)
                        wireInput(nodeId, "a", a)
                        wireInput(nodeId, "b", b)
                        InputValue.Port(nodeId, "result")
                    }

                    is Expr.Unary -> when (expr.op) {
                        TokenType.BANG -> {
                            val value = convertExpr(expr.operand, frontier, depth)
                            val nodeId = pureNode(AzoraNodeType.NOT, "Not", depth)
                            wireInput(nodeId, "value", value)
                            InputValue.Port(nodeId, "result")
                        }
                        TokenType.MINUS -> {
                            val value = convertExpr(expr.operand, frontier, depth)
                            val nodeId = pureNode(AzoraNodeType.NEGATE, "Negate", depth)
                            wireInput(nodeId, "value", value)
                            InputValue.Port(nodeId, "result")
                        }
                        else -> rawExpr(expr, frontier, depth)
                    }

                    is Expr.Call -> {
                        // Pack constructor → pure Create node.
                        val classId = packIds[expr.callee]
                        if (classId != null) {
                            val dataClass = dataClasses[classId]!!
                            if (expr.args.size == dataClass.fields.size && expr.args.none { it is Expr.NamedArg }) {
                                val args = expr.args.map { convertExpr(it, frontier, depth) }
                                val nodeId = pureNode(AzoraNodeType.DATA_CLASS_CREATE, "Create ${dataClass.name}", depth,
                                    mapOf("classId" to classId))
                                for ((i, field) in dataClass.fields.withIndex()) {
                                    wireInput(nodeId, "field_${field.name}", args[i])
                                }
                                return InputValue.Port(nodeId, "instance")
                            }
                        }
                        val callNode = hoistCall(expr, frontier, depth) ?: return rawExpr(expr, frontier, depth)
                        InputValue.Port(callNode, "result")
                    }

                    is Expr.Member -> {
                        // Enum value `Color.Red`
                        val enumId = (expr.target as? Expr.Identifier)?.name?.let { enumIds[it] }
                        if (enumId != null && enums[enumId]?.values?.contains(expr.name) == true) {
                            val nodeId = pureNode(AzoraNodeType.ENUM_VALUE, "${enums[enumId]!!.name}.${expr.name}", depth,
                                mapOf("enumId" to enumId, "value" to expr.name))
                            return InputValue.Port(nodeId, "value")
                        }
                        // Pack field read `p.x`
                        val packName = (expr.target as? Expr.Identifier)?.let { packNameOfBinding(scope[it.name]) }
                        val classId = packName?.let { packIds[it] }
                        if (classId != null && dataClasses[classId]?.fields?.any { it.name == expr.name } == true) {
                            val instance = convertExpr(expr.target, frontier, depth)
                            val nodeId = pureNode(AzoraNodeType.DATA_CLASS_GET_FIELD, "Get .${expr.name}", depth,
                                mapOf("classId" to classId, "fieldName" to expr.name))
                            wireInput(nodeId, "instance", instance)
                            return InputValue.Port(nodeId, "value")
                        }
                        rawExpr(expr, frontier, depth)
                    }

                    is Expr.Cast -> {
                        val toType = (expr.targetType as? TypeRef.Named)?.name?.let {
                            when (it) {
                                "Int" -> "INTEGER"; "Real" -> "REAL"; "String" -> "STRING"; "Bool" -> "BOOLEAN"
                                else -> null
                            }
                        } ?: return rawExpr(expr, frontier, depth)
                        val value = convertExpr(expr.expr, frontier, depth)
                        val nodeId = pureNode(AzoraNodeType.CAST, "Cast", depth, mapOf("toType" to toType))
                        wireInput(nodeId, "value", value)
                        InputValue.Port(nodeId, "result")
                    }

                    else -> rawExpr(expr, frontier, depth)
                }
            }

            /**
             * Emits a call as an exec node (calls only run on the exec path) and
             * returns its node id, or `null` when the call shape is unsupported.
             */
            private fun hoistCall(expr: Expr.Call, frontier: MutableList<ExecSrc>, depth: Int): String? {
                if (expr.args.any { it is Expr.NamedArg || it is Expr.Spread }) return null
                val functionId = funcIds[expr.callee]
                return if (functionId != null) {
                    val funcDecl = funcDecls[expr.callee]!!
                    if (expr.args.size != funcDecl.params.size) return null
                    val args = expr.args.map { convertExpr(it, frontier, depth) }
                    val nodeId = execNode(AzoraNodeType.FUNCTION_CALL, "Call ${expr.callee}", frontier, depth,
                        mapOf("functionId" to functionId))
                    for ((i, param) in funcDecl.params.withIndex()) {
                        wireInput(nodeId, "param_${param.name}", args[i])
                    }
                    frontier.clear(); frontier.add(ExecSrc(nodeId, 0))
                    nodeId
                } else {
                    // External function (engine/stdlib) → AZ_CALL by name.
                    val args = expr.args.map { convertExpr(it, frontier, depth) }
                    val nodeId = execNode(AzoraNodeType.AZ_CALL, "Call ${expr.callee}", frontier, depth,
                        mapOf("name" to expr.callee, "argCount" to expr.args.size.toString()))
                    for ((i, arg) in args.withIndex()) {
                        wireInput(nodeId, "arg_$i", arg)
                    }
                    frontier.clear(); frontier.add(ExecSrc(nodeId, 0))
                    nodeId
                }
            }

            /** Expression preserved verbatim as a pure AZ_EXPR node. */
            private fun rawExpr(expr: Expr, depth: Int): InputValue = rawExpr(expr, null, depth)

            private fun rawExpr(expr: Expr, frontier: MutableList<ExecSrc>?, depth: Int): InputValue {
                val code = AzSourcePrinter.printExpr(expr)
                val title = if (code.length <= 24) code else code.take(21) + "…"
                val nodeId = pureNode(AzoraNodeType.AZ_EXPR, title, depth, mapOf("code" to code))
                return InputValue.Port(nodeId, "value")
            }

            // ---------------------------------------------------------
            // Node placement & wiring
            // ---------------------------------------------------------

            /** Adds an exec node at the flow cursor and links it from [frontier]. */
            private fun execNode(
                type: AzoraNodeType,
                title: String,
                frontier: MutableList<ExecSrc>,
                depth: Int,
                properties: Map<String, String> = emptyMap()
            ): String {
                val nodeId = addNode(type, title, cursorX, rowY + depth * 170f, properties)
                cursorX += 280f
                pureSlot = 0
                for (src in frontier) addExecLink(src.nodeId, src.portIndex, nodeId)
                return nodeId
            }

            /** Adds a pure data node below-left of the upcoming consumer. */
            private fun pureNode(
                type: AzoraNodeType,
                title: String,
                depth: Int,
                properties: Map<String, String> = emptyMap()
            ): String {
                val x = cursorX - 60f
                val y = rowY + depth * 170f + 150f + pureSlot * 110f
                pureSlot++
                return addNode(type, title, x, y, properties)
            }

            private fun wireInput(nodeId: String, portName: String, value: InputValue) {
                when (value) {
                    is InputValue.Lit -> setNodeProperty(nodeId, "literal_$portName", value.text)
                    is InputValue.Port -> addDataLink(value.nodeId, value.portName, nodeId, portName)
                }
            }

            private fun packNameOfBinding(binding: Binding?): String? = when (binding) {
                is Binding.GraphVar -> binding.packName
                is Binding.Param -> binding.packName
                else -> null
            }
        }

        // -------------------------------------------------------------
        // Small helpers
        // -------------------------------------------------------------

        /** The inline-literal text of [expr], or `null` when it isn't a plain literal. */
        private fun literalText(expr: Expr): String? = when (expr) {
            is Expr.IntLiteral -> if (expr.suffix == NumericSuffix.NONE) expr.value.toString() else null
            is Expr.RealLiteral -> if (expr.suffix == NumericSuffix.NONE) expr.value.toString() else null
            is Expr.BoolLiteral -> expr.value.toString()
            is Expr.StringLiteral -> expr.value
            is Expr.Unary -> if (expr.op == TokenType.MINUS) {
                when (val operand = expr.operand) {
                    is Expr.IntLiteral -> if (operand.suffix == NumericSuffix.NONE) "-${operand.value}" else null
                    is Expr.RealLiteral -> if (operand.suffix == NumericSuffix.NONE) "-${operand.value}" else null
                    else -> null
                }
            } else null
            else -> null
        }

        private fun literalDataType(expr: Expr): AzoraNodeDataType? = when (expr) {
            is Expr.IntLiteral -> AzoraNodeDataType.INTEGER
            is Expr.RealLiteral -> AzoraNodeDataType.REAL
            is Expr.BoolLiteral -> AzoraNodeDataType.BOOLEAN
            is Expr.StringLiteral -> AzoraNodeDataType.STRING
            is Expr.Unary -> if (expr.op == TokenType.MINUS) literalDataType(expr.operand) else null
            else -> null
        }

        private fun inferDataType(expr: Expr): AzoraNodeDataType? = when (expr) {
            is Expr.Call -> if (expr.callee in packIds) AzoraNodeDataType.DATA_CLASS else null
            is Expr.Member -> {
                val enumId = (expr.target as? Expr.Identifier)?.name?.let { enumIds[it] }
                if (enumId != null) AzoraNodeDataType.ENUM else null
            }
            is Expr.StringTemplate -> AzoraNodeDataType.STRING
            else -> null
        }

        /** Maps a primitive azora type reference to its canvas data type. */
        private fun dataTypeOf(ref: TypeRef): AzoraNodeDataType? = when (ref) {
            is TypeRef.Named -> when (ref.name) {
                "Int" -> AzoraNodeDataType.INTEGER
                "Real" -> AzoraNodeDataType.REAL
                "String" -> AzoraNodeDataType.STRING
                "Bool" -> AzoraNodeDataType.BOOLEAN
                else -> when {
                    ref.name in packIds -> AzoraNodeDataType.DATA_CLASS
                    ref.name in enumIds -> AzoraNodeDataType.ENUM
                    else -> null
                }
            }
            else -> null
        }

        private fun packNameOf(ref: TypeRef): String? =
            ((ref as? TypeRef.Named)?.name)?.takeIf { it in packIds }

        private fun ctorPackName(expr: Expr): String? =
            ((expr as? Expr.Call)?.callee)?.takeIf { it in packIds }

        /** The MATCH `case_N` display value of a literal pattern, or `null`. */
        private fun patternText(pattern: Expr): String? = when (pattern) {
            is Expr.IntLiteral -> if (pattern.suffix == NumericSuffix.NONE) pattern.value.toString() else null
            is Expr.StringLiteral -> pattern.value
            is Expr.BoolLiteral -> pattern.value.toString()
            is Expr.Member -> {
                val enumName = (pattern.target as? Expr.Identifier)?.name
                val enumId = enumName?.let { enumIds[it] }
                if (enumId != null && enums[enumId]?.values?.contains(pattern.name) == true) pattern.name else null
            }
            else -> null
        }

        private fun binaryNodeType(op: TokenType): AzoraNodeType? = when (op) {
            TokenType.PLUS -> AzoraNodeType.ADD
            TokenType.MINUS -> AzoraNodeType.SUBTRACT
            TokenType.STAR -> AzoraNodeType.MULTIPLY
            TokenType.SLASH -> AzoraNodeType.DIVIDE
            TokenType.PERCENT -> AzoraNodeType.MODULO
            TokenType.EQUAL_EQUAL -> AzoraNodeType.EQUAL
            TokenType.BANG_EQUAL -> AzoraNodeType.NOT_EQUAL
            TokenType.GREATER -> AzoraNodeType.GREATER_THAN
            TokenType.LESS -> AzoraNodeType.LESS_THAN
            TokenType.GREATER_EQUAL -> AzoraNodeType.GREATER_OR_EQUAL
            TokenType.LESS_EQUAL -> AzoraNodeType.LESS_OR_EQUAL
            TokenType.AND_AND -> AzoraNodeType.AND
            TokenType.OR_OR -> AzoraNodeType.OR
            else -> null
        }

        /** True when the expression tree contains any function/method call. */
        private fun containsCall(expr: Expr): Boolean = when (expr) {
            is Expr.Call, is Expr.MethodCall -> true
            is Expr.IfExpr -> containsCall(expr.condition) || containsCall(expr.thenExpr) || containsCall(expr.elseExpr)
            is Expr.Binary -> containsCall(expr.left) || containsCall(expr.right)
            is Expr.Unary -> containsCall(expr.operand)
            is Expr.Grouping -> containsCall(expr.expr)
            is Expr.Member -> containsCall(expr.target)
            is Expr.Index -> containsCall(expr.target) || containsCall(expr.index)
            is Expr.Cast -> containsCall(expr.expr)
            else -> false
        }
    }
}
