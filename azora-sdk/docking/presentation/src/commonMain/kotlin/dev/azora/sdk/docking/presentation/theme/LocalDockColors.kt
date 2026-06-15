package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local providing [DockColors] to dock UI components.
 *
 * Access the current dock colors in any composable within a [DockTheme]:
 *
 * ```kotlin
 * @Composable
 * fun MyDockComponent() {
 *     val colors = LocalDockColors.current
 *     // Use colors.panelBackground, colors.tabText, etc.
 * }
 * ```
 *
 * This is a static composition local, meaning it won't trigger recomposition
 * when the colors change (colors are expected to be stable).
 *
 * @throws IllegalStateException if accessed outside a [DockTheme]
 * @see DockColors
 * @see DockTheme
 */
val LocalDockColors = staticCompositionLocalOf<DockColors> {
    error("DockColors not provided")
}