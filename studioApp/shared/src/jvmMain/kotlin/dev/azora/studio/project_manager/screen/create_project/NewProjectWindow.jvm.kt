package dev.azora.studio.project_manager.screen.create_project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.awt.Dimension

@Composable
actual fun NewProjectWindow(
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = rememberDialogState(width = 720.dp, height = 620.dp),
        title = "New Project"
    ) {
        // Enforce a minimum size on the underlying AWT window so it can't be
        // resized smaller than the form needs.
        LaunchedEffect(window) {
            window.minimumSize = Dimension(560, 480)
        }
        content()
    }
}
