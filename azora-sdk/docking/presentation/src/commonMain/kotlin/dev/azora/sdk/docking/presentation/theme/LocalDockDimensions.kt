package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local providing [DockDimensions] to dock UI components.
 *
 * Access the current dock dimensions in any composable within a [DockTheme]:
 *
 * ```kotlin
 * @Composable
 * fun MyDockComponent() {
 *     val dimensions = LocalDockDimensions.current
 *     Box(modifier = Modifier
 *         .height(dimensions.tabHeight)
 *         .padding(dimensions.tabPadding)
 *     ) {
 *         // ...
 *     }
 * }
 * ```
 *
 * Unlike [LocalDockColors], this composition local has a default value
 * ([DockDimensions] with default parameters), so it can be accessed
 * outside a [DockTheme] without throwing an exception.
 *
 * This is a static composition local, meaning it won't trigger recomposition
 * when dimensions change (dimensions are expected to be stable).
 *
 * @see DockDimensions for available dimension properties
 * @see DockTheme for providing custom dimensions
 */
val LocalDockDimensions = staticCompositionLocalOf { DockDimensions() }