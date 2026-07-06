package dev.azora.studio.project_manager.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
actual fun PluginsWindow(
    onCloseRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onCloseRequest) {
        content()
    }
}
