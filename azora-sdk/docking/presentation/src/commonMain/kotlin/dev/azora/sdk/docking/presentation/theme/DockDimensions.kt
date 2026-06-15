package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimension specifications for dock UI components.
 *
 * Defines all size-related values used in the docking system, ensuring
 * consistent spacing and proportions across the UI. Use [LocalDockDimensions]
 * to access these values in composables.
 *
 * ## Dimension Categories
 *
 * **Panel Dimensions:** Header height, corner radius, and internal padding.
 *
 * **Tab Dimensions:** Tab bar height, close button size, padding, spacing,
 * corner radius, and the active indicator height.
 *
 * **Splitter Dimensions:** Visual size and hit area (larger for easier interaction).
 *
 * **Drop Zone Dimensions:** Button sizes, corner radius, and spacing for the
 * dock zone indicators shown during drag operations.
 *
 * **Floating Window Dimensions:** Minimum sizes, resize handle dimensions,
 * corner radius, and shadow elevation.
 *
 * ## Usage
 *
 * Access dimensions via the composition local:
 *
 * ```kotlin
 * @Composable
 * fun MyDockComponent() {
 *     val dimensions = LocalDockDimensions.current
 *     Box(modifier = Modifier.height(dimensions.tabHeight)) {
 *         // ...
 *     }
 * }
 * ```
 *
 * ## Customization
 *
 * Create a custom instance to adjust sizing:
 *
 * ```kotlin
 * val compactDimensions = DockDimensions(
 *     tabHeight = 28.dp,
 *     panelHeaderHeight = 32.dp
 * )
 * DockTheme(dimensions = compactDimensions) { ... }
 * ```
 *
 * @property panelHeaderHeight Height of panel header bars
 * @property panelCornerRadius Corner radius for panel containers
 * @property panelPadding Internal padding within panels
 * @property tabHeight Height of the tab bar and individual tabs
 * @property tabCloseButtonSize Size of the close button in tabs
 * @property tabPadding Horizontal padding inside each tab
 * @property tabSpacing Space between adjacent tabs
 * @property tabCornerRadius Corner radius for tab backgrounds
 * @property tabIndicatorHeight Height of the active tab indicator line
 * @property splitterSize Visual width/height of splitter bars
 * @property splitterHitArea Interactive hit area for splitters (larger than visual)
 * @property dropZoneIndicatorSize Size of edge drop zone buttons
 * @property dropZoneCenterSize Size of the center drop zone button
 * @property dropZoneCornerRadius Corner radius for drop zone elements
 * @property dropZoneSpacing Space between drop zone buttons
 * @property floatingWindowMinWidth Minimum width for floating windows
 * @property floatingWindowMinHeight Minimum height for floating windows
 * @property floatingWindowResizeHandleSize Size of corner resize handles
 * @property floatingWindowCornerRadius Corner radius for floating windows
 * @property floatingWindowShadowElevation Shadow elevation for floating windows
 *
 * @see LocalDockDimensions
 * @see DockTheme
 */
@Immutable
data class DockDimensions(
    // Panel dimensions
    val panelHeaderHeight: Dp = 36.dp,
    val panelCornerRadius: Dp = 6.dp,
    val panelPadding: Dp = 0.dp,

    // Tab dimensions
    val tabHeight: Dp = 32.dp,
    val tabCloseButtonSize: Dp = 18.dp,
    val tabPadding: Dp = 12.dp,
    val tabSpacing: Dp = 1.dp,
    val tabCornerRadius: Dp = 6.dp,
    val tabIndicatorHeight: Dp = 2.dp,

    // Splitter dimensions
    val splitterSize: Dp = 3.dp,
    val splitterHitArea: Dp = 6.dp,

    // Drop zone dimensions
    val dropZoneIndicatorSize: Dp = 48.dp,
    val dropZoneCenterSize: Dp = 40.dp,
    val dropZoneCornerRadius: Dp = 8.dp,
    val dropZoneSpacing: Dp = 6.dp,

    // Floating window dimensions
    val floatingWindowMinWidth: Dp = 250.dp,
    val floatingWindowMinHeight: Dp = 180.dp,
    val floatingWindowResizeHandleSize: Dp = 10.dp,
    val floatingWindowCornerRadius: Dp = 8.dp,
    val floatingWindowShadowElevation: Dp = 16.dp
)