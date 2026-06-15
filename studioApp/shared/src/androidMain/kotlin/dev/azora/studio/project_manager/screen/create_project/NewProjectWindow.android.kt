package dev.azora.studio.project_manager.screen.create_project

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
actual fun NewProjectWindow(
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onCloseRequest) {
        content()
    }
}
