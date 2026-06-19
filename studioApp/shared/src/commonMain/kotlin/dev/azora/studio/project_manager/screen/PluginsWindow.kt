package dev.azora.studio.project_manager.screen

import androidx.compose.runtime.Composable

/**
 * Hosts the Plugins management UI opened from the Project Browser.
 *
 * On desktop this opens a separate OS window (titled "Plugins"); on mobile it falls back to an
 * in-app modal dialog.
 *
 * @param onCloseRequest Invoked when the window/dialog is dismissed.
 * @param content The Plugins UI content.
 */
@Composable
expect fun PluginsWindow(
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
)
