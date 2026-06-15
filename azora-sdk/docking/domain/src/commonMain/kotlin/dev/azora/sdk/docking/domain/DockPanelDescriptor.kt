package dev.azora.sdk.docking.domain

import kotlinx.serialization.Serializable

/**
 * Metadata describing a dockable panel.
 *
 * A panel descriptor contains all the information needed to display and manage
 * a panel in the docking system, separate from the panel's actual content.
 * This separation allows the layout to be serialized and restored without
 * needing to serialize the UI components themselves.
 *
 * ## Usage
 *
 * Create descriptors for each panel type in your application:
 *
 * ```kotlin
 * val explorerDescriptor = DockPanelDescriptor(
 *     id = "explorer",
 *     title = "Explorer",
 *     icon = "folder",
 *     closeable = true
 * )
 *
 * val viewportDescriptor = DockPanelDescriptor(
 *     id = "viewport",
 *     title = "Viewport",
 *     closeable = false,  // Cannot be closed by user
 *     minimumWidth = 200f,
 *     minimumHeight = 200f
 * )
 * ```
 *
 * ## Panel Registration
 *
 * Descriptors define metadata; content is registered separately:
 *
 * ```kotlin
 * // Register the panel content with the UI
 * RegisterDockPanel("explorer") {
 *     ExplorerContent()
 * }
 * ```
 *
 * @property id Unique identifier for the panel. Used to reference the panel
 *              in layout operations and to match with registered content.
 * @property title Display title shown in the panel header and tabs.
 * @property icon Optional icon identifier for display in tabs/headers.
 *                The format depends on your icon system.
 * @property closeable Whether the user can close this panel. Non-closeable
 *                     panels don't show a close button in the header.
 * @property minimumWidth Minimum width in pixels. The panel and any floating
 *                        window containing it will not shrink below this.
 * @property minimumHeight Minimum height in pixels. The panel and any floating
 *                         window containing it will not shrink below this.
 *
 * @see DockLayout.panelDescriptors
 * @see DockDefaults for default minimum dimensions
 */
@Serializable
data class DockPanelDescriptor(
    val id: String,
    val title: String,
    val icon: String? = null,
    val closeable: Boolean = true,
    val minimumWidth: Float = DockDefaults.PANEL_MIN_WIDTH,
    val minimumHeight: Float = DockDefaults.PANEL_MIN_HEIGHT
)