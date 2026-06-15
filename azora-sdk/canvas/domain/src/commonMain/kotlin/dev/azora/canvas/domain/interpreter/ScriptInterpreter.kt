package dev.azora.canvas.domain.interpreter

import dev.azora.canvas.domain.AzoraPortDefinition
import dev.azora.canvas.domain.model.AzoraGraphModel
import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.domain.type.AzoraNodeType
import dev.azora.canvas.domain.ConstantType
import dev.azora.canvas.domain.GlobalConstant
import kotlinx.coroutines.*

/**
 * Walks an [AzoraGraphModel] from its `START` node and runs it.
 *
 * Execution model:
 * - **Exec flow.** [executeNode] dispatches on [AzoraNodeType] and follows the active outgoing
 *   exec link via [followExecOutput]. Branching nodes (`IF`, `MATCH`, `LOOP`) pick their port index
 *   based on evaluated data.
 * - **Data flow.** Inputs are pulled lazily by [evaluateDataInput]: it walks back along a data link
 *   and asks [evaluateDataOutput] to compute the source's output. With no link connected it falls
 *   back to a `literal_<port>` property on the consuming node.
 * - **Functions.** `FUNCTION_CALL` evaluates parameters, pushes a [FunctionFrame], jumps to the
 *   matching `FUNCTION_DEF`, then pops the frame and stashes the return value under
 *   `__return_<callNodeId>` so the call's `result` data output can read it.
 * - **Cancellation.** [stop] flips an internal flag; long-running paths also honor coroutine
 *   cancellation. Either condition unwinds via an internal `ScriptStopException`.
 *
 * The interpreter only handles the core scripting nodes; pure-data Game Builder/Audio/Tilemap
 * nodes that lack exec ports are not driven from here.
 *
 * @param graph The graph to execute.
 * @param globalConstants Project-level constants resolvable by `GET_CONSTANT` nodes.
 * @param consoleOutput Sink for `PRINT` output and runtime errors.
 */
class ScriptInterpreter(
    private val graph: AzoraGraphModel,
    private val globalConstants: List<GlobalConstant>,
    private val consoleOutput: ConsoleOutputManager
) {
    private val variables = mutableMapOf<String, ScriptValue>()
    private var isRunning = false

    private val callStack = ArrayDeque<FunctionFrame>()

    private data class FunctionFrame(
        val functionId: String,
        var returnValue: ScriptValue = ScriptValue.NullValue,
        val parameterValues: Map<String, ScriptValue> = emptyMap()
    )

    /**
     * Run the graph to completion (or until [stop] or coroutine cancellation).
     *
     * Initializes every variable in [graph] from its [dev.azora.canvas.domain.model.node.AzoraNodeVar.defaultValue]
     * (or the type's own default when empty), locates the unique `START` node, and follows its exec
     * output. Reports an error to [consoleOutput] if no `START` node exists or if a runtime exception
     * escapes a node handler.
     */
    suspend fun execute() {
        isRunning = true

        // Initialize variables with defaults
        graph.variables.values.forEach { variable ->
            val defaultVal = variable.defaultValue.ifEmpty { variable.type.defaultValue }
            variables[variable.id] = parseDefaultValue(variable.type, defaultVal)
        }

        // Find START node
        val startNode = graph.nodes.values.find { it.type == AzoraNodeType.START }
        if (startNode == null) {
            consoleOutput.error("No START node found in script graph.")
            isRunning = false
            return
        }

        try {
            executeNode(startNode.id)
        } catch (e: ScriptStopException) {
            // Script was stopped
        } catch (e: Exception) {
            if (currentCoroutineContext().isActive) {
                consoleOutput.error("Runtime error: ${e.message}")
            }
        } finally {
            isRunning = false
        }
    }

    /**
     * Request graceful termination. The currently running node finishes its synchronous work,
     * then the next [executeNode] / [followExecOutput] hop unwinds via an internal stop exception.
     */
    fun stop() {
        isRunning = false
    }

    private suspend fun executeNode(nodeId: String) {
        if (!isRunning || !currentCoroutineContext().isActive) throw ScriptStopException()

        val node = graph.nodes[nodeId] ?: run {
            consoleOutput.error("Node $nodeId not found.")
            return
        }

        when (node.type) {
            AzoraNodeType.START -> {
                followExecOutput(nodeId, 0)
            }

            AzoraNodeType.PRINT -> {
                val value = evaluateDataInput(nodeId, "value")
                val prefix = node.properties["prefix"]
                val text = if (prefix.isNullOrEmpty()) value.toDisplayString()
                    else "$prefix: ${value.toDisplayString()}"
                consoleOutput.print(text)
                followExecOutput(nodeId, 0)
            }

            AzoraNodeType.IF -> {
                val condition = evaluateDataInput(nodeId, "condition")
                if (condition.toBoolean()) {
                    followExecOutput(nodeId, 0) // true branch
                } else {
                    followExecOutput(nodeId, 1) // false branch
                }
            }

            AzoraNodeType.LOOP -> {
                val count = evaluateDataInput(nodeId, "count").toNumericLong()
                try {
                    for (i in 0 until count) {
                        if (!isRunning || !currentCoroutineContext().isActive) throw ScriptStopException()
                        variables["__loop_index_$nodeId"] = ScriptValue.IntegerValue(i)
                        followExecOutput(nodeId, 0) // body
                    }
                } catch (e: ScriptStopException) {
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consoleOutput.error("Error in loop body: ${e.message}")
                } finally {
                    variables.remove("__loop_index_$nodeId")
                }
                followExecOutput(nodeId, 1) // completed
            }

            AzoraNodeType.MATCH -> {
                val value = evaluateDataInput(nodeId, "value")
                val caseCount = node.properties["caseCount"]?.toIntOrNull() ?: 2
                var matched = false
                for (i in 0 until caseCount) {
                    val caseValue = node.properties["case_$i"] ?: continue
                    if (value.toDisplayString() == caseValue) {
                        followExecOutput(nodeId, i)
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    // Default branch is the last exec output
                    followExecOutput(nodeId, caseCount)
                }
            }

            AzoraNodeType.DELAY -> {
                val ms = evaluateDataInput(nodeId, "milliseconds").toNumericLong()
                delay(ms)
                followExecOutput(nodeId, 0)
            }

            AzoraNodeType.FUNCTION_CALL -> {
                val functionId = node.properties["functionId"] ?: run {
                    consoleOutput.error("FUNCTION_CALL node missing functionId property.")
                    return
                }
                val funcDef = graph.functions[functionId] ?: run {
                    consoleOutput.error("Function $functionId not found.")
                    return
                }

                // Evaluate parameter values
                val paramValues = mutableMapOf<String, ScriptValue>()
                funcDef.parameters.forEach { param ->
                    paramValues[param.name] = evaluateDataInput(nodeId, "param_${param.name}")
                }

                // Push call frame
                callStack.addLast(FunctionFrame(functionId, parameterValues = paramValues))

                // Find the FUNCTION_DEF node for this function
                val defNode = graph.nodes.values.find {
                    it.type == AzoraNodeType.FUNCTION_DEF && it.properties["functionId"] == functionId
                }
                if (defNode != null) {
                    followExecOutput(defNode.id, 0)
                }

                // Pop frame and store return value
                val frame = callStack.removeLast()
                variables["__return_$nodeId"] = frame.returnValue

                followExecOutput(nodeId, 0)
            }

            AzoraNodeType.FUNCTION_DEF -> {
                followExecOutput(nodeId, 0)
            }

            AzoraNodeType.FUNCTION_RETURN -> {
                if (callStack.isNotEmpty()) {
                    val value = evaluateDataInput(nodeId, "value")
                    callStack.last().returnValue = value
                }
                // Don't follow exec - return exits the function
            }

            AzoraNodeType.DATA_CLASS_SET_FIELD -> {
                val classId = node.properties["classId"] ?: return
                val fieldName = node.properties["fieldName"] ?: return
                val instance = evaluateDataInput(nodeId, "instance")
                val value = evaluateDataInput(nodeId, "value")
                if (instance is ScriptValue.DataClassValue) {
                    instance.fields[fieldName] = value
                }
                followExecOutput(nodeId, 0)
            }

            AzoraNodeType.SET_VARIABLE -> {
                val variableId = node.properties["variableId"] ?: run {
                    consoleOutput.error("SET_VARIABLE node missing variableId property.")
                    return
                }
                val value = evaluateDataInput(nodeId, "value")
                variables[variableId] = value
                followExecOutput(nodeId, 0)
            }

            // Pure data nodes should not be executed directly via exec links
            else -> {
                consoleOutput.error("Unexpected exec on data-only node: ${node.type.label}")
            }
        }
    }

    private suspend fun followExecOutput(nodeId: String, portIndex: Int) {
        val link = graph.execLinks.values.find {
            it.sourceNodeId == nodeId && it.sourcePortIndex == portIndex
        } ?: return // No link - end of execution path

        executeNode(link.targetNodeId)
    }

    /**
     * Public hook to evaluate a node's data input from outside the interpreter — useful when the
     * editor wants to preview the value flowing into a port without running the whole graph.
     */
    fun evaluateData(nodeId: String, portName: String): ScriptValue {
        return evaluateDataInput(nodeId, portName)
    }

    private fun evaluateDataInput(nodeId: String, portName: String): ScriptValue {
        // Find a data link that feeds into this port
        val link = graph.dataLinks.values.find {
            it.targetNodeId == nodeId && it.targetPortName == portName
        }

        if (link == null) {
            // No link connected - check for inline literal value
            val node = graph.nodes[nodeId] ?: return ScriptValue.NullValue
            val literalKey = "literal_$portName"
            val literal = node.properties[literalKey]
                ?: node.properties["literal_${portName.replaceFirstChar { it.uppercaseChar() }}"]
            if (literal != null) {
                return parseLiteral(literal, nodeId, portName)
            }
            return ScriptValue.NullValue
        }

        // Evaluate the source node's output
        return evaluateDataOutput(link.sourceNodeId, link.sourcePortName)
    }

    private fun evaluateDataOutput(nodeId: String, portName: String): ScriptValue {
        val node = graph.nodes[nodeId] ?: return ScriptValue.NullValue

        return when (node.type) {
            AzoraNodeType.ADD -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                performArithmetic(a, b) { x, y -> x + y }
            }

            AzoraNodeType.SUBTRACT -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                performArithmetic(a, b) { x, y -> x - y }
            }

            AzoraNodeType.MULTIPLY -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                performArithmetic(a, b) { x, y -> x * y }
            }

            AzoraNodeType.DIVIDE -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                if (b.isInteger && b.toNumericLong() == 0L) {
                    consoleOutput.error("Division by zero")
                    ScriptValue.IntegerValue(0)
                } else if (b.toNumericDouble() == 0.0) {
                    consoleOutput.error("Division by zero")
                    ScriptValue.RealValue(0.0)
                } else {
                    performArithmetic(a, b) { x, y -> x / y }
                }
            }

            AzoraNodeType.MODULO -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                if (a.isInteger && b.isInteger) {
                    val bVal = b.toNumericLong()
                    if (bVal == 0L) {
                        consoleOutput.error("Modulo by zero")
                        ScriptValue.IntegerValue(0)
                    } else {
                        ScriptValue.IntegerValue(a.toNumericLong() % bVal)
                    }
                } else {
                    val bVal = b.toNumericDouble()
                    if (bVal == 0.0) {
                        consoleOutput.error("Modulo by zero")
                        ScriptValue.RealValue(0.0)
                    } else {
                        ScriptValue.RealValue(a.toNumericDouble() % bVal)
                    }
                }
            }

            AzoraNodeType.NEGATE -> {
                val value = evaluateDataInput(nodeId, "value")
                if (value.isInteger) ScriptValue.IntegerValue(-value.toNumericLong())
                else ScriptValue.RealValue(-value.toNumericDouble())
            }

            AzoraNodeType.EQUAL -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(compareValues(a, b) == 0)
            }

            AzoraNodeType.NOT_EQUAL -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(compareValues(a, b) != 0)
            }

            AzoraNodeType.GREATER_THAN -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(compareValues(a, b) > 0)
            }

            AzoraNodeType.LESS_THAN -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(compareValues(a, b) < 0)
            }

            AzoraNodeType.GREATER_OR_EQUAL -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(compareValues(a, b) >= 0)
            }

            AzoraNodeType.LESS_OR_EQUAL -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(compareValues(a, b) <= 0)
            }

            AzoraNodeType.AND -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(a.toBoolean() && b.toBoolean())
            }

            AzoraNodeType.OR -> {
                val a = evaluateDataInput(nodeId, "a")
                val b = evaluateDataInput(nodeId, "b")
                ScriptValue.BooleanValue(a.toBoolean() || b.toBoolean())
            }

            AzoraNodeType.NOT -> {
                val value = evaluateDataInput(nodeId, "value")
                ScriptValue.BooleanValue(!value.toBoolean())
            }

            AzoraNodeType.CAST -> {
                val value = evaluateDataInput(nodeId, "value")
                val toTypeName = node.properties["toType"] ?: return value
                val toType = AzoraNodeDataType.entries.find { it.name == toTypeName } ?: return value
                cast(value, toType)
            }

            AzoraNodeType.LOOP -> {
                // Return current loop index
                if (portName == "index") {
                    variables["__loop_index_$nodeId"] ?: ScriptValue.IntegerValue(0)
                } else {
                    ScriptValue.NullValue
                }
            }

            AzoraNodeType.FUNCTION_CALL -> {
                // Return the stored return value from the last call
                if (portName == "result") {
                    variables["__return_$nodeId"] ?: ScriptValue.NullValue
                } else {
                    ScriptValue.NullValue
                }
            }

            AzoraNodeType.ENUM_VALUE -> {
                val enumId = node.properties["enumId"] ?: return ScriptValue.NullValue
                val value = node.properties["value"] ?: return ScriptValue.NullValue
                ScriptValue.EnumValue(enumId, value)
            }

            AzoraNodeType.DATA_CLASS_CREATE -> {
                val classId = node.properties["classId"] ?: return ScriptValue.NullValue
                val classDef = graph.dataClasses[classId] ?: return ScriptValue.NullValue
                val fields = mutableMapOf<String, ScriptValue>()
                classDef.fields.forEach { field ->
                    fields[field.name] = evaluateDataInput(nodeId, "field_${field.name}")
                }
                ScriptValue.DataClassValue(classId, fields)
            }

            AzoraNodeType.DATA_CLASS_GET_FIELD -> {
                val fieldName = node.properties["fieldName"] ?: return ScriptValue.NullValue
                val instance = evaluateDataInput(nodeId, "instance")
                if (instance is ScriptValue.DataClassValue) {
                    instance.fields[fieldName] ?: ScriptValue.NullValue
                } else {
                    ScriptValue.NullValue
                }
            }

            AzoraNodeType.GET_VARIABLE -> {
                val variableId = node.properties["variableId"] ?: return ScriptValue.NullValue
                variables[variableId] ?: ScriptValue.NullValue
            }

            AzoraNodeType.GET_CONSTANT -> {
                val constantId = node.properties["constantId"] ?: return ScriptValue.NullValue
                val constant = globalConstants.find { it.id == constantId }
                if (constant != null) {
                    constantToScriptValue(constant)
                } else {
                    ScriptValue.NullValue
                }
            }

            else -> ScriptValue.NullValue
        }
    }

    private fun performArithmetic(
        a: ScriptValue,
        b: ScriptValue,
        op: (Double, Double) -> Double
    ): ScriptValue {
        // If both are text, concatenate for ADD
        if (a is ScriptValue.TextValue && b is ScriptValue.TextValue) {
            return ScriptValue.TextValue(a.value + b.value)
        }
        // If both are integers, keep integer result
        if (a.isInteger && b.isInteger) {
            val result = op(a.toNumericDouble(), b.toNumericDouble())
            return if (result == result.toLong().toDouble()) {
                ScriptValue.IntegerValue(result.toLong())
            } else {
                ScriptValue.RealValue(result)
            }
        }
        return ScriptValue.RealValue(op(a.toNumericDouble(), b.toNumericDouble()))
    }

    private fun compareValues(a: ScriptValue, b: ScriptValue): Int {
        if (a is ScriptValue.TextValue && b is ScriptValue.TextValue) {
            return a.value.compareTo(b.value)
        }
        if (a is ScriptValue.EnumValue && b is ScriptValue.EnumValue) {
            return if (a.enumId == b.enumId && a.value == b.value) 0
            else a.value.compareTo(b.value)
        }
        return a.toNumericDouble().compareTo(b.toNumericDouble())
    }

    private fun cast(value: ScriptValue, targetType: AzoraNodeDataType): ScriptValue = when (targetType) {
        AzoraNodeDataType.BOOLEAN -> ScriptValue.BooleanValue(value.toBoolean())
        AzoraNodeDataType.INTEGER -> ScriptValue.IntegerValue(value.toNumericLong())
        AzoraNodeDataType.REAL -> ScriptValue.RealValue(value.toNumericDouble())
        AzoraNodeDataType.STRING -> ScriptValue.TextValue(value.toDisplayString())
        else -> value
    }

    private fun constantToScriptValue(constant: GlobalConstant): ScriptValue = when (constant.type) {
        ConstantType.BOOLEAN -> ScriptValue.BooleanValue(constant.value.toBooleanStrictOrNull() ?: false)
        ConstantType.INTEGER -> ScriptValue.IntegerValue(constant.value.toLongOrNull() ?: 0L)
        ConstantType.REAL -> ScriptValue.RealValue(constant.value.toDoubleOrNull() ?: 0.0)
        ConstantType.TEXT -> ScriptValue.TextValue(constant.value)
    }

    private fun parseDefaultValue(type: AzoraNodeDataType, value: String): ScriptValue = when (type) {
        AzoraNodeDataType.BOOLEAN -> ScriptValue.BooleanValue(value.toBooleanStrictOrNull() ?: false)
        AzoraNodeDataType.INTEGER -> ScriptValue.IntegerValue(value.toLongOrNull() ?: 0L)
        AzoraNodeDataType.REAL -> ScriptValue.RealValue(value.toDoubleOrNull() ?: 0.0)
        AzoraNodeDataType.STRING -> ScriptValue.TextValue(value)
        else -> ScriptValue.NullValue
    }

    private fun parseLiteral(literal: String, nodeId: String, portName: String): ScriptValue {
        // Try to infer type from the expected port type
        val node = graph.nodes[nodeId] ?: return ScriptValue.TextValue(literal)
        val inputs = AzoraPortDefinition.dataInputs(node.type, node.properties)
        val expectedType = inputs.find { it.first == portName }?.second

        return when (expectedType) {
            AzoraNodeDataType.BOOLEAN -> ScriptValue.BooleanValue(literal.toBooleanStrictOrNull() ?: false)
            AzoraNodeDataType.INTEGER -> ScriptValue.IntegerValue(literal.toLongOrNull() ?: 0L)
            AzoraNodeDataType.REAL -> ScriptValue.RealValue(literal.toDoubleOrNull() ?: 0.0)
            AzoraNodeDataType.STRING -> ScriptValue.TextValue(literal)
            else -> {
                // Try to auto-detect
                literal.toLongOrNull()?.let { return ScriptValue.IntegerValue(it) }
                literal.toDoubleOrNull()?.let { return ScriptValue.RealValue(it) }
                literal.toBooleanStrictOrNull()?.let { return ScriptValue.BooleanValue(it) }
                ScriptValue.TextValue(literal)
            }
        }
    }

    private class ScriptStopException : Exception("Script stopped")
}
