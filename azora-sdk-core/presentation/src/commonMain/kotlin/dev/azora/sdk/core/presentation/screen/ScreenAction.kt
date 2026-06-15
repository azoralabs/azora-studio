package dev.azora.sdk.core.presentation.screen

import androidx.compose.ui.unit.Dp

/**
 * Represents all user-driven actions that modify [ScreenState].
 *
 * These actions typically originate from composables and instruct the
 * [ScreenViewModel] to update the screen-level UI.
 */
sealed interface ScreenAction {

    /**
     * Request to show a dialog.
     */
    data class PushDialog(val blurAmount: Dp? = null) : ScreenAction

    /**
     * Request to hide the currently displayed dialog.
     */
    data object PopDialog : ScreenAction
}