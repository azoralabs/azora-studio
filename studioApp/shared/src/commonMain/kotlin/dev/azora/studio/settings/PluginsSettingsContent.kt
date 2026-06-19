package dev.azora.studio.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.plugin.core.InstalledPlugin
import dev.azora.sdk.plugin.presentation.PluginManagerViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

/**
 * Plugins settings content for managing installed plugins.
 */
@Composable
fun PluginsSettingsContent(
    onLaunchPlugin: (String) -> Unit = {},
    /** Whether to show the per-plugin "Launch" action. Hidden where no project is open (e.g. the Project Browser). */
    showLaunchButton: Boolean = true
) {
    val viewModel: PluginManagerViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val palette = LocalAzoraPalette.current

    // Desktop provides a native JAR picker; on other targets it is unbound (button hidden).
    val filePicker = getKoin().getOrNull<PluginFilePicker>()

    // Load plugins on first composition
    LaunchedEffect(Unit) {
        viewModel.loadPlugins()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installed Plugins",
                color = palette.contentTop,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${state.installedPlugins.size} plugins",
                    color = palette.contentLow,
                    fontSize = 11.sp
                )
                if (filePicker != null) {
                    InstallPluginButton {
                        filePicker.pickPluginJar()?.let { path -> viewModel.installPlugin(path) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Plugin directory hint
        Text(
            text = "Plugins are loaded from ~/.azora/plugins/",
            color = palette.contentLow,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = palette.primary,
                    strokeWidth = 2.dp
                )
            }
        }

        // Error message
        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.error.copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Text(
                    text = error,
                    color = palette.error,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Plugin list
        if (!state.isLoading && state.installedPlugins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No plugins installed",
                        color = palette.contentMid,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Place plugin JAR files in ~/.azora/plugins/",
                        color = palette.contentLow,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.installedPlugins.forEach { plugin ->
                    PluginCard(
                        plugin = plugin,
                        onToggle = { enabled ->
                            viewModel.togglePlugin(plugin.id, enabled)
                        },
                        onLaunch = { onLaunchPlugin(plugin.id) },
                        showLaunchButton = showLaunchButton
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: InstalledPlugin,
    onToggle: (Boolean) -> Unit,
    onLaunch: () -> Unit,
    showLaunchButton: Boolean = true
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isHovered) palette.surfaceTop else palette.surfaceLow
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plugin info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin icon placeholder with color based on plugin name
                val accentColor = remember(plugin.name) {
                    val hash = plugin.name.hashCode()
                    Color(
                        red = ((hash shr 16) and 0xFF) / 255f * 0.6f + 0.4f,
                        green = ((hash shr 8) and 0xFF) / 255f * 0.6f + 0.4f,
                        blue = (hash and 0xFF) / 255f * 0.6f + 0.4f,
                        alpha = 1f
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plugin.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plugin.name,
                            color = palette.contentTop,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "v${plugin.version}",
                            color = palette.contentLow,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = plugin.description,
                        color = palette.contentMid,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (plugin.author.isNotEmpty()) {
                        Text(
                            text = "by ${plugin.author}",
                            color = palette.contentLow,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Launch button (only if enabled and requested)
                if (showLaunchButton && plugin.enabled) {
                    LaunchButton(onClick = onLaunch)
                }

                // Toggle switch
                PluginToggle(
                    checked = plugin.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@Composable
private fun LaunchButton(onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isHovered) palette.primary else palette.primary.copy(alpha = 0.8f)
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Launch",
            color = palette.content,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InstallPluginButton(onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isHovered) palette.primary else palette.surfaceLow
    )
    val borderColor by animateColorAsState(
        if (isHovered) palette.primary else palette.surfaceDisabled
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Install Plugin…",
            color = if (isHovered) palette.content else palette.contentTop,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PluginToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            checked -> palette.success
            isHovered -> palette.surfaceTop
            else -> palette.surfaceMid
        }
    )

    val indicatorColor by animateColorAsState(
        if (checked) palette.content else palette.contentLow
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(indicatorColor)
        )
    }
}
