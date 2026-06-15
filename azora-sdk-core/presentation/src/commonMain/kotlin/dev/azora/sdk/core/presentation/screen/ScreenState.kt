package dev.azora.sdk.core.presentation.screen

import androidx.compose.ui.unit.*

/**
 * UI state for global screen-level controls across the application.
 *
 * @property onDialog When true, a modal dialog is currently shown.
 * This can be used to blur the background or block UI interaction
 * at the app level.
 * @property blurAmount The amount of blur to apply to the background.
 */
data class ScreenState(
    val onDialog: Boolean = false,
    val blurAmount: Dp = 8.dp
)