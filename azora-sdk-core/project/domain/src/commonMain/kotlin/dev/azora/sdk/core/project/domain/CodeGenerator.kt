package dev.azora.sdk.core.project.domain

/**
 * Interface for generating formatted code with automatic indentation.
 *
 * This generator provides a DSL-style API for creating structured code output
 * with proper indentation levels managed automatically through nested blocks.
 *
 * Example usage:
 * ```kotlin
 * val generator: CodeGenerator = CodeGeneratorImpl()
 * generator.gen {
 *     write("class MyClass {")
 *     gen {
 *         write("fun myFunction() {")
 *         gen {
 *             write("println(\"Hello\")")
 *         }
 *         write("}")
 *     }
 *     write("}")
 * }
 * val code = generator.build()
 * ```
 */
interface CodeGenerator {

    /**
     * Starts a code generation block at the root indentation level (tab = 0).
     *
     * @param block The generation scope containing write operations and nested gen blocks.
     * @return The CodeGenerator instance for method chaining.
     */
    fun gen(block: GenScope.() -> Unit): CodeGenerator

    /**
     * Builds and returns the complete generated code as a string.
     *
     * @return The formatted code with proper indentation.
     */
    fun build(): String

    /**
     * Scope for writing code within a specific indentation level.
     *
     * Provides methods to write lines of code, create nested indentation blocks,
     * and insert blank lines while maintaining proper formatting.
     */
    interface GenScope {

        /**
         * Writes a line of code at the current indentation level.
         *
         * The line will be automatically indented based on the current nesting depth
         * and a newline will be appended.
         *
         * @param content The code content to write (without indentation or newline).
         */
        fun write(content: String)

        /**
         * Writes an empty line of code at the current indentation level.
         */
        fun emptyLine()

        /**
         * Creates a nested indentation block with tab level increased by 1.
         *
         * All write operations within this block will be indented one level deeper
         * than the current scope.
         *
         * @param block The nested generation scope with increased indentation.
         */
        fun gen(block: GenScope.() -> Unit)

        /**
         * Inserts a blank line in the generated code.
         *
         * Useful for improving readability by adding spacing between code sections.
         */
        fun blank()
    }
}
