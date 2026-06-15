package dev.azora.sdk.docking.presentation.panel

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that provides access to the [DockPanelRegistry].
 *
 * This composition local allows dock composables to access the panel registry
 * to retrieve content composables for rendering. It is provided by [DockTheme]
 * and should not typically be provided manually.
 *
 * ## Usage
 *
 * Access the registry within dock composables:
 *
 * ```kotlin
 * @Composable
 * fun MyDockPanel(panelId: String) {
 *     val registry = LocalDockPanelRegistry.current
 *     val content = registry.getContent(panelId)
 *     content?.invoke()
 * }
 * ```
 *
 * ## Error Handling
 *
 * If accessed outside of a [DockTheme], an error will be thrown. Always
 * ensure dock composables are wrapped in a [DockTheme] or provide the
 * registry explicitly via [CompositionLocalProvider].
 *
 * @see DockPanelRegistry for the registry implementation
 * @see DockTheme for the standard way to provide the registry
 */
val LocalDockPanelRegistry = staticCompositionLocalOf<DockPanelRegistry> {
    error("DockPanelRegistry not provided")
}