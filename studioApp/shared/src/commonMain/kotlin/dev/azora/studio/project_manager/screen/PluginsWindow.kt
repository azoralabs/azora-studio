package dev.azora.studio.project_manager.screen

import androidx.compose.runtime.Composable

/**
 * Hosts a management UI (Plugins, Libraries, …) opened from the Project Browser.
 *
 * On desktop this opens a separate OS window; on mobile it falls back to an
 * in-app modal dialog.
 *
 * @param onCloseRequest Invoked when the window/dialog is dismissed.
 * @param title The window title (desktop only).
 * @param content The window content.
 */
@Composable
expect fun PluginsWindow(
    onCloseRequest: () -> Unit,
    title: String = "Plugins",
    content: @Composable () -> Unit
)
