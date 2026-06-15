package dev.azora.sdk.core.presentation.screen

import androidx.lifecycle.*
import kotlinx.coroutines.flow.*

/**
 * A global ViewModel responsible for screen-wide UI state.
 *
 * This ViewModel is typically scoped to the entire application UI,
 * not to a single screen. It exposes a [StateFlow] of [ScreenState]
 * and allows composables to trigger global UI changes, such as showing
 * or hiding dialogs.
 *
 * It should generally be created once (e.g., at the app root level),
 * and shared across the navigation graph.
 */
class ScreenViewModel() : ViewModel() {

    /**
     * Backing state flow that holds the current [ScreenState].
     * Use [state] to observe changes.
     */
    private val _state = MutableStateFlow(ScreenState())

    /**
     * Public, read-only state flow containing the global screen state.
     */
    val state = _state

    /**
     * Handles incoming UI actions and updates the state accordingly.
     *
     * @param action The [ScreenAction] triggered by user interaction.
     */
    fun onAction(action: ScreenAction) = when (action) {
        is ScreenAction.PushDialog -> {
            _state.update {
                it.copy(
                    onDialog = true,
                    blurAmount = action.blurAmount ?: it.blurAmount
                )
            }
        }
        ScreenAction.PopDialog -> {
            _state.update {
                it.copy(onDialog = false)
            }
        }
    }
}