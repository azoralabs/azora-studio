package dev.azora.sdk.core.project.domain

/**
 * Default [CodeGenerator] implementation.
 *
 * Accumulates generated lines into a [StringBuilder], tracking the current indentation
 * depth so nested [CodeGenerator.GenScope.gen] blocks are indented automatically. Indentation
 * uses [indentUnit] (four spaces by default) repeated per depth level.
 *
 * The implementation is pure and platform-independent, so it can be unit-tested directly and
 * reused from any source set.
 *
 * @param indentUnit The string emitted once per indentation level. Defaults to four spaces.
 */
class CodeGeneratorImpl(
    private val indentUnit: String = "    "
) : CodeGenerator {

    private val builder = StringBuilder()

    override fun gen(block: CodeGenerator.GenScope.() -> Unit): CodeGenerator {
        ScopeImpl(depth = 0).block()
        return this
    }

    override fun build(): String = builder.toString()

    /**
     * Indentation-aware [CodeGenerator.GenScope]. Each nested [gen] call creates a new scope with
     * [depth] increased by one; all scopes write into the shared [builder].
     */
    private inner class ScopeImpl(private val depth: Int) : CodeGenerator.GenScope {

        override fun write(content: String) {
            // Preserve internal newlines: a multi-line string written as one call should have every
            // line indented at the current depth so callers can pass small blocks verbatim.
            if (content.isEmpty()) {
                emptyLine()
                return
            }
            val prefix = indentUnit.repeat(depth)
            content.split('\n').forEach { line ->
                if (line.isEmpty()) {
                    builder.append('\n')
                } else {
                    builder.append(prefix).append(line).append('\n')
                }
            }
        }

        override fun emptyLine() {
            builder.append('\n')
        }

        override fun gen(block: CodeGenerator.GenScope.() -> Unit) {
            ScopeImpl(depth = depth + 1).block()
        }

        override fun blank() {
            builder.append('\n')
        }
    }
}
