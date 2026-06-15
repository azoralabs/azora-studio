package dev.azora.sdk.core.util

import kotlinx.datetime.*
import kotlinx.serialization.json.*
import kotlin.time.Instant

/**
 * Converts a string to camelCase format.
 *
 * The conversion process:
 * 1. Removes all non-alphanumeric characters except spaces
 * 2. Trims leading/trailing whitespace
 * 3. Splits on whitespace
 * 4. Lowercases the first word
 * 5. Capitalizes the first letter of subsequent words
 * 6. Joins all words together without spaces
 *
 * @receiver the string to convert
 * @return the camelCase formatted string
 *
 * Examples:
 * - "Hello World" -> "helloWorld"
 * - "user-name_value" -> "usernameValue"
 * - "API Response Data" -> "apiResponseData"
 */
fun String.toCamelCase() = this
    .replace(Regex("[^a-zA-Z0-9\\s]"), "")
    .trim()
    .split("\\s+".toRegex())
    .mapIndexed { index, word ->
        if (index == 0) word.lowercase()
        else word.replaceFirstChar { it.titlecase() }
    }
    .joinToString("")

/**
 * Capitalizes the first character of the string while converting the rest to lowercase.
 *
 * @receiver the string to capitalize
 * @return the string with only the first character in uppercase
 *
 * Examples:
 * - "hello" -> "Hello"
 * - "WORLD" -> "World"
 * - "mIxEd CaSe" -> "Mixed case"
 */
fun String.capitalize() = this.lowercase().replaceFirstChar { it.uppercase() }

/**
 * Executes an action if the boolean value is false, otherwise returns true.
 *
 * This is useful for validation chains where you want to execute error handling
 * only when a condition fails.
 *
 * @receiver the boolean condition to check
 * @param action the action to execute if the condition is false
 * @return true if the original condition was false (after executing the action),
 *         true if the original condition was true
 *
 * Example:
 * ```
 * val isValid = username.isNotEmpty()
 * isValid.failed { throw IllegalArgumentException("Username cannot be empty") }
 * ```
 */
inline fun Boolean.failed(action: () -> Unit) = if (!this) action() else true

/**
 * Safely converts any nullable value to a human-readable [String].
 *
 * This extension is especially useful when working with dynamic data sources
 * such as JSON, Maps, APIs, or mixed-type responses.
 *
 * Behavior:
 * - If the value is a [JsonElement], it extracts the actual string content
 *   (avoids quoted JSON output like `"text"`).
 * - If the value is already a [String], it returns it directly.
 * - If the value is `null`, it returns an empty string.
 * - For any other type, it falls back to [toString()].
 *
 * @return A safe, non-null [String] representation of the value.
 *
 * @sample
 * ```
 * val jsonValue: JsonElement? = JsonPrimitive("Hello")
 * jsonValue.asSafeString() // returns "Hello"
 *
 * val text: Any? = null
 * text.asSafeString() // returns ""
 *
 * val number: Any? = 42
 * number.asSafeString() // returns "42"
 * ```
 */
fun Any?.asSafeString() = when (this) {
    is JsonElement -> this.jsonPrimitive.content
    is String -> this
    null -> ""
    else -> this.toString()
}

/**
 * Executes the given [block] if the receiver is null, otherwise returns the receiver.
 *
 * This function provides a fluent way to handle null cases after operations like `let`,
 * allowing you to chain an alternative execution path when the preceding operation
 * returns null.
 *
 * @param T The type of the receiver (can be null)
 * @param R The non-null return type of both the receiver and the [block]
 * @param block A lambda function that will be executed if the receiver is null.
 *              Must return a non-null value of type [R].
 * @return The receiver if it's not null, or the result of [block] if the receiver is null.
 *
 * @sample
 * ```
 * val result = status?.let {
 *     "Status: $it"
 * }.orElse {
 *     "No status available"
 * }
 * ```
 *
 * @see let
 * @see run
 */
inline fun <T, R> R?.orElse(block: () -> R): R = this ?: block()

/**
 * Executes the given [block] if the receiver is null.
 *
 * Useful for chaining null-handling logic in scenarios where you don't need a return value.
 *
 * Example:
 * ```
 * status?.let {
 *     DisplayStatus(it)
 * }.orElse {
 *     DisplayError()
 * }
 * ```
 */
inline fun <T> T?.orElse(block: () -> Unit) {
    if (this == null) block()
}

/**
 * Formats an Instant to a date string in yyyy/MM/dd format.
 *
 * @receiver the Instant to format
 * @return the formatted date string
 *
 * Example:
 * ```
 * val instant = Clock.System.now()
 * instant.toDateString() // returns "2025/12/30"
 * ```
 */
fun Instant.toDateString(): String {
    val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())
    val year = localDateTime.year
    val month = localDateTime.month.number.toString().padStart(2, '0')
    val day = localDateTime.date.day.toString().padStart(2, '0')
    return "$year/$month/$day"
}