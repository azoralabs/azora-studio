package dev.azora.sdk.plugin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Screen that displays the content of a loaded plugin.
 * If the plugin is not loaded, shows a fallback message.
 */
@Composable
fun PluginScreen(
    pluginId: String,
    project: AzoraProjectModel,
    pluginManager: PluginManager
) {
    val pluginContent = pluginManager.getPluginContent(pluginId)

    if (pluginContent != null) {
        pluginContent(project)
    } else {
        // Plugin not loaded - show placeholder
        PluginNotLoadedScreen(pluginId)
    }
}

@Composable
private fun PluginNotLoadedScreen(pluginId: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzoraPalette.Neutral85),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Plugin Not Loaded",
                color = AzoraPalette.Neutral30,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Plugin \"$pluginId\" could not be loaded.",
                color = AzoraPalette.Neutral50,
                fontSize = 13.sp
            )
            Text(
                text = "Please check the plugin installation or restart the application.",
                color = AzoraPalette.Neutral60,
                fontSize = 11.sp
            )
        }
    }
}
