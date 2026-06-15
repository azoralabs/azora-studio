package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color scheme for the docking system UI components.
 *
 * Defines all colors used throughout the dock UI, organized by component type.
 * Use [defaultDockColors] to create an instance that integrates with the
 * Azora theme, or create a custom instance for specialized theming.
 *
 * ## Color Categories
 *
 * **Panel Colors:** Background, header, text, icons, and borders for dock panels.
 *
 * **Tab Colors:** Tab bar, individual tabs (normal/active/hover), text, indicator,
 * and close button states.
 *
 * **Splitter Colors:** The draggable dividers between split panes with hover
 * and dragging states.
 *
 * **Drop Zone Colors:** Visual feedback during drag-and-drop showing where
 * panels can be docked.
 *
 * **Floating Window Colors:** Background, shadow, border, and header for
 * floating (undocked) windows.
 *
 * ## Usage
 *
 * Colors are provided via [LocalDockColors] composition local:
 *
 * ```kotlin
 * DockTheme(colors = defaultDockColors()) {
 *     // Dock components access colors via LocalDockColors.current
 *     DockContainer(...)
 * }
 *
 * // Inside a dock component:
 * @Composable
 * fun DockPanel() {
 *     val colors = LocalDockColors.current
 *     Box(modifier = Modifier.background(colors.panelBackground)) {
 *         // ...
 *     }
 * }
 * ```
 *
 * @property panelBackground Background color for panel content areas
 * @property panelHeaderBackground Background color for panel headers (inactive)
 * @property panelHeaderBackgroundActive Background color for active panel headers
 * @property panelHeaderText Text color for panel header titles (inactive)
 * @property panelHeaderTextActive Text color for active panel header titles
 * @property panelHeaderIcon Icon color in panel headers
 * @property panelHeaderCloseIcon Close button icon color in panel headers
 * @property panelBorder Border color between panels
 * @property tabBarBackground Background color for the tab bar container
 * @property tabBackground Background color for inactive tabs
 * @property tabBackgroundActive Background color for the active/selected tab
 * @property tabBackgroundHover Background color for tabs on mouse hover
 * @property tabText Text color for inactive tab labels
 * @property tabTextActive Text color for the active tab label
 * @property tabIndicator Color for the active tab indicator line
 * @property tabCloseButton Close button color in tabs (normal state)
 * @property tabCloseButtonHover Close button color on hover
 * @property tabCloseButtonBackground Close button background on hover
 * @property splitterBackground Splitter color in normal state
 * @property splitterBackgroundHover Splitter color on mouse hover
 * @property splitterBackgroundDragging Splitter color while being dragged
 * @property dropZoneIndicator Fill color for drop zone overlays
 * @property dropZoneIndicatorBorder Border color for drop zone indicators
 * @property dropZoneHighlight Highlight color for hovered drop zones
 * @property floatingWindowBackground Background for floating window content
 * @property floatingWindowShadow Shadow color for floating windows
 * @property floatingWindowBorder Border color for floating windows
 * @property floatingWindowHeaderBackground Header background for floating windows
 *
 * @see defaultDockColors
 * @see LocalDockColors
 * @see DockTheme
 */
@Immutable
data class DockColors(
    // Panel colors
    val panelBackground: Color,
    val panelHeaderBackground: Color,
    val panelHeaderBackgroundActive: Color,
    val panelHeaderText: Color,
    val panelHeaderTextActive: Color,
    val panelHeaderIcon: Color,
    val panelHeaderCloseIcon: Color,
    val panelBorder: Color,

    // Tab colors
    val tabBarBackground: Color,
    val tabBackground: Color,
    val tabBackgroundActive: Color,
    val tabBackgroundHover: Color,
    val tabText: Color,
    val tabTextActive: Color,
    val tabIndicator: Color,
    val tabCloseButton: Color,
    val tabCloseButtonHover: Color,
    val tabCloseButtonBackground: Color,

    // Splitter colors
    val splitterBackground: Color,
    val splitterBackgroundHover: Color,
    val splitterBackgroundDragging: Color,

    // Drop zone colors
    val dropZoneIndicator: Color,
    val dropZoneIndicatorBorder: Color,
    val dropZoneHighlight: Color,

    // Floating window colors
    val floatingWindowBackground: Color,
    val floatingWindowShadow: Color,
    val floatingWindowBorder: Color,
    val floatingWindowHeaderBackground: Color
)