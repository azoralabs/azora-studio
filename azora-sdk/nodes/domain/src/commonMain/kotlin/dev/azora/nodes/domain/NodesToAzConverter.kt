package dev.azora.nodes.domain

import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.model.node.AzoraNodeVar
import dev.azora.canvas.domain.type.AzoraNodeType

/**
 * Result of a graph → azora-source conversion.
 *
 * @property source The generated azora source (always produced; unsupported
 *   nodes degrade to explanatory `//` comments rather than failing the build).
 * @property warnings Human-readable notes about constructs that could not be
 *   converted exactly. Surfaced in the Studio console.
 */
data class NodesToAzResult(val source: String, val warnings: List<String>)

/**
 * Lowers an [AzoraGraphModel] node graph to azora-lang source code.
 *
 * Structure of the generated file:
 * 1. `use` imports and the verbatim preamble carried in [AzoraGraphModel.meta]
 *    (round-tripped from a previous .az → .azn conversion).
 * 2. `enum` declarations from [AzoraGraphModel.enums].
 * 3. `pack` declarations from [AzoraGraphModel.dataClasses].
 * 4. Top-level `var` declarations for variables used by more than one function.
 * 5. `func main()` from the `START` node's execution chain, then one `func`
 *    per `FUNCTION_DEF` node.
 *
 * Execution chains are walked link-by-link. Branching nodes (`IF`, `MATCH`)
 * find their merge point (the first node reachable from every branch) so the
 * emitted `if`/`when` block re-joins the statement flow exactly like the
 * interpreter's control flow. Pure data nodes are inlined as expressions at
 * their point of use; `FUNCTION_CALL`/`AZ_CALL` results are hoisted into
 * `let __rN = …` temporaries because calls only run on the exec path.
 */
class NodesToAzConverter {

    fun convert(graph: AzoraGraphModel): NodesToAzResult {
        val emitter = Emitter(graph)
        return NodesToAzResult(emitter.emitProgram(), emitter.warnings)
    }

    // =====================================================================

    private class Emitter(private val graph: AzoraGraphModel) {

        val warnings = mutableListOf<String>()

        /** FUNCTION_CALL / AZ_CALL node id → temp variable holding its result. */
        private val callTemps = mutableMapOf<String, String>()

        /** FUNCTION_CALL / AZ_CALL node id → call expression inlined into its single consumer. */
        private val inlinedCalls = mutableMapOf<String, String>()
        private var tempCounter = 0
        private var counterCounter = 0

        /** Loop node id → counter variable name (FOR_RANGE `counter` property or generated). */
        private val loopCounters = mutableMapOf<String, String>()

        /** Variable ids declared locally in the function currently being emitted. */
        private val declaredVars = mutableSetOf<String>()

        /** Variable ids that are declared at top level (used by several functions). */
        private val globalVarIds = mutableSetOf<String>()

        // -------------------------------------------------------------
        // Program layout
        // -------------------------------------------------------------

        fun emitProgram(): String {
            val out = StringBuilder()

            graph.meta["imports"]?.takeIf { it.isNotBlank() }?.let { out.appendLine(it.trimEnd()).appendLine() }
            graph.meta["preamble"]?.takeIf { it.isNotBlank() }?.let { out.appendLine(it.trimEnd()).appendLine() }

            for (enum in graph.enums.values) {
                out.appendLine("enum ${enum.name} {")
                for (variant in enum.values) out.appendLine("    $variant")
                out.appendLine("}").appendLine()
            }

            for (dataClass in graph.dataClasses.values) {
                out.appendLine("pack ${dataClass.name} {")
                for (field in dataClass.fields) {
                    out.appendLine("    var ${field.name}: ${azTypeName(field.type, "field ${dataClass.name}.${field.name}")}")
                }
                out.appendLine("}").appendLine()
            }

            val regions = computeVariableRegions()
            for ((varId, regionCount) in regions) {
                if (regionCount != 1) globalVarIds.add(varId)
            }
            for (varId in globalVarIds) {
                val variable = graph.variables[varId] ?: continue
                out.appendLine(topLevelVarDecl(variable))
            }
            if (globalVarIds.isNotEmpty()) out.appendLine()

            // main() from START, then user functions ordered by canvas position.
            val startNode = graph.nodes.values.find { it.type == AzoraNodeType.START }
            if (startNode != null) {
                out.append(emitFunction("main", emptyList(), null, startNode))
                out.appendLine()
            }
            val defNodes = graph.nodes.values
                .filter { it.type == AzoraNodeType.FUNCTION_DEF }
                .sortedWith(compareBy({ it.positionY }, { it.positionX }))
            for (defNode in defNodes) {
                val func = defNode.properties["functionId"]?.let { graph.functions[it] }
                if (func == null) {
                    warnings.add("Function node \"${defNode.title}\" has no function definition — skipped.")
                    continue
                }
                val params = func.parameters.map { it.name to azTypeName(it.type, "parameter ${func.name}.${it.name}") }
                val returnType = func.returnType?.let { azTypeName(it, "return type of ${func.name}") }
                out.append(emitFunction(func.name, params, returnType, defNode))
                out.appendLine()
            }

            return out.toString().trimEnd() + "\n"
        }

        /**
         * For each variable id: the number of distinct functions (START + FUNCTION_DEFs)
         * whose execution chain (including pure-data feeders) references it.
         */
        private fun computeVariableRegions(): Map<String, Int> {
            val roots = graph.nodes.values.filter {
                it.type == AzoraNodeType.START || it.type == AzoraNodeType.FUNCTION_DEF
            }
            val varRegions = mutableMapOf<String, MutableSet<String>>()
            for (root in roots) {
                val execNodes = reachableExecNodes(root.id)
                // Pull in pure data nodes feeding this region (transitively).
                val regionNodes = execNodes.toMutableSet()
                var changed = true
                while (changed) {
                    changed = false
                    for (link in graph.dataLinks.values) {
                        if (link.targetNodeId in regionNodes && link.sourceNodeId !in regionNodes) {
                            regionNodes.add(link.sourceNodeId)
                            changed = true
                        }
                    }
                }
                for (nodeId in regionNodes) {
                    val node = graph.nodes[nodeId] ?: continue
                    if (node.type == AzoraNodeType.GET_VARIABLE || node.type == AzoraNodeType.SET_VARIABLE) {
                        val varId = node.properties["variableId"] ?: continue
                        varRegions.getOrPut(varId) { mutableSetOf() }.add(root.id)
                    }
                }
            }
            return graph.variables.keys.associateWith { varRegions[it]?.size ?: 0 }
        }

        /** All exec-reachable node ids from [rootId], following every exec output. */
        private fun reachableExecNodes(rootId: String): Set<String> {
            val seen = mutableSetOf(rootId)
            val queue = ArrayDeque(listOf(rootId))
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                for (link in graph.execLinks.values) {
                    if (link.sourceNodeId == id && seen.add(link.targetNodeId)) {
                        queue.add(link.targetNodeId)
                    }
                }
            }
            return seen
        }

        private fun topLevelVarDecl(variable: AzoraNodeVar): String {
            val type = variable.type
            return when (type) {
                AzoraNodeDataType.BOOLEAN, AzoraNodeDataType.INTEGER,
                AzoraNodeDataType.REAL, AzoraNodeDataType.STRING ->
                    "var ${variable.name}: ${azTypeName(type, variable.name)} = ${defaultLiteral(variable)}"
                else -> {
                    warnings.add("Variable \"${variable.name}\" (${type.label}) is shared between functions but has no concrete azora type — declared as Int 0.")
                    "var ${variable.name}: Int = 0"
                }
            }
        }

        // -------------------------------------------------------------
        // Function emission
        // -------------------------------------------------------------

        private fun emitFunction(
            name: String,
            params: List<Pair<String, String>>,
            returnType: String?,
            rootNode: AzoraNodeModel
        ): String {
            declaredVars.clear()
            val body = StringBuilder()

            // Locally-owned primitive variables are declared up front; other local
            // variables are declared at their first SET (see emitSetVariable).
            val localVarIds = localVariableIds(rootNode.id)
            for (varId in localVarIds) {
                val variable = graph.variables[varId] ?: continue
                if (variable.type in PRIMITIVE_TYPES && !isSetExactlyOnceReadonly(variable, varId)) {
                    body.appendLine("    var ${variable.name}: ${azTypeName(variable.type, variable.name)} = ${defaultLiteral(variable)}")
                    declaredVars.add(varId)
                }
            }

            val chain = ChainEmitter(indent = 1)
            chain.emitChain(execTarget(rootNode.id, 0), stopAt = null)
            body.append(chain.text())

            val paramList = params.joinToString(", ") { (n, t) -> "$n: $t" }
            val ret = returnType?.let { ": $it" } ?: ""
            return buildString {
                appendLine("func $name($paramList)$ret {")
                append(body.toString().trimEnd('\n'))
                appendLine()
                appendLine("}")
            }
        }

        /** Variable ids used only by [rootId]'s region (locals of that function). */
        private fun localVariableIds(rootId: String): List<String> {
            val regionNodes = reachableExecNodes(rootId).toMutableSet()
            var changed = true
            while (changed) {
                changed = false
                for (link in graph.dataLinks.values) {
                    if (link.targetNodeId in regionNodes && link.sourceNodeId !in regionNodes) {
                        regionNodes.add(link.sourceNodeId)
                        changed = true
                    }
                }
            }
            return graph.variables.keys.filter { varId ->
                varId !in globalVarIds && regionNodes.any { nodeId ->
                    graph.nodes[nodeId]?.properties?.get("variableId") == varId
                }
            }
        }

        /** `let`-style variable: readonly and written by exactly one SET node. */
        private fun isSetExactlyOnceReadonly(variable: AzoraNodeVar, varId: String): Boolean {
            if (!variable.readonly) return false
            val sets = graph.nodes.values.count {
                it.type == AzoraNodeType.SET_VARIABLE && it.properties["variableId"] == varId
            }
            return sets == 1
        }

        // -------------------------------------------------------------
        // Exec-chain walking
        // -------------------------------------------------------------

        private fun execTarget(nodeId: String, portIndex: Int): String? =
            graph.execLinks.values.find { it.sourceNodeId == nodeId && it.sourcePortIndex == portIndex }
                ?.targetNodeId

        /** First node reachable from every non-null head — where branches re-join. */
        private fun findMerge(heads: List<String?>): String? {
            val present = heads.filterNotNull()
            if (present.size < 2) return null
            val reachSets = present.map { head -> reachableExecNodes(head) }
            // Walk BFS order from the first head; the merge is the first node in all sets.
            val order = bfsOrder(present.first())
            return order.firstOrNull { candidate -> reachSets.all { candidate in it } }
        }

        private fun bfsOrder(rootId: String): List<String> {
            val seen = linkedSetOf(rootId)
            val queue = ArrayDeque(listOf(rootId))
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                for (link in graph.execLinks.values.sortedBy { it.sourcePortIndex }) {
                    if (link.sourceNodeId == id && seen.add(link.targetNodeId)) {
                        queue.add(link.targetNodeId)
                    }
                }
            }
            return seen.toList()
        }

        private inner class ChainEmitter(private var indent: Int) {
            private val sb = StringBuilder()
            private val visited = mutableSetOf<String>()

            fun text(): String = sb.toString()

            private fun line(text: String) {
                sb.append("    ".repeat(indent)).appendLine(text)
            }

            private fun raw(textBlock: String) {
                for (l in textBlock.lines()) sb.append("    ".repeat(indent)).appendLine(l)
            }

            fun emitChain(start: String?, stopAt: String?) {
                var current = start
                while (current != null && current != stopAt) {
                    if (!visited.add(current)) {
                        warnings.add("Cyclic execution link at node $current — chain truncated.")
                        return
                    }
                    current = emitNode(current, stopAt)
                }
            }

            /** Emits one node's statement(s); returns the next node in this chain. */
            private fun emitNode(nodeId: String, stopAt: String?): String? {
                val node = graph.nodes[nodeId] ?: return null
                when (node.type) {
                    AzoraNodeType.START, AzoraNodeType.FUNCTION_DEF -> return execTarget(nodeId, 0)

                    AzoraNodeType.PRINT -> {
                        val value = dataExpr(nodeId, "value", AzoraNodeDataType.ANY)
                        val prefix = node.properties["prefix"]
                        if (prefix.isNullOrEmpty()) line("println($value)")
                        else line("println(\"${AzSourcePrinter.escapeString(prefix)}: \${$value}\")")
                        return execTarget(nodeId, 0)
                    }

                    AzoraNodeType.SET_VARIABLE -> {
                        emitSetVariable(node)
                        return execTarget(nodeId, 0)
                    }

                    AzoraNodeType.IF -> {
                        val condition = dataExpr(nodeId, "condition", AzoraNodeDataType.BOOLEAN)
                        val thenHead = execTarget(nodeId, 0)
                        val elseHead = execTarget(nodeId, 1)
                        val merge = findMerge(listOf(thenHead, elseHead))
                        line("if $condition {")
                        indented { emitChain(thenHead, merge ?: stopAt) }
                        if (elseHead != null && elseHead != merge) {
                            line("} else {")
                            indented { emitChain(elseHead, merge ?: stopAt) }
                        }
                        line("}")
                        return merge
                    }

                    AzoraNodeType.WHILE -> {
                        val condition = dataExpr(nodeId, "condition", AzoraNodeDataType.BOOLEAN)
                        line("while $condition {")
                        indented { emitChain(execTarget(nodeId, 0), stopAt = nodeId) }
                        line("}")
                        return execTarget(nodeId, 1)
                    }

                    AzoraNodeType.FOR_RANGE -> {
                        val counter = counterName(node)
                        val from = dataExpr(nodeId, "from", AzoraNodeDataType.INTEGER)
                        val to = dataExpr(nodeId, "to", AzoraNodeDataType.INTEGER)
                        val inclusive = node.properties["inclusive"]?.toBooleanStrictOrNull() ?: true
                        line("for $counter in $from${if (inclusive) ".." else "..<"}$to {")
                        indented { emitChain(execTarget(nodeId, 0), stopAt = nodeId) }
                        line("}")
                        return execTarget(nodeId, 1)
                    }

                    AzoraNodeType.LOOP -> {
                        val counter = counterName(node)
                        val count = dataExpr(nodeId, "count", AzoraNodeDataType.INTEGER)
                        line("for $counter in 0..<$count {")
                        indented { emitChain(execTarget(nodeId, 0), stopAt = nodeId) }
                        line("}")
                        return execTarget(nodeId, 1)
                    }

                    AzoraNodeType.MATCH -> return emitMatch(node, stopAt)

                    AzoraNodeType.FUNCTION_CALL -> {
                        emitCall(node, calleeName(node), functionCallArgs(node))
                        return execTarget(nodeId, 0)
                    }

                    AzoraNodeType.AZ_CALL -> {
                        val name = node.properties["name"] ?: run {
                            warnings.add("Call node ${node.id} has no function name — skipped.")
                            return execTarget(nodeId, 0)
                        }
                        val argCount = node.properties["argCount"]?.toIntOrNull() ?: 0
                        val args = (0 until argCount).map { dataExpr(nodeId, "arg_$it", AzoraNodeDataType.ANY) }
                        emitCall(node, name, args)
                        return execTarget(nodeId, 0)
                    }

                    AzoraNodeType.FUNCTION_RETURN -> {
                        val hasValue = graph.dataLinks.values.any {
                            it.targetNodeId == nodeId && it.targetPortName == "value"
                        } || node.properties.containsKey("literal_value")
                        if (hasValue) line("return ${dataExpr(nodeId, "value", AzoraNodeDataType.ANY)}")
                        else line("return")
                        return null // return ends the chain
                    }

                    AzoraNodeType.DATA_CLASS_SET_FIELD -> {
                        val fieldName = node.properties["fieldName"] ?: "?"
                        val instance = dataExpr(nodeId, "instance", AzoraNodeDataType.DATA_CLASS)
                        val value = dataExpr(nodeId, "value", AzoraNodeDataType.ANY)
                        line("$instance.$fieldName = $value")
                        return execTarget(nodeId, 0)
                    }

                    AzoraNodeType.AZ_CODE -> {
                        val code = node.properties["code"].orEmpty()
                        if (code.isBlank()) warnings.add("Empty Azora Code node ${node.id}.")
                        else raw(code.trimEnd())
                        return execTarget(nodeId, 0)
                    }

                    AzoraNodeType.DELAY -> {
                        warnings.add("DELAY nodes have no azora equivalent yet — emitted as a comment.")
                        line("// TODO: DELAY (${dataExpr(nodeId, "milliseconds", AzoraNodeDataType.INTEGER)} ms) is editor-only")
                        return execTarget(nodeId, 0)
                    }

                    else -> {
                        warnings.add("Node type ${node.type.label} is not supported by azora codegen — emitted as a comment.")
                        line("// unsupported node: ${node.type.label}")
                        return execTarget(nodeId, 0)
                    }
                }
            }

            private fun emitMatch(node: AzoraNodeModel, stopAt: String?): String? {
                val nodeId = node.id
                val value = dataExpr(nodeId, "value", AzoraNodeDataType.ANY)
                val caseCount = node.properties["caseCount"]?.toIntOrNull() ?: 2
                val caseHeads = (0 until caseCount).map { execTarget(nodeId, it) }
                val defaultHead = execTarget(nodeId, caseCount)
                val merge = findMerge((caseHeads + defaultHead).distinct())

                // Cases jumping to the same body become one multi-pattern branch.
                val grouped = LinkedHashMap<String?, MutableList<Int>>()
                for (i in 0 until caseCount) grouped.getOrPut(caseHeads[i]) { mutableListOf() }.add(i)

                line("when $value {")
                indented {
                    for ((head, caseIndices) in grouped) {
                        val patterns = caseIndices.joinToString(", ") { matchPattern(node.properties["case_$it"].orEmpty()) }
                        line("$patterns -> {")
                        indented { emitChain(head, merge ?: stopAt) }
                        line("}")
                    }
                    if (defaultHead != null && defaultHead != merge) {
                        line("else -> {")
                        indented { emitChain(defaultHead, merge ?: stopAt) }
                        line("}")
                    }
                }
                line("}")
                return merge
            }

            private fun emitCall(node: AzoraNodeModel, name: String, args: List<String>) {
                val call = "$name(${args.joinToString(", ")})"
                val consumers = graph.dataLinks.values.filter {
                    it.sourceNodeId == node.id && it.sourcePortName == "result"
                }
                when {
                    consumers.isEmpty() -> line(call)
                    // Single consumer that executes right after the call: inline the
                    // call expression there (`let x = f(1)` / `println(f(1))`) instead
                    // of introducing a `__rN` temp — keeps round-trips a fixpoint.
                    consumers.size == 1 && consumers[0].targetNodeId == execTarget(node.id, 0) -> {
                        inlinedCalls[node.id] = call
                    }
                    else -> {
                        val temp = uniqueTempName()
                        callTemps[node.id] = temp
                        line("let $temp = $call")
                    }
                }
            }

            /** A `__rN` name not colliding with any graph variable. */
            private fun uniqueTempName(): String {
                val taken = graph.variables.values.mapTo(mutableSetOf()) { it.name }
                var name: String
                do { name = "__r${tempCounter++}" } while (name in taken)
                return name
            }

            private fun emitSetVariable(node: AzoraNodeModel) {
                val varId = node.properties["variableId"]
                val variable = varId?.let { graph.variables[it] }
                if (variable == null) {
                    warnings.add("Set Variable node ${node.id} references an unknown variable — skipped.")
                    return
                }
                val value = dataExpr(node.id, "value", variable.type)
                val declared = varId in declaredVars || varId in globalVarIds
                if (!declared) {
                    declaredVars.add(varId)
                    val keyword = if (isSetExactlyOnceReadonly(variable, varId)) "let" else "var"
                    line("$keyword ${variable.name} = $value")
                } else {
                    line("${variable.name} = $value")
                }
            }

            private fun indented(block: () -> Unit) {
                indent++
                block()
                indent--
            }
        }

        // -------------------------------------------------------------
        // Data (expression) resolution
        // -------------------------------------------------------------

        /** The azora expression feeding data input [portName] of [nodeId]. */
        private fun dataExpr(nodeId: String, portName: String, expectedType: AzoraNodeDataType): String {
            val link = graph.dataLinks.values.find {
                it.targetNodeId == nodeId && it.targetPortName == portName
            }
            if (link != null) return outputExpr(link.sourceNodeId, link.sourcePortName)

            val node = graph.nodes[nodeId]
            val literal = node?.properties?.get("literal_$portName")
                ?: node?.properties?.get("literal_${portName.replaceFirstChar { it.uppercaseChar() }}")
            if (literal != null) return formatLiteral(literal, expectedType)
            return defaultLiteralForType(expectedType)
        }

        /** The azora expression for data output [portName] of node [nodeId]. */
        private fun outputExpr(nodeId: String, portName: String): String {
            val node = graph.nodes[nodeId] ?: return "0"
            return when (node.type) {
                AzoraNodeType.GET_VARIABLE -> {
                    val variable = node.properties["variableId"]?.let { graph.variables[it] }
                    variable?.name ?: run {
                        warnings.add("Get Variable node $nodeId references an unknown variable.")
                        "0"
                    }
                }
                AzoraNodeType.PARAM_GET -> node.properties["name"] ?: run {
                    warnings.add("Get Parameter node $nodeId has no parameter name.")
                    "0"
                }
                AzoraNodeType.GET_CONSTANT -> {
                    warnings.add("Get Constant nodes are editor-only — emitted as 0. Declare the constant in azora source instead.")
                    "0"
                }
                AzoraNodeType.ADD -> binary(nodeId, "+")
                AzoraNodeType.SUBTRACT -> binary(nodeId, "-")
                AzoraNodeType.MULTIPLY -> binary(nodeId, "*")
                AzoraNodeType.DIVIDE -> binary(nodeId, "/")
                AzoraNodeType.MODULO -> binary(nodeId, "%")
                AzoraNodeType.EQUAL -> binary(nodeId, "==")
                AzoraNodeType.NOT_EQUAL -> binary(nodeId, "!=")
                AzoraNodeType.GREATER_THAN -> binary(nodeId, ">")
                AzoraNodeType.LESS_THAN -> binary(nodeId, "<")
                AzoraNodeType.GREATER_OR_EQUAL -> binary(nodeId, ">=")
                AzoraNodeType.LESS_OR_EQUAL -> binary(nodeId, "<=")
                AzoraNodeType.AND -> binary(nodeId, "&&", AzoraNodeDataType.BOOLEAN)
                AzoraNodeType.OR -> binary(nodeId, "||", AzoraNodeDataType.BOOLEAN)
                AzoraNodeType.NOT -> "(!${dataExpr(nodeId, "value", AzoraNodeDataType.BOOLEAN)})"
                AzoraNodeType.NEGATE -> "(-${dataExpr(nodeId, "value", AzoraNodeDataType.ANY)})"
                AzoraNodeType.CAST -> {
                    val target = when (node.properties["toType"]) {
                        "BOOLEAN" -> "Bool"; "INTEGER" -> "Int"; "REAL" -> "Real"; "STRING" -> "String"
                        else -> null
                    }
                    val value = dataExpr(nodeId, "value", AzoraNodeDataType.ANY)
                    if (target == null) value else "($value as $target)"
                }
                AzoraNodeType.AZ_EXPR -> {
                    val code = node.properties["code"].orEmpty().trim()
                    if (code.isEmpty()) {
                        warnings.add("Empty Azora Expression node $nodeId — emitted as 0.")
                        "0"
                    } else "($code)"
                }
                AzoraNodeType.ENUM_VALUE -> {
                    val enum = node.properties["enumId"]?.let { graph.enums[it] }
                    val value = node.properties["value"] ?: "?"
                    if (enum != null) "${enum.name}.$value" else value
                }
                AzoraNodeType.DATA_CLASS_CREATE -> {
                    val dataClass = node.properties["classId"]?.let { graph.dataClasses[it] }
                    if (dataClass == null) {
                        warnings.add("Create node $nodeId references an unknown pack.")
                        "0"
                    } else {
                        val args = dataClass.fields.joinToString(", ") { field ->
                            dataExpr(nodeId, "field_${field.name}", field.type)
                        }
                        "${dataClass.name}($args)"
                    }
                }
                AzoraNodeType.DATA_CLASS_GET_FIELD -> {
                    val fieldName = node.properties["fieldName"] ?: "?"
                    "${dataExpr(nodeId, "instance", AzoraNodeDataType.DATA_CLASS)}.$fieldName"
                }
                AzoraNodeType.LOOP, AzoraNodeType.FOR_RANGE ->
                    loopCounters[nodeId] ?: counterName(node)
                AzoraNodeType.FUNCTION_CALL, AzoraNodeType.AZ_CALL ->
                    inlinedCalls[nodeId] ?: callTemps[nodeId] ?: run {
                        warnings.add("Result of call node $nodeId is read before the call executes — emitted as 0.")
                        "0"
                    }
                else -> {
                    warnings.add("Data output of ${node.type.label} is not supported by azora codegen — emitted as 0.")
                    "0"
                }
            }
        }

        private fun binary(nodeId: String, op: String, operandType: AzoraNodeDataType = AzoraNodeDataType.ANY): String {
            val a = dataExpr(nodeId, "a", operandType)
            val b = dataExpr(nodeId, "b", operandType)
            return "($a $op $b)"
        }

        private fun functionCallArgs(node: AzoraNodeModel): List<String> {
            val func = node.properties["functionId"]?.let { graph.functions[it] } ?: return emptyList()
            return func.parameters.map { param -> dataExpr(node.id, "param_${param.name}", param.type) }
        }

        private fun calleeName(node: AzoraNodeModel): String {
            val func = node.properties["functionId"]?.let { graph.functions[it] }
            if (func == null) warnings.add("Call node ${node.id} references an unknown function.")
            return func?.name ?: "unknownFunction"
        }

        private fun counterName(node: AzoraNodeModel): String =
            loopCounters.getOrPut(node.id) {
                node.properties["counter"]?.takeIf { it.isNotBlank() } ?: "__i${counterCounter++}"
            }

        // -------------------------------------------------------------
        // Literals & types
        // -------------------------------------------------------------

        private fun matchPattern(caseValue: String): String {
            // MATCH compares by display string; regenerate the most natural literal.
            caseValue.toLongOrNull()?.let { return caseValue }
            caseValue.toDoubleOrNull()?.let { return caseValue }
            caseValue.toBooleanStrictOrNull()?.let { return caseValue }
            // An enum variant name becomes a qualified enum pattern.
            graph.enums.values.find { caseValue in it.values }?.let { return "${it.name}.$caseValue" }
            return "\"${AzSourcePrinter.escapeString(caseValue)}\""
        }

        private fun formatLiteral(literal: String, expectedType: AzoraNodeDataType): String = when (expectedType) {
            AzoraNodeDataType.INTEGER -> literal.toLongOrNull()?.toString() ?: "0"
            AzoraNodeDataType.REAL -> {
                val d = literal.toDoubleOrNull() ?: 0.0
                if ('.' in literal || 'e' in literal || 'E' in literal) literal else "$d"
            }
            AzoraNodeDataType.BOOLEAN -> (literal.toBooleanStrictOrNull() ?: false).toString()
            AzoraNodeDataType.STRING -> "\"${AzSourcePrinter.escapeString(literal)}\""
            else -> {
                literal.toLongOrNull()?.let { return literal }
                literal.toDoubleOrNull()?.let { return literal }
                literal.toBooleanStrictOrNull()?.let { return literal }
                "\"${AzSourcePrinter.escapeString(literal)}\""
            }
        }

        private fun defaultLiteralForType(type: AzoraNodeDataType): String = when (type) {
            AzoraNodeDataType.BOOLEAN -> "false"
            AzoraNodeDataType.INTEGER -> "0"
            AzoraNodeDataType.REAL -> "0.0"
            AzoraNodeDataType.STRING -> "\"\""
            else -> "0"
        }

        private fun defaultLiteral(variable: AzoraNodeVar): String {
            val raw = variable.defaultValue.ifEmpty { variable.type.defaultValue }
            return formatLiteral(raw, variable.type)
        }

        private fun azTypeName(type: AzoraNodeDataType, context: String): String = when (type) {
            AzoraNodeDataType.BOOLEAN -> "Bool"
            AzoraNodeDataType.INTEGER -> "Int"
            AzoraNodeDataType.REAL -> "Real"
            AzoraNodeDataType.STRING -> "String"
            else -> {
                warnings.add("No concrete azora type for ${type.label} ($context) — using Any.")
                "Any"
            }
        }

        private companion object {
            val PRIMITIVE_TYPES = setOf(
                AzoraNodeDataType.BOOLEAN,
                AzoraNodeDataType.INTEGER,
                AzoraNodeDataType.REAL,
                AzoraNodeDataType.STRING
            )
        }
    }
}
