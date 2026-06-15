package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Controls whether tabs can be dragged out of their container to become
 * floating windows or to dock at a different location.
 *
 * Defaults to `true`. Set to `false` for nested dock containers where
 * panels should stay confined to their parent.
 */
val LocalAllowExternalDrag = compositionLocalOf { true }
