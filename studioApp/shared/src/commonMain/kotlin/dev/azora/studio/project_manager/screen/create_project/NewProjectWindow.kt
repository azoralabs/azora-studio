package dev.azora.studio.project_manager.screen.create_project

import androidx.compose.runtime.Composable

/**
 * Hosts the "New Project" UI.
 *
 * On desktop this opens a separate OS window; on mobile it falls back to an
 * in-app modal dialog.
 *
 * @param onCloseRequest Invoked when the window/dialog is dismissed.
 * @param content The New Project form content.
 */
@Composable
expect fun NewProjectWindow(
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
)
