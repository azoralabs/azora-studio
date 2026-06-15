package dev.azora.launcher

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.*
import dev.azora.sdk.plugin.core.*
import java.awt.Dimension
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT),
        position = WindowPosition(Alignment.Center)
    )

    var plugins by remember { mutableStateOf<List<AzoraPlugin>>(emptyList()) }

    // Load plugins on startup
    LaunchedEffect(Unit) {
        val pluginsDir = File(System.getProperty("user.home"), ".azora/plugins")
        if (pluginsDir.exists()) {
            plugins = PluginLoader.loadPlugins(pluginsDir)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Azora Launcher",
        state = windowState
    ) {
        window.minimumSize = Dimension(
            MIN_WINDOW_WIDTH.value.toInt(),
            MIN_WINDOW_HEIGHT.value.toInt()
        )

        LauncherApp(plugins = plugins)
    }
}

