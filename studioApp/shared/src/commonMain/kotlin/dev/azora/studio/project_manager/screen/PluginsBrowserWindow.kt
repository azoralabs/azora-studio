package dev.azora.studio.project_manager.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.studio.settings.PluginsSettingsContent

/**
 * Opens the Plugins management UI in its own OS window (desktop) / modal dialog (mobile) from the
 * Project Browser. Reuses the same install/enable/disable UI as Settings → Plugins; the "Launch"
 * action is hidden since there is no open project.
 */
@Composable
fun PluginsBrowserWindow(onCloseRequest: () -> Unit) {
    val palette = LocalAzoraPalette.current
    PluginsWindow(onCloseRequest = onCloseRequest) {
        Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
            PluginsSettingsContent(showLaunchButton = false)
        }
    }
}
