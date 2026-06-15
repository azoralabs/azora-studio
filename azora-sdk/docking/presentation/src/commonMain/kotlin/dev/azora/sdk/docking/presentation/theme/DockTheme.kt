package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.*
import dev.azora.sdk.docking.presentation.panel.DockPanelRegistry
import dev.azora.sdk.docking.presentation.panel.LocalDockPanelRegistry

/**
 * Theme provider for the docking system.
 *
 * Wraps dock content with [CompositionLocalProvider] to supply colors,
 * dimensions, and the panel registry to all dock composables. This is
 * the standard way to configure dock appearance.
 *
 * ## Usage
 *
 * Wrap your dock UI in a DockTheme:
 *
 * ```kotlin
 * DockTheme(
 *     colors = defaultDockColors(),
 *     dimensions = DockDimensions()
 * ) {
 *     DockContainer(
 *         layout = layout,
 *         dragState = dragState,
 *         onAction = { dispatch(it) }
 *     )
 * }
 * ```
 *
 * ## Custom Theming
 *
 * Override any theme component:
 *
 * ```kotlin
 * // Custom colors
 * val myColors = defaultDockColors().copy(
 *     tabIndicator = Color.Red
 * )
 *
 * // Compact dimensions
 * val compactDims = DockDimensions(
 *     tabHeight = 28.dp,
 *     panelHeaderHeight = 32.dp
 * )
 *
 * DockTheme(
 *     colors = myColors,
 *     dimensions = compactDims
 * ) { ... }
 * ```
 *
 * ## Panel Registry
 *
 * The panel registry maps panel IDs to their content composables. It's
 * typically managed automatically, but can be provided explicitly for
 * advanced use cases.
 *
 * @param colors Color scheme for dock UI (default: from Azora palette)
 * @param dimensions Size specifications for dock UI (default: standard sizes)
 * @param registry Panel content registry (default: new empty registry)
 * @param content The dock UI content to render
 *
 * @see DockColors for available color properties
 * @see DockDimensions for available dimension properties
 * @see dev.azora.sdk.docking.presentation.panel.DockPanelRegistry for panel content registration
 * @see defaultDockColors for the default color factory
 */
@Composable
fun DockTheme(
    colors: DockColors = defaultDockColors(),
    dimensions: DockDimensions = DockDimensions(),
    registry: DockPanelRegistry = remember { DockPanelRegistry() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDockColors provides colors,
        LocalDockDimensions provides dimensions,
        LocalDockPanelRegistry provides registry
    ) {
        content()
    }
}