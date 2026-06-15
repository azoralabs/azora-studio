package dev.azora.canvas.domain

import kotlinx.serialization.Serializable

/**
 * Type of a global constant.
 */
enum class ConstantType {
    BOOLEAN,
    INTEGER,
    REAL,
    TEXT
}

/**
 * A global constant that can be used throughout the project.
 * Values are stored as strings and parsed based on the type.
 *
 * Ported from the previous Azora project-domain module; canvas scripting owns this
 * type directly since the new sdk-core project model no longer defines it.
 */
@Serializable
data class GlobalConstant(
    val id: String = generateId(),
    val name: String,
    val type: ConstantType,
    val value: String
) {
    companion object {
        private fun generateId(): String = kotlin.random.Random.nextLong().toString(36)

        /**
         * Creates a default constant of the specified type with a generated name.
         */
        fun createDefault(type: ConstantType, existingNames: Set<String> = emptySet()): GlobalConstant {
            val baseName = when (type) {
                ConstantType.BOOLEAN -> "boolConstant"
                ConstantType.INTEGER -> "intConstant"
                ConstantType.REAL -> "realConstant"
                ConstantType.TEXT -> "textConstant"
            }

            var name = baseName
            var counter = 1
            while (name in existingNames) {
                name = "$baseName$counter"
                counter++
            }

            val defaultValue = when (type) {
                ConstantType.BOOLEAN -> "false"
                ConstantType.INTEGER -> "0"
                ConstantType.REAL -> "0.0"
                ConstantType.TEXT -> ""
            }

            return GlobalConstant(
                name = name,
                type = type,
                value = defaultValue
            )
        }
    }

    /**
     * Returns the parsed value based on the constant's type.
     * Returns null if the value cannot be parsed.
     */
    fun parsedValue(): Any? = when (type) {
        ConstantType.BOOLEAN -> value.toBooleanStrictOrNull()
        ConstantType.INTEGER -> value.toLongOrNull()
        ConstantType.REAL -> value.toDoubleOrNull()
        ConstantType.TEXT -> value
    }

    /**
     * Checks if the current value is valid for the constant's type.
     */
    fun isValid(): Boolean = when (type) {
        ConstantType.BOOLEAN -> value.toBooleanStrictOrNull() != null
        ConstantType.INTEGER -> value.toLongOrNull() != null
        ConstantType.REAL -> value.toDoubleOrNull() != null
        ConstantType.TEXT -> true
    }
}
