package dev.azora.studio.project_manager.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.studio.settings.LibrariesSettingsContent

/**
 * Opens the Libraries management UI in its own OS window (desktop) / modal
 * dialog (mobile) from the Project Browser. Installed libraries (e.g. the
 * Azora Engine) contribute project templates such as "App" and "Game" to the
 * create-project dialog.
 */
@Composable
fun LibrariesBrowserWindow(onCloseRequest: () -> Unit) {
    val palette = LocalAzoraPalette.current
    PluginsWindow(onCloseRequest = onCloseRequest, title = "Libraries") {
        Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
            LibrariesSettingsContent()
        }
    }
}
