package dev.azora.sdk.core.presentation.util

/**
 * Represents the state of a generic form field.
 *
 * This class is used to hold:
 *  - The current value of the field (`field`)
 *  - Whether the field is valid (`isValid`)
 *  - The validation error to display (`error`), if any
 *
 * `FieldState` is generic, meaning you can represent the state of any type of input:
 *  - Text fields (String)
 *  - Numbers
 *  - Dates
 *  - Domain models
 *  - Custom UI components
 *
 * Typical usage:
 * ```
 * val nameState = FieldState(
 *     field = "John",
 *     isValid = true
 * )
 * ```
 *
 * In most cases, each input in a form is modeled by its own `FieldState<T>`.
 *
 * @param T Type of the underlying field value.
 * @property field Current value of the field.
 * @property isValid Whether this field is currently valid.
 * @property error Optional UI-friendly error message shown when the field is invalid.
 */
data class FieldState<T>(
    val field: T,
    val isValid: Boolean = false,
    val error: UiText? = null
) {

    companion object {

        /**
         * Factory helper for creating a default text field state.
         *
         * This initializes the field using an empty String or one
         * pre-initialized with [initialText].
         *
         * Example:
         * ```
         * val emailState = FieldState.textField()
         * val usernameState = FieldState.textField("john_doe")
         * ```
         *
         * @param initialText The initial text value.
         * Defaults to an empty string.
         *
         * @return A new [FieldState] instance wrapping a String.
         */
        fun textField(initialText: String = "") = FieldState(initialText)
    }
}