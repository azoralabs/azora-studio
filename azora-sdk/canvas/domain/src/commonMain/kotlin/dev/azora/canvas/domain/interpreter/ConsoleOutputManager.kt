package dev.azora.canvas.domain.interpreter

import kotlinx.coroutines.flow.*
import kotlin.time.TimeSource

/** Severity of a [ConsoleMessage]; drives styling in the script console UI. */
enum class ConsoleMessageType {
    /** Regular script output - produced by `PRINT` nodes. */
    OUTPUT,
    /** A runtime error reported by the interpreter. */
    ERROR,
    /** A non-fatal warning. */
    WARNING,
    /** An informational message from tooling rather than the script itself. */
    INFO
}

/**
 * One line in the script console.
 *
 * @property text The message text.
 * @property type Severity used for styling.
 * @property timestamp Milliseconds since the owning [ConsoleOutputManager] was created - useful for
 *   sequencing output relative to a single script run rather than wall-clock time.
 */
data class ConsoleMessage(
    val text: String,
    val type: ConsoleMessageType,
    val timestamp: Long
)

/**
 * Collects and exposes script console output as a [StateFlow] for the editor UI.
 *
 * Behaves like a tiny stdout/stderr buffer:
 * - [print] appends to a buffer without flushing - so consecutive prints concatenate on one line.
 * - [println], [error], [warn] and [info] flush the print buffer first to preserve ordering, then
 *   emit a finished line.
 *
 * Timestamps are derived from a monotonic time source captured at construction, so message times
 * are relative to the start of this manager (typically one per script run).
 */
class ConsoleOutputManager {

    private val timeSource = TimeSource.Monotonic
    private val startMark = timeSource.markNow()

    private val _messages = MutableStateFlow<List<ConsoleMessage>>(emptyList())
    /** All messages emitted so far, in order. Replaced in full on each emission. */
    val messages: StateFlow<List<ConsoleMessage>> = _messages.asStateFlow()

    private val printBuffer = StringBuilder()

    /** Append [text] to the pending output line without emitting a message yet. */
    fun print(text: String) {
        printBuffer.append(text)
    }

    /** Append [text] and flush the pending output buffer as a single [ConsoleMessageType.OUTPUT] message. */
    fun println(text: String) {
        printBuffer.append(text)
        flush()
    }

    /** Flush any pending output, then emit [text] as an [ConsoleMessageType.ERROR] message. */
    fun error(text: String) {
        flush()
        addMessage(text, ConsoleMessageType.ERROR)
    }

    /** Flush any pending output, then emit [text] as a [ConsoleMessageType.WARNING] message. */
    fun warn(text: String) {
        flush()
        addMessage(text, ConsoleMessageType.WARNING)
    }

    /** Flush any pending output, then emit [text] as an [ConsoleMessageType.INFO] message. */
    fun info(text: String) {
        flush()
        addMessage(text, ConsoleMessageType.INFO)
    }

    /** Drop all messages and any buffered partial line. */
    fun clear() {
        printBuffer.clear()
        _messages.value = emptyList()
    }

    private fun flush() {
        if (printBuffer.isNotEmpty()) {
            addMessage(printBuffer.toString(), ConsoleMessageType.OUTPUT)
            printBuffer.clear()
        }
    }

    private fun addMessage(text: String, type: ConsoleMessageType) {
        val elapsed = startMark.elapsedNow().inWholeMilliseconds
        _messages.value += ConsoleMessage(text, type, elapsed)
    }
}
