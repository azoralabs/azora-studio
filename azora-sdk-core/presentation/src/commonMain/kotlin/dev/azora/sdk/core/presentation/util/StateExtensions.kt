package dev.azora.sdk.core.presentation.util

/**
 * Resets the [ProcessState] to its default, idle configuration.
 *
 * This clears any running operation state, disables availability,
 * and removes any associated error.
 *
 * Useful when restarting a workflow or after a successful/failed operation
 * that should return the UI to its initial state.
 */
fun ProcessState.reset(): ProcessState = copy(
    inProcess = false,
    available = false,
    error = null
)

/**
 * Resets the [FieldState] wrapping a String and optionally
 * initializes it with a new value.
 *
 * This function:
 * - Sets the field to the provided value or empty string
 * - Resets validation state and clears any associated error.
 *
 * This is useful when reloading form data, resetting input after submission,
 * or synchronizing UI state with a new domain model.
 *
 * @param value Optional text to set. If null, the field will be empty.
 */
fun FieldState<String>.reset(
    value: String? = null
): FieldState<String> = copy(
    field = value ?: "",
    isValid = false,
    error = null
)

/**
 * Clears validation errors from the [FieldState] without modifying
 * the underlying input value.
 *
 * This resets the field to a neutral validation state by:
 * - Removing any associated error message
 * - Marking the field as not validated
 *
 * Useful when resetting form errors after user interaction,
 * screen re-entry, or before re-validating inputs.
 */
fun <T> FieldState<T>.clearErrors(): FieldState<T> = copy(
    error = null,
    isValid = false
)