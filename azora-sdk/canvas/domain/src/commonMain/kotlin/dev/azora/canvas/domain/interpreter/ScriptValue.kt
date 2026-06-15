package dev.azora.canvas.domain.interpreter

/**
 * Runtime value representation used by [ScriptInterpreter].
 *
 * `ScriptValue` is a closed hierarchy: every literal, every variable, every node output and every
 * function return materializes as one of these variants. Conversion helpers ([toDisplayString],
 * [toBoolean], [toNumericLong], [toNumericDouble]) provide best-effort coercions so the interpreter
 * can compare, print, and arithmetic-combine values across types.
 *
 * The hierarchy spans three groups:
 * - **Primitives & strings** - [BooleanValue], [IntegerValue], [RealValue], [BigIntegerValue],
 *   [BigRealValue], [TextValue], [CharValue].
 * - **Composites** - [EnumValue], [DataClassValue], [ArrayValue], [SetValue], [MapValue],
 *   [ScopeMemberValue].
 * - **Callables, futures and concurrency primitives** - [LambdaValue], [TaskValue], [FlowValue],
 *   [PointerValue], [MutexValue], [RwLockValue], [AtomicValue], [BarrierValue], [OnceValue],
 *   [ThreadPoolValue], [IsolatedValue], [ThreadHandleValue], [SoloInstanceValue], [WrapValue].
 *
 * [NullValue] is the single absent-value sentinel.
 */
sealed class ScriptValue {
    /** Boolean primitive. */
    data class BooleanValue(val value: Boolean) : ScriptValue()
    /** Integer primitive. [numericTypeName] preserves the original declared width (e.g. "Int", "Long") for display. */
    data class IntegerValue(val value: Long, val numericTypeName: String = "Int") : ScriptValue()
    /** Floating-point primitive. [numericTypeName] preserves the original declared precision (e.g. "Real", "Float") for display. */
    data class RealValue(val value: Double, val numericTypeName: String = "Real") : ScriptValue()
    /** Wider-than-Long integer kept in a `Long` for the interpreter; [numericTypeName] records the declared type. */
    data class BigIntegerValue(val value: Long, val numericTypeName: String) : ScriptValue()
    /** Wider-than-Double real kept in a `Double` for the interpreter. */
    data class BigRealValue(val value: Double) : ScriptValue()
    /** UTF-16 string. */
    data class TextValue(val value: String) : ScriptValue()
    /** Single character. */
    data class CharValue(val value: Char) : ScriptValue()
    /**
     * One case of a user-defined enum. [enumId] identifies the declaring enum, [value] is the case
     * name; [fields] is non-empty only for enums whose cases carry associated values.
     */
    data class EnumValue(val enumId: String, val value: String, val fields: Map<String, ScriptValue> = emptyMap()) : ScriptValue()
    /**
     * Instance of a user-defined data class. [fields] is a mutable map so `DATA_CLASS_SET_FIELD`
     * can update individual fields in place.
     */
    data class DataClassValue(
        val classId: String,
        val fields: MutableMap<String, ScriptValue>
    ) : ScriptValue()
    /** Ordered collection. [isMutable] gates mutation operations at the language level. */
    data class ArrayValue(val elements: MutableList<ScriptValue>, val isMutable: Boolean) : ScriptValue()
    /** Unordered, deduplicated collection backed by a list for predictable iteration order. [isMutable] gates mutation. */
    data class SetValue(val elements: MutableList<ScriptValue>, val isMutable: Boolean) : ScriptValue()
    /** Key-value collection backed by a list of pairs to preserve insertion order. [isMutable] gates mutation. */
    data class MapValue(val entries: MutableList<Pair<ScriptValue, ScriptValue>>, val isMutable: Boolean) : ScriptValue()
    /** Reference to a member of a named scope/namespace, e.g. `Foo::bar`. */
    data class ScopeMemberValue(val scope: String, val member: String) : ScriptValue()
    /**
     * First-class function value capturing its parameter list, AST body, and closure environment.
     * [body] and [closure] are typed as `Any` so this domain module stays AST-implementation agnostic.
     */
    data class LambdaValue(val params: List<String>, val body: Any, val closure: Any, val isVariadic: Boolean = false, val whereConstraint: Any? = null) : ScriptValue()
    /** Handle to an asynchronous computation; [deferred] is the underlying coroutine `Deferred`. */
    data class TaskValue(val deferred: Any) : ScriptValue()
    /** A coroutine/flow callable with bound arguments awaiting collection. */
    data class FlowValue(val name: String, val params: List<Any>, val body: Any, val closure: Any, val args: List<ScriptValue>) : ScriptValue()
    /** A failure value from a `fail` set, identifying the set and variant that failed. */
    data class FailValue(val failSetName: String, val variantName: String) : ScriptValue()
    /** Manually-managed pointer. [isFreed] is set after explicit deallocation; deref after free is unsafe. */
    data class PointerValue(var pointee: ScriptValue, val typeName: String, var isFreed: Boolean = false) : ScriptValue()
    /** Mutual-exclusion lock guarding [inner]; [isLocked] is the runtime state. */
    data class MutexValue(var inner: ScriptValue, var isLocked: Boolean = false) : ScriptValue()
    /** Reader/writer lock guarding [inner]; tracks active [readers] and the [isWriteLocked] flag. */
    data class RwLockValue(var inner: ScriptValue, var readers: Int = 0, var isWriteLocked: Boolean = false) : ScriptValue()
    /** Atomic cell wrapping [inner] for lock-free updates. */
    data class AtomicValue(var inner: ScriptValue) : ScriptValue()
    /** Synchronization barrier for [count] participants; [arrived] tracks how many have reached it. */
    data class BarrierValue(val count: Int, var arrived: Int = 0) : ScriptValue()
    /** One-shot guard; [executed] flips true after the protected action runs. */
    data class OnceValue(var executed: Boolean = false) : ScriptValue()
    /** Worker pool with a fixed number of [workers]. */
    data class ThreadPoolValue(val workers: Int) : ScriptValue()
    /** Value confined to a single thread; access from elsewhere is illegal. */
    data class IsolatedValue(var inner: ScriptValue) : ScriptValue()
    /** Handle to a running thread; [deferred] is the underlying coroutine handle. */
    data class ThreadHandleValue(val deferred: Any) : ScriptValue()
    /**
     * Singleton-style instance produced by a `solo` declaration. [methods] maps each method name to
     * its AST node (typed `Any` here to keep this module AST-agnostic).
     */
    data class SoloInstanceValue(
        val soloName: String,
        val fields: MutableMap<String, ScriptValue>,
        val methods: Map<String, Any>
    ) : ScriptValue()
    /** Activated module wrapper holding the live `solo` instances under [moduleName]. */
    data class WrapValue(
        val moduleName: String,
        val soloInstances: MutableMap<String, ScriptValue>,
        val isActive: Boolean
    ) : ScriptValue()
    /** The absent-value sentinel. */
    data object NullValue : ScriptValue()

    /**
     * Render this value as a user-visible string for `PRINT` nodes and the script console.
     * Composite values produce a parenthesized form that mirrors source syntax (e.g. `Point(x: 1, y: 2)`);
     * opaque concurrency primitives produce a `<…>` placeholder.
     */
    fun toDisplayString(): String = when (this) {
        is BooleanValue -> value.toString()
        is IntegerValue -> value.toString()
        is RealValue -> value.toString()
        is BigIntegerValue -> value.toString()
        is BigRealValue -> value.toString()
        is TextValue -> value
        is CharValue -> value.toString()
        is EnumValue -> if (fields.isEmpty()) "$enumId.$value"
            else "$enumId.$value(${fields.entries.joinToString(", ") { "${it.key}: ${it.value.toDisplayString()}" }})"
        is DataClassValue -> "${demangleGenericName(classId)}(${fields.entries.joinToString(", ") { "${it.key}: ${it.value.toDisplayString()}" }})"
        is ArrayValue -> "[${elements.joinToString(", ") { it.toDisplayString() }}]"
        is SetValue -> "![${elements.joinToString(", ") { it.toDisplayString() }}]"
        is MapValue -> "{${entries.joinToString(", ") { "${it.first.toDisplayString()}: ${it.second.toDisplayString()}" }}}"
        is ScopeMemberValue -> "$scope::$member"
        is LambdaValue -> if (isVariadic) "<variadic-lambda>" else "<lambda(${params.joinToString(", ")})>"
        is TaskValue -> "<task>"
        is FlowValue -> "<flow:$name>"
        is FailValue -> "fail $failSetName.$variantName"
        is PointerValue -> if (isFreed) "<freed pointer>" else "<pointer:$typeName → ${pointee.toDisplayString()}>"
        is MutexValue -> "<Mutex(${inner.toDisplayString()})>"
        is RwLockValue -> "<RwLock(${inner.toDisplayString()})>"
        is AtomicValue -> "<Atomic(${inner.toDisplayString()})>"
        is BarrierValue -> "<Barrier($count)>"
        is OnceValue -> "<Once(executed=$executed)>"
        is ThreadPoolValue -> "<ThreadPool($workers)>"
        is IsolatedValue -> "<isolated(${inner.toDisplayString()})>"
        is ThreadHandleValue -> "<thread>"
        is SoloInstanceValue -> "<solo:$soloName>"
        is WrapValue -> "<wrap:$moduleName>"
        is NullValue -> "null"
    }

    /**
     * Truthiness coercion used by `IF`, `AND`, `OR`, `NOT`, and similar control nodes.
     *
     * Numbers are false at zero, strings/collections are false when empty, [FailValue] and
     * [NullValue] are always false, and concurrency primitives are always true.
     */
    fun toBoolean(): Boolean = when (this) {
        is BooleanValue -> value
        is IntegerValue -> value != 0L
        is RealValue -> value != 0.0
        is BigIntegerValue -> value != 0L
        is BigRealValue -> value != 0.0
        is TextValue -> value.isNotEmpty()
        is CharValue -> value != '\u0000'
        is EnumValue -> true
        is DataClassValue -> true
        is ArrayValue -> elements.isNotEmpty()
        is SetValue -> elements.isNotEmpty()
        is MapValue -> entries.isNotEmpty()
        is ScopeMemberValue -> true
        is LambdaValue -> true
        is TaskValue -> true
        is FlowValue -> true
        is FailValue -> false
        is PointerValue -> !isFreed
        is MutexValue -> true
        is RwLockValue -> true
        is AtomicValue -> true
        is BarrierValue -> true
        is OnceValue -> true
        is ThreadPoolValue -> true
        is IsolatedValue -> true
        is ThreadHandleValue -> true
        is SoloInstanceValue -> true
        is WrapValue -> true
        is NullValue -> false
    }

    /**
     * Coerce to a `Double` for floating-point arithmetic and comparison. Strings parse via
     * [String.toDoubleOrNull], characters use their code point, booleans map to 0/1, and
     * non-numeric variants fall back to 0.0.
     */
    fun toNumericDouble(): Double = when (this) {
        is IntegerValue -> value.toDouble()
        is RealValue -> value
        is BigIntegerValue -> value.toDouble()
        is BigRealValue -> value
        is BooleanValue -> if (value) 1.0 else 0.0
        is TextValue -> value.toDoubleOrNull() ?: 0.0
        is CharValue -> value.code.toDouble()
        else -> 0.0
    }

    /**
     * Coerce to a `Long` for integer arithmetic, indexing and comparison. Reals truncate toward zero,
     * strings parse via [String.toLongOrNull], characters use their code point, booleans map to 0/1,
     * and non-numeric variants fall back to 0.
     */
    fun toNumericLong(): Long = when (this) {
        is IntegerValue -> value
        is RealValue -> value.toLong()
        is BigIntegerValue -> value
        is BigRealValue -> value.toLong()
        is BooleanValue -> if (value) 1L else 0L
        is TextValue -> value.toLongOrNull() ?: 0L
        is CharValue -> value.code.toLong()
        else -> 0L
    }

    /** True for any integer variant ([IntegerValue], [BigIntegerValue]). */
    val isInteger: Boolean get() = this is IntegerValue || this is BigIntegerValue
    /** True for any floating-point variant ([RealValue], [BigRealValue]). */
    val isReal: Boolean get() = this is RealValue || this is BigRealValue
    /** True for any numeric variant - the union of [isInteger] and [isReal]. */
    val isNumeric: Boolean get() = isInteger || isReal

    companion object {
        /** Converts a mangled generic name like `Pair__Int__Int` back to `Pair<Int, Int>`. */
        fun demangleGenericName(name: String): String {
            val parts = name.split("__")
            return if (parts.size > 1) {
                "${parts[0]}<${parts.drop(1).joinToString(", ")}>"
            } else {
                name
            }
        }
    }
}
