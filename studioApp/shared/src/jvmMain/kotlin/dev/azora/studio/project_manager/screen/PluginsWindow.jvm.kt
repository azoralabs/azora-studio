package dev.azora.studio.project_manager.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.awt.Dimension

@Composable
actual fun PluginsWindow(
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = rememberDialogState(width = 580.dp, height = 560.dp),
        title = "Plugins",
        resizable = true
    ) {
        LaunchedEffect(window) {
            window.minimumSize = Dimension(420, 440)
        }
        content()
    }
}
