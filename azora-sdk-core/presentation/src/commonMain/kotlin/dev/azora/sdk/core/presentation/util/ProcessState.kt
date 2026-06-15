package dev.azora.sdk.core.presentation.util

/**
 * Represents the state of an ongoing process, such as a network request,
 * database operation, background task, or any long-running action.
 *
 * This class is commonly used in ViewModels, BLoCs, and state holders to express:
 *  - Whether the process is currently running (`inProcess`)
 *  - Whether the process is available to start (`available`)
 *  - An optional error message (`error`) when the operation fails
 *
 * Typical usage:
 * ```
 * val uploadState = ProcessState(
 *     inProcess = true,
 *     available = false
 * )
 * ```
 *
 * You might use this state to:
 *  - Show or hide loading indicators
 *  - Disable UI actions when a process is active
 *  - Display errors after failed operations
 *
 * @property inProcess True when the process is currently running.
 * @property available True when the process is ready to be triggered.
 * @property error Optional UI-friendly error to display if the process failed.
 */
data class ProcessState(
    val inProcess: Boolean = false,
    val available: Boolean = false,
    val error: UiText? = null
)